package org.bukkit.craftbukkit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.Warning.WarningState;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.conversations.Conversable;
import org.bukkit.craftbukkit.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.help.SimpleHelpMap;
import org.bukkit.craftbukkit.inventory.CraftFurnaceRecipe;
import org.bukkit.craftbukkit.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.inventory.CraftItemFactory;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapedRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapelessRecipe;
import org.bukkit.craftbukkit.inventory.RecipeIterator;
import org.bukkit.craftbukkit.map.CraftMapView;
import org.bukkit.craftbukkit.metadata.EntityMetadataStore;
import org.bukkit.craftbukkit.metadata.PlayerMetadataStore;
import org.bukkit.craftbukkit.metadata.WorldMetadataStore;
import org.bukkit.craftbukkit.potion.CraftPotionBrewer;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager;
import org.bukkit.craftbukkit.updater.AutoUpdater;
import org.bukkit.craftbukkit.updater.BukkitDLUpdaterService;
import org.bukkit.craftbukkit.util.CraftIconCache;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.DatFileFilter;
import org.bukkit.craftbukkit.util.Versioning;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.help.HelpMap;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.SimpleServicesManager;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.bukkit.scheduler.BukkitWorker;
import org.bukkit.util.StringUtil;
import org.bukkit.util.permissions.DefaultPermissions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.apache.commons.lang.Validate;

import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.SQLitePlatform;
import com.avaje.ebeaninternal.server.lib.sql.TransactionIsolation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
// Cauldron start
import org.bukkit.craftbukkit.command.CraftSimpleCommandMap;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.SaveHandler;
import net.minecraftforge.cauldron.CauldronConfig;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
// Cauldron end

import jline.console.ConsoleReader;

public final class CraftServer implements Server {
    private final String serverName = "Cauldron-MCPC-Plus"; // Cauldron - temporarily keep MCPC-Plus name until plugins adapt
    private final String serverVersion;
    private final String bukkitVersion = Versioning.getBukkitVersion();
    private final Logger logger = Logger.getLogger("Minecraft");
    private final ServicesManager servicesManager = new SimpleServicesManager();
    private final CraftScheduler scheduler = new CraftScheduler();
    private final CraftSimpleCommandMap craftCommandMap = new CraftSimpleCommandMap(this); // Cauldron
    private final SimpleCommandMap commandMap = new SimpleCommandMap(this);
    private final SimpleHelpMap helpMap = new SimpleHelpMap(this);
    private final StandardMessenger messenger = new StandardMessenger();
    private final PluginManager pluginManager = new SimplePluginManager(this, commandMap);
    protected final net.minecraft.server.MinecraftServer console;
    protected final net.minecraft.server.dedicated.DedicatedPlayerList playerList;
    private final Map<String, World> worlds = new LinkedHashMap<String, World>();
    public YamlConfiguration configuration = MinecraftServer.configuration; // Cauldron
    private YamlConfiguration commandsConfiguration = MinecraftServer.commandsConfiguration; // Cauldron
    private final Yaml yaml = new Yaml(new SafeConstructor());
    private final Map<String, OfflinePlayer> offlinePlayers = new MapMaker().softValues().makeMap();
    private final AutoUpdater updater;
    private final EntityMetadataStore entityMetadata = new EntityMetadataStore();
    private final PlayerMetadataStore playerMetadata = new PlayerMetadataStore();
    private final WorldMetadataStore worldMetadata = new WorldMetadataStore();
    private int monsterSpawn = -1;
    private int animalSpawn = -1;
    private int waterAnimalSpawn = -1;
    private int ambientSpawn = -1;
    private File container;
    private WarningState warningState = WarningState.DEFAULT;
    private final BooleanWrapper online = new BooleanWrapper();
    public CraftScoreboardManager scoreboardManager;
    public boolean playerCommandState;
    private boolean printSaveWarning;
    private CraftIconCache icon;
    private boolean overrideAllCommandBlockCommands = false;

    private final class BooleanWrapper {
        private boolean value = true;
    }

    static {
        ConfigurationSerialization.registerClass(CraftOfflinePlayer.class);
        CraftItemFactory.instance();
    }

    public CraftServer(net.minecraft.server.MinecraftServer console, net.minecraft.server.management.ServerConfigurationManager playerList) {
        this.console = console;
        this.playerList = (net.minecraft.server.dedicated.DedicatedPlayerList) playerList;
        this.serverVersion = CraftServer.class.getPackage().getImplementationVersion();
        online.value = console.getPropertyManager().getBooleanProperty("online-mode", true);

        Bukkit.setServer(this);

        // Register all the Enchantments and PotionTypes now so we can stop new registration immediately after
        net.minecraft.enchantment.Enchantment.sharpness.getClass();
        //org.bukkit.enchantments.Enchantment.stopAcceptingRegistrations(); // Cauldron - allow registrations

        Potion.setPotionBrewer(new CraftPotionBrewer());
        net.minecraft.potion.Potion.blindness.getClass();
        //PotionEffectType.stopAcceptingRegistrations(); // Cauldron - allow registrations
        // Ugly hack :(

        if (!MinecraftServer.useConsole) { // Cauldron
            getLogger().info("Console input is disabled due to --noconsole command argument");
        }

        /* Cauldron start - moved to MinecraftServer so FML/Forge can access during server startup
        configuration = YamlConfiguration.loadConfiguration(getConfigFile());        
        configuration.options().copyDefaults(true);
        configuration.setDefaults(YamlConfiguration.loadConfiguration(getClass().getClassLoader().getResourceAsStream("configurations/bukkit.yml")));
        ConfigurationSection legacyAlias = null;
        if (!configuration.isString("aliases")) {
            legacyAlias = configuration.getConfigurationSection("aliases");
            configuration.set("aliases", "now-in-commands.yml");
        }
        saveConfig();
        if (getCommandsConfigFile().isFile()) {
            legacyAlias = null;
        }
        commandsConfiguration = YamlConfiguration.loadConfiguration(getCommandsConfigFile());
        commandsConfiguration.options().copyDefaults(true);
        commandsConfiguration.setDefaults(YamlConfiguration.loadConfiguration(getClass().getClassLoader().getResourceAsStream("configurations/commands.yml")));
        saveCommandsConfig();

        // Migrate aliases from old file and add previously implicit $1- to pass all arguments
        if (legacyAlias != null) {
            ConfigurationSection aliases = commandsConfiguration.createSection("aliases");
            for (String key : legacyAlias.getKeys(false)) {
                ArrayList<String> commands = new ArrayList<String>();

                if (legacyAlias.isList(key)) {
                    for (String command : legacyAlias.getStringList(key)) {
                        commands.add(command + " $1-");
                    }
                } else {
                    commands.add(legacyAlias.getString(key) + " $1-");
                }

                aliases.set(key, commands);
            }
        }

        saveCommandsConfig();
        // Cauldron end */
        overrideAllCommandBlockCommands = commandsConfiguration.getStringList("command-block-overrides").contains("*");
        ((SimplePluginManager) pluginManager).useTimings(configuration.getBoolean("settings.plugin-profiling"));
        monsterSpawn = configuration.getInt("spawn-limits.monsters");
        animalSpawn = configuration.getInt("spawn-limits.animals");
        waterAnimalSpawn = configuration.getInt("spawn-limits.water-animals");
        ambientSpawn = configuration.getInt("spawn-limits.ambient");
        console.autosavePeriod = configuration.getInt("ticks-per.autosave");
        warningState = WarningState.value(configuration.getString("settings.deprecated-verbose"));
        loadIcon();
        net.minecraftforge.cauldron.CauldronConfig.init();

        updater = new AutoUpdater(new BukkitDLUpdaterService(configuration.getString("auto-updater.host")), getLogger(), configuration.getString("auto-updater.preferred-channel"));
        updater.setEnabled(false); // Spigot
        updater.setSuggestChannels(configuration.getBoolean("auto-updater.suggest-channels"));
        updater.getOnBroken().addAll(configuration.getStringList("auto-updater.on-broken"));
        updater.getOnUpdate().addAll(configuration.getStringList("auto-updater.on-update"));
        updater.check(serverVersion);

        // Spigot Start - Moved to old location of new DedicatedPlayerList in DedicatedServer
        // loadPlugins();
        // enablePlugins(PluginLoadOrder.STARTUP);
        // Spigot End
    }

