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

package org.hawkular.datamining.inventory;

import javax.jms.JMSException;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.inventory.api.Action;
import org.hawkular.inventory.bus.api.MetricEvent;

/**
 * @author Pavol Loffay
 */
public class InventoryChangesListener extends BasicMessageListener<MetricEvent> {

    public InventoryChangesListener() {
        try {
            ConnectionContextFactory factory = new ConnectionContextFactory(InventoryConfiguration.BROKER_URL);
            Endpoint endpoint = new Endpoint(Endpoint.Type.TOPIC, InventoryConfiguration.TOPIC_INVENTORY_CHANGES);
            ConsumerConnectionContext consumerConnectionContext = factory.createConsumerConnectionContext(endpoint);

            MessageProcessor processor = new MessageProcessor();
            processor.listen(consumerConnectionContext, this);
        } catch (JMSException ex) {
        }
    }

    @Override
    protected void onBasicMessage(MetricEvent inventoryEvent) {

        inventoryEvent.getMessageId();

        Action.Enumerated action = inventoryEvent.getAction();
        action = null;

//        BusLogger.LOGGER.debugf("InventoryEvent event %s", inventoryEvent.toString());
    }
}
