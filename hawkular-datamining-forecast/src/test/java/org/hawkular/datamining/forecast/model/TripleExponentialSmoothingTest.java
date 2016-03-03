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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.IOException;

import org.hawkular.datamining.forecast.AccuracyStatistics;
import org.hawkular.datamining.forecast.model.r.ModelData;
import org.hawkular.datamining.forecast.model.r.ModelReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class TripleExponentialSmoothingTest extends AbstractTest {

    @Test
    public void testInit() throws IOException {
        ModelData rModel = ModelReader.read("sineLowVar");

        TripleExponentialSmoothing tripleExponentialSmoothing = new TripleExponentialSmoothing(20);

        AccuracyStatistics initStat = tripleExponentialSmoothing.init(rModel.getData());
    }

    @Test
    public void testSineLowVarShort() throws IOException {
        double ACCURACY_LOW = 0.05;
        double ACCURACY_HIGH = 1.05;

        ModelData rModel = ModelReader.read("sineLowVarShort");

        TimeSeriesModel seasonalModel = TripleExponentialSmoothing.optimizer()
                .setSeasons(20).minimizedMSE(rModel.getData());
        AccuracyStatistics seasonalStat = seasonalModel.initStatistics();

        TimeSeriesModel doubleExpSmoot = DoubleExponentialSmoothing.optimizer().minimizedMSE(rModel.getData());
        AccuracyStatistics doubleExpSmootStat = doubleExpSmoot.initStatistics();

        System.out.println("Seasonal: " + seasonalStat);
        System.out.println(seasonalModel);

        Assert.assertTrue(seasonalStat.getMse() < doubleExpSmootStat.getMse());
        assertThat(seasonalStat.getMse()).withFailMessage("rModel: %s", rModel)
                .isBetween(rModel.getMse()*ACCURACY_LOW,rModel.getMse()*ACCURACY_HIGH);
    }

    @Test
    public void testSineLowVarLong() throws IOException {
        double ACCURACY_LOW = 0.05;
        double ACCURACY_HIGH = 1.05;

        ModelData rModel = ModelReader.read("sineLowVarLong");

        TimeSeriesModel seasonalModel = TripleExponentialSmoothing.optimizer()
                .setSeasons(20).minimizedMSE(rModel.getData());
        AccuracyStatistics seasonalStat = seasonalModel.initStatistics();

        TimeSeriesModel doubleExpSmoot = DoubleExponentialSmoothing.optimizer().minimizedMSE(rModel.getData());
        AccuracyStatistics doubleExpSmootStat = doubleExpSmoot.initStatistics();

        System.out.println("Seasonal: " + seasonalStat);
        System.out.println(seasonalModel);

        Assert.assertTrue(seasonalStat.getMse() < doubleExpSmootStat.getMse());
        assertThat(seasonalStat.getMse()).withFailMessage("rModel: %s", rModel)
                .isBetween(rModel.getMse()*ACCURACY_LOW,rModel.getMse()*ACCURACY_HIGH);
    }

    @Test
    public void testAustourists() throws IOException {
        double ACCURACY_LOW = 0.05;
        double ACCURACY_HIGH = 1.05;

        ModelData rModel = ModelReader.read("austourists");

        TimeSeriesModel seasonalModel = TripleExponentialSmoothing.optimizer()
                .setSeasons(4).minimizedMSE(rModel.getData());
        AccuracyStatistics seasonalStat = seasonalModel.initStatistics();

        System.out.println("Seasonal: " + seasonalStat);
        System.out.println(seasonalModel);

        assertThat(seasonalStat.getMse()).withFailMessage("rModel: %s", rModel)
                .isBetween(rModel.getMse()*ACCURACY_LOW,rModel.getMse()*ACCURACY_HIGH);
    }
}
