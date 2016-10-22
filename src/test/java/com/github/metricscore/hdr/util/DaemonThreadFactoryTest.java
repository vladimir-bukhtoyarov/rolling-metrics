/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.metricscore.hdr.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by vermut on 22.10.16.
 */
public class DaemonThreadFactoryTest {

    @Test
    public void createdThreadShouldNotBeStarted() {
        DaemonThreadFactory factory = new DaemonThreadFactory("xyz");
        Thread thread = factory.newThread(() -> {});
        assertEquals(false, thread.isAlive());
    }

    @Test
    public void newThreadShouldBeDaemon() throws Exception {
        DaemonThreadFactory factory = new DaemonThreadFactory("xyz");
        Thread thread = factory.newThread(() -> {});
        assertEquals(true, thread.isDaemon());
    }

    @Test
    public void newThreadShouldHasNameAccordingToFormat() throws Exception {
        DaemonThreadFactory factory = new DaemonThreadFactory("my-thread-%d");
        Thread thread = factory.newThread(() -> {});
        assertEquals("my-thread-1", thread.getName());

        DaemonThreadFactory factory2 = new DaemonThreadFactory("my-thread");
        Thread thread2 = factory2.newThread(() -> {});
        assertEquals("my-thread", thread2.getName());
    }

}