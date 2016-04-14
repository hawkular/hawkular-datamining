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

package org.hawkular.datamining.itest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.forecast.AutomaticForecaster;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Forecaster;
import org.hawkular.datamining.forecast.models.Model;
import org.hawkular.datamining.forecast.stats.InformationCriterion;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.squareup.okhttp.Response;

/**
 * @author Pavol Loffay
 */
public class HawkularDataminingITest extends AbstractITest {

    private static final String tenant = "jdoe";
    private static final String metricId = "metric1";

    @Test
    public void testPing() throws IOException {

        Response response = get("");

        assertThat(response.code(), is(200));
    }

    @Test
    public void testModelLearnAndPredict() throws Throwable {
        // create
        Metric.RestBlueprint blueprint = new Metric.RestBlueprint(metricId, 100L);
        postNewEntity("metrics", tenant, blueprint);

        // get
        Response responseGet = get("metrics", tenant);
        assertThat(responseGet.code(), is(200));

        // learn
        List<DataPoint> dataPoints = dataPoints(1L, 60);
        Response responseLearn = post("metrics/" + metricId + "/forecaster/learn", tenant, dataPoints);
        assertThat(responseLearn.code(), is(204));

        // predict
        int ahead = 5;
        Response responsePredict = get("metrics/" + metricId + "/forecaster/forecast?ahead=" + ahead, tenant);
        assertThat(responsePredict.code(), is(200));
        List<DataPoint> predicted = parseResponseBody(responsePredict, new TypeReference<List<DataPoint>>() {});
        assertThat(predicted.size(), is(ahead));
    }

    @Test
    public void testForecasterConfigUpdate() throws Throwable {

        String metricId = "metricForUpdate";

        // create
        Metric.RestBlueprint blueprint = new Metric.RestBlueprint(metricId, 100L);
        postNewEntity("metrics", tenant, blueprint);

        // get
        Response responseGet = get("metrics", tenant);
        assertThat(responseGet.code(), is(200));

        // learn
        List<DataPoint> dataPoints = dataPoints(1L, 60);
        Response responseLearn = post("metrics/" + metricId + "/forecaster/learn", tenant, dataPoints);
        assertThat(responseLearn.code(), is(204));

        //change
        Forecaster.Update update = new Forecaster.Update(55, Model.SimpleExponentialSmoothing,
                InformationCriterion.BIC, new AutomaticForecaster.ErrorChangeStrategy(19,
                AutomaticForecaster.ErrorChangeStrategy.Statistics.MSE));
        Response responsePut = put("metrics/" + metricId + "/forecaster", tenant, update);
        assertThat(responsePut.code(), is(204));
    }

    private List<DataPoint> dataPoints(long firstTimestamp, Double... values) {
        List<DataPoint> dataPoints = new ArrayList<>();

        for (int i = 0; i < values.length; i++) {
            DataPoint dataPoint = new DataPoint(values[i], firstTimestamp++);
            dataPoints.add(dataPoint);
        }

        return dataPoints;
    }

    private List<DataPoint> dataPoints(long firstTimestamp, int size) {
        List<DataPoint> dataPoints = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            DataPoint dataPoint = new DataPoint((double)i, firstTimestamp++);
            dataPoints.add(dataPoint);
        }

        return dataPoints;
    }
}
