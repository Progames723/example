package dev.progames723.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleCommon {
	public static final String MOD_ID = "example";
	public static final Logger LOGGER = LoggerFactory.getLogger("Example");

	public static void init() {
		//? if fabric {
		LOGGER.info("fabric");
		//?} else if forge {
		/*LOGGER.info("forge");
		*///?} else if neoforge {
		/*LOGGER.info("neoforge");
		*///?}
	}
}