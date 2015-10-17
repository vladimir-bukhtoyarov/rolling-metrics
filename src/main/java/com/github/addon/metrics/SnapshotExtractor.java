package com.github.addon.metrics;

import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;

public interface SnapshotExtractor {

    SnapshotExtractor DEFAULT = new SnapshotExtractor() {
        @Override
        public Snapshot extract(Sampling sampling) {
            return sampling.getSnapshot();
        }
    };

    Snapshot extract(Sampling sampling);

}
