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

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class FormatterTest {

    @Test
    public void roundToDigits() throws Exception {
        checkRounding(new BigDecimal("0.6"), 0.555, 1);
        checkRounding(new BigDecimal("0.56"), 0.555, 2);
        checkRounding(new BigDecimal("0.555"), 0.555, 3);
    }

    private void checkRounding(BigDecimal bigDecimal, double source, int digits) {
        assertEquals(bigDecimal, Formatter.roundToDigits(source, digits));
    }

    @Test
    public void toPrintablePercentileName() throws Exception {
        checkPercentileFormatting(0.05, "5thPercentile");
        checkPercentileFormatting(0.5, "50thPercentile");
        checkPercentileFormatting(0.75, "75thPercentile");
        checkPercentileFormatting(0.90, "90thPercentile");
        checkPercentileFormatting(0.999, "999thPercentile");
        checkPercentileFormatting(0.9999, "9999thPercentile");
    }

    private void checkPercentileFormatting(double percentile, String required) {
        assertEquals(required, Formatter.toPrintablePercentileName(percentile));
    }

}