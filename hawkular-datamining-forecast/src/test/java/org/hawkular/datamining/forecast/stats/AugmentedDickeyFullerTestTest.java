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

package org.hawkular.datamining.forecast.stats;

import java.io.IOException;

import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;
import org.hawkular.datamining.forecast.utils.Utils;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Pavol Loffay
 *
 * Values in the tests are compared to the results from R's adfTest() function
 *
 * library(fUnitRoots)
 * library(fpp)
 * adfTest(austourists, lags=1, type='c')
 *
 */
public class AugmentedDickeyFullerTestTest extends AbstractTest {

    @Test
    public void testAustouristsInterceptTimeTrend() throws Exception {
        ModelData rModel = ModelReader.read("austourists");
        double[] x = Utils.toArray(rModel.getData());

        AugmentedDickeyFullerTest augmentedDickeyFullerTest = new AugmentedDickeyFullerTest(x, 1,
                AugmentedDickeyFullerTest.Type.InterceptTimeTrend);
        double statistics = augmentedDickeyFullerTest.statistics();
        double pValue = augmentedDickeyFullerTest.pValue();

        Assert.assertEquals(-7.4083, statistics, 0.001);
        Assert.assertTrue(pValue <= 0.01);
    }

    @Test
    public void testAustourists4Lags() throws IOException {
        ModelData rModel = ModelReader.read("austourists");
        double[] x = Utils.toArray(rModel.getData());

        AugmentedDickeyFullerTest augmentedDickeyFullerTest = new AugmentedDickeyFullerTest(x, 4,
                AugmentedDickeyFullerTest.Type.InterceptTimeTrend);
        double statistics = augmentedDickeyFullerTest.statistics();
        double pValue = augmentedDickeyFullerTest.pValue();

        Assert.assertEquals(-1.8381, statistics, 0.001);
        Assert.assertEquals(0.6388, pValue, 0.05);
    }

    @Test
    public void testAustourists4LagsInterceptNoTimeTrend() throws IOException {
        ModelData rModel = ModelReader.read("austourists");
        double[] x = Utils.toArray(rModel.getData());

        AugmentedDickeyFullerTest augmentedDickeyFullerTest = new AugmentedDickeyFullerTest(x, 4,
                AugmentedDickeyFullerTest.Type.InterceptNoTimeTrend);
        double statistics = augmentedDickeyFullerTest.statistics();
        double pValue = augmentedDickeyFullerTest.pValue();

        Assert.assertEquals(-0.0625, statistics, 0.001);
        Assert.assertEquals(0.9451, pValue, 0.02);
    }

    @Test
    public void testAustouristsNoInterceptNoTimeTrend() throws Exception {
        ModelData rModel = ModelReader.read("austourists");
        double[] x = Utils.toArray(rModel.getData());

        AugmentedDickeyFullerTest augmentedDickeyFullerTest = new AugmentedDickeyFullerTest(x, 1,
                AugmentedDickeyFullerTest.Type.NoInterceptNoTimeTrend);
        double statistics = augmentedDickeyFullerTest.statistics();
        double pValue = augmentedDickeyFullerTest.pValue();

        System.out.println(AugmentedDickeyFullerTest.Type.NoInterceptNoTimeTrend.name() + "=" + statistics +
            ", pValue=" + pValue);

        Assert.assertEquals(-0.0206, statistics, 0.001);
        Assert.assertEquals(0.6042, pValue, 0.1);
    }

    @Test
    public void testAustouristsInterceptNoTimeTrend() throws Exception {
        ModelData rModel = ModelReader.read("austourists");
        double[] x = Utils.toArray(rModel.getData());

        AugmentedDickeyFullerTest augmentedDickeyFullerTest = new AugmentedDickeyFullerTest(x, 1,
                AugmentedDickeyFullerTest.Type.InterceptNoTimeTrend);
        double statistics = augmentedDickeyFullerTest.statistics();
        double pValue = augmentedDickeyFullerTest.pValue();

        System.out.println(AugmentedDickeyFullerTest.Type.InterceptNoTimeTrend.name() + "=" + statistics +
            ", pValue=" + pValue);

        Assert.assertEquals(-3.1384, statistics, 0.001);
        Assert.assertEquals(0.03304, pValue, 0.01);
    }

    @Test
    public void testTrendStationary() throws IOException {
        ModelData rModel = ModelReader.read("trendStatDownwardLowVar");
        double[] x = Utils.toArray(rModel.getData());

        AugmentedDickeyFullerTest augmentedDickeyFullerTest = new AugmentedDickeyFullerTest(x, 1,
                AugmentedDickeyFullerTest.Type.InterceptNoTimeTrend);
        double statistics = augmentedDickeyFullerTest.statistics();
        double pValue = augmentedDickeyFullerTest.pValue();

        System.out.println(AugmentedDickeyFullerTest.Type.InterceptNoTimeTrend.name() + "=" + statistics +
                ", pValue=" + pValue);

        Assert.assertEquals(0.8683, pValue, 0.09);
    }

    @Test
    public void testStationary() throws IOException {
        ModelData rModel = ModelReader.read("wnLowVariance");
        double[] x = Utils.toArray(rModel.getData());

        AugmentedDickeyFullerTest augmentedDickeyFullerTest = new AugmentedDickeyFullerTest(x, 1,
                AugmentedDickeyFullerTest.Type.InterceptNoTimeTrend);
        double statistics = augmentedDickeyFullerTest.statistics();
        double pValue = augmentedDickeyFullerTest.pValue();

        System.out.println(AugmentedDickeyFullerTest.Type.InterceptNoTimeTrend.name() + "=" + statistics +
                ", pValue=" + pValue);

        Assert.assertTrue(pValue <= 0.01);
    }
}
