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

import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.receiver.Receiver;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Pavol Loffay
 */
public class SparkEngine implements AnalyticEngine, Serializable {

    private Receiver<String> receiver;
    private JavaStreamingContext streamingContext;
    private Duration batchDuration = Durations.seconds(1);


    public SparkEngine(Receiver<String> receiver) throws IOException {

        Configuration configuration = new Configuration();

        this.receiver = receiver;
        this.streamingContext = new JavaStreamingContext(configuration.getSparkConf(), batchDuration);

        ObjectMapper objectMapper = new ObjectMapper();
        EngineLogger.LOGGER.debugf("Jackson Databind version:" + objectMapper.version().toString());
        EngineLogger.LOGGER.debug("Version should be 2.4.4");
    }

    @Override
    public void start() {

        JavaDStream<String> inputDStream = streamingContext.receiverStream(receiver);
        inputDStream.print();

        streamingContext.start();
//        streamingContext.awaitTermination();

        streamingContext.close();
        EngineLogger.LOGGER.startInfo();
    }

    @Override
    public void stop() {
        streamingContext.stop();
        EngineLogger.LOGGER.stopInfo();
    }
}
