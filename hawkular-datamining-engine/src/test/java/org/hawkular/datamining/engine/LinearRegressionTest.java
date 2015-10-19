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

    public LinearRegressionTest() throws IOException {
        super();
    }

    @Test
    public void testSimpleData() {
        final double GRADIENT_DESCENT_STEP = 0.011;
        final int NUMBER_OF_ITERATIONS = 10000;
        final String DATA_FILE = "simple-data.txt";
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL testFile = classLoader.getResource(DATA_FILE);

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
//        linearRegressionWithSGD.optimizer().setUpdater(new ANNUpdater());
        linearRegressionWithSGD.optimizer().setNumIterations(NUMBER_OF_ITERATIONS);
        linearRegressionWithSGD.optimizer().setStepSize(GRADIENT_DESCENT_STEP);
        linearRegressionWithSGD.setIntercept(true);
//
        LinearRegressionModel customModel = linearRegressionWithSGD.run(parsedData.rdd());

        LabeledPoint newPoint = new LabeledPoint(0, Vectors.dense(66));
        LabeledPoint newPoint2 = new LabeledPoint(0, Vectors.dense(0));
        JavaRDD<LabeledPoint> rddToAdd = sparkContext.parallelize(Arrays.asList(newPoint));
        JavaRDD<LabeledPoint> newData = parsedData.union(rddToAdd)
                .union(sparkContext.parallelize(Arrays.asList(newPoint2)));

        // Evaluate model on training examples and compute training error
        JavaRDD<Tuple2<Double, Double>> valuesAndPreds = newData.map(x -> {
                    double prediction = customModel.predict(x.features());

                    return new Tuple2<>(prediction, x.features().toArray()[0]);
                }
        );

        valuesAndPreds.foreach(x -> System.out.println(x._2() + ", " + x._1()));
        sparkContext.close();
    }


    @Test
    public void testHeapData() {
        double STEP_SIZE = 0.00000000000000000011;
//        double STEP_SIZE = 0.0000000000000000000000005;
//        double STEP_SIZE = 0.005;
        int NUMBER_OF_ITERATIONS = 100;
        String DATA_FILE = "heap-data.txt";
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL testFile = classLoader.getResource(DATA_FILE);


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
//        linearRegressionWithSGD.optimizer().setUpdater(new ANNUpdater());
        linearRegressionWithSGD.optimizer().setNumIterations(NUMBER_OF_ITERATIONS);
        linearRegressionWithSGD.optimizer().setStepSize(STEP_SIZE);
        linearRegressionWithSGD.setIntercept(true);


        // train
        LinearRegressionModel customModel = linearRegressionWithSGD.run(parsedData.rdd());


        // create data - predict on
        LabeledPoint newPoint = new LabeledPoint(0, Vectors.dense(66));
        LabeledPoint newPoint2 = new LabeledPoint(0, Vectors.dense(0));
        JavaRDD<LabeledPoint> rddToAdd = sparkContext.parallelize(Arrays.asList(newPoint));
        JavaRDD<LabeledPoint> newData = parsedData.union(rddToAdd)
                .union(sparkContext.parallelize(Arrays.asList(newPoint2)));

        // Evaluate model on training examples and compute training error
        JavaRDD<Tuple2<Double, Double>> valuesAndPreds = newData.map(labeledPoint -> {
                    double prediction = customModel.predict(labeledPoint.features());

                    return new Tuple2<>(prediction, labeledPoint.features().toArray()[0]);
                }
        );

        valuesAndPreds.foreach(x -> System.out.println(x._2() + ", " + x._1()));
        sparkContext.close();
    }

    @Test
    public void testHeapDataNormalized() {
        double STEP_SIZE = 0.000001;
        int NUMBER_OF_ITERATIONS = 50;
        String DATA_FILE = "heap-data-normalized.txt";
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL testFile = classLoader.getResource(DATA_FILE);


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
//        linearRegressionWithSGD.optimizer().setUpdater(new ANNUpdater());
        linearRegressionWithSGD.optimizer().setNumIterations(NUMBER_OF_ITERATIONS);
        linearRegressionWithSGD.optimizer().setStepSize(STEP_SIZE);
        linearRegressionWithSGD.setIntercept(true);


        // train
        LinearRegressionModel customModel = linearRegressionWithSGD.run(parsedData.rdd());


        // create data - predict on
        LabeledPoint newPoint = new LabeledPoint(0, Vectors.dense(66));
        LabeledPoint newPoint2 = new LabeledPoint(0, Vectors.dense(0));
        JavaRDD<LabeledPoint> rddToAdd = sparkContext.parallelize(Arrays.asList(newPoint));
        JavaRDD<LabeledPoint> newData = parsedData.union(rddToAdd)
                .union(sparkContext.parallelize(Arrays.asList(newPoint2)));

        // Evaluate model on training examples and compute training error
        JavaRDD<Tuple2<Double, Double>> valuesAndPreds = newData.map(labeledPoint -> {
                    double prediction = customModel.predict(labeledPoint.features());

                    return new Tuple2<>(prediction, labeledPoint.features().toArray()[0]);
                }
        );

        valuesAndPreds.foreach(x -> System.out.println(x._2() + ", " + x._1()));
        sparkContext.close();
    }
}
