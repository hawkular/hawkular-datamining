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

package org.hawkular.datamining.dist.integration.metrics;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hawkular.alerts.bus.api.MetricDataMessage;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.dist.Logger;
import org.hawkular.datamining.dist.integration.Configuration;
import org.hawkular.datamining.forecast.DataPoint;

/**
 * @author Pavol Loffay
 */
public class JMSMetricDataListener extends BasicMessageListener<MetricDataMessage> {

    private final SubscriptionManager subscriptionManager;


    public JMSMetricDataListener(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;

        try {
            InitialContext initialContext = new InitialContext();
            ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup(
                    Configuration.BUS_CONNECTION_FACTORY_JNDI);

            ConnectionContextFactory factory = new ConnectionContextFactory(connectionFactory);
            Endpoint endpoint = new Endpoint(Endpoint.Type.TOPIC, Configuration.TOPIC_METRIC_DATA);
            ConsumerConnectionContext consumerConnectionContext = factory.createConsumerConnectionContext(endpoint);

            MessageProcessor processor = new MessageProcessor();
            processor.listen(consumerConnectionContext, this);

            Logger.LOGGER.connectedToTopic(Configuration.TOPIC_METRIC_DATA);
        } catch (JMSException | NamingException ex) {
            Logger.LOGGER.errorf("Could not connect to: %s, exception: %s", Configuration.TOPIC_METRIC_DATA, ex);
        }
    }

    @Override
    protected void onBasicMessage(MetricDataMessage metricDataMessage) {

        MetricDataMessage.MetricData metricData = metricDataMessage.getMetricData();
        String tenantId = metricData.getTenantId();

        for (MetricDataMessage.SingleMetric singleMetric: metricData.getData()) {

            if (singleMetric.getSource().startsWith("prediction_")) {
                continue;
            }

            if (subscriptionManager.isSubscribed(tenantId, singleMetric.getSource())) {
                DataPoint dataPoint = new DataPoint(singleMetric.getValue(), singleMetric.getTimestamp());
                subscriptionManager.subscription(tenantId, singleMetric.getSource()).forecaster().learn(dataPoint);
            }
        }
    }

    // Metrics is not currently exposing the class it uses for the message.  So we needed to
    // implement a compatible class that we can use to deserialize the JSON.  If the class becomes
    // something we can get as a dependency, then import that and this can be removed.
    @Override
    protected String convertReceivedMessageClassNameToDesiredMessageClassName(String className) {

        if (className.equals("org.hawkular.metrics.component.publish.MetricDataMessage")) {
            return MetricDataMessage.class.getCanonicalName();
        }

        return null;
    }
}
