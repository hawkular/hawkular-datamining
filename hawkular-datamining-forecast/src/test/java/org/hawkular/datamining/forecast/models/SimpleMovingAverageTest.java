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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class SimpleMovingAverageTest extends AbstractTest {

    @Test
    public void testError() {
        try {
            new SimpleMovingAverage(Collections.emptyList(), 0, false);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            //ok
            System.out.println(ex.getMessage());
        }

        try {
            List<DataPoint> dataPoints = Arrays.asList(new DataPoint(0D, 0L), new DataPoint(0D,0L));
            new SimpleMovingAverage(dataPoints, 3, false).learn();
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            //ok
            System.out.println(ex.getMessage());
        }
    }

    @Test
    public void testResultLength() throws IOException {
        ModelData rModel = ModelReader.read("trendStatUpwardLowVar");

        SimpleMovingAverage simpleMovingAverage = new SimpleMovingAverage(rModel.getData(), 5, false);
        List<DataPoint> result = simpleMovingAverage.learn();
        int nullAtBeginning = nullAt(result, true);
        int nullAtEnd = nullAt(result, false);

        Assert.assertTrue(nullAtBeginning == 2);
        Assert.assertTrue(nullAtEnd == 2);

        simpleMovingAverage = new SimpleMovingAverage(rModel.getData(), 4, false);
        result = simpleMovingAverage.learn();
        nullAtBeginning = nullAt(result, true);
        nullAtEnd = nullAt(result, false);

        Assert.assertTrue(nullAtBeginning == 1);
        Assert.assertTrue(nullAtEnd == 2);

        simpleMovingAverage = new SimpleMovingAverage(rModel.getData(), 2, false);
        result = simpleMovingAverage.learn();
        nullAtBeginning = nullAt(result, true);
        nullAtEnd = nullAt(result, false);
        Assert.assertTrue(nullAtBeginning == 0);
        Assert.assertTrue(nullAtEnd == 1);
    }

    @Test
    public void testDoubleMovingAverages() throws IOException {
        ModelData rModel = ModelReader.read("trendStatUpwardLowVar");
        List<DataPoint> data = rModel.getData().subList(0, 15);


        SimpleMovingAverage simpleMovingAverage = new SimpleMovingAverage(data, 4, true);
        List<DataPoint> second = simpleMovingAverage.learn();
        int nullAtBeginning = nullAt(second, true);
        int nullAtEnd = nullAt(second, false);
        Assert.assertTrue(nullAtBeginning == 2);
        Assert.assertTrue(nullAtEnd == 2);
    }

    public static int nullAt(List<DataPoint> dataPoints, boolean start) {

        int counter = 0;
        for (int i = 0; i < dataPoints.size(); i++) {
            if (dataPoints.get(i).getValue() == null) {
                if (start && i < dataPoints.size() / 2) {
                    counter++;
                } else if (!start && i > dataPoints.size() / 2) {
                    counter++;
                }
            }
        }

        return counter;
    }
}
