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
import java.util.Arrays;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.ann.ANNUpdater;
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

    private static final String DATA_FILE = "simple-data-scaled.txt";

    private static final double GRADIENT_DESCENT_STEP = 0.000000000001;
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

        LinearRegressionWithSGD linearRegressionWithSGD = new LinearRegressionWithSGD();
        linearRegressionWithSGD.optimizer().setUpdater(new ANNUpdater());
        linearRegressionWithSGD.optimizer().setNumIterations(NUMBER_OF_ITERATIONS);
        linearRegressionWithSGD.optimizer().setStepSize(GRADIENT_DESCENT_STEP);

        LinearRegressionModel xxModel = linearRegressionWithSGD.run(parsedData.rdd());

//            StandardScaler standardScaler = new StandardScaler(true, rue).fit((RDD)parsedData.map(x -> x.features
// ()));


        LinearRegressionModel regressionModel = LinearRegressionWithSGD.train(parsedData.rdd(),
                NUMBER_OF_ITERATIONS,
                GRADIENT_DESCENT_STEP);


        LabeledPoint newPoint = new LabeledPoint(0, Vectors.dense(66));
        JavaRDD<LabeledPoint> rddToAdd = sparkContext.parallelize(Arrays.asList(newPoint));
        JavaRDD<LabeledPoint> newData = parsedData.union(rddToAdd);

        // Evaluate model on training examples and compute training error
        JavaRDD<Tuple2<Double, Double>> valuesAndPreds = newData.map(x -> {
                    double prediction = xxModel.predict(x.features());

//                    System.out.println("Label = " + x.label());
//                    System.out.println("Prediction = " + prediction);

                    return new Tuple2<>(prediction, x.features().toArray()[0]);
                }
        );

        valuesAndPreds.foreach(x -> System.out.println(x._2() + ", " + x._1()));
        sparkContext.close();
    }
}
