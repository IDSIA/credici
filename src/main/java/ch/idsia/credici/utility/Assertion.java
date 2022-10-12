package ch.idsia.credici.utility;

import ch.idsia.credici.utility.experiments.Logger;

public class Assertion {
    public static void assertTrue(boolean cond, String message) {
        if (!cond)
            throw new IllegalArgumentException(message);
    }

    public static void assertTrueWarning(boolean cond, Logger logger, String message) {
        if (!cond)
            logger.warn(message);
    }

    public static void assertTrue(boolean cond) {
        if (!cond)
            throw new IllegalArgumentException();
    }
}
