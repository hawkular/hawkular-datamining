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

package org.hawkular.datamining.bus.sender;

import java.util.List;

import javax.inject.Singleton;
import javax.jms.JMSException;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.hawkular.datamining.api.model.PredictionRequest;
import org.hawkular.datamining.bus.BusConfiguration;
import org.hawkular.datamining.bus.BusLogger;
import org.hawkular.datamining.bus.message.PredictionRequestMessage;

/**
 * @author Pavol Loffay
 */
@Singleton
public class PredictionsRequestSender {

    private final MessageProcessor messageProcessor;


    public PredictionsRequestSender() {
        this.messageProcessor = new MessageProcessor();
    }

    public void send(List<PredictionRequest> predictionRequests) {

        PredictionRequestMessage message = new PredictionRequestMessage(predictionRequests);

        try (ConnectionContextFactory ccf = new ConnectionContextFactory(BusConfiguration.BROKER_URL)) {

            ProducerConnectionContext producerConnectionContext = ccf.createProducerConnectionContext(
                    new Endpoint(Endpoint.Type.TOPIC, BusConfiguration.TOPIC_PREDICTION_REQUEST));

            messageProcessor.send(producerConnectionContext, message);

        } catch (JMSException e) {
            BusLogger.LOGGER.failedToSendMessageError(message.toString());
        }

    }
}
