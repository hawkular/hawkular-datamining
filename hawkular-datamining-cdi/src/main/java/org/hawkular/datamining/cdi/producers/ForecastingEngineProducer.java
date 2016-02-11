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

package org.hawkular.datamining.cdi.producers;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hawkular.datamining.api.Official;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.base.DataMiningEngine;
import org.hawkular.datamining.api.model.MetricData;
import org.hawkular.datamining.cdi.Logger;


/**
 * @author Pavol Loffay
 */
@Singleton
public class ForecastingEngineProducer {

    @Inject
    private SubscriptionManager subscriptionManager;

    @Official
    @Produces
    @Singleton
    public org.hawkular.datamining.api.DataMiningEngine<MetricData> getAnalyticEngine() {

        org.hawkular.datamining.api.DataMiningEngine<MetricData> dataMiningEngine =
                new DataMiningEngine(subscriptionManager);

        Logger.LOGGER.infof("New instance of engine produced");

        return dataMiningEngine;
    }
}
