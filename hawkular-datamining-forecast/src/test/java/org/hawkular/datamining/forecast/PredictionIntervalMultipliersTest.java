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

package org.hawkular.datamining.forecast;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class PredictionIntervalMultipliersTest {

    @Test
    public void testMultiplier() {
        Assert.assertEquals(0.67d, PredictionIntervalMultipliers.multiplier(50), 0.01);
        Assert.assertEquals(1.28d, PredictionIntervalMultipliers.multiplier(80), 0.01);
        Assert.assertEquals(1.96d, PredictionIntervalMultipliers.multiplier(95), 0.01);

        Assert.assertEquals(0d, PredictionIntervalMultipliers.multiplier(0), 0.01);
        Assert.assertEquals(Double.POSITIVE_INFINITY, PredictionIntervalMultipliers.multiplier(100), 0.01);
    }
}
