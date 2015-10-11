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
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.spark.api.java.function.Function;
import org.apache.spark.ml.ann.ANNUpdater;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.regression.StreamingLinearRegressionWithSGD;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.receiver.Receiver;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.hawkular.dataminig.api.AnalyticEngine;
import org.hawkular.dataminig.api.model.MetricData;
import org.hawkular.dataminig.api.model.PredictionRequest;
import org.hawkular.dataminig.api.model.PredictionResult;
import org.hawkular.dataminig.api.model.TimeSeries;
import org.hawkular.datamining.bus.BusConfiguration;
import org.hawkular.datamining.bus.BusLogger;
import org.hawkular.datamining.bus.message.PredictionResultMessage;
import org.hawkular.datamining.engine.receiver.PredictionRequestReceiver;

import com.fasterxml.jackson.databind.ObjectMapper;

import scala.Tuple2;


/**
 * @author Pavol Loffay
 */
public class SparkEngine implements AnalyticEngine, Serializable {

    private static final double STEP_SIZE =  0.000000000001;
    private static final int ITERATIONS = 20;

    private Receiver<MetricData> metricDataReceiver;
    private Receiver<PredictionRequest> predictionRequestReceiver;

    private JavaStreamingContext streamingContext;
    private Duration batchDuration = Durations.seconds(5);

    private final Thread sparkJob;


    public SparkEngine(Receiver<MetricData> receiver, PredictionRequestReceiver predictionRequestReceiver) throws
            IOException {
        this.metricDataReceiver = receiver;
        this.predictionRequestReceiver = predictionRequestReceiver;

        EngineConfiguration configuration = new EngineConfiguration();
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


    private class StreamingJob implements Runnable, Serializable {


        @Override
        public void run() {
            Map<String, StreamingLinearRegressionWithSGD> modelInsideMethod = new HashMap<>();

            JavaDStream<MetricData> metricDataDStream = streamingContext.receiverStream(metricDataReceiver);
            metricDataDStream.cache();

            JavaDStream<LabeledPoint> streamToTrainOn = metricDataDStream.map(metricData -> new LabeledPoint(metricData
                    .getValue(), Vectors.dense(metricData.getTimestamp())));

            // construct model
            StreamingLinearRegressionWithSGD regressionWithSGD = getNewModel();
            regressionWithSGD.trainOn(streamToTrainOn);

            // predict
            JavaDStream<PredictionRequest> predictionRequestDStream =
                    streamingContext.receiverStream(predictionRequestReceiver);
            predictionRequestDStream.cache();

            JavaDStream<Vector> streamToPredict = predictionRequestDStream.map(request ->
                Vectors.dense(request.getTimestamp()));

            JavaDStream<Double> predictedValues = regressionWithSGD.predictOn(streamToPredict);

            /**
             * tuples
             */
            JavaPairDStream<PredictionRequest, Vector> streamToPredictTuple =
                    predictionRequestDStream.mapToPair(request -> {
                        return new Tuple2<PredictionRequest, Vector>(request, Vectors.dense(request.getTimestamp()));
                    });
            JavaPairDStream<PredictionRequest, Double> predictedValuesTupes =
                    regressionWithSGD.predictOnValues(streamToPredictTuple);

            /**
             * Predicted values output
             */
            predictedValues.foreach(rdd -> {
                rdd.foreach(value -> {
                    EngineLogger.LOGGER.debugf("Predicted value: %.2f", value);
                });
                return null;
            });
            /**
             * Predicted tuples output
             */
            predictedValuesTupes.foreach(rdd -> {

                rdd.foreachPartition(partitionOfRecord -> {

                    // create respond message
                    PredictionResult predictionResult = new PredictionResult();
                    partitionOfRecord.forEachRemaining(tuple -> {
                        PredictionRequest predictionRequest = tuple._1();
                        Double result = tuple._2();

                        TimeSeries timeSeries = new TimeSeries(tuple._2(), tuple._1().getTimestamp());

                        // TODO this repeats
                        predictionResult.setMetricId(predictionRequest.getMetricId());
                        predictionResult.setRequestId(predictionRequest.getRequestId());
                        predictionResult.addTimeSeries(timeSeries);
                    });

                    /**
                     * Send message
                     */
                    PredictionResultMessage message = new PredictionResultMessage(predictionResult);
                    try (ConnectionContextFactory ccf = new ConnectionContextFactory(BusConfiguration.BROKER_URL)) {

                        ProducerConnectionContext producerConnectionContext = ccf.createProducerConnectionContext(
                                new Endpoint(Endpoint.Type.TOPIC, BusConfiguration.TOPIC_PREDICTION_RESULT));

                        (new MessageProcessor()).send(producerConnectionContext, message);

                    } catch (JMSException e) {
                        BusLogger.LOGGER.failedToSendMessageError(message.toString());
                    }
                });

                rdd.foreach(tuple -> {
                    EngineLogger.LOGGER.debugf("Predicted metric %s, -> value: %.2f", tuple._1(), tuple._2());

                    EngineLogger.LOGGER.debugf("Sending prediction result to the bus");
                });
                return null;
            });

            streamingContext.start();
            streamingContext.awaitTermination();
            EngineLogger.LOGGER.debug("\n\n\n\n\nStreaming job stopped\n\n\n\n\n");
        }

        private JavaDStream<MetricData> a(JavaDStream<MetricData> stream, MetricData metricData) {

            final Function<MetricData, Boolean> p = x -> x.getId().equals(metricData.getId());
            JavaDStream<MetricData> XX = stream.filter(p);

            return XX;
        }



        public StreamingLinearRegressionWithSGD getNewModel() {
            StreamingLinearRegressionWithSGD model = new StreamingLinearRegressionWithSGD()
                    .setInitialWeights(Vectors.zeros(1));

            model.algorithm().optimizer().setNumIterations(ITERATIONS);
            model.algorithm().optimizer().setStepSize(STEP_SIZE);
            model.algorithm().optimizer().setUpdater(new ANNUpdater());

            return model;
        }
    }
}
