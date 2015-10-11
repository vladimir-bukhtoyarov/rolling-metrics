package com.github.addon.metrics.decorator;

import com.codahale.metrics.Timer;
import com.github.addon.metrics.decorator.timer.TimerDecoratorBuilder;

public class Decorators {

    public static TimerDecoratorBuilder forTimer(Timer timer) {
        return new TimerDecoratorBuilder(timer);
    }

}
