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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.hawkular.datamining.forecast.AccuracyStatistics;
import org.hawkular.datamining.forecast.AutomaticForecaster;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Forecaster;
import org.hawkular.datamining.forecast.ImmutableMetricContext;
import org.hawkular.datamining.forecast.model.r.ModelData;
import org.hawkular.datamining.forecast.model.r.ModelReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class AutomaticForecasterTest extends AbstractTest {

    // in % - MSE of the model is within x % to model estimated by R ets()
    private double ACCURACY_LOW = 0.75;
    private double ACCURACY_HIGH = 1.05;

    private List<String> tests = Arrays.asList("wnLowVariance", "wnHighVariance",
            "trendStatUpwardLowVar", "trendStatUpwardHighVar",
            "trendStatDownwardLowVar", "trendStatDownwardHighVar");

    @Test
    public void testModelSelection() {

        ACCURACY_LOW = 0.65;
        ACCURACY_HIGH = 1.03;
        tests.forEach(test -> {
            try {
                ModelData rModel = ModelReader.read(test);

                Forecaster forecaster = new AutomaticForecaster(new ImmutableMetricContext("", rModel.getName(), 1L));
                forecaster.learn(rModel.getData());

                Assert.assertTrue("Model should be always selected", forecaster.model() != null);

                AccuracyStatistics initStatistics = forecaster.model().initStatistics();
                System.out.println(initStatistics);

                // mse should be in some range (better than R's fit)
                assertThat(initStatistics.getMse()).withFailMessage("rModel: %s", rModel)
                        .isBetween(rModel.getMse()*ACCURACY_LOW, rModel.getMse()*ACCURACY_HIGH);

                // model should be "same" with R's estimation
//                Assert.assertEquals(test + ", extected: " + rModel.getModel() + ", MSE=" + rModel.getMse(),
//                        rModel.getModel(), forecaster.model().getClass());

            } catch (IOException e) {
                Assert.fail();
            }
        });
    }

    @Test
    public void testVariableDataLength() {
        double ACCURACY_LOW = 0.65;
        double ACCURACY_HIGH = 1.10;

        tests.forEach(test -> {
            try {
                ModelData rModel = ModelReader.read(test);

                int runs = 5;
                while (runs-- > 0) {
                    int dataSize = ThreadLocalRandom.current().nextInt(AutomaticForecaster.WINDOW_SIZE + 50,
                            rModel.getData().size() + 1);
                    System.out.format("%s random sample size (0, %d)\n", rModel.getName(), dataSize);

                    Forecaster forecaster =
                            new AutomaticForecaster(new ImmutableMetricContext("", rModel.getName(), 1L));
                    List<DataPoint> dataSubSet = rModel.getData().subList(0, dataSize);
                    forecaster.learn(dataSubSet);

                    assertThat(forecaster.model().initStatistics().getMse()).withFailMessage("rModel: %s", rModel)
                            .isBetween(rModel.getMse()*ACCURACY_LOW,rModel.getMse()*ACCURACY_HIGH);
                }
            } catch (IOException e) {
                Assert.fail("IOException, test file is probably missing");
            }
        });
    }

    @Test
    public void testContinualModelSelectionSimpleEx() throws IOException {
        ModelData rModel = ModelReader.read("wnLowVariance");

        Forecaster forecaster =
                new AutomaticForecaster(new ImmutableMetricContext("", rModel.getName(), 1L));

        rModel.getData().forEach(dataPoint -> {
            forecaster.learn(dataPoint);

            Assert.assertEquals("extected: " + rModel.getModel() + ", MSE=" + rModel.getMse(),
                    rModel.getModel(), forecaster.model().getClass());
        });
    }

    @Test
    public void testContinualModelSelectionDoubleEx() throws IOException {
        ModelData rModel = ModelReader.read("trendStatUpwardLowVar");

        Forecaster forecaster =
                new AutomaticForecaster(new ImmutableMetricContext("", rModel.getName(), 1L));

        int counter = 0;
        for (DataPoint dataPoint: rModel.getData()) {
            forecaster.learn(dataPoint);

            if (counter++ < 51) {
                continue;
            }

            Assert.assertEquals("extected: " + rModel.getModel() + ", MSE=" + rModel.getMse(),
                    rModel.getModel(), forecaster.model().getClass());
        }
    }

    @Test
    public void testEmptyDataSet() throws IOException {

        Forecaster forecaster =
                new AutomaticForecaster(new ImmutableMetricContext("", "empty", 1L));

        try {
            forecaster.learn(Collections.emptyList());
        } catch (Throwable ex) {
            Assert.fail();
        }

        ModelData rModel = ModelReader.read("trendStatUpwardLowVar");

        rModel.getData().forEach(dataPoint -> forecaster.learn(dataPoint));
    }
}
