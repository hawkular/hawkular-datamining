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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.models.SimpleMovingAverageTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class AdditiveSeasonalDecompositionTest extends AbstractTest {

    // trendStationary(sine(periods=5, seasons=3, amplitude=18, sigma=4, error='uniform'), slope=1.337), frequency=5)
    private int periods = 5;
    private Double[] values = new Double[] {
            2.6894891, 8.8009588, -9.1917104, 24.3551737, -8.9494696, 21.0914715,
            -0.3514645, 9.5879129, 18.3310093, -1.1620090, 30.5930243, -2.3743748,
            31.7202965, 10.2222619, 21.8288987
    };
    private final List<DataPoint> testData;


    public AdditiveSeasonalDecompositionTest() {
        long counter = 0;
        testData = new ArrayList<>();
        for (Double val: values) {
            testData.add(new DataPoint(val, counter++));
        }
    }

    @Test
    public void testOnThreeSeasons() throws IOException {
        AdditiveSeasonalDecomposition decomposition = new AdditiveSeasonalDecomposition(testData, periods);
        decomposition.decompose();

        int nullsAtStart = SimpleMovingAverageTest.nullAt(decomposition.trend(), true);
        int nullsAtEnd = SimpleMovingAverageTest.nullAt(decomposition.trend(), false);
        Assert.assertTrue(nullsAtStart == 2);
        Assert.assertTrue(nullsAtEnd == 2);

        nullsAtStart = SimpleMovingAverageTest.nullAt(decomposition.random(), true);
        nullsAtEnd = SimpleMovingAverageTest.nullAt(decomposition.random(), false);
        Assert.assertTrue(nullsAtStart == 2);
        Assert.assertTrue(nullsAtEnd == 2);

        nullsAtStart = SimpleMovingAverageTest.nullAt(decomposition.seasonal(), true);
        nullsAtEnd = SimpleMovingAverageTest.nullAt(decomposition.seasonal(), true);
        Assert.assertTrue(nullsAtStart == 0);
        Assert.assertTrue(nullsAtEnd == 0);

        Double[] seasonal = new Double[] {
                13.4912349, -12.3006415, 0.1592124, 11.9657458, -13.3155516
        };
        for (int i = 0; i < decomposition.seasonal().size(); i++) {
            Assert.assertEquals(seasonal[i % periods], decomposition.seasonal().get(i).getValue(), 0.000001);
        }

        Double[] random = new Double[] {
                null, null, -12.89181117, 5.16814309, -1.02471811, -1.54648818, 4.00728507, -0.07068359,
                -5.03443111, 1.15843009, 1.68020016, -3.87357309, 13.16306273, null, null
        };
        for (int i = 0; i < decomposition.seasonal().size(); i++) {
            if (random[i] == null) {
                if (decomposition.random().get(i).getValue() != null) {
                    Assert.fail();
                }
                continue;
            }
            Assert.assertEquals(random[i], decomposition.random().get(i).getValue(), 0.000001);
        }
    }

    @Test
    public void testOnShorterSeries() {
        List<DataPoint> testData = this.testData.subList(0, this.testData.size() - 2);
        AdditiveSeasonalDecomposition decomposition = new AdditiveSeasonalDecomposition(testData, periods);
        decomposition.decompose();

        int nullsAtStart = SimpleMovingAverageTest.nullAt(decomposition.trend(), true);
        int nullsAtEnd = SimpleMovingAverageTest.nullAt(decomposition.trend(), false);
        Assert.assertTrue(nullsAtStart == 2);
        Assert.assertTrue(nullsAtEnd == 2);

        nullsAtStart = SimpleMovingAverageTest.nullAt(decomposition.random(), true);
        nullsAtEnd = SimpleMovingAverageTest.nullAt(decomposition.random(), false);
        Assert.assertTrue(nullsAtStart == 2);
        Assert.assertTrue(nullsAtEnd == 2);

        nullsAtStart = SimpleMovingAverageTest.nullAt(decomposition.seasonal(), true);
        nullsAtEnd = SimpleMovingAverageTest.nullAt(decomposition.seasonal(), true);
        Assert.assertTrue(nullsAtStart == 0);
        Assert.assertTrue(nullsAtEnd == 0);


        Double[] trend = {
                null, null, 3.540888, 7.221285, 5.390800, 9.146725, 7.941892, 9.499384, 11.399695,
                10.995113, 15.421589, null, null
        };
        for (int i = 0; i < decomposition.seasonal().size(); i++) {
            if (trend[i] == null) {
                if (decomposition.random().get(i).getValue() != null) {
                    Assert.fail();
                }
                continue;
            }
            Assert.assertEquals(trend[i], decomposition.trend().get(i).getValue(), 0.000001);
        }

        Double[] seasonal = new Double[] {
                14.012770, -7.838678, -5.867356, 12.487281, -12.794017
        };
        for (int i = 0; i < decomposition.seasonal().size(); i++) {
            Assert.assertEquals(seasonal[i % periods], decomposition.seasonal().get(i).getValue(), 0.000001);
        }

        Double[] random = new Double[]{
                null, null, -6.8652427, 4.6466082, -1.5462530, -2.0680230,
                -0.4546789, 5.9558849, -5.5559660, 0.6368952, 1.1586653, null, null
        };
        for (int i = 0; i < decomposition.seasonal().size(); i++) {
            if (random[i] == null) {
                if (decomposition.random().get(i).getValue() != null) {
                    Assert.fail();
                }
                continue;
            }
            Assert.assertEquals(random[i], decomposition.random().get(i).getValue(), 0.000001);
        }
    }
}
