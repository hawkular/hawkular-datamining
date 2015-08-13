/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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

package org.hawkular.datamining.engine;

import java.io.IOException;
import java.net.URL;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.regression.LinearRegressionModel;
import org.apache.spark.mllib.regression.LinearRegressionWithSGD;
import org.junit.Test;

import scala.Tuple2;

/**
 * @author Pavol Loffay
 */
public class LinearRegressionTest extends BaseTest {

    private static final String DATA_FILE = "simple-data.txt";

    private static final double GRADIENT_DESCENT_STEP = 0.01;
    private static final int NUMBER_OF_ITERATIONS = 100;

    private URL testFile;

    public LinearRegressionTest() throws IOException {
        super();

        ClassLoader classLoader = this.getClass().getClassLoader();
        testFile = classLoader.getResource(DATA_FILE);
    }

    @Test
    public void testSimpleData() {

        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        // Load and parse the data
        JavaRDD<String> data = sparkContext.textFile(testFile.getPath());
        JavaRDD<LabeledPoint> parsedData = data.map(line -> {
                    String[] parts = line.split(",");
                    return new LabeledPoint(Double.parseDouble(parts[1]), Vectors.dense(Double.parseDouble(parts[0])));
                }
        );
        parsedData.cache();
        parsedData.foreach(x -> System.out.println("label " + x.label() + " feature" + x.features()));

        LinearRegressionModel regressionModel = LinearRegressionWithSGD.train(parsedData.rdd(),
                NUMBER_OF_ITERATIONS,
                GRADIENT_DESCENT_STEP);

        // Evaluate model on training examples and compute training error
        JavaRDD<Tuple2<Double, Double>> valuesAndPreds = parsedData.map(x -> {
                    double prediction = regressionModel.predict(x.features());

//                    System.out.println("Label = " + x.label());
//                    System.out.println("Prediction = " + prediction);

                    return new Tuple2<Double, Double>(prediction, x.features().toArray()[0]);
                }
        );

        valuesAndPreds.foreach(x -> System.out.println(x._2() + ", " + x._1()));
        sparkContext.close();
    }
}
