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

import javax.jms.JMSException;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.dataminig.api.EngineDataReceiver;
import org.hawkular.datamining.bus.BusConfiguration;
import org.hawkular.datamining.bus.listener.MetricDataListener;
import org.hawkular.datamining.engine.EngineLogger;


/**
 * @author Pavol Loffay
 */
public class JMSEngineDataReceiver extends Receiver<String> implements EngineDataReceiver {

    private static final StorageLevel STORAGE_LEVEL = StorageLevel.MEMORY_ONLY();

    private MetricDataListener metricDataListener;
    private ConsumerConnectionContext consumerConnectionContext;


    public JMSEngineDataReceiver() {
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

        } catch (JMSException e) {
            EngineLogger.LOGGER.dataListenerFailedStartError();
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
    public void store(String data){
        super.store(data);
    }
}
