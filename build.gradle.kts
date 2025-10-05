@file:Suppress("UnstableApiUsage")
import java.util.*

plugins {
	id("dev.architectury.loom")
	id("architectury-plugin")
	id("me.modmuss50.mod-publish-plugin")
	id("com.github.johnrengelman.shadow")
}

//replaces buildSrc
data class ModData(private val project: Project) {
	val id: String get() = mod("id")
	val name: String get() = mod("name")
	val version: String get() = mod("version")
	val group: String get() = mod("group")
	val author: String get() = mod("author")
	val description: String get() = mod("description")
	val license: String get() = mod("license")
	val url: String get() = mod("url")

	fun cfg(key: String) = requireNotNull(project.prop("cfg.$key")) { "Missing 'cfg.$key'" }
	fun mod(key: String) = requireNotNull(project.prop("mod.$key")) { "Missing 'mod.$key'" }
	fun dep(key: String) = requireNotNull(project.prop("deps.$key")) { "Missing 'deps.$key'" }
}

val Project.mod: ModData get() = ModData(this)
fun Project.prop(key: String): String? = findProperty(key)?.toString()
fun String.upperCaseFirst() = replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }

fun RepositoryHandler.strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
	forRepository { maven(url) { name = alias } }
	filter { groups.forEach(::includeGroup) }
}

fun ProcessResources.properties(files: Iterable<String>, vararg properties: Pair<String, Any>) {
	for ((name, value) in properties) inputs.property(name, value)
	filesMatching(files) {
		expand(properties.toMap())
	}
}
//why does buildSrc break my runs

val minecraft = stonecutter.current.version
val loader = stonecutter.current.project.split("-")[1]

version = "${mod.version}+$minecraft"
group = mod.group
base {
	archivesName.set("${mod.id}-$loader")
}

architectury.common(loader)

loom.silentMojangMappingsLicense()

stonecutter {
	//loader constants
	constants {
		put("fabric", loader == "fabric")
		put("forge", loader == "forge")
		put("neoforge", loader == "neoforge")
		put("forgelike", loader == "forge" || loader == "neoforge")
	}
	dependencies {
		put("minecraft", minecraft)
		put("mixinextras", mod.dep("mixin_extras"))
		put("forge", mod.dep("forge_version"))
		put("neoforge", mod.dep("neoforge_version"))
		put("fabric", mod.dep("fabric_version"))
		put("fabric-api", mod.dep("fabric-api_version"))
	}
	swaps["clientOnly"] = when (loader) {
		"forge" -> "@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)"
		"neoforge" -> "@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)"
		else -> "@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)"
	}
	swaps["serverOnly"] = when (loader) {
		"forge" -> "@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.SERVER)"
		"neoforge" -> "@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.SERVER)"
		else -> "@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.SERVER)"
	}
}

repositories {
	maven("https://maven.neoforged.net/releases/")
	maven("https://maven.terraformersmc.com/")
	maven("https://maven.nucleoid.xyz/")
	maven("https://maven.parchmentmc.org")
}

dependencies {
	minecraft("com.mojang:minecraft:$minecraft")
	mappings(loom.layered {
		officialMojangMappings()
		parchment("org.parchmentmc.data:parchment-${minecraft}:${mod.dep("parchment_version")}@zip")
	})
	//remove everything after this if you dont want mixin extras
	"io.github.llamalad7:mixinextras-$loader:${mod.dep("mixin_extras")}".let {modApi(it); /*compileOnlyApi(it); annotationProcessor(it);*/ include(it) }
	compileOnlyApi("io.github.llamalad7:mixinextras-common:${mod.dep("mixin_extras")}")//fixes forge not being able to find mixinextras for whatever reason
	if (loader == "fabric") {
		modImplementation("net.fabricmc:fabric-loader:${mod.dep("fabric_version")}")
		modApi("net.fabricmc.fabric-api:fabric-api:${mod.dep("fabric-api_version")}")
	}
	if (loader == "forge") {
		"forge"("net.minecraftforge:forge:${minecraft}-${mod.dep("forge_version")}")
		runtimeOnly("io.github.llamalad7:mixinextras-common:${mod.dep("mixin_extras")}")
	}
	if (loader == "neoforge") {
		"neoForge"("net.neoforged:neoforge:${mod.dep("neoforge_version")}")
	}
}

