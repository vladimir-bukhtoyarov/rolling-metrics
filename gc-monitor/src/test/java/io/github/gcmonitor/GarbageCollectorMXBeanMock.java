/*
 *  Copyright 2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.gcmonitor;import javax.management.*;
import java.lang.management.GarbageCollectorMXBean;
import java.util.HashMap;
import java.util.Map;

public class GarbageCollectorMXBeanMock implements GarbageCollectorMXBean, NotificationEmitter {

    private final String name;
    private final Map<NotificationListener, Object> listeners = new HashMap<>();

    private long collectionCount;
    private long collectionTime;

    public GarbageCollectorMXBeanMock(String name) {
        this.name = name;
    }

    public void addFakeCollection(long time) {
        collectionCount++;
        collectionTime += time;
        for (Map.Entry<NotificationListener, Object> listenerEntry : listeners.entrySet()) {
            NotificationListener listener = listenerEntry.getKey();
            Object handback = listenerEntry.getValue();
            listener.handleNotification(null, handback);
        }
    }

    public int getListenersCount() {
        return listeners.size();
    }

    @Override
    public long getCollectionCount() {
        return collectionCount;
    }

    @Override
    public long getCollectionTime() {
        return collectionTime;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
        listeners.put(listener, handback);
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        listeners.remove(listener);
    }

    @Override
    public boolean isValid() {
        throw new UnsupportedOperationException("Not necessary for tests");
    }

    @Override
    public String[] getMemoryPoolNames() {
        throw new UnsupportedOperationException("Not necessary for tests");
    }

    @Override
    public ObjectName getObjectName() {
        throw new UnsupportedOperationException("Not necessary for tests");
    }

    @Override
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        throw new UnsupportedOperationException("Not necessary for tests");
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        throw new UnsupportedOperationException("Not necessary for tests");
    }

}
