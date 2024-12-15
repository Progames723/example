package dev.progames723.example.forge;

import dev.progames723.example.Example;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Example.MOD_ID)
public final class ExampleForge {
	public ExampleForge() {
		// Submit our event bus to let Architectury API register our content on the right time.
		EventBuses.registerModEventBus(Example.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		
		// Run our common setup.
		Example.init();
	}
}
