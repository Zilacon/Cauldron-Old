package net.minecraft.server.network;

import java.math.BigInteger;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerLoginServer.LoginState;
import net.minecraft.util.CryptManager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;

// CraftBukkit start
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
// CraftBukkit end

class ThreadPlayerLookupUUID extends Thread
{
    final NetHandlerLoginServer field_151292_a;

    ThreadPlayerLookupUUID(NetHandlerLoginServer p_i45296_1_, String p_i45296_2_)
    {
        super(p_i45296_2_);
        this.field_151292_a = p_i45296_1_;
    }

    public void run()
    {
        GameProfile gameprofile = NetHandlerLoginServer.getGameProfile(this.field_151292_a);
        try
        {
            // Spigot Start
            if (!MinecraftServer.getServer().isServerInOnlineMode())
            {
                this.field_151292_a.initUUID();
                fireLoginEvents();
                return;
            }
            // Spigot End
            String s = (new BigInteger(CryptManager.getServerIdHash(NetHandlerLoginServer.getLoginServerId(this.field_151292_a), NetHandlerLoginServer.getMinecraftServer(this.field_151292_a).getKeyPair().getPublic(), NetHandlerLoginServer.getSecretKey(this.field_151292_a)))).toString(16);

            NetHandlerLoginServer.doGameProfilesMatch(this.field_151292_a, NetHandlerLoginServer.getMinecraftServer(this.field_151292_a).func_147130_as().hasJoinedServer(new GameProfile((UUID) null, gameprofile.getName()), s));
            if (NetHandlerLoginServer.getGameProfile(this.field_151292_a) != null)
            {
                fireLoginEvents(); // Spigot
            }
            else if (NetHandlerLoginServer.getMinecraftServer(this.field_151292_a).isSinglePlayer())
            {
                NetHandlerLoginServer.getLogger().warn("Failed to verify username but will let them in anyway!");
                NetHandlerLoginServer.doGameProfilesMatch(this.field_151292_a, this.field_151292_a.func_152506_a(gameprofile));
                NetHandlerLoginServer.setLoginState(this.field_151292_a, LoginState.READY_TO_ACCEPT);
            }
            else
            {
                this.field_151292_a.func_147322_a("Failed to verify username!");
                NetHandlerLoginServer.getLogger().error("Username \'" + NetHandlerLoginServer.getGameProfile(this.field_151292_a).getName() + "\' tried to join with an invalid session");
            }
        }
        catch (AuthenticationUnavailableException authenticationunavailableexception)
        {
            if (NetHandlerLoginServer.getMinecraftServer(this.field_151292_a).isSinglePlayer())
            {
                NetHandlerLoginServer.getLogger().warn("Authentication servers are down but will let them in anyway!");
                NetHandlerLoginServer.doGameProfilesMatch(this.field_151292_a, this.field_151292_a.func_152506_a(gameprofile));
                NetHandlerLoginServer.setLoginState(this.field_151292_a, LoginState.READY_TO_ACCEPT);
            }
            else
            {
                this.field_151292_a.func_147322_a("Authentication servers are down. Please try again later, sorry!");
                NetHandlerLoginServer.getLogger().error("Couldn\'t verify username because servers are unavailable");
            }
            // CraftBukkit start - catch all exceptions
        }
        catch (Exception exception)
        {
            this.field_151292_a.func_147322_a("Failed to verify username!");
            NetHandlerLoginServer.getMinecraftServer(this.field_151292_a).server.getLogger().log(java.util.logging.Level.WARNING, "Exception verifying " + NetHandlerLoginServer.getGameProfile(this.field_151292_a).getName(), exception);
            // CraftBukkit end
        }
    }

    private void fireLoginEvents() throws Exception
    {
        // CraftBukkit start - fire PlayerPreLoginEvent
        if (!this.field_151292_a.field_147333_a.isChannelOpen())
        {
            return;
        }

        String playerName = NetHandlerLoginServer.getGameProfile(this.field_151292_a).getName();
        java.net.InetAddress address = ((java.net.InetSocketAddress) field_151292_a.field_147333_a.getSocketAddress()).getAddress();
        java.util.UUID uniqueId = this.field_151292_a.getGameProfile(this.field_151292_a).getId();
        final org.bukkit.craftbukkit.CraftServer server = NetHandlerLoginServer.getMinecraftServer(this.field_151292_a).server;

        AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(playerName, address);
        server.getPluginManager().callEvent(asyncEvent);

        if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0)
        {
            final PlayerPreLoginEvent event = new PlayerPreLoginEvent(playerName, address);

            if (asyncEvent.getResult() != PlayerPreLoginEvent.Result.ALLOWED)
            {
                event.disallow(asyncEvent.getResult(), asyncEvent.getKickMessage());
            }

            Waitable<PlayerPreLoginEvent.Result> waitable = new Waitable<PlayerPreLoginEvent.Result>()
            {
                @Override
                protected PlayerPreLoginEvent.Result evaluate()
                {
                    server.getPluginManager().callEvent(event);
                    return event.getResult();
                }
            };

            NetHandlerLoginServer.getMinecraftServer(this.field_151292_a).processQueue.add(waitable);

            if (waitable.get() != PlayerPreLoginEvent.Result.ALLOWED)
            {
                this.field_151292_a.func_147322_a(event.getKickMessage());
                return;
            }
        }
        else
        {
            if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            {
                this.field_151292_a.func_147322_a(asyncEvent.getKickMessage());
                return;
            }
        }
        // CraftBukkit end

        NetHandlerLoginServer.getLogger().info("UUID of player " + this.field_151292_a.getGameProfile(this.field_151292_a).getName() + " is " + this.field_151292_a.getGameProfile(this.field_151292_a).getId());
        NetHandlerLoginServer.setLoginState(this.field_151292_a, LoginState.READY_TO_ACCEPT);
    }
}