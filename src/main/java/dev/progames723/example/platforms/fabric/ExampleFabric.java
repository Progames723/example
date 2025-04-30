//? if fabric {
package dev.progames723.example.platforms.fabric;

import dev.progames723.example.ExampleCommon;
import net.fabricmc.api.ModInitializer;

public class ExampleFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		ExampleCommon.init();
	}
}
//?}