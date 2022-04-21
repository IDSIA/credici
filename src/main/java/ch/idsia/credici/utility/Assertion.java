package ch.idsia.credici.utility;

public class Assertion {
    public static void assertTrue(boolean cond, String message) {
        if (!cond)
            throw new IllegalArgumentException(message);
    }

    public static void assertTrue(boolean cond) {
        if (!cond)
            throw new IllegalArgumentException();
    }
}
