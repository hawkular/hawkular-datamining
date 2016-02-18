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
package org.hawkular.datamining.dist;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.dist.integration.Configuration;
import org.hawkular.datamining.dist.integration.metrics.JMSPredictionSender;
import org.hawkular.datamining.dist.integration.metrics.MetricDataListener;

/**
 * @author Pavol Loffay
 */
@Startup
@Singleton //todo eager?
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class HawkularIntegration {

    @Inject
    private SubscriptionManager subscriptionManager;

    private MetricDataListener metricDataListener;
    private JMSPredictionSender predictionSender;

    @PostConstruct
    public void postConstruct() {

        this.metricDataListener = new MetricDataListener(subscriptionManager);

        this.predictionSender = new JMSPredictionSender(Configuration.TOPIC_METRIC_DATA, Configuration.BROKER_URL);
        subscriptionManager.setPredictionListener(predictionSender);

        Logger.LOGGER.infof("Datamining Hawkular Integration");
    }
}
