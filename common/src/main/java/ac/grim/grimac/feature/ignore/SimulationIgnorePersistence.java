package ac.grim.grimac.feature.ignore;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

final class SimulationIgnorePersistence {

    private static final String FILE_NAME = "simulation-ignore.yml";
    private static final String KEY_IGNORED_PLAYERS = "ignored-players";
    private static final long FLUSH_DELAY_MS = 500L;

    private final Path file;
    private final Yaml yaml;
    private final ScheduledExecutorService scheduler;
    private final AtomicReference<ScheduledFuture<?>> pendingFlush = new AtomicReference<>();

    SimulationIgnorePersistence() {
        this.file = GrimAPI.INSTANCE.getGrimPlugin().getDataFolder().toPath().resolve(FILE_NAME);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "grim-simulation-ignore-flush");
            thread.setDaemon(true);
            return thread;
        });
    }

    @NotNull Set<UUID> load() {
        if (!Files.isRegularFile(file)) {
            return Set.of();
        }
        try (InputStream input = Files.newInputStream(file)) {
            Object raw = yaml.load(input);
            if (!(raw instanceof Map<?, ?> map)) {
                return Set.of();
            }
            Object players = map.get(KEY_IGNORED_PLAYERS);
            if (!(players instanceof List<?> list)) {
                return Set.of();
            }
            List<UUID> parsed = new ArrayList<>();
            for (Object entry : list) {
                if (entry == null) continue;
                try {
                    parsed.add(UUID.fromString(entry.toString().trim()));
                } catch (IllegalArgumentException ignored) {
                    LogUtil.warn("Ignoring invalid UUID in " + FILE_NAME + ": " + entry);
                }
            }
            return Set.copyOf(parsed);
        } catch (IOException e) {
            LogUtil.error("Failed to load " + FILE_NAME, e);
            return Set.of();
        }
    }

    void scheduleSave(@NotNull Set<UUID> ignoredPlayers) {
        ScheduledFuture<?> next = scheduler.schedule(
                () -> flush(ignoredPlayers),
                FLUSH_DELAY_MS,
                TimeUnit.MILLISECONDS
        );
        ScheduledFuture<?> previous = pendingFlush.getAndSet(next);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    void flushNow(@NotNull Set<UUID> ignoredPlayers) {
        ScheduledFuture<?> pending = pendingFlush.getAndSet(null);
        if (pending != null) {
            pending.cancel(false);
        }
        flush(ignoredPlayers);
    }

    void shutdown() {
        ScheduledFuture<?> pending = pendingFlush.getAndSet(null);
        if (pending != null) {
            pending.cancel(false);
        }
        scheduler.shutdownNow();
    }

    private void flush(@NotNull Set<UUID> ignoredPlayers) {
        try {
            Files.createDirectories(file.getParent());
            Map<String, Object> root = new LinkedHashMap<>();
            List<String> serialized = ignoredPlayers.stream()
                    .map(UUID::toString)
                    .sorted()
                    .collect(Collectors.toList());
            root.put(KEY_IGNORED_PLAYERS, serialized);
            Files.writeString(file, yaml.dump(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogUtil.error("Failed to save " + FILE_NAME, e);
        }
    }
}
