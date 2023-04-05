package com.silentsniperoo.base_assault;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import javax.json.*;
import javax.json.stream.JsonGenerator;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(BaseAssaultMod.MODID)
public class BaseAssaultMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "base_assault";
    // Location of the configuration files for administrators to use
    public static final String SERVER_CONFIG = "config/base_assault.json";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final boolean hasLoaded() { return serverConfig != null; }
    private static JsonObject serverConfig = null;
    private static JsonObject teams = null;
    private static JsonObject games = null;

    public BaseAssaultMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for mod loading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("commonSetup");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("onServerStarting");
        if (!load()) {
        	LOGGER.warn("Missing configuration details in %s".formatted(SERVER_CONFIG));
        }
    }

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
    	if (event.getEntity() instanceof Player) {
    		Player player = (Player)event.getEntity();
    		LOGGER.info("Player of name[%s], display name[%s], and scoreboard name[%s] joined!".formatted(
    				player.getName(),
    				player.getDisplayName(),
    				player.getScoreboardName()));
    	}
    }

    private static boolean load() {
        serverConfig = getServerConfig();
        if (serverConfig.isEmpty()) {
        	return false;
        }
        teams = serverConfig.getJsonObject("teams");
        games = serverConfig.getJsonObject("games");
        if (teams.isEmpty() || games.isEmpty()) {
        	serverConfig = null;
        	teams = null;
        	games = null;
        	return false;
        }
    	return true;
    }

	private static JsonObject getServerConfig() {
		// Get default
		JsonObject serverConfig = Json.createObjectBuilder()
				.add("teams", Json.createObjectBuilder()
						.add("Tower Lords", Json.createObjectBuilder()
								.add("score", 0)
								.add("players", Json.createArrayBuilder()
										.add("TheMaybeMonster")
										.add("TheYesMonster")))
						.add("Tortuga Kings", Json.createObjectBuilder()
								.add("score", 0)
								.add("players", Json.createArrayBuilder()
										.add("TheNoMonster"))))
				.add("games",  Json.createObjectBuilder()
						.add("Tag", Json.createObjectBuilder()
								.add("base_game", "tag")
								.add("time_limit", 30.0) // In seconds
								.add("chasing_players", Json.createArrayBuilder()
										.add("@teams")) // Each team gets a turn as "it"
								.add("fleeing_players", Json.createArrayBuilder()
										.add("*")))
						.add("Marco Polo", Json.createObjectBuilder()
								.add("base_game", "tag")
								.add("time_limit", 180.0)
								.add("chasing_players", Json.createArrayBuilder()
										.add("@nose-goes")) // Last person to sneak on each team
								.add("fleeing_players", Json.createArrayBuilder()
										.add("*"))
								.add("blindness", true)
								// Ring a bell or blow a horn to apply effect for a duration
								.add("spectral", Json.createObjectBuilder()
										.add("duration", 1.0)
										.add("cooldown", 2.0))
								// Avoid spectral effect for a duration by sneaking
								.add("antispectral", Json.createObjectBuilder()
										.add("duration", 3.0)
										.add("cooldown", 4.0)))
						.add("Ender Tag", Json.createObjectBuilder()
								.add("base_game", "tag")
								.add("time_limit", 20.0)
								.add("chasing_players", Json.createArrayBuilder()
										.add("@turns")) // Each player gets a turn to be the seeker for their team 
								.add("fleeing_players", Json.createArrayBuilder()
										.add("*"))
								.add("teleport", 1.0)) // Cooldown of teleport
						.add("Lava Monster", Json.createObjectBuilder()
								.add("base_game", "tag")
								.add("time_limit", 300.0)
								.add("chasing_players", Json.createArrayBuilder()
										.add("@teams"))
								.add("fleeing_players", Json.createArrayBuilder()
										.add("*"))
								.add("lava_blocks", Json.createArrayBuilder()
										.add("lava")
										.add("magma")
										.add("basalt")
										.add("netherack")
										.add("soul_sand")
										.add("smooth_soul_sand")))
						.add("Hunger Games", Json.createObjectBuilder()
								.add("base_game", "pvp")
								.add("time_limit", 600.0)
								.add("tribute_players", Json.createArrayBuilder() // Tributes fight
										.add("@volunteer"))
								.add("sponsor_players", Json.createArrayBuilder() // Sponsors help their team's tributes
										.add("*"))))
				// Dodge Ball
				// Hide and Seek
				.build();
		// Attempt to read non-default
        try {
            JsonReader reader = Json.createReader(new FileReader(SERVER_CONFIG));
            serverConfig = reader.readObject();
            reader.close();
        }
        catch (Exception e) {
        	// Attempt to write default
        	try {
        		HashMap<String, Object> options = new HashMap<>();
        		options.put(JsonGenerator.PRETTY_PRINTING, true);
        		JsonWriterFactory factory = Json.createWriterFactory(options);
				JsonWriter writer = factory.createWriter(new FileWriter(SERVER_CONFIG));
				writer.writeObject(serverConfig);
				writer.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
        }
        return serverConfig;
	}

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("onClientSetup");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
