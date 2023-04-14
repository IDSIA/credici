package ch.idsia.credici.learning.inference;

public interface TotalTiming {
    double getSetupTime();
    double getQueryTime();

    void reset();
}
