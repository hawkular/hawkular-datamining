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

package org.hawkular.datamining.forecast.utils;


import org.junit.Assert;
import org.junit.Test;


/**
 * @author Pavol Loffay
 */
public class TimeSeriesLagTest {

    @Test
    public void testLag() throws Exception {

        double[] x = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        };

        double[][] lagged = TimeSeriesLag.lag(x, 1);
        Assert.assertTrue(lagged.length == 1);
        Assert.assertTrue(lagged[0].length == 10);
        Assert.assertArrayEquals(x, lagged[0], 0);

        lagged = TimeSeriesLag.lag(x, 2);
        Assert.assertTrue(lagged.length == 2);
        Assert.assertTrue(lagged[0].length == 9);
        Assert.assertTrue(lagged[1].length == 9);
        Assert.assertArrayEquals(new double[]{2, 3, 4, 5, 6, 7, 8, 9, 10}, lagged[0], 0);
        Assert.assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, lagged[1], 0);

        x = new double[] {
                1, 2, 3
        };

        lagged = TimeSeriesLag.lag(x, 3);
        Assert.assertTrue(lagged.length == 3);
        Assert.assertTrue(lagged[0].length == 1);
        Assert.assertTrue(lagged[1].length == 1);
        Assert.assertTrue(lagged[2].length == 1);
        Assert.assertArrayEquals(new double[]{3}, lagged[0], 0);
        Assert.assertArrayEquals(new double[]{2}, lagged[1], 0);
        Assert.assertArrayEquals(new double[]{1}, lagged[2], 0);
    }

    @Test
    public void testErrorStates() {
        try {
            TimeSeriesLag.lag(new double[]{1,2}, 3);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // ok
        }

        try {
            TimeSeriesLag.lag(null, -6);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }
}
