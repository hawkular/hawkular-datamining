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

package org.hawkular.datamining.inventory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.TenantSubscriptions;
import org.hawkular.datamining.api.TimeSeriesLinkedModel;
import org.hawkular.datamining.api.exception.DataMiningException;
import org.hawkular.datamining.api.util.Eager;
import org.hawkular.inventory.api.Action;
import org.hawkular.inventory.api.model.CanonicalPath;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.model.Tenant;
import org.hawkular.inventory.bus.api.InventoryEvent;
import org.hawkular.inventory.bus.api.InventoryEventMessageListener;
import org.hawkular.inventory.bus.api.MetricEvent;
import org.hawkular.inventory.bus.api.MetricTypeEvent;
import org.hawkular.inventory.bus.api.RelationshipEvent;

/**
 * @author Pavol Loffay
 */
@Eager
@ApplicationScoped
public class InventoryChangesListener extends InventoryEventMessageListener {

    @Inject
    private SubscriptionManager subscriptionManager;

    @Inject
    private InventoryStorage inventoryStorage;

    @PostConstruct
    public void init() {
        try {
            InitialContext initialContext = new InitialContext();
            ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup(
                    "java:/HawkularBusConnectionFactory");

            ConnectionContextFactory factory = new ConnectionContextFactory(connectionFactory);
            Endpoint endpoint = new Endpoint(Endpoint.Type.TOPIC, InventoryConfiguration.TOPIC_INVENTORY_CHANGES);
            ConsumerConnectionContext consumerConnectionContext = factory.createConsumerConnectionContext(endpoint);

            MessageProcessor processor = new MessageProcessor();
            processor.listen(consumerConnectionContext, this);
        } catch (JMSException ex) {
            ex.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onBasicMessage(InventoryEvent<?> inventoryEvent) {

        Action.Enumerated action = inventoryEvent.getAction();

        if (action == Action.Enumerated.REGISTERED ||
            action == Action.Enumerated.COPIED) {
            return;
        }

        try {
            if (inventoryEvent instanceof RelationshipEvent) {
                relationshipEvent(((RelationshipEvent) inventoryEvent).getObject(), action);
            } else if (inventoryEvent instanceof MetricEvent) {
                metricEvent(((MetricEvent) inventoryEvent).getObject(), action);
            } else if (inventoryEvent instanceof MetricTypeEvent) {
                metricTypeEvent(((MetricTypeEvent) inventoryEvent).getObject(), action);
            }
        } catch (DataMiningException ex) {
        }
    }

    private void relationshipEvent(Relationship relationship, Action.Enumerated action) {
        CanonicalPath target = relationship.getTarget();
        CanonicalPath source = relationship.getSource();

        if (! (source.getSegment().getElementType().equals(Tenant.class) &&
                (target.getSegment().getElementType().equals(Metric.class) ||
                 target.getSegment().getElementType().equals(MetricType.class) ||
                 target.getSegment().getElementType().equals(Tenant.class)) &&
                relationship.getName().equals(InventoryConfiguration.PREDICTION_RELATIONSHIP))) {
            return;
        }

       final Long predictionInterval = InventoryUtil.parsePredictionInterval(relationship.getProperties());

        switch (action) {
            case CREATED: {
                if (target.getSegment().getElementType().equals(Metric.class)) {

                    Metric metric = inventoryStorage.metric(target);
                    org.hawkular.datamining.api.model.Metric dataminingMetric =
                            InventoryUtil.convertMetric(metric, relationship);

                    subscriptionManager.subscribe(dataminingMetric,
                            InventoryUtil.predictionRelationshipsToOwners(relationship));

                } else if (target.getSegment().getElementType().equals(MetricType.class)){
                    // get all metrics of that type
                    Set<Metric> metrics = inventoryStorage.metricsOfType(target);
                    Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics =
                            InventoryUtil.convertMetrics(metrics,
                                    new HashSet<>(Arrays.asList(relationship)));

                    dataminingMetrics.forEach(metric ->
                            subscriptionManager.subscribe(metric,
                                    InventoryUtil.predictionRelationshipsToOwners(relationship)));
                } else {
                    // tenant
                    CanonicalPath tenant = relationship.getTarget();
                    Set<Metric> metricsUnderTenant = inventoryStorage.metricsUnderTenant(tenant);

                    Set<CanonicalPath> metricsMetricAndTypes = new HashSet<>();
                    for (Metric metric: metricsUnderTenant) {
                        metricsMetricAndTypes.add(metric.getPath());
                        metricsMetricAndTypes.add(metric.getType().getPath());
                    }

                    Set<Relationship> relationships = inventoryStorage
                            .predictionRelationships(metricsMetricAndTypes.toArray(new CanonicalPath[0]));

                    Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics =
                            InventoryUtil.convertMetrics(metricsUnderTenant, relationships);

                    dataminingMetrics.forEach(metric ->
                            subscriptionManager.subscribe(metric,
                                    InventoryUtil.predictionRelationshipsToOwners(relationship)));

                    // set prediction interval for tenant
                    subscriptionManager.subscriptionsOfTenant(tenant.ids().getTenantId())
                            .setPredictionInterval(predictionInterval);
                }
            }
                break;
            case UPDATED: {
                if (target.getSegment().getElementType().equals(Metric.class)) {

                    TimeSeriesLinkedModel model = subscriptionManager
                            .model(target.ids().getTenantId(), target.getSegment().getElementId());

                    model.getLinkedMetric().setPredictionInterval(predictionInterval);
                } else if (target.getSegment().getElementType().equals(MetricType.class)) {
                    Set<Metric> metricsOfType = inventoryStorage.metricsOfType(target);
                    Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics =
                            InventoryUtil.convertMetrics(metricsOfType, new HashSet<>(Arrays.asList(relationship)));

                    dataminingMetrics.forEach(metric -> {
                        TimeSeriesLinkedModel model = subscriptionManager.model(metric.getTenant(), metric.getId());
                        model.getLinkedMetric().getMetricType().setPredictionInterval(predictionInterval);
                    });
                } else  {
                    // tenant
                    CanonicalPath tenant = relationship.getTarget();
                    subscriptionManager.subscriptionsOfTenant(tenant.ids().getTenantId())
                            .setPredictionInterval(predictionInterval);
                }
            }
                break;
            case DELETED: {
                if (target.getSegment().getElementType().equals(Metric.class)) {
                    subscriptionManager.unSubscribe(target.ids().getTenantId(), target.getSegment().getElementId(),
                            SubscriptionManager.SubscriptionOwner.Metric);
                } else if (target.getSegment().getElementType().equals(MetricType.class)) {
                    Set<Metric> metricsOfType = inventoryStorage.metricsOfType(target);
                    Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics =
                            InventoryUtil.convertMetrics(metricsOfType, relationship);

                    dataminingMetrics.forEach(x ->
                            subscriptionManager.unSubscribe(x.getTenant(), x.getId(),
                                    SubscriptionManager.SubscriptionOwner.MetricType));
                } else {
                    // tenant
                    CanonicalPath tenant = relationship.getTarget();
                    TenantSubscriptions tenantSubscriptions =
                            subscriptionManager.subscriptionsOfTenant(tenant.ids().getTenantId());

                    tenantSubscriptions.getSubscriptions().forEach((metricId, model) ->
                        subscriptionManager.unSubscribe(tenant.ids().getTenantId(), metricId,
                                SubscriptionManager.SubscriptionOwner.Tenant));
                }
            }
                break;
        }
    }

    private void metricEvent(final Metric metric, Action.Enumerated action) {
        //get relationship to metric or metric type, and decide if predict

        CanonicalPath tenant = metric.getPath().getRoot();

        Set<Relationship> predictionRelationships =
                inventoryStorage.predictionRelationships(metric.getPath(), metric.getType().getPath(), tenant);

        if (predictionRelationships.isEmpty()) {
            return;
        }

        switch (action) {
            case CREATED: {
                org.hawkular.datamining.api.model.Metric dataminingMetric =
                        InventoryUtil.convertMetric(metric, predictionRelationships);

                subscriptionManager.subscribe(dataminingMetric,
                        InventoryUtil.predictionRelationshipsToOwners(predictionRelationships));
            }
            break;

            case UPDATED: {
                TimeSeriesLinkedModel model = subscriptionManager.model(metric.getPath().ids().getTenantId(),
                        metric.getId());

                org.hawkular.datamining.api.model.Metric dataminingMetric = model.getLinkedMetric();
                dataminingMetric.setCollectionInterval(metric.getCollectionInterval());
            }
            break;

            case DELETED: {
                subscriptionManager.unSubscribe(metric.getPath().ids().getTenantId(), metric.getId(),
                        SubscriptionManager.SubscriptionOwner.Metric);
            }
            break;
        }
    }

    private void metricTypeEvent(MetricType metricType, Action.Enumerated action) {

        if (action == Action.Enumerated.CREATED) {
            // for freshly created there are no metrics
            return;
        }

        CanonicalPath tenant = metricType.getPath().getRoot();

        Set<Relationship> predictionRelationships =
                inventoryStorage.predictionRelationships(CanonicalPath.empty().get(), metricType.getPath(), tenant);

        if (predictionRelationships.isEmpty()) {
            // no predicted metrics
            return;
        }

        switch (action) {
            case UPDATED: {

                Set<Metric> metricsOfType = inventoryStorage.metricsOfType(metricType.getPath());
                Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics =
                        InventoryUtil.convertMetrics(metricsOfType, predictionRelationships);

                dataminingMetrics.forEach(x -> {
                    TimeSeriesLinkedModel model = subscriptionManager.model(x.getTenant(), x.getId());
                    model.getLinkedMetric().getMetricType().setCollectionInterval(metricType.getCollectionInterval());
                });
            }
                break;
            case DELETED: {

                Set<Metric> metrics = inventoryStorage.metricsOfType(metricType.getPath());
                Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics =
                        InventoryUtil.convertMetrics(metrics, predictionRelationships);

                dataminingMetrics.forEach(metric -> subscriptionManager.unSubscribe(metric.getTenant(), metric.getId(),
                        SubscriptionManager.SubscriptionOwner.MetricType));
            }
                break;
        }
    }
}
