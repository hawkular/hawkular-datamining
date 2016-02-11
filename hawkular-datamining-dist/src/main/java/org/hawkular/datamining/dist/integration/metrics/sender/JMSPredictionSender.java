/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

package org.hawkular.datamining.integration.metrics.sender;

import java.util.ArrayList;
import java.util.List;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hawkular.alerts.bus.api.MetricDataMessage;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.hawkular.datamining.api.storage.PredictionListener;
import org.hawkular.datamining.dist.Logger;
import org.hawkular.datamining.forecast.DataPoint;

/**
 * @author Pavol Loffay
 */
public class JMSPredictionSender implements PredictionListener {

    private final String topicName;
    private final String brokerUrl;
    private final MessageProcessor messageProcessor;

    private ConnectionFactory connectionFactory;

    public JMSPredictionSender(String topicName, String brokerUrl) {
        this.topicName = topicName;
        this.brokerUrl = brokerUrl;

        this.messageProcessor = new MessageProcessor();

        try {
            InitialContext initialContext = new InitialContext();
            connectionFactory = (ConnectionFactory) initialContext.lookup(
                    "java:/HawkularBusConnectionFactory");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(List<DataPoint> predictedPoints, String tenant, String metricId) {

        org.hawkular.alerts.bus.api.MetricDataMessage message = convertToMessage(predictedPoints, tenant,
                "prediction_" + metricId);

        try (ConnectionContextFactory ccf = new ConnectionContextFactory(connectionFactory)) {

            ProducerConnectionContext producerConnectionContext = ccf.createProducerConnectionContext(
                    new Endpoint(Endpoint.Type.TOPIC, topicName));
            messageProcessor.send(producerConnectionContext, message);

            Logger.LOGGER.tracef("Sent message %s with headers %s to %s", message,
                    producerConnectionContext.getDestination());
        } catch (JMSException ex) {
            Logger.LOGGER.failedToSendMessage(ex.getMessage());
        }
    }

    private org.hawkular.alerts.bus.api.MetricDataMessage convertToMessage(List<DataPoint> points, String tenant,
                                                                           String metricId) {

        List<MetricDataMessage.SingleMetric> singleMetrics = dataPointToSingleMetric(points, metricId);

        MetricDataMessage.MetricData metricData = new MetricDataMessage.MetricData();
        metricData.setTenantId(tenant);
        metricData.setData(singleMetrics);

        MetricDataMessage metricDataMessage = new MetricDataMessage(metricData);
        return metricDataMessage;
    }

    public List<MetricDataMessage.SingleMetric> dataPointToSingleMetric(List<DataPoint> dataPoints,
                                                                               String metricId) {

        List<MetricDataMessage.SingleMetric> singleMetrics = new ArrayList<>(dataPoints.size());
        for (DataPoint point: dataPoints) {
            singleMetrics.add(new MetricDataMessage.SingleMetric(metricId, point.getTimestamp(), point.getValue()));
        }

        return singleMetrics;
    }
}
