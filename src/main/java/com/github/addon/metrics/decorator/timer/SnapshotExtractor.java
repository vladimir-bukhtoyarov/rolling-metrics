package com.github.addon.metrics.decorator.timer;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

public interface SnapshotExtractor {

    SnapshotExtractor DEFAULT = new SnapshotExtractor() {
        @Override
        public Snapshot extract(Timer timer) {
            return timer.getSnapshot();
        }
    };

    Snapshot extract(Timer timer);

}
