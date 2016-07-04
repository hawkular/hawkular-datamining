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
public class TimeSeriesDifferencingTest {

    @Test
    public void testDiff() throws Exception {
        double[] x = {
                1, 11, 3, 5, 8
        };
        double[] expected1 = {
            10, -8, 2, 3
        };
        // double differences
        double[] expected2 = {
                -18, 10, 1
        } ;

        double[] diff1 = TimeSeriesDifferencing.differencesAtLag(x, 1);
        double[] diff2 = TimeSeriesDifferencing.differencesAtLag(x, 2);

        Assert.assertEquals(x.length - 1, diff1.length);
        Assert.assertArrayEquals(expected1, diff1, 0.00001);
        Assert.assertArrayEquals(expected2, diff2, 0.00001);
    }

    @Test
    public void testErrorStates() {
        try {
            TimeSeriesDifferencing.differencesAtLag(new double[0], 0);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // ok
        }

        try {
            TimeSeriesDifferencing.differencesAtLag(new double[]{1}, 1);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // ok
        }

        try {
            TimeSeriesDifferencing.differencesAtLag(new double[]{1, 3, 2}, 3);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }
}
