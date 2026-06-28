package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.feature.ignore.PlayerIgnoreManager;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class GrimIgnore implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        PlayerIgnoreManager.get().load();

        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("ignore", Description.of("Toggle Simulation alert ignore for a player"))
                        .permission("grim.ignore")
                        .literal("list", Description.of("List players ignored for Simulation alerts"))
                        .handler(this::handleList)
        );

        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("ignore", Description.of("Toggle Simulation alert ignore for a player"))
                        .permission("grim.ignore")
                        .required("target", arguments.singlePlayerSelectorParser())
                        .handler(this::handleIgnore)
        );
    }

    private void handleIgnore(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");

        PlatformPlayer targetPlatformPlayer = target.getSinglePlayer().getPlatformPlayer();
        if (Objects.requireNonNull(targetPlatformPlayer).isExternalPlayer()) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-this-server", "%prefix% &cThis player isn't on this server!"));
            return;
        }

        UUID targetUuid = targetPlatformPlayer.getUniqueId();
        boolean ignored = PlayerIgnoreManager.get().toggleIgnore(targetUuid);
        String messageKey = ignored ? "ignore-enabled" : "ignore-disabled";
        String raw = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse(
                messageKey,
                "%prefix% &fSimulation alert ignore toggled for &b%player%"
        );
        raw = MessageUtil.replacePlaceholders(sender, raw);
        raw = raw.replace("%player%", targetPlatformPlayer.getName());
        sender.sendMessage(MessageUtil.miniMessage(raw));
    }

    private void handleList(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        Set<UUID> ignoredPlayers = PlayerIgnoreManager.get().getIgnoredPlayers();
        if (ignoredPlayers.isEmpty()) {
            sender.sendMessage(MessageUtil.getParsedComponent(
                    sender,
                    "ignore-list-empty",
                    "%prefix% &7No players are ignored for Simulation alerts"
            ));
            return;
        }

        sender.sendMessage(MessageUtil.getParsedComponent(
                sender,
                "ignore-list-header",
                "%prefix% &fIgnored Simulation alert players:"
        ));
        for (UUID uuid : ignoredPlayers) {
            String name = resolveName(uuid);
            String raw = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse(
                    "ignore-list-entry",
                    "&7- &b%player% &8(%uuid%)"
            );
            raw = MessageUtil.replacePlaceholders(sender, raw);
            raw = raw.replace("%player%", name).replace("%uuid%", uuid.toString());
            sender.sendMessage(MessageUtil.miniMessage(raw));
        }
    }

    private static @NotNull String resolveName(@NotNull UUID uuid) {
        PlatformPlayer online = GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromUUID(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlatformPlayer offline = GrimAPI.INSTANCE.getPlatformPlayerFactory().getOfflineFromUUID(uuid);
        if (offline != null && offline.getName() != null && !offline.getName().isBlank()) {
            return offline.getName();
        }
        return uuid.toString();
    }
}
