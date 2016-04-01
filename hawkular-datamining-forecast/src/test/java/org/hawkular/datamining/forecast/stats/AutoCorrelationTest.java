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

import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;
import org.hawkular.datamining.forecast.utils.Utils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class AutoCorrelationTest {

    @Test
    public void testCorrelation() throws Exception {
        ModelData modelData = ModelReader.read("austourists");
        double[] data = Utils.toArray(modelData.getData());

        double[] result = AutoCorrelationFunction.correlation(data, 6);

        double[] expected = {
                1.000, 0.334, 0.261, 0.292, 0.765, 0.208, 0.130
        };

        Assert.assertEquals(result.length, 7);
        Assert.assertEquals(result[0], 1D, 0.00001);

        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(expected[i], result[i], 0.0005);
        }
    }

    @Test
    public void testCovariance() throws Exception {
        ModelData modelData = ModelReader.read("austourists");
        double[] data = Utils.toArray(modelData.getData());

        double[] result = AutoCorrelationFunction.covariance(data, 6);

        double[] expected = {
                79.9, 26.7, 20.9, 23.4, 61.1, 16.6, 10.4
        };

        Assert.assertEquals(result.length, 7);

        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(expected[i], result[i], 0.05);
        }
    }
}
