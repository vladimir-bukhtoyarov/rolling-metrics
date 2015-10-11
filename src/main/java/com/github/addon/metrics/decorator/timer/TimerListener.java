package com.github.addon.metrics.decorator.timer;

import java.util.concurrent.TimeUnit;

public interface TimerListener {

    void onUpdate(long duration, TimeUnit unit);

}