loom {
	accessWidenerPath = rootProject.file("src/main/resources/${mod.id}.accesswidener")

	decompilers {
		this["vineflower"].apply {
			options.putAll(mapOf(
				"mark-corresponding-synthetics" to "1",
				"keep-literal" to "1",
				"rename-members" to "1"
			))
		}
	}
	if (loader == "forge") {
		forge.convertAccessWideners = true
		forge.mixinConfigs(
			"${mod.id}-common.mixins.json",
			"${mod.id}-forge.mixins.json"
		)
	}
}
afterEvaluate {
	loom.runs.configureEach {
		this.programArgs.addAll(mutableListOf("--username", "Progames723"))
		//the mixin hotswap thingy
		vmArg("-XX:+AllowEnhancedClassRedefinition")
		if (loader == "fabric")
			vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
		property("mixin.hotSwap=true")
	}
}

//yes this file doesnt exist so you'll have to create it
//i might make a workflow for this later
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
	localProperties.load(localPropertiesFile.inputStream())
}

publishMods {
	val modrinthToken = localProperties.getProperty("publish.modrinthToken", "")
	val curseforgeToken = localProperties.getProperty("publish.curseforgeToken", "")

	file = project.tasks.remapJar.get().archiveFile

	displayName = "${mod.name} ${loader.replaceFirstChar { it.uppercase() }} ${property("mod.mc_title")}-${mod.version}"
	version = mod.version
	changelog = rootProject.file("CHANGELOG.md").readText()
	type = BETA//TODO change this

	modLoaders.add(loader)

	val targets = property("mod.mc_targets").toString().split(' ')
	if (modrinthToken != "") {
		modrinth {
			projectId = property("publish.modrinth").toString()
			accessToken = modrinthToken
			targets.forEach(minecraftVersions::add)
			if (loader == "fabric") {
				requires("fabric-api")
			}
		}
	}
	if (curseforgeToken != "") {
		curseforge {
			projectId = property("publish.curseforge").toString()
			accessToken = curseforgeToken.toString()
			targets.forEach(minecraftVersions::add)
			if (loader == "fabric") {
				requires("fabric-api")
			}
		}
	}
}

java {
	withSourcesJar()
	val java = if (stonecutter.eval(minecraft, ">=1.20.5")) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
	targetCompatibility = java
	sourceCompatibility = java
}

val shadowBundle: Configuration by configurations.creating {
	isCanBeConsumed = true
	isCanBeResolved = true
	isTransitive = true
}

tasks.shadowJar {
	configurations = listOf(shadowBundle)
	archiveClassifier = "dev-shadow"
}

tasks.remapJar {
	injectAccessWidener = true
	inputFile = tasks.shadowJar.get().archiveFile
	archiveClassifier = null
	if (loader != "fabric") atAccessWideners.add("${mod.id}.accesswidener")
	dependsOn(tasks.shadowJar)
}

tasks.jar {
	archiveClassifier = "dev"
}

val collect = tasks.register<Copy>("collect") {
	group = "versioned"
	description = "Must run through 'chiseledBuild'"
	from(tasks.remapJar.get().archiveFile, tasks.remapSourcesJar.get().archiveFile)
	into(rootProject.layout.buildDirectory.file("libs/${mod.version}/$loader"))
}

val buildAndCollect = tasks.register<Copy>("buildAndCollect") {
	group = "versioned"
	description = "Must run through 'chiseledBuild'"
	from(tasks.remapJar.get().archiveFile, tasks.remapSourcesJar.get().archiveFile)
	into(rootProject.layout.buildDirectory.file("libs/${mod.version}/$loader"))
	dependsOn("build")
}

if (stonecutter.current.isActive) {
	rootProject.tasks.register("buildActive") {
		group = "project"
		dependsOn(tasks.build)
	}

	rootProject.tasks.register("runActive") {
		group = "project"
		dependsOn(tasks.named("runClient"))
	}
}

tasks.processResources {
	properties(
		listOf("fabric.mod.json"),
		"id" to mod.id,
		"name" to mod.name,
		"version" to mod.version,
		"minecraft" to mod.mod("mc_dep_fabric"),
		"description" to mod.description,
		"author" to mod.author,
		"license" to mod.license,
		"url" to mod.url,
		"fabric_api" to mod.dep("fabric-api_version")
	)
	properties(
		listOf("META-INF/mods.toml", "pack.mcmeta"),
		"id" to mod.id,
		"name" to mod.name,
		"version" to mod.version,
		"description" to mod.description,
		"author" to mod.author,
		"license" to mod.license,
		"url" to mod.url,
		"minecraft" to mod.mod("mc_dep_forgelike")
	)
	properties(
		listOf("META-INF/neoforge.mods.toml", "pack.mcmeta"),
		"id" to mod.id,
		"name" to mod.name,
		"version" to mod.version,
		"description" to mod.description,
		"author" to mod.author,
		"license" to mod.license,
		"url" to mod.url,
		"minecraft" to mod.mod("mc_dep_forgelike")
	)
}
