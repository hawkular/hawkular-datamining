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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.IOException;
import java.util.Collections;

import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class DoubleExponentialSmoothingTest extends AbstractTest {

    @Test
    public void testMinimalPoints() throws IOException {
        int minimalPoints = 2;

        ModelData rModel = ModelReader.read("trendStatUpwardLowVar");

        DoubleExponentialSmoothing.DoubleExOptimizer optimizer = DoubleExponentialSmoothing.optimizer();

        try {
            TimeSeriesModel model = optimizer.minimizedMSE(rModel.getData().subList(0, minimalPoints));
            Assert.assertTrue(model != null);
        } catch (IllegalArgumentException ex) {
            Assert.fail();
        }
    }

    @Test
    public void testBatchInitAndLearn() throws IOException {
        ModelData rModel = ModelReader.read("wnLowVariance");

        DoubleExponentialSmoothing.DoubleExOptimizer optimizer = DoubleExponentialSmoothing.optimizer();
        TimeSeriesModel modelInit = optimizer.minimizedMSE(rModel.getData());

        TimeSeriesModel modelLearn = DoubleExponentialSmoothing.createWithSmoothingParams(optimizer.result()[0], optimizer.result()[1]);
        modelLearn.learn(rModel.getData());

        AccuracyStatistics batchInitStatistics = modelInit.initStatistics();
        AccuracyStatistics batchLearnStatistics = modelLearn.runStatistics();

        Assert.assertEquals(batchInitStatistics, batchLearnStatistics);
    }

    @Test
    public void testContinuousLearning() throws IOException {
        ModelData rModel = ModelReader.read("trendStatUpwardLowVar");

        DoubleExponentialSmoothing.DoubleExOptimizer optimizer = DoubleExponentialSmoothing.optimizer();
        TimeSeriesModel modelInit = optimizer.minimizedMSE(rModel.getData());

        TimeSeriesModel continuousModel = new ContinuousModel(
                DoubleExponentialSmoothing.createWithSmoothingParams(optimizer.result()[0], optimizer.result()[1]));

        rModel.getData().forEach(dataPoint -> continuousModel.learn(dataPoint));

        AccuracyStatistics batchInitStatistics = modelInit.initStatistics();
        AccuracyStatistics continuousLearnStatistics = continuousModel.runStatistics();

        Assert.assertTrue(continuousLearnStatistics.getMse() > batchInitStatistics.getMse());
    }

    @Test
    public void testEmpty() {
        try {
            TimeSeriesModel model = DoubleExponentialSmoothing.optimizer().minimizedMSE(Collections.EMPTY_LIST);
        } catch (Throwable ex) {
            Assert.fail();
        }
    }

    @Test
    public void testTrend() throws IOException {
        double ACCURACY_LOW = 0.95;
        double ACCURACY_HIGH = 1.05;
        ModelData rModel = ModelReader.read("trendStatUpwardLowVar");

        DoubleExponentialSmoothing.DoubleExOptimizer optimizer = DoubleExponentialSmoothing.optimizer();
        TimeSeriesModel model = optimizer.minimizedMSE(rModel.getData());
        AccuracyStatistics initStatistics = model.initStatistics();

        assertThat(initStatistics.getMse()).withFailMessage("rModel: %s\nMy: %s\n%s", rModel,
                model, initStatistics)
                .isBetween(rModel.getMse()*ACCURACY_LOW,rModel.getMse()*ACCURACY_HIGH);
    }

    @Test
    public void testStationary() throws IOException {
        double ACCURACY_LOW = 0.95;
        double ACCURACY_HIGH = 1.05;
        ModelData rModel = ModelReader.read("wnHighVariance");

        DoubleExponentialSmoothing.DoubleExOptimizer optimizer = DoubleExponentialSmoothing.optimizer();
        TimeSeriesModel model = optimizer.minimizedMSE(rModel.getData());
        AccuracyStatistics initStatistics = model.initStatistics();

        assertThat(initStatistics.getMse()).withFailMessage("rModel: %s\nMy: %s\n%s", rModel,
                model, initStatistics)
                .isBetween(rModel.getMse()*ACCURACY_LOW,rModel.getMse()*ACCURACY_HIGH);
    }
}
