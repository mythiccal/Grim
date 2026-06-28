package ac.grim.grimac.feature.ignore;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.Check;
import org.jetbrains.annotations.NotNull;

public final class SimulationCheck {

    public static final String STABLE_KEY = "grim.prediction.simulation";

    private SimulationCheck() {}

    public static boolean isSimulation(@NotNull Check check) {
        return STABLE_KEY.equals(check.getStableKey());
    }

    public static boolean isSimulation(@NotNull AbstractCheck check) {
        return STABLE_KEY.equals(check.getStableKey());
    }
}
