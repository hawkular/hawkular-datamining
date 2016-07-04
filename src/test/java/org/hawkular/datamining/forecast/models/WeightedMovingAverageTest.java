/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.datamining.forecast.models;

import static org.hawkular.datamining.forecast.models.SimpleMovingAverageTest.nullAt;

import java.util.Arrays;
import java.util.List;

import org.hawkular.datamining.forecast.DataPoint;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class WeightedMovingAverageTest {

    @Test
    public void testResultLength() {
        List<DataPoint> points = Arrays.asList(new DataPoint(1D, 0L), new DataPoint(1D, 5L),
                new DataPoint(1D, 10L), new DataPoint(1D, 15L), new DataPoint(1D, 20L), new DataPoint(1D, 25L),
                new DataPoint(1D, 30L));

        WeightedMovingAverage weightedMovingAverage = new WeightedMovingAverage(points, new double[]{1, 1});
        List<DataPoint> result = weightedMovingAverage.learn();
        int nullAtBeginning = nullAt(result, true);
        int nullAtEnd = nullAt(result, false);
        Assert.assertTrue(nullAtBeginning == 0);
        Assert.assertTrue(nullAtEnd == 1);
        long timestamp = 0;
        for (DataPoint point: result) {
            if (point.getValue() != null) {
                Assert.assertTrue(point.getValue().equals(2.0));
            }
            // timestamps should be equal
            Assert.assertTrue(point.getTimestamp().equals(timestamp));
            timestamp += 5;
        }

        weightedMovingAverage = new WeightedMovingAverage(points, new double[]{0.5, 0.5});
        result = weightedMovingAverage.learn();
        nullAtBeginning = nullAt(result, true);
        nullAtEnd = nullAt(result, false);
        Assert.assertTrue(nullAtBeginning == 0);
        Assert.assertTrue(nullAtEnd == 1);
        for (DataPoint point: result) {
            if (point.getValue() != null) {
                Assert.assertTrue(point.getValue().equals(1.0));
            }
        }

        weightedMovingAverage = new WeightedMovingAverage(points, new double[]{0.5, 0.5, 0.5, 0.5});
        result = weightedMovingAverage.learn();
        nullAtBeginning = nullAt(result, true);
        nullAtEnd = nullAt(result, false);
        Assert.assertTrue(nullAtBeginning == 1);
        Assert.assertTrue(nullAtEnd == 2);

        weightedMovingAverage = new WeightedMovingAverage(points, new double[]{0.5, 0.5, 0.5, 0.5, 0.5});
        result = weightedMovingAverage.learn();
        nullAtBeginning = nullAt(result, true);
        nullAtEnd = nullAt(result, false);
        Assert.assertTrue(nullAtBeginning == 2);
        Assert.assertTrue(nullAtEnd == 2);
    }
}
