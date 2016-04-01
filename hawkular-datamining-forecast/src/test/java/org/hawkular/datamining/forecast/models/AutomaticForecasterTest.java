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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.AutomaticForecaster;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Forecaster;
import org.hawkular.datamining.forecast.ImmutableMetricContext;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;
import org.hawkular.datamining.forecast.stats.InformationCriterionHolder;
import org.hawkular.datamining.forecast.utils.AutomaticPeriodIdentification;
import org.hawkular.datamining.forecast.utils.Utils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class AutomaticForecasterTest extends AbstractTest {

    // in % - MSE of the model is within x % to model estimated by R ets()
    private double ACCURACY_LOW = 0.65;
    private double ACCURACY_HIGH = 1.03;


    @Test
    public void testModelSelectionNonSeasonal() {

        nonSeasonalModels.forEach(test -> {
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
            } catch (IOException e) {
                Assert.fail();
            }
        });
    }

    @Test
    public void testModelSelectionSeasonal() {

        double ACCURACY_LOW = 0.65;
        double ACCURACY_HIGH = 1.4;

        seasonalModels.forEach(test -> {
            try {
                ModelData rModel = ModelReader.read(test);

                Forecaster forecaster = new AutomaticForecaster(new ImmutableMetricContext("", rModel.getName(), 1L));
                forecaster.learn(rModel.getData());

                Assert.assertTrue("Model should be always selected", forecaster.model() != null);

                AccuracyStatistics initStatistics = forecaster.model().initStatistics();
                System.out.println(initStatistics);

                Assert.assertEquals(test + ", extected: " + rModel.getModel() + ", MSE=" + rModel.getMse(),
                        rModel.getModel(), forecaster.model().getClass());

                assertThat(initStatistics.getMse()).withFailMessage("rModel: %s", rModel)
                        .isBetween(rModel.getMse()*ACCURACY_LOW, rModel.getMse()*ACCURACY_HIGH);
            } catch (IOException e) {
                Assert.fail();
            }
        });
    }

    @Test
    public void testVariableDataLength() {
        double ACCURACY_LOW = 0.65;
        double ACCURACY_HIGH = 1.10;

        nonSeasonalModels.forEach(test -> {
            try {
                ModelData rModel = ModelReader.read(test);

                int runs = 5;
                while (runs-- > 0) {
                    AutomaticForecaster.PeriodicIntervalStrategy periodicStrategy =
                            new AutomaticForecaster.PeriodicIntervalStrategy(25);


                    int dataSize = ThreadLocalRandom.current().nextInt(periodicStrategy.getPeriod() + 50,
                            rModel.getData().size() + 1);
                    System.out.format("%s random sample size (0, %d)\n", rModel.getName(), dataSize);

                    Forecaster forecaster =
                            new AutomaticForecaster(new ImmutableMetricContext("", rModel.getName(), 1L),
                                    periodicStrategy);
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


    @Test
    public void testConceptDriftPeriodicStrategy() throws IOException {
        ModelData wnLowVariance = ModelReader.read("wnLowVariance");
        ModelData trendStationary = ModelReader.read("trendStatUpwardLowVar");
        ModelData sineStationary = ModelReader.read("sineTrendLowVar");
        ModelData trendStationaryDownward = ModelReader.read("trendStatDownwardLowVar");

        Forecaster periodicForecaster = new AutomaticForecaster(new ImmutableMetricContext("tenant",
                wnLowVariance.getName() + trendStationary.getName() + sineStationary.getName(), 1L));

        learn(periodicForecaster, wnLowVariance);
        learn(periodicForecaster, sineStationary);
        learn(periodicForecaster, trendStationary);
        learn(periodicForecaster, trendStationaryDownward);
    }

    @Test
    public void testConceptDriftStatStrategy() throws IOException {
        ModelData wnLowVariance = ModelReader.read("wnLowVariance");
        ModelData trendStationary = ModelReader.read("trendStatUpwardLowVar");
        ModelData sineStationary = ModelReader.read("sineTrendLowVar");
        ModelData trendStationaryDownward = ModelReader.read("trendStatDownwardLowVar");

        Forecaster statisticsForecaster = new AutomaticForecaster(
                new ImmutableMetricContext("tenant", wnLowVariance.getName()+ trendStationary.getName() +
                        sineStationary.getName(), 1L),
                new AutomaticForecaster.ErrorChangeStrategy(20, AutomaticForecaster.ErrorChangeStrategy.Statistics.MAE));

        learn(statisticsForecaster, wnLowVariance);
        learn(statisticsForecaster, sineStationary);
        learn(statisticsForecaster, trendStationary);
        learn(statisticsForecaster, trendStationaryDownward);
        learn(statisticsForecaster, wnLowVariance);
    }

    @Test
    public void test() throws IOException {
        ModelData trendStationary = ModelReader.read("trendStatUpwardLowVar");
        ModelData sineTrendStationary = ModelReader.read("sineTrendLowVar");

        List<DataPoint> trainData = sineTrendStationary.getData().subList(43, 94);

        int periods = AutomaticPeriodIdentification.periods(trainData);
        System.out.println("\n\nperiods = " + periods);


        System.out.println(Arrays.toString(Utils.toArray(trainData)));
        TimeSeriesModel seasonalModel = TripleExponentialSmoothing.optimizer(20).minimizedMSE(trainData);
        InformationCriterionHolder criterionHolder =
                new InformationCriterionHolder(seasonalModel.initStatistics().getSse(), seasonalModel.numberOfParams(),
                        trainData.size());
        System.out.println("\n\nDefault:" + seasonalModel);
        System.out.println("accuracy:" + seasonalModel.initStatistics() + criterionHolder + "\n\n");

        TimeSeriesModel trendModel = DoubleExponentialSmoothing.optimizer().minimizedMSE(trainData);
        InformationCriterionHolder trendCriterionHolder =
                new InformationCriterionHolder(trendModel.initStatistics().getSse(), trendModel.numberOfParams(),
                        trainData.size());
        System.out.println("\n\nDefault:" + trendModel);
        System.out.println("accuracy:" + trendModel.initStatistics() + trendCriterionHolder + "\n\n");

        AutomaticForecaster automaticForecaster = new AutomaticForecaster(new ImmutableMetricContext("tenant",
                sineTrendStationary.getName(), 1L));
        automaticForecaster.learn(trainData);
        System.out.println("AutomaticForecaster = " + automaticForecaster.model());
        periods = AutomaticPeriodIdentification.periods(trainData);
        System.out.println("\n\nperiods = " + periods);

        Forecaster periodicForecaster = new AutomaticForecaster(new ImmutableMetricContext("tenant",
                sineTrendStationary.getName(), 1L));

        learn(periodicForecaster, trendStationary);
        learn(periodicForecaster, sineTrendStationary);
    }

    private void learn(Forecaster forecaster, ModelData model) {

        long startTimestamp = forecaster.lastTimestamp();

        for (DataPoint point: model.getData()) {
            DataPoint pointToLearn = new DataPoint(point.getValue(), startTimestamp++);
            System.out.println(startTimestamp);
            forecaster.learn(pointToLearn);
        }

        Assert.assertEquals(model.getModel(), forecaster.model().getClass());
    }
}
