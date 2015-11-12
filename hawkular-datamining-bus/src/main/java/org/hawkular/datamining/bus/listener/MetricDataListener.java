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

import javax.jms.JMSException;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.dataminig.api.EngineDataReceiver;
import org.hawkular.dataminig.api.MetricFilter;
import org.hawkular.dataminig.api.model.MetricData;
import org.hawkular.datamining.bus.BusConfiguration;
import org.hawkular.datamining.bus.BusLogger;
import org.hawkular.datamining.bus.message.MetricDataMessage;

/**
 * @author Pavol Loffay
 */
public class MetricDataListener extends BasicMessageListener<MetricDataMessage> {

    private final EngineDataReceiver engineDataReceiver;


    public MetricDataListener(EngineDataReceiver engineDataReceiver) {
        this.engineDataReceiver = engineDataReceiver;

        try {
            ConnectionContextFactory factory = new ConnectionContextFactory(BusConfiguration.BROKER_URL);
            Endpoint endpoint = new Endpoint(Endpoint.Type.TOPIC, BusConfiguration.TOPIC_METRIC_DATA);
            ConsumerConnectionContext consumerConnectionContext = factory.createConsumerConnectionContext(endpoint);

            MessageProcessor processor = new MessageProcessor();
            processor.listen(consumerConnectionContext, this);
        } catch (JMSException ex) {

        }
    }

    @Override
    protected void onBasicMessage(MetricDataMessage metricDataMessage) {

        MetricDataMessage.MetricData metricData = metricDataMessage.getMetricData();
        String tenantId = metricData.getTenantId();

        for (MetricDataMessage.SingleMetric singleMetric: metricData.getData()) {

//             filter data
            if (MetricFilter.contains(tenantId, singleMetric.getSource())) {
                BusLogger.LOGGER.debugf("\ntenant %s", tenantId);
                BusLogger.LOGGER.debugf("source: %s", singleMetric.getSource());
                BusLogger.LOGGER.debugf("value: %s", singleMetric.getValue());


                MetricData engineData = new MetricData(tenantId, singleMetric.getSource(),
                        singleMetric.getTimestamp(),
                        singleMetric.getValue());

                engineDataReceiver.process(engineData);
            }
        }
    }
}