    public boolean getCommandBlockOverride(String command) {
        return overrideAllCommandBlockCommands || commandsConfiguration.getStringList("command-block-overrides").contains(command);
    }

    private File getConfigFile() {
        return (File) console.options.valueOf("bukkit-settings");
    }

    private File getCommandsConfigFile() {
        return (File) console.options.valueOf("commands-settings");
    }

    private void saveConfig() {
        try {
            configuration.save(getConfigFile());
        } catch (IOException ex) {
            Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, "Could not save " + getConfigFile(), ex);
        }
    }

    private void saveCommandsConfig() {
        try {
            commandsConfiguration.save(getCommandsConfigFile());
        } catch (IOException ex) {
            Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, "Could not save " + getCommandsConfigFile(), ex);
        }
    }

    public void loadPlugins() {
        pluginManager.registerInterface(JavaPluginLoader.class);

        File pluginFolder = (File) console.options.valueOf("plugins");

        if (pluginFolder.exists()) {
            Plugin[] plugins = pluginManager.loadPlugins(pluginFolder);
            for (Plugin plugin : plugins) {
                try {
                    String message = String.format("Loading %s", plugin.getDescription().getFullName());
                    plugin.getLogger().info(message);
                    plugin.onLoad();
                } catch (Throwable ex) {
                    Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, ex.getMessage() + " initializing " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
                }
            }
        } else {
            pluginFolder.mkdir();
        }
    }

    public void enablePlugins(PluginLoadOrder type) {
        // Cauldron start - initialize mod wrappers
        org.bukkit.craftbukkit.block.CraftBlock.initMappings();
        org.bukkit.craftbukkit.entity.CraftEntity.initMappings();
        // Cauldron end
        if (type == PluginLoadOrder.STARTUP) {
            helpMap.clear();
            helpMap.initializeGeneralTopics();
        }

        Plugin[] plugins = pluginManager.getPlugins();

        for (Plugin plugin : plugins) {
            if ((!plugin.isEnabled()) && (plugin.getDescription().getLoad() == type)) {
                loadPlugin(plugin);
            }
        }

        if (type == PluginLoadOrder.POSTWORLD) {
            commandMap.setFallbackCommands();
            setVanillaCommands();
            commandMap.registerServerAliases();
            loadCustomPermissions();
            DefaultPermissions.registerCorePermissions();
            helpMap.initializeCommands();
        }
    }

    public void disablePlugins() {
        pluginManager.disablePlugins();
    }

    private void setVanillaCommands() {
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandAchievement(), "/achievement give <stat_name> [player]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandBanPlayer(), "/ban <playername> [reason]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandBanIp(), "/ban-ip <ip-address|playername>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandListBans(), "/banlist [ips]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandClearInventory(), "/clear <playername> [item] [metadata]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandDefaultGameMode(), "/defaultgamemode <mode>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandDeOp(), "/deop <playername>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandDifficulty(), "/difficulty <new difficulty>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandEffect(), "/effect <player> <effect|clear> [seconds] [amplifier]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandEnchant(), "/enchant <playername> <enchantment ID> [enchantment level]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandGameMode(), "/gamemode <mode> [player]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandGameRule(), "/gamerule <rulename> [true|false]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandGive(), "/give <playername> <item> [amount] [metadata] [dataTag]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandHelp(), "/help [page|commandname]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandSetPlayerTimeout(), "/setidletimeout <Minutes until kick>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandServerKick(), "/kick <playername> [reason]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandKill(), "/kill [playername]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandListPlayers(), "/list"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandEmote(), "/me <actiontext>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandOp(), "/op <playername>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandPardonPlayer(), "/pardon <playername>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandPardonIp(), "/pardon-ip <ip-address>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandPlaySound(), "/playsound <sound> <playername> [x] [y] [z] [volume] [pitch] [minimumVolume]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandBroadcast(), "/say <message>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandScoreboard(), "/scoreboard"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandShowSeed(), "/seed"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandSetBlock(), "/setblock <x> <y> <z> <tilename> [datavalue] [oldblockHandling] [dataTag]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandSetDefaultSpawnpoint(), "/setworldspawn [x] [y] [z]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandSetSpawnpoint(), "/spawnpoint <playername> [x] [y] [z]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandSpreadPlayers(), "/spreadplayers <x> <z> [spreadDistance] [maxRange] [respectTeams] <playernames>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandSummon(), "/summon <EntityName> [x] [y] [z] [dataTag]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandTeleport(), "/tp [player] <target>\n/tp [player] <x> <y> <z>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandMessage(), "/tell <playername> <message>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandMessageRaw(), "/tellraw <playername> <raw message>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandTestFor(), "/testfor <playername | selector> [dataTag]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandTestForBlock(), "/testforblock <x> <y> <z> <tilename> [datavalue] [dataTag]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandTime(), "/time set <value>\n/time add <value>"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandToggleDownfall(), "/toggledownfall"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandWeather(), "/weather <clear/rain/thunder> [duration in seconds]"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.server.CommandWhitelist(), "/whitelist (add|remove) <player>\n/whitelist (on|off|list|reload)"));
        commandMap.register("minecraft", new VanillaCommandWrapper(new net.minecraft.command.CommandXP(), "/xp <amount> [player]\n/xp <amount>L [player]"));
    }

    private void loadPlugin(Plugin plugin) {
        try {
            pluginManager.enablePlugin(plugin);

            List<Permission> perms = plugin.getDescription().getPermissions();

            for (Permission perm : perms) {
                try {
                    pluginManager.addPermission(perm);
                } catch (IllegalArgumentException ex) {
                    getLogger().log(Level.WARNING, "Plugin " + plugin.getDescription().getFullName() + " tried to register permission '" + perm.getName() + "' but it's already registered", ex);
                }
            }
        } catch (Throwable ex) {
            Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, ex.getMessage() + " loading " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
        }
    }

    public String getName() {
        return serverName;
    }

    public String getVersion() {
        return serverVersion + " (MC: " + console.getMinecraftVersion() + ")";
    }

    public String getBukkitVersion() {
        return bukkitVersion;
    }

    @SuppressWarnings("unchecked")
    public Player[] getOnlinePlayers() {
        List<net.minecraft.entity.player.EntityPlayerMP> online = playerList.playerEntityList;
        Player[] players = new Player[online.size()];

        for (int i = 0; i < players.length; i++) {
            players[i] = online.get(i).playerNetServerHandler.getPlayerB(); // Cauldron
        }

        return players;
    }

    public Player getPlayer(final String name) {
        Validate.notNull(name, "Name cannot be null");

        Player[] players = getOnlinePlayers();

        Player found = null;
        String lowerName = name.toLowerCase();
        int delta = Integer.MAX_VALUE;
        for (Player player : players) {
            if (player.getName().toLowerCase().startsWith(lowerName)) {
                int curDelta = player.getName().length() - lowerName.length();
                if (curDelta < delta) {
                    found = player;
                    delta = curDelta;
                }
                if (curDelta == 0) break;
            }
        }
        return found;
    }

    public Player getPlayerExact(String name) {
        Validate.notNull(name, "Name cannot be null");

        String lname = name.toLowerCase();

        for (Player player : getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(lname)) {
                return player;
            }
        }

        return null;
    }

    public int broadcastMessage(String message) {
        return broadcast(message, BROADCAST_CHANNEL_USERS);
    }

    public Player getPlayer(final net.minecraft.entity.player.EntityPlayerMP entity) {
        return entity.playerNetServerHandler.getPlayerB();
    }

    public List<Player> matchPlayer(String partialName) {
        Validate.notNull(partialName, "PartialName cannot be null");

        List<Player> matchedPlayers = new ArrayList<Player>();

        for (Player iterPlayer : this.getOnlinePlayers()) {
            String iterPlayerName = iterPlayer.getName();

            if (partialName.equalsIgnoreCase(iterPlayerName)) {
                // Exact match
                matchedPlayers.clear();
                matchedPlayers.add(iterPlayer);
                break;
            }
            if (iterPlayerName.toLowerCase().contains(partialName.toLowerCase())) {
                // Partial match
                matchedPlayers.add(iterPlayer);
            }
        }

        return matchedPlayers;
    }

    public int getMaxPlayers() {
        return playerList.getMaxPlayers();
    }

    // NOTE: These are dependent on the corrisponding call in MinecraftServer
    // so if that changes this will need to as well
    public int getPort() {
        return this.getConfigInt("server-port", 25565);
    }

    public int getViewDistance() {
        return this.getConfigInt("view-distance", 10);
    }

    public String getIp() {
        return this.getConfigString("server-ip", "");
    }

    public String getServerName() {
        return this.getConfigString("server-name", "Unknown Server");
    }

    public String getServerId() {
        return this.getConfigString("server-id", "unnamed");
    }

    public String getWorldType() {
        return this.getConfigString("level-type", "DEFAULT");
    }

    public boolean getGenerateStructures() {
        return this.getConfigBoolean("generate-structures", true);
    }

    public boolean getAllowEnd() {
        return this.configuration.getBoolean("settings.allow-end");
    }

    public boolean getAllowNether() {
        return this.getConfigBoolean("allow-nether", true);
    }

    public boolean getWarnOnOverload() {
        return this.configuration.getBoolean("settings.warn-on-overload");
    }

    public boolean getQueryPlugins() {
        return this.configuration.getBoolean("settings.query-plugins");
    }

    public boolean hasWhitelist() {
        return this.getConfigBoolean("white-list", false);
    }

    // NOTE: Temporary calls through to server.properies until its replaced
    private String getConfigString(String variable, String defaultValue) {
        return this.console.getPropertyManager().getStringProperty(variable, defaultValue);
    }

    private int getConfigInt(String variable, int defaultValue) {
        return this.console.getPropertyManager().getIntProperty(variable, defaultValue);
    }

    private boolean getConfigBoolean(String variable, boolean defaultValue) {
        return this.console.getPropertyManager().getBooleanProperty(variable, defaultValue);
    }

    // End Temporary calls

    public String getUpdateFolder() {
        return this.configuration.getString("settings.update-folder", "update");
    }

    public File getUpdateFolderFile() {
        return new File((File) console.options.valueOf("plugins"), this.configuration.getString("settings.update-folder", "update"));
    }

    public int getPingPacketLimit() {
        return this.configuration.getInt("settings.ping-packet-limit", 100);
    }

    public long getConnectionThrottle() {
        return this.configuration.getInt("settings.connection-throttle");
    }

    public int getTicksPerAnimalSpawns() {
        return this.configuration.getInt("ticks-per.animal-spawns");
    }

    public int getTicksPerMonsterSpawns() {
        return this.configuration.getInt("ticks-per.monster-spawns");
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public CraftScheduler getScheduler() {
        return scheduler;
    }

    public ServicesManager getServicesManager() {
        return servicesManager;
    }

    public List<World> getWorlds() {
        return new ArrayList<World>(worlds.values());
    }

    public net.minecraft.server.dedicated.DedicatedPlayerList getHandle() {
        return playerList;
    }

    // NOTE: Should only be called from DedicatedServer.ah()
    public boolean dispatchServerCommand(CommandSender sender, net.minecraft.command.ServerCommand serverCommand) {
        if (sender instanceof Conversable) {
            Conversable conversable = (Conversable)sender;

            if (conversable.isConversing()) {
                conversable.acceptConversationInput(serverCommand.command);
                return true;
            }
        }
        try {
            this.playerCommandState = true;
            // Cauldron start - handle bukkit/vanilla console commands
            int space = serverCommand.command.indexOf(" ");
            // if bukkit command exists then execute it over vanilla
            if (this.getCommandMap().getCommand(serverCommand.command.substring(0, space != -1 ? space : serverCommand.command.length())) != null)
            {
                return this.dispatchCommand(sender, serverCommand.command);
            }
            else { // process vanilla console command
                craftCommandMap.setVanillaConsoleSender(serverCommand.sender);
                return this.dispatchVanillaCommand(sender, serverCommand.command);
            }
            // Cauldron end
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Unexpected exception while parsing console command \"" + serverCommand.command + '"', ex);
            return false;
        } finally {
            this.playerCommandState = false;
        }
    }

    public boolean dispatchCommand(CommandSender sender, String commandLine) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(commandLine, "CommandLine cannot be null");

        if (commandMap.dispatch(sender, commandLine)) {
            return true;
        }

        // Cauldron start - handle vanilla commands called from plugins
        if(sender instanceof ConsoleCommandSender) {
            craftCommandMap.setVanillaConsoleSender(this.console);
        }
            
        return this.dispatchVanillaCommand(sender, commandLine);
        // Cauldron end
    }
    
    // Cauldron start
    // used to process vanilla commands
    public boolean dispatchVanillaCommand(CommandSender sender, String commandLine) {
        if (craftCommandMap.dispatch(sender, commandLine)) {
            return true;
        }

        sender.sendMessage(org.spigotmc.SpigotConfig.unknownCommandMessage); // Spigot

        return false;
    }

    public String getBukkitToForgeMapping(String name)
    {
        String result = CauldronConfig.getString("bukkit-to-forge-mappings." + name, name, false);
        return result;
    }
    // Cauldron end    

    public void reload() {
        configuration = YamlConfiguration.loadConfiguration(getConfigFile());
        commandsConfiguration = YamlConfiguration.loadConfiguration(getCommandsConfigFile());
        net.minecraft.server.dedicated.PropertyManager config = new net.minecraft.server.dedicated.PropertyManager(console.options);

        ((net.minecraft.server.dedicated.DedicatedServer) console).settings = config;

        boolean animals = config.getBooleanProperty("spawn-animals", console.getCanSpawnAnimals());
        boolean monsters = config.getBooleanProperty("spawn-monsters", console.worlds.get(0).difficultySetting != net.minecraft.world.EnumDifficulty.PEACEFUL);
        net.minecraft.world.EnumDifficulty difficulty = net.minecraft.world.EnumDifficulty.getDifficultyEnum(config.getIntProperty("difficulty", console.worlds.get(0).difficultySetting.ordinal()));

        online.value = config.getBooleanProperty("online-mode", console.isServerInOnlineMode());
        console.setCanSpawnAnimals(config.getBooleanProperty("spawn-animals", console.getCanSpawnAnimals()));
        console.setAllowPvp(config.getBooleanProperty("pvp", console.isPVPEnabled()));
        console.setAllowFlight(config.getBooleanProperty("allow-flight", console.isFlightAllowed()));
        console.setMOTD(config.getStringProperty("motd", console.getMOTD()));
        monsterSpawn = configuration.getInt("spawn-limits.monsters");
        animalSpawn = configuration.getInt("spawn-limits.animals");
        waterAnimalSpawn = configuration.getInt("spawn-limits.water-animals");
        ambientSpawn = configuration.getInt("spawn-limits.ambient");
        warningState = WarningState.value(configuration.getString("settings.deprecated-verbose"));
        printSaveWarning = false;
        console.autosavePeriod = configuration.getInt("ticks-per.autosave");
        loadIcon();

        playerList.getBannedIPs().loadBanList();
        playerList.getBannedPlayers().loadBanList();

        org.spigotmc.SpigotConfig.init(); // Spigot
        net.minecraftforge.cauldron.CauldronConfig.init(); // Cauldron
        for (net.minecraft.world.WorldServer world : console.worlds) {
            world.difficultySetting = difficulty;
            world.setAllowedSpawnTypes(monsters, animals);
            if (this.getTicksPerAnimalSpawns() < 0) {
                world.ticksPerAnimalSpawns = 400;
            } else {
                world.ticksPerAnimalSpawns = this.getTicksPerAnimalSpawns();
            }

            if (this.getTicksPerMonsterSpawns() < 0) {
                world.ticksPerMonsterSpawns = 1;
            } else {
                world.ticksPerMonsterSpawns = this.getTicksPerMonsterSpawns();
            }
            world.spigotConfig.init(); // Spigot
        }

        pluginManager.clearPlugins();
        commandMap.clearCommands();
        resetRecipes();
        overrideAllCommandBlockCommands = commandsConfiguration.getStringList("command-block-overrides").contains("*");
        org.spigotmc.SpigotConfig.registerCommands(); // Spigot

        int pollCount = 0;

        // Wait for at most 2.5 seconds for plugins to close their threads
        while (pollCount < 50 && getScheduler().getActiveWorkers().size() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {}
            pollCount++;
        }

        List<BukkitWorker> overdueWorkers = getScheduler().getActiveWorkers();
        for (BukkitWorker worker : overdueWorkers) {
            Plugin plugin = worker.getOwner();
            String author = "<NoAuthorGiven>";
            if (plugin.getDescription().getAuthors().size() > 0) {
                author = plugin.getDescription().getAuthors().get(0);
            }
            getLogger().log(Level.SEVERE, String.format(
                "Nag author: '%s' of '%s' about the following: %s",
                author,
                plugin.getDescription().getName(),
                "This plugin is not properly shutting down its async tasks when it is being reloaded.  This may cause conflicts with the newly loaded version of the plugin"
            ));
        }
 
        loadPlugins();
        enablePlugins(PluginLoadOrder.STARTUP);
        enablePlugins(PluginLoadOrder.POSTWORLD);
    }

    private void loadIcon() {
        icon = new CraftIconCache(null);
        try {
            final File file = new File(new File("."), "server-icon.png");
            if (file.isFile()) {
                icon = loadServerIcon0(file);
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Couldn't load server icon", ex);
        }
    }

    @SuppressWarnings({ "unchecked", "finally" })
    private void loadCustomPermissions() {
        File file = new File(configuration.getString("settings.permissions-file"));
        FileInputStream stream;

        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            try {
                file.createNewFile();
            } finally {
                return;
            }
        }

        Map<String, Map<String, Object>> perms;

        try {
            perms = (Map<String, Map<String, Object>>) yaml.load(stream);
        } catch (MarkedYAMLException ex) {
            getLogger().log(Level.WARNING, "Server permissions file " + file + " is not valid YAML: " + ex.toString());
            return;
        } catch (Throwable ex) {
            getLogger().log(Level.WARNING, "Server permissions file " + file + " is not valid YAML.", ex);
            return;
        } finally {
            try {
                stream.close();
            } catch (IOException ex) {}
        }

        if (perms == null) {
            getLogger().log(Level.INFO, "Server permissions file " + file + " is empty, ignoring it");
            return;
        }

        List<Permission> permsList = Permission.loadPermissions(perms, "Permission node '%s' in " + file + " is invalid", Permission.DEFAULT_PERMISSION);

        for (Permission perm : permsList) {
            try {
                pluginManager.addPermission(perm);
            } catch (IllegalArgumentException ex) {
                getLogger().log(Level.SEVERE, "Permission in " + file + " was already defined", ex);
            }
        }
    }

    @Override
    public String toString() {
        return "CraftServer{" + "serverName=" + serverName + ",serverVersion=" + serverVersion + ",minecraftVersion=" + console.getMinecraftVersion() + '}';
    }

    public World createWorld(String name, World.Environment environment) {
        return WorldCreator.name(name).environment(environment).createWorld();
    }

    public World createWorld(String name, World.Environment environment, long seed) {
        return WorldCreator.name(name).environment(environment).seed(seed).createWorld();
    }

    public World createWorld(String name, Environment environment, ChunkGenerator generator) {
        return WorldCreator.name(name).environment(environment).generator(generator).createWorld();
    }

    public World createWorld(String name, Environment environment, long seed, ChunkGenerator generator) {
        return WorldCreator.name(name).environment(environment).seed(seed).generator(generator).createWorld();
    }

    public World createWorld(WorldCreator creator) {
        Validate.notNull(creator, "Creator may not be null");

        String name = creator.name();
        ChunkGenerator generator = creator.generator();
        File folder = new File(getWorldContainer(), name);
        World world = getWorld(name);
        net.minecraft.world.WorldType type = net.minecraft.world.WorldType.parseWorldType(creator.type().getName());
        boolean generateStructures = creator.generateStructures();

        if ((folder.exists()) && (!folder.isDirectory())) {
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
        }

        if (world != null) {
            return world;
        }

        boolean hardcore = false;
        WorldSettings worldSettings = new WorldSettings(creator.seed(), net.minecraft.world.WorldSettings.GameType.getByID(getDefaultGameMode().getValue()), generateStructures, hardcore, type);
        net.minecraft.world.WorldServer worldserver = DimensionManager.initDimension(creator, worldSettings);

        pluginManager.callEvent(new WorldInitEvent(worldserver.getWorld()));
        net.minecraftforge.cauldron.CauldronHooks.craftWorldLoading = true;
        System.out.print("Preparing start region for level " + (console.worlds.size() - 1) + " (Dimension: " + worldserver.provider.dimensionId + ", Seed: " + worldserver.getSeed() + ")"); // Cauldron - log dimension

        if (worldserver.getWorld().getKeepSpawnInMemory()) {
            short short1 = 196;
            long i = System.currentTimeMillis();
            for (int j = -short1; j <= short1; j += 16) {
                for (int k = -short1; k <= short1; k += 16) {
                    long l = System.currentTimeMillis();

                    if (l < i) {
                        i = l;
                    }

                    if (l > i + 1000L) {
                        int i1 = (short1 * 2 + 1) * (short1 * 2 + 1);
                        int j1 = (j + short1) * (short1 * 2 + 1) + k + 1;

                        System.out.println("Preparing spawn area for " + worldserver.getWorld().getName() + ", " + (j1 * 100 / i1) + "%");
                        i = l;
                    }

                    net.minecraft.util.ChunkCoordinates chunkcoordinates = worldserver.getSpawnPoint();
                    worldserver.theChunkProviderServer.loadChunk(chunkcoordinates.posX + j >> 4, chunkcoordinates.posZ + k >> 4);
                }
            }
        }
        pluginManager.callEvent(new WorldLoadEvent(worldserver.getWorld()));
        net.minecraftforge.cauldron.CauldronHooks.craftWorldLoading = false;
        return worldserver.getWorld();
    }

    public boolean unloadWorld(String name, boolean save) {
        return unloadWorld(getWorld(name), save);
    }

    public boolean unloadWorld(World world, boolean save) {
        if (world == null) {
            return false;
        }

        net.minecraft.world.WorldServer handle = ((CraftWorld) world).getHandle();

        if (!(console.worlds.contains(handle))) {
            return false;
        }

        if (handle.playerEntities.size() > 0) {
            return false;
        }

        WorldUnloadEvent e = new WorldUnloadEvent(handle.getWorld());
        pluginManager.callEvent(e);

        if (e.isCancelled()) {
            return false;
        }

        if (save) {
            try {
                handle.saveAllChunks(true, null);
                handle.flush();
                WorldSaveEvent event = new WorldSaveEvent(handle.getWorld());
                getPluginManager().callEvent(event);
            } catch (net.minecraft.world.MinecraftException ex) {
                getLogger().log(Level.SEVERE, null, ex);
                FMLLog.log(org.apache.logging.log4j.Level.ERROR, ex, "Failed to save world " + handle.getWorld().getName() + " while unloading it.");
            }
        }
        MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(handle)); // Cauldron - fire unload event before removing world
        worlds.remove(world.getName().toLowerCase());
        DimensionManager.setWorld(handle.provider.dimensionId, null); // Cauldron - remove world from DimensionManager
        return true;
    }

    public net.minecraft.server.MinecraftServer getServer() {
        return console;
    }

    public World getWorld(String name) {
        Validate.notNull(name, "Name cannot be null");

        return worlds.get(name.toLowerCase());
    }

    public World getWorld(UUID uid) {
        for (World world : worlds.values()) {
            if (world.getUID().equals(uid)) {
                return world;
            }
        }
        return null;
    }

    public void addWorld(World world) {
        // Check if a World already exists with the UID.
        if (getWorld(world.getUID()) != null) {
            System.out.println("World " + world.getName() + " is a duplicate of another world and has been prevented from loading. Please delete the uid.dat file from " + world.getName() + "'s world directory if you want to be able to load the duplicate world.");
            return;
        }
        worlds.put(world.getName().toLowerCase(), world);
    }

    public Logger getLogger() {
        return logger;
    }

    public ConsoleReader getReader() {
        return console.reader;
    }

    public PluginCommand getPluginCommand(String name) {
        Command command = commandMap.getCommand(name);

        if (command instanceof PluginCommand) {
            return (PluginCommand) command;
        } else {
            return null;
        }
    }

    public void savePlayers() {
        checkSaveState();
        playerList.saveAllPlayerData();
    }

    public void configureDbConfig(ServerConfig config) {
        Validate.notNull(config, "Config cannot be null");

        DataSourceConfig ds = new DataSourceConfig();
        ds.setDriver(configuration.getString("database.driver"));
        ds.setUrl(configuration.getString("database.url"));
        ds.setUsername(configuration.getString("database.username"));
        ds.setPassword(configuration.getString("database.password"));
        ds.setIsolationLevel(TransactionIsolation.getLevel(configuration.getString("database.isolation")));

        if (ds.getDriver().contains("sqlite")) {
            config.setDatabasePlatform(new SQLitePlatform());
            config.getDatabasePlatform().getDbDdlSyntax().setIdentity("");
        }

        config.setDataSourceConfig(ds);
    }

    public boolean addRecipe(Recipe recipe) {
        CraftRecipe toAdd;
        if (recipe instanceof CraftRecipe) {
            toAdd = (CraftRecipe) recipe;
        } else {
            if (recipe instanceof ShapedRecipe) {
                toAdd = CraftShapedRecipe.fromBukkitRecipe((ShapedRecipe) recipe);
            } else if (recipe instanceof ShapelessRecipe) {
                toAdd = CraftShapelessRecipe.fromBukkitRecipe((ShapelessRecipe) recipe);
            } else if (recipe instanceof FurnaceRecipe) {
                toAdd = CraftFurnaceRecipe.fromBukkitRecipe((FurnaceRecipe) recipe);
            } else {
                return false;
            }
        }
        toAdd.addToCraftingManager();
        //net.minecraft.item.crafting.CraftingManager.getInstance().sort(); // Cauldron - mod recipes not necessarily sortable
        return true;
    }

    public List<Recipe> getRecipesFor(ItemStack result) {
        Validate.notNull(result, "Result cannot be null");

        List<Recipe> results = new ArrayList<Recipe>();
        Iterator<Recipe> iter = recipeIterator();
        while (iter.hasNext()) {
            Recipe recipe = iter.next();
            ItemStack stack = recipe.getResult();
            if (stack.getType() != result.getType()) {
                continue;
            }
            if (result.getDurability() == -1 || result.getDurability() == stack.getDurability()) {
                results.add(recipe);
            }
        }
        return results;
    }

    public Iterator<Recipe> recipeIterator() {
        return new RecipeIterator();
    }

    public void clearRecipes() {
        net.minecraft.item.crafting.CraftingManager.getInstance().recipes.clear();
        net.minecraft.item.crafting.FurnaceRecipes.smelting().smeltingList.clear();
        net.minecraft.item.crafting.FurnaceRecipes.smelting().customRecipes.clear();
    }

    public void resetRecipes() {
        net.minecraft.item.crafting.CraftingManager.getInstance().recipes = new net.minecraft.item.crafting.CraftingManager().recipes;
        net.minecraft.item.crafting.FurnaceRecipes.smelting().smeltingList = new net.minecraft.item.crafting.FurnaceRecipes().smeltingList;
        net.minecraft.item.crafting.FurnaceRecipes.smelting().customRecipes.clear();
    }

    public Map<String, String[]> getCommandAliases() {
        ConfigurationSection section = configuration.getConfigurationSection("aliases");
        Map<String, String[]> result = new LinkedHashMap<String, String[]>();

        if (section != null) {
            for (String key : section.getKeys(false)) {
                List<String> commands;

                if (section.isList(key)) {
                    commands = section.getStringList(key);
                } else {
                    commands = ImmutableList.of(section.getString(key));
                }

                result.put(key, commands.toArray(new String[commands.size()]));
            }
        }

        return result;
    }

    public void removeBukkitSpawnRadius() {
        configuration.set("settings.spawn-radius", null);
        saveConfig();
    }

    public int getBukkitSpawnRadius() {
        return configuration.getInt("settings.spawn-radius", -1);
    }

    public String getShutdownMessage() {
        return configuration.getString("settings.shutdown-message");
    }

    public int getSpawnRadius() {
        return ((net.minecraft.server.dedicated.DedicatedServer) console).settings.getIntProperty("spawn-protection", 16);
    }

    public void setSpawnRadius(int value) {
        configuration.set("settings.spawn-radius", value);
        saveConfig();
    }

    public boolean getOnlineMode() {
        return online.value;
    }

    public boolean getAllowFlight() {
        return console.isFlightAllowed();
    }

    public boolean isHardcore() {
        return console.isHardcore();
    }

    public boolean useExactLoginLocation() {
        return configuration.getBoolean("settings.use-exact-login-location");
    }

    public ChunkGenerator getGenerator(String world) {
        ConfigurationSection section = configuration.getConfigurationSection("worlds");
        ChunkGenerator result = null;

        if (section != null) {
            section = section.getConfigurationSection(world);

            if (section != null) {
                String name = section.getString("generator");

                if ((name != null) && (!name.equals(""))) {
                    String[] split = name.split(":", 2);
                    String id = (split.length > 1) ? split[1] : null;
                    Plugin plugin = pluginManager.getPlugin(split[0]);

                    if (plugin == null) {
                        getLogger().severe("Could not set generator for default world '" + world + "': Plugin '" + split[0] + "' does not exist");
                    } else if (!plugin.isEnabled()) {
                        getLogger().severe("Could not set generator for default world '" + world + "': Plugin '" + plugin.getDescription().getFullName() + "' is not enabled yet (is it load:STARTUP?)");
                    } else {
                        try {
                            result = plugin.getDefaultWorldGenerator(world, id);
                            if (result == null) {
                                getLogger().severe("Could not set generator for default world '" + world + "': Plugin '" + plugin.getDescription().getFullName() + "' lacks a default world generator");
                            }
                        } catch (Throwable t) {
                            plugin.getLogger().log(Level.SEVERE, "Could not set generator for default world '" + world + "': Plugin '" + plugin.getDescription().getFullName(), t);
                        }
                    }
                }
            }
        }

        return result;
    }

    public CraftMapView getMap(short id) {
        net.minecraft.world.storage.MapStorage collection = console.worlds.get(0).mapStorage;
        net.minecraft.world.storage.MapData worldmap = (net.minecraft.world.storage.MapData) collection.loadData(net.minecraft.world.storage.MapData.class, "map_" + id);
        if (worldmap == null) {
            return null;
        }
        return worldmap.mapView;
    }

    public CraftMapView createMap(World world) {
        Validate.notNull(world, "World cannot be null");

        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(net.minecraft.init.Items.filled_map, 1, -1);
        net.minecraft.world.storage.MapData worldmap = net.minecraft.init.Items.filled_map.getMapData(stack, ((CraftWorld) world).getHandle());
        return worldmap.mapView;
    }

    public void shutdown() {
        console.initiateShutdown();
    }

    public int broadcast(String message, String permission) {
        int count = 0;
        Set<Permissible> permissibles = getPluginManager().getPermissionSubscriptions(permission);

        for (Permissible permissible : permissibles) {
            if (permissible instanceof CommandSender && permissible.hasPermission(permission)) {
                CommandSender user = (CommandSender) permissible;
                user.sendMessage(message);
                count++;
            }
        }

        return count;
    }

    public OfflinePlayer getOfflinePlayer(String name) {
        return getOfflinePlayer(name, false); // Spigot
    }

    public OfflinePlayer getOfflinePlayer(String name, boolean search) {
        Validate.notNull(name, "Name cannot be null");

        OfflinePlayer result = getPlayerExact(name);
        String lname = name.toLowerCase();

        if (result == null) {
            result = offlinePlayers.get(lname);

            if (result == null) {
                if (search) {
                    net.minecraft.world.storage.SaveHandler storage = (net.minecraft.world.storage.SaveHandler) console.worlds.get(0).getSaveHandler();
                    for (String dat : storage.getPlayerDir().list(new DatFileFilter())) {
                        String datName = dat.substring(0, dat.length() - 4);
                        if (datName.equalsIgnoreCase(name)) {
                            name = datName;
                            break;
                        }
                    }
                }

                result = new CraftOfflinePlayer(this, name);
                offlinePlayers.put(lname, result);
            }
        } else {
            offlinePlayers.remove(lname);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getIPBans() {
        return new HashSet<String>(playerList.getBannedIPs().getBannedList().keySet());
    }

    public void banIP(String address) {
        Validate.notNull(address, "Address cannot be null.");

        this.getBanList(org.bukkit.BanList.Type.IP).addBan(address, null, null, null);
    }

    public void unbanIP(String address) {
        Validate.notNull(address, "Address cannot be null.");

        this.getBanList(org.bukkit.BanList.Type.IP).pardon(address);
    }

    public Set<OfflinePlayer> getBannedPlayers() {
        Set<OfflinePlayer> result = new HashSet<OfflinePlayer>();

        for (Object name : playerList.getBannedPlayers().getBannedList().keySet()) {
            result.add(getOfflinePlayer((String) name));
        }

        return result;
    }

    @Override
    public BanList getBanList(BanList.Type type){
        Validate.notNull(type, "Type cannot be null");

        switch(type){
        case IP:
            return new CraftBanList(playerList.getBannedIPs());
        case NAME:
        default: // Fall through as a player name list for safety
            return new CraftBanList(playerList.getBannedPlayers());
        }
    }

    public void setWhitelist(boolean value) {
        playerList.whiteListEnforced = value;
        console.getPropertyManager().setProperty("white-list", value);
    }

    public Set<OfflinePlayer> getWhitelistedPlayers() {
        Set<OfflinePlayer> result = new LinkedHashSet<OfflinePlayer>();

        for (Object name : playerList.getWhiteListedPlayers()) {
            if (((String)name).length() == 0 || ((String)name).startsWith("#")) {
                continue;
            }
            result.add(getOfflinePlayer((String) name));
        }

        return result;
    }

    public Set<OfflinePlayer> getOperators() {
        Set<OfflinePlayer> result = new HashSet<OfflinePlayer>();

        for (Object name : playerList.getOps()) {
            result.add(getOfflinePlayer((String) name));
        }

        return result;
    }

    public void reloadWhitelist() {
        playerList.loadWhiteList();
    }

    public GameMode getDefaultGameMode() {
        return GameMode.getByValue(console.worlds.get(0).getWorldInfo().getGameType().getID());
    }

    public void setDefaultGameMode(GameMode mode) {
        Validate.notNull(mode, "Mode cannot be null");

        for (World world : getWorlds()) {
            ((CraftWorld) world).getHandle().worldInfo.setGameType(net.minecraft.world.WorldSettings.GameType.getByID(mode.getValue()));
        }
    }

    public ConsoleCommandSender getConsoleSender() {
        return console.console;
    }

    public EntityMetadataStore getEntityMetadata() {
        return entityMetadata;
    }

    public PlayerMetadataStore getPlayerMetadata() {
        return playerMetadata;
    }

    public WorldMetadataStore getWorldMetadata() {
        return worldMetadata;
    }

    public void detectListNameConflict(net.minecraft.entity.player.EntityPlayerMP entityPlayer) {
        // Collisions will make for invisible people
        for (int i = 0; i < getHandle().playerEntityList.size(); ++i) {
            net.minecraft.entity.player.EntityPlayerMP testEntityPlayer = (net.minecraft.entity.player.EntityPlayerMP) getHandle().playerEntityList.get(i);

            // We have a problem!
            if (testEntityPlayer != entityPlayer && testEntityPlayer.listName.equals(entityPlayer.listName)) {
                String oldName = entityPlayer.listName;
                int spaceLeft = 16 - oldName.length();

                if (spaceLeft <= 1) { // We also hit the list name length limit!
                    entityPlayer.listName = oldName.subSequence(0, oldName.length() - 2 - spaceLeft) + String.valueOf(System.currentTimeMillis() % 99);
                } else {
                    entityPlayer.listName = oldName + String.valueOf(System.currentTimeMillis() % 99);
                }

                return;
            }
        }
    }

    public File getWorldContainer() {
        // Cauldron start - return the proper container
        if (DimensionManager.getWorld(0) != null)
        {
            return ((SaveHandler)DimensionManager.getWorld(0).getSaveHandler()).getWorldDirectory();
        }
        // Cauldron end
        if (container == null) {
            container = new File(configuration.getString("settings.world-container", "."));
        }

        return container;
    }

    public OfflinePlayer[] getOfflinePlayers() {
        net.minecraft.world.storage.SaveHandler storage = (net.minecraft.world.storage.SaveHandler) console.worlds.get(0).getSaveHandler();
        String[] files = storage.getPlayerDir().list(new DatFileFilter());
        Set<OfflinePlayer> players = new HashSet<OfflinePlayer>();

        for (String file : files) {
            players.add(getOfflinePlayer(file.substring(0, file.length() - 4), false));
        }
        players.addAll(Arrays.asList(getOnlinePlayers()));

        return players.toArray(new OfflinePlayer[players.size()]);
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        StandardMessenger.validatePluginMessage(getMessenger(), source, channel, message);

        for (Player player : getOnlinePlayers()) {
            player.sendPluginMessage(source, channel, message);
        }
    }

    public Set<String> getListeningPluginChannels() {
        Set<String> result = new HashSet<String>();

        for (Player player : getOnlinePlayers()) {
            result.addAll(player.getListeningPluginChannels());
        }

        return result;
    }

    public void onPlayerJoin(Player player) {
        if ((updater.isEnabled()) && (updater.getCurrent() != null) && (player.hasPermission(Server.BROADCAST_CHANNEL_ADMINISTRATIVE))) {
            if ((updater.getCurrent().isBroken()) && (updater.getOnBroken().contains(AutoUpdater.WARN_OPERATORS))) {
                player.sendMessage(ChatColor.DARK_RED + "The version of CraftBukkit that this server is running is known to be broken. Please consider updating to the latest version at dl.bukkit.org.");
            } else if ((updater.isUpdateAvailable()) && (updater.getOnUpdate().contains(AutoUpdater.WARN_OPERATORS))) {
                player.sendMessage(ChatColor.DARK_PURPLE + "The version of CraftBukkit that this server is running is out of date. Please consider updating to the latest version at dl.bukkit.org.");
            }
        }
    }

    public Inventory createInventory(InventoryHolder owner, InventoryType type) {
        // TODO: Create the appropriate type, rather than Custom?
        return new CraftInventoryCustom(owner, type);
    }

    public Inventory createInventory(InventoryHolder owner, int size) throws IllegalArgumentException {
        Validate.isTrue(size % 9 == 0, "Chests must have a size that is a multiple of 9!");
        return new CraftInventoryCustom(owner, size);
    }

    public Inventory createInventory(InventoryHolder owner, int size, String title) throws IllegalArgumentException {
        Validate.isTrue(size % 9 == 0, "Chests must have a size that is a multiple of 9!");
        return new CraftInventoryCustom(owner, size, title);
    }

    public HelpMap getHelpMap() {
        return helpMap;
    }

    public SimpleCommandMap getCommandMap() {
        return commandMap;
    }
    
    // Cauldron start
    public CraftSimpleCommandMap getCraftCommandMap() {
        return craftCommandMap;
    }
    // Cauldron end
    
    public int getMonsterSpawnLimit() {
        return monsterSpawn;
    }

    public int getAnimalSpawnLimit() {
        return animalSpawn;
    }

    public int getWaterAnimalSpawnLimit() {
        return waterAnimalSpawn;
    }

    public int getAmbientSpawnLimit() {
        return ambientSpawn;
    }

    public boolean isPrimaryThread() {
        return Thread.currentThread().equals(console.primaryThread);
    }

    public String getMotd() {
        return console.getMOTD();
    }

    public WarningState getWarningState() {
        return warningState;
    }

    public List<String> tabComplete(net.minecraft.command.ICommandSender sender, String message) {
        if (!(sender instanceof net.minecraft.entity.player.EntityPlayerMP)) {
            return ImmutableList.of();
        }

        Player player = ((net.minecraft.entity.player.EntityPlayerMP) sender).getBukkitEntity();
        if (message.startsWith("/")) {
            return tabCompleteCommand(player, message);
        } else {
            return tabCompleteChat(player, message);
        }
    }

    public List<String> tabCompleteCommand(Player player, String message) {
        // Spigot Start
        if ( !org.spigotmc.SpigotConfig.tabComplete && !message.contains( " " ) )
        {
            return ImmutableList.of();
        }
        // Spigot End

        // Spigot Start
        List<String> completions = new ArrayList<String>();
        try {
            message = message.substring( 1 );
            List<String> bukkitCompletions = getCommandMap().tabComplete( player, message );
            if ( bukkitCompletions != null )
            {
                completions.addAll( bukkitCompletions );
            }
            List<String> vanillaCompletions = org.spigotmc.VanillaCommandWrapper.complete( player, message );
            if ( vanillaCompletions != null )
            {
                completions.addAll( vanillaCompletions );
            }
            // Spigot End
        } catch (CommandException ex) {
            player.sendMessage(ChatColor.RED + "An internal error occurred while attempting to tab-complete this command");
            getLogger().log(Level.SEVERE, "Exception when " + player.getName() + " attempted to tab complete " + message, ex);
        }

        return completions; // Spigot
    }

    public List<String> tabCompleteChat(Player player, String message) {
        Player[] players = getOnlinePlayers();
        List<String> completions = new ArrayList<String>();
        PlayerChatTabCompleteEvent event = new PlayerChatTabCompleteEvent(player, message, completions);
        String token = event.getLastToken();
        for (Player p : players) {
            if (player.canSee(p) && StringUtil.startsWithIgnoreCase(p.getName(), token)) {
                completions.add(p.getName());
            }
        }
        pluginManager.callEvent(event);

        Iterator<?> it = completions.iterator();
        while (it.hasNext()) {
            Object current = it.next();
            if (!(current instanceof String)) {
                // Sanity
                it.remove();
            }
        }
        Collections.sort(completions, String.CASE_INSENSITIVE_ORDER);
        return completions;
    }

    public CraftItemFactory getItemFactory() {
        return CraftItemFactory.instance();
    }

    public CraftScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public void checkSaveState() {
        if (this.playerCommandState || this.printSaveWarning || this.console.autosavePeriod <= 0) {
            return;
        }
        this.printSaveWarning = true;
        getLogger().log(Level.WARNING, "A manual (plugin-induced) save has been detected while server is configured to auto-save. This may affect performance.", warningState == WarningState.ON ? new Throwable() : null);
    }

    @Override
    public CraftIconCache getServerIcon() {
        return icon;
    }

    @Override
    public CraftIconCache loadServerIcon(File file) throws Exception {
        Validate.notNull(file, "File cannot be null");
        if (!file.isFile()) {
            throw new IllegalArgumentException(file + " is not a file");
        }
        return loadServerIcon0(file);
    }

    static CraftIconCache loadServerIcon0(File file) throws Exception {
        return loadServerIcon0(ImageIO.read(file));
    }

    @Override
    public CraftIconCache loadServerIcon(BufferedImage image) throws Exception {
        Validate.notNull(image, "Image cannot be null");
        return loadServerIcon0(image);
    }

    static CraftIconCache loadServerIcon0(BufferedImage image) throws Exception {
        ByteBuf bytebuf = Unpooled.buffer();

        Validate.isTrue(image.getWidth() == 64, "Must be 64 pixels wide");
        Validate.isTrue(image.getHeight() == 64, "Must be 64 pixels high");
        ImageIO.write(image, "PNG", new ByteBufOutputStream(bytebuf));
        ByteBuf bytebuf1 = Base64.encode(bytebuf);

        return new CraftIconCache("data:image/png;base64," + bytebuf1.toString(Charsets.UTF_8));
    }

    public void setIdleTimeout(int threshold) {
        console.func_143006_e(threshold); // Should be setIdleTimeout
    }

    public int getIdleTimeout() {
        return console.func_143007_ar(); // Should be getIdleTimeout
    }

    @Deprecated
    @Override
    public UnsafeValues getUnsafe() {
        return CraftMagicNumbers.INSTANCE;
    }
}
