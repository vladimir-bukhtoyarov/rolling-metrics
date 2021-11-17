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

package io.github.gcmonitor.stat;

import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.histogram.hdr.RollingSnapshot;


public class ReadOnlyHistogram implements RollingHdrHistogram {

    private final RollingHdrHistogram target;

    public ReadOnlyHistogram(RollingHdrHistogram target) {
        this.target = target;
    }

    @Override
    public int getEstimatedFootprintInBytes() {
        return target.getEstimatedFootprintInBytes();
    }

    @Override
    public RollingSnapshot getSnapshot() {
        return target.getSnapshot();
    }

    @Override
    public void update(long value) {
        throw new UnsupportedOperationException();
    }

}
