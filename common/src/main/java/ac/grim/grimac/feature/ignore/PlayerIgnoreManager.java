package ac.grim.grimac.feature.ignore;

import ac.grim.grimac.checks.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerIgnoreManager {

    private static volatile PlayerIgnoreManager instance;

    private final SimulationIgnorePersistence persistence = new SimulationIgnorePersistence();
    private final Set<UUID> ignoredPlayers = ConcurrentHashMap.newKeySet();
    private volatile boolean loaded;

    private PlayerIgnoreManager() {}

    public static @NotNull PlayerIgnoreManager get() {
        if (instance == null) {
            synchronized (PlayerIgnoreManager.class) {
                if (instance == null) {
                    instance = new PlayerIgnoreManager();
                }
            }
        }
        return instance;
    }

    public synchronized void load() {
        if (loaded) return;
        ignoredPlayers.clear();
        ignoredPlayers.addAll(persistence.load());
        loaded = true;
    }

    /**
     * @return {@code true} if the player is now ignored, {@code false} if removed from the ignore list
     */
    public boolean toggleIgnore(@NotNull UUID targetUuid) {
        ensureLoaded();
        boolean ignored;
        if (ignoredPlayers.remove(targetUuid)) {
            ignored = false;
        } else {
            ignoredPlayers.add(targetUuid);
            ignored = true;
        }
        persistence.scheduleSave(Set.copyOf(ignoredPlayers));
        return ignored;
    }

    public boolean isIgnored(@NotNull UUID targetUuid) {
        ensureLoaded();
        return ignoredPlayers.contains(targetUuid);
    }

    public @NotNull Set<UUID> getIgnoredPlayers() {
        ensureLoaded();
        return Collections.unmodifiableSet(Set.copyOf(ignoredPlayers));
    }

    public boolean shouldSuppressSimulationAlert(@NotNull UUID flaggedUuid, @NotNull Check check) {
        return SimulationCheck.isSimulation(check) && isIgnored(flaggedUuid);
    }

    private void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }
}
