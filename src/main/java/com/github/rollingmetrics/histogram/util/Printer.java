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

package com.github.rollingmetrics.histogram.util;

import org.HdrHistogram.Histogram;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Printer {

    public static String histogramToString(Histogram histogram) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PrintStream writer = new PrintStream(baos);
            histogram.outputPercentileDistribution(writer, 1.0);
            byte[] resultBytes = baos.toByteArray();
            return new String(resultBytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String printArray(Object[] array, String elementName) {
        StringBuilder msg = new StringBuilder("{");
        for (int i = 0; i < array.length; i++) {
            Object element = array[i];
            msg.append("\n").append(elementName).append("[").append(i).append("]=").append(element);
        }
        msg.append("\n}");
        return msg.toString();
    }
    
}
