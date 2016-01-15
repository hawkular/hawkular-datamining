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

import org.hawkular.datamining.api.ForecastingEngine;
import org.hawkular.datamining.api.Official;
import org.hawkular.datamining.api.model.MetricData;
import org.hawkular.datamining.bus.listener.MetricDataListener;
import org.jboss.logging.Logger;

/**
 * @author Pavol Loffay
 */
@Startup
@Singleton
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED) //todo never?
public class DataMiningStartup {

    private static final Logger LOG = Logger.getLogger(DataMiningStartup.class);

    @Official
    @Inject
    private ForecastingEngine<MetricData> forecastingEngine;

    private MetricDataListener metricDataListener;

    @PostConstruct
    public void postConstruct() {

        metricDataListener = new MetricDataListener(forecastingEngine);

        LOG.debug("Ejb starting");
    }
}
