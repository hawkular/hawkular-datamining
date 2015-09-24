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
import java.io.Serializable;

import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.regression.StreamingLinearRegressionWithSGD;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.receiver.Receiver;
import org.hawkular.dataminig.api.AnalyticEngine;
import org.hawkular.dataminig.api.model.MetricData;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author Pavol Loffay
 */
public class SparkEngine implements AnalyticEngine, Serializable {

    private Receiver<?> receiver;
    private JavaStreamingContext streamingContext;
    private Duration batchDuration = Durations.seconds(5);

    private final Thread sparkJob;


    public SparkEngine(Receiver<?> receiver) throws IOException {

        EngineConfiguration configuration = new EngineConfiguration();

        this.receiver = receiver;
        this.streamingContext = new JavaStreamingContext(configuration.getSparkConf(), batchDuration);
        sparkJob = new Thread(new StreamingJob());

        // log the version of databind, higher version can cause problems
        EngineLogger.LOGGER.jacksonDatabindVersion((new ObjectMapper()).version().toString());
    }

    @Override
    public void start() {
        sparkJob.start();
        EngineLogger.LOGGER.engineStartInfo();
    }

    @Override
    public void stop() {
        streamingContext.stop();
        EngineLogger.LOGGER.engineStopInfo();
    }

    private class StreamingJob implements Runnable {

        @Override
        public void run() {
            JavaDStream<MetricData> inputDStream = streamingContext.receiverStream((Receiver<MetricData>) receiver);
            inputDStream.cache();

            // transform to Labeled point
            JavaDStream<LabeledPoint> parsedData = inputDStream.map(metricData ->
                    new LabeledPoint(metricData.getValue(), Vectors.dense(metricData.getTimestamp())));
            parsedData.cache();

            StreamingLinearRegressionWithSGD model = new StreamingLinearRegressionWithSGD()
                    .setStepSize(0.1)
                    .setNumIterations(1000)
                    .setInitialWeights(Vectors.zeros(1));

            model.trainOn(parsedData);

            JavaDStream<Vector> predictOn = inputDStream.map(x -> Vectors.dense(10));
            predictOn.print();
            model.predictOn(predictOn).print();

            inputDStream.print(); //output operation
            parsedData.print();
            streamingContext.start();
            streamingContext.awaitTermination();

            EngineLogger.LOGGER.debug("\n\n\n\n\nStreaming job stopped\n\n\n\n\n");
        }
    }
}
