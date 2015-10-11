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

package org.hawkular.datamining.bus.listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;
import javax.jms.JMSException;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.dataminig.api.model.PredictionResult;
import org.hawkular.datamining.bus.BusConfiguration;
import org.hawkular.datamining.bus.BusLogger;
import org.hawkular.datamining.bus.message.PredictionResultMessage;

/**
 * @author Pavol Loffay
 */
@Singleton
public class PredictionResultListener extends BasicMessageListener<PredictionResultMessage> {

    public Map<String, PredictionResult> cache = new ConcurrentHashMap<>();


    public PredictionResultListener() {
        try {
            ConnectionContextFactory factory = new ConnectionContextFactory(BusConfiguration.BROKER_URL);
            Endpoint endpoint = new Endpoint(Endpoint.Type.TOPIC, BusConfiguration.TOPIC_PREDICTION_RESULT);
            ConsumerConnectionContext consumerConnectionContext = factory.createConsumerConnectionContext(endpoint);

            MessageProcessor processor = new MessageProcessor();
            processor.listen(consumerConnectionContext, this);

            BusLogger.LOGGER.infof("PredictionResultListener started");
        } catch (JMSException e) {
            BusLogger.LOGGER.errorf("PredictionResultListener stopped");
        }
    }

    @Override
    public void onBasicMessage(PredictionResultMessage resultMessage) {
        BusLogger.LOGGER.debugf("Result received for metric %s", resultMessage.getPredictionResult().getMetricId());

        PredictionResult predictionResult = resultMessage.getPredictionResult();
        cache.put(predictionResult.getRequestId(), predictionResult);
    }
}
