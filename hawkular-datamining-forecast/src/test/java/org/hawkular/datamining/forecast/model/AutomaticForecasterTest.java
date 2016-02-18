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

package org.hawkular.datamining.forecast.model;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.hawkular.datamining.forecast.AutomaticForecaster;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Forecaster;
import org.hawkular.datamining.forecast.ImmutableMetricContext;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class AutomaticForecasterTest {

    private static List<DataPoint> stationaryAr2Series;

    @BeforeClass
    public static void init() throws IOException {
        stationaryAr2Series = RTimeSeriesReader.getData("wnLowVariance.csv");
    }

    @Test
    public void testParametersEstimation() {
        Forecaster forecaster = new AutomaticForecaster(ImmutableMetricContext.getDefault());

        forecaster.learn(stationaryAr2Series.subList(0, 50));

        TimeSeriesModel model = forecaster.model();

        assertTrue(model instanceof SimpleExponentialSmoothing);
    }
}
