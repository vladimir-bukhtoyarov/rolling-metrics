package com.github.metricscore.hdrhistogram;

public class SnapshotConfiguration {

    private final long cacheDurationMillis;
    private final boolean storeValues;
    private final double[] predefinedPercentiles;

    public SnapshotConfiguration(long cacheDurationMillis) {
        this.cacheDurationMillis = cacheDurationMillis;
        this.storeValues = true;
        this.predefinedPercentiles = null;
    }

    public SnapshotConfiguration(long cacheDurationMillis, double[] predefinedPercentiles) {
        this.cacheDurationMillis = cacheDurationMillis;
        this.storeValues = false;
        this.predefinedPercentiles = predefinedPercentiles;
    }

    public boolean shouldStoreValues() {
        return storeValues;
    }

    public double[] getPredefinedPercentiles() {
        return predefinedPercentiles;
    }

    public long getCacheDurationMillis() {
        return cacheDurationMillis;
    }

}
