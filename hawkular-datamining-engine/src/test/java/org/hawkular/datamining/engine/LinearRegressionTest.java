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

import org.apache.spark.api.java.JavaDoubleRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
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

    private static final String DATA_FILE = "lpsa.data.txt";
    private static final String SAVE_NAME = "REG_MODEL";
    private URL testFile;

    public LinearRegressionTest() throws IOException {
        super();

        ClassLoader classLoader = this.getClass().getClassLoader();
        testFile = classLoader.getResource(DATA_FILE);
    }

    @Test
    public void testLinearRegression() {
        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        // Load and parse the data
        JavaRDD<String> data = sparkContext.textFile(testFile.getPath());
        JavaRDD<LabeledPoint> parsedData = data.map(
                new Function<String, LabeledPoint>() {
                    public LabeledPoint call(String line) {
                        String[] parts = line.split(",");
                        String[] features = parts[1].split(" ");
                        double[] v = new double[features.length];
                        for (int i = 0; i < features.length - 1; i++)
                            v[i] = Double.parseDouble(features[i]);
                        return new LabeledPoint(Double.parseDouble(parts[0]), Vectors.dense(v));
                    }
                }
        );
        parsedData.cache();

        // Building the model
        int numIterations = 100;
        final LinearRegressionModel model = LinearRegressionWithSGD.train(JavaRDD.toRDD(parsedData), numIterations);

        // Evaluate model on training examples and compute training error
        JavaRDD<Tuple2<Double, Double>> valuesAndPreds = parsedData.map(
                new Function<LabeledPoint, Tuple2<Double, Double>>() {
                    public Tuple2<Double, Double> call(LabeledPoint point) {
                        double prediction = model.predict(point.features());
                        System.out.println(prediction);
                        return new Tuple2<Double, Double>(prediction, point.label());
                    }
                }
        );
        double MSE = new JavaDoubleRDD(valuesAndPreds.map(
                new Function<Tuple2<Double, Double>, Object>() {
                    public Object call(Tuple2<Double, Double> pair) {
                        return Math.pow(pair._1() - pair._2(), 2.0);
                    }
                }
        ).rdd()).mean();

        // Save and load model
//        model.save(sparkContext.sc(), SAVE_NAME);
//        LinearRegressionModel sameModel = LinearRegressionModel.load(sparkContext.sc(), SAVE_NAME);

        System.out.println("training Mean Squared Error = " + MSE);
        valuesAndPreds.foreach(x -> System.out.println(x));
        sparkContext.close();
    }
}
