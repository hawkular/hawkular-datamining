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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.jms.JMSException;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.dataminig.api.EngineDataReceiver;
import org.hawkular.dataminig.api.model.MetricData;
import org.hawkular.datamining.bus.BusConfiguration;
import org.hawkular.datamining.bus.listener.MetricDataListener;
import org.hawkular.datamining.engine.EngineLogger;

/**
 * @author Pavol Loffay
 */
public class MetricDataReceiver extends Receiver<MetricData> implements EngineDataReceiver<MetricData> {

    private static final StorageLevel STORAGE_LEVEL = StorageLevel.MEMORY_ONLY();

//    public static final double firstTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7;
    public static final double firstTimestamp = 0;

    private MetricDataListener metricDataListener;
    private ConsumerConnectionContext consumerConnectionContext;

    public MetricDataReceiver() {
        super(STORAGE_LEVEL);
    }

    @Override
    public StorageLevel storageLevel() {
        return STORAGE_LEVEL;
    }

    @Override
    public void onStart() {
        try {
            ConnectionContextFactory factory = new ConnectionContextFactory(BusConfiguration.BROKER_URL);
            Endpoint endpoint = new Endpoint(Endpoint.Type.TOPIC, BusConfiguration.TOPIC_METRIC_DATA);
            this.consumerConnectionContext = factory.createConsumerConnectionContext(endpoint);

            MessageProcessor processor = new MessageProcessor();
            this.metricDataListener = new MetricDataListener(this);
            processor.listen(consumerConnectionContext, metricDataListener);

            batchLoadMetrics();

            EngineLogger.LOGGER.dataListenerStartInfo();
        } catch (JMSException e) {
            EngineLogger.LOGGER.dataListenerFailedStartError(this.getClass().getCanonicalName());
        }

    }

    @Override
    public void onStop() {
        try {
            consumerConnectionContext.close();
            EngineLogger.LOGGER.dataListenerStopInfo();
        } catch (IOException e) {
            EngineLogger.LOGGER.dataListenerFailedStopError();
        }
    }

    @Override
    public void store(MetricData data) {

        MetricData modified = modifyData(data);

        EngineLogger.LOGGER.debugf("Original metric data = " + data.toString());
        EngineLogger.LOGGER.debugf("Modified metric data = " + modified.toString());
        super.store(modified);
    }

    @Override
    public void store(Collection<MetricData> data) {
        Collection<MetricData> modified = data.stream().map(x -> modifyData(x)).collect(Collectors.toList());

        super.store(modified.iterator());
    }

    private MetricData modifyData(MetricData input) {
        return new MetricData(input.getId(), input.getTimestamp() - firstTimestamp, input.getValue());
    }

    private void batchLoadMetrics() {
        BatchMetricsLoader batchLoader =
                new BatchMetricsLoader("MI~R~[dhcp130-144~Local~~]~MT~WildFly Memory Metrics~Heap Used");

        List<MetricData> oldMetrics = batchLoader.load();

        store(oldMetrics);
    }
}
