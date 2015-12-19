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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.jms.JMSException;

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.hawkular.inventory.api.Query;
import org.hawkular.inventory.api.model.AbstractElement;
import org.hawkular.inventory.api.paging.Order;
import org.hawkular.inventory.api.paging.Pager;
import org.hawkular.inventory.bus.api.InventoryQueryRequestMessage;
import org.hawkular.inventory.bus.api.InventoryQueryResponseMessage;

/**
 * @author Pavol Loffay
 */
public class InventoryBusClient<T extends AbstractElement<?, ?>> extends
        BasicMessageListener<InventoryQueryResponseMessage> {

    private final String queueName = InventoryConfiguration.QUEUE_INVENTORY_QUERY;
    private final String brokerUrl = InventoryConfiguration.BROKER_URL;

    private ConnectionContextFactory connectionContextFactory;
    private ProducerConnectionContext producerConnectionContext;

    private final Query query;
    private Set<T> result = new HashSet<>();

    private boolean messageReceived;

    public InventoryBusClient(Query query) {
        this.query = query;
        this.messageReceived = false;
    }

    public Set<T> sendQuery() {
        try {
            connectionContextFactory = new ConnectionContextFactory(brokerUrl);
            producerConnectionContext = connectionContextFactory.createProducerConnectionContext(
                    new Endpoint(Endpoint.Type.QUEUE, queueName));

            InventoryQueryRequestMessage inventoryMessage =
                    new InventoryQueryRequestMessage(query, AbstractElement.class,
                            Pager.unlimited(Order.unspecified()));
            new MessageProcessor().sendAndListen(producerConnectionContext, inventoryMessage, this);

            int waitIter = 0;
            while (!messageReceived && waitIter++ < 5) {
                Thread.sleep(1000);
            }

            producerConnectionContext.close();
        } catch (JMSException ex) {
            //todo
        } catch (InterruptedException e)  {
            //todo
        } catch (IOException e) {
            //todo
        }

        return result;
    }

    @Override
    public void onBasicMessage(BasicMessageWithExtraData<InventoryQueryResponseMessage> messageWithExtraData) {

        final InventoryQueryResponseMessage<?> message = messageWithExtraData.getBasicMessage();

        result = new HashSet<>((Collection<T>) message.getResult().getEntities());
        messageReceived = true;
    }
}
