package com.github.addon.metrics;


import com.codahale.metrics.WeightedSnapshot;

public class ExpirableWeightedSample implements Expirable {

    private final WeightedSnapshot.WeightedSample weightedSample;
    private final long expirationTimestamp;

    public ExpirableWeightedSample(WeightedSnapshot.WeightedSample weightedSample, long expirationTimestamp) {
        this.weightedSample = weightedSample;
        this.expirationTimestamp = expirationTimestamp;
    }

    public ExpirableWeightedSample with(WeightedSnapshot.WeightedSample weightedSample) {
        return new ExpirableWeightedSample(weightedSample, expirationTimestamp);
    }

    public WeightedSnapshot.WeightedSample getWeightedSample() {
        return weightedSample;
    }

    @Override
    public long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    @Override
    public boolean isExpired(long currentTimestamp) {
        return currentTimestamp > expirationTimestamp;
    }

}
