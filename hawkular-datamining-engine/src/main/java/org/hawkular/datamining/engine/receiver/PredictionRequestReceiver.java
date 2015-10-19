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

package org.hawkular.datamining.engine.receiver;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.jms.JMSException;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.dataminig.api.EngineDataReceiver;
import org.hawkular.dataminig.api.model.PredictionRequest;
import org.hawkular.datamining.bus.BusConfiguration;
import org.hawkular.datamining.bus.listener.PredictionRequestListener;
import org.hawkular.datamining.engine.EngineLogger;

/**
 * @author Pavol Loffay
 */
public class PredictionRequestReceiver extends Receiver<PredictionRequest>
        implements EngineDataReceiver<PredictionRequest> {

    private static StorageLevel storageLevel = StorageLevel.MEMORY_ONLY();

    private PredictionRequestListener predictionRequestListener;
    private ConsumerConnectionContext consumerConnectionContext;


    public PredictionRequestReceiver() {
        super(storageLevel);
    }

    @Override
    public StorageLevel storageLevel() {
        return storageLevel;
    }

    @Override
    public void store(PredictionRequest data) {
//        super.store(data);
        super.store(modifyRequest(data));
    }

    @Override
    public void store(Collection<PredictionRequest> data) {

        super.store(data.stream().map(x -> modifyRequest(x)).collect(Collectors.toList()).iterator());
//        super.store(data.iterator());
    }

    private PredictionRequest modifyRequest(PredictionRequest original) {
        return new PredictionRequest(original.getRequestId(), original.getMetricId(), original.getTimestamp() -
                MetricDataReceiver.firstTimestamp);
    }

    @Override
    public void onStart() {
        try {
            ConnectionContextFactory factory = new ConnectionContextFactory(BusConfiguration.BROKER_URL);
            Endpoint endpoint = new Endpoint(Endpoint.Type.TOPIC, BusConfiguration.TOPIC_PREDICTION_REQUEST);
            this.consumerConnectionContext = factory.createConsumerConnectionContext(endpoint);

            MessageProcessor processor = new MessageProcessor();
            this.predictionRequestListener = new PredictionRequestListener(this);
            processor.listen(consumerConnectionContext, predictionRequestListener);

            EngineLogger.LOGGER.dataListenerStartInfo();
        } catch (JMSException e) {
            EngineLogger.LOGGER.dataListenerFailedStartError(this.getClass().getCanonicalName());
        }
    }

    @Override
    public void onStop() {

    }
}
