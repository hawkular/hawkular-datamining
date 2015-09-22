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

import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.dataminig.api.EngineDataReceiver;
import org.hawkular.dataminig.api.MetricFilter;
import org.hawkular.datamining.bus.BusLogger;
import org.hawkular.datamining.bus.model.MetricDataMessage;

/**
 * @author Pavol Loffay
 */
public class MetricDataListener extends BasicMessageListener<MetricDataMessage> {

    private final EngineDataReceiver engineDataReceiver;


    public MetricDataListener(EngineDataReceiver engineDataReceiver) {
        this.engineDataReceiver = engineDataReceiver;
    }

    @Override
    protected void onBasicMessage(MetricDataMessage metricDataMessage) {

        MetricDataMessage.MetricData metricData = metricDataMessage.getMetricData();
        String tenantId = metricData.getTenantId();

        for (MetricDataMessage.SingleMetric singleMetric: metricData.getData()) {

//             filter data
            if (MetricFilter.contains(singleMetric.getSource())) {
                BusLogger.LOGGER.debugf("\ntenant %s", tenantId);
                BusLogger.LOGGER.debugf("source: %s", singleMetric.getSource());
                BusLogger.LOGGER.debugf("value: %s", singleMetric.getValue());

                engineDataReceiver.store(String.valueOf(singleMetric.getValue()));
            }
        }
    }
}
