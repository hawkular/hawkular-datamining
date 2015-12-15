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

package org.hawkular.datamining.bus.sender;

import java.util.HashSet;
import java.util.Set;

import javax.jms.JMSException;

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.hawkular.datamining.bus.BusConfiguration;
import org.hawkular.datamining.bus.BusLogger;
import org.hawkular.inventory.api.Query;
import org.hawkular.inventory.api.filters.RelationWith;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.AbstractElement;
import org.hawkular.inventory.api.model.CanonicalPath;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.model.Tenant;
import org.hawkular.inventory.api.paging.Order;
import org.hawkular.inventory.api.paging.Pager;
import org.hawkular.inventory.base.spi.SwitchElementType;
import org.hawkular.inventory.bus.api.InventoryQueryRequestMessage;
import org.hawkular.inventory.bus.api.InventoryQueryResponseMessage;

/**
 * @author Pavol Loffay
 */
public class InventoryInitializer extends BasicMessageListener<InventoryQueryResponseMessage> {

    private final String queueName = "HawkularInventoryQuery";
    private final String brokerUrl = BusConfiguration.BROKER_URL;

    private ConnectionContextFactory connectionContextFactory;
    private ProducerConnectionContext producerConnectionContext;


    /**
     * TODO execute query which returns Metric and MetricTypes
     *
     * I need to get RelationShips -> predictionInterval
     * I need to get Metric and MetricTypes of those relationShips
     */
    public void init() {
        Query predictionRelationships = Query.path().with(
                With.type(Tenant.class), SwitchElementType.outgoingRelationships(), RelationWith.name("__inPrediction"),
                RelationWith.targetsOfTypes(Metric.class, MetricType.class)).get();
        Pager pager = Pager.unlimited(Order.unspecified());

        InventoryQueryRequestMessage inventoryMessage =
                new InventoryQueryRequestMessage(predictionRelationships, AbstractElement.class, pager);
        try {
            connectionContextFactory = new ConnectionContextFactory(brokerUrl);
            producerConnectionContext = connectionContextFactory.createProducerConnectionContext(
                    new Endpoint(Endpoint.Type.QUEUE, queueName));

            new MessageProcessor().sendAndListen(producerConnectionContext, inventoryMessage, this);

            BusLogger.LOGGER.info("Sent query request to inventory");
        } catch (JMSException ex) {
            BusLogger.LOGGER.failedToSendMessage(ex.getMessage());
        }
    }

    @Override
    public void onBasicMessage(BasicMessageWithExtraData<InventoryQueryResponseMessage> messageWithExtraData) {

        final InventoryQueryResponseMessage<Relationship> message = messageWithExtraData.getBasicMessage();

        BusLogger.LOGGER.infof("Response from inventory, num. of predicted metrics %s",
                message.getResult().getTotalSize());

        Set<CanonicalPath> metricsPaths = new HashSet<>();
        Set<CanonicalPath> metricsTypesPaths = new HashSet<>();

        for(Relationship relationship: message.getResult().getEntities()) {

            CanonicalPath canonicalPath = relationship.getTarget();
            Class<?> entityClass = canonicalPath.getSegment().getElementType();

            if (entityClass.equals(Metric.class)) {
                metricsPaths.add(relationship.getTarget());
            } else if (entityClass.equals(MetricType.class)) {
                metricsTypesPaths.add(relationship.getTarget());
            }
        }

        try {
            connectionContextFactory.close();
        } catch (JMSException ex) {
            BusLogger.LOGGER.error(ex);
        }
    }
}
