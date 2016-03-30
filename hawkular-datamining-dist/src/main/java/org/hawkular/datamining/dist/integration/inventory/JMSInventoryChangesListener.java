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

package org.hawkular.datamining.dist.integration.inventory;

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
import org.hawkular.datamining.api.Subscription;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.base.DataMiningForecaster;
import org.hawkular.datamining.api.base.DataMiningSubscription;
import org.hawkular.datamining.api.exception.DataMiningException;
import org.hawkular.datamining.cdi.qualifiers.Eager;
import org.hawkular.datamining.dist.integration.Configuration;
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
public class JMSInventoryChangesListener extends InventoryEventMessageListener {

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
            Endpoint endpoint = new Endpoint(Endpoint.Type.TOPIC, Configuration.TOPIC_INVENTORY_CHANGES);
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
            // ignore
        }
    }

    private void relationshipEvent(Relationship relationship, Action.Enumerated action) {
        CanonicalPath target = relationship.getTarget();
        CanonicalPath source = relationship.getSource();

        if (! (source.getSegment().getElementType().equals(Tenant.class) &&
                (target.getSegment().getElementType().equals(Metric.class) ||
                 target.getSegment().getElementType().equals(MetricType.class) ||
                 target.getSegment().getElementType().equals(Tenant.class)) &&
                relationship.getName().equals(Configuration.PREDICTION_RELATIONSHIP))) {
            return;
        }

       final Long forecastingHorizon = InventoryUtil.parseForecastingHorizon(relationship);

        switch (action) {
            case CREATED: {
                inventoryStorage.addPredictionRelationship(relationship);

                if (target.getSegment().getElementType().equals(Metric.class)) {

                    Metric invMetric = inventoryStorage.metric(target);
                    if (subscriptionManager.subscribes(invMetric.getPath().ids().getTenantId(), invMetric.getId())) {
                        Subscription subscription =
                                subscriptionManager.subscription(invMetric.getPath().ids().getTenantId(),
                                        invMetric.getId());

                        subscription.addSubscriptionOwner(Subscription.SubscriptionOwner.Metric);
                        subscription.forecaster().context().setForecastingHorizon(forecastingHorizon);
                    } else {
                        org.hawkular.datamining.api.model.Metric dataminingMetric =
                                InventoryUtil.convertMetric(invMetric, forecastingHorizon);

                        final Subscription subscription = new DataMiningSubscription(
                                new DataMiningForecaster(dataminingMetric),
                                new HashSet<>(Arrays.asList(Subscription.SubscriptionOwner.Metric)));
                        subscriptionManager.subscribe(subscription);
                    }
                } else if (target.getSegment().getElementType().equals(MetricType.class)) {
                    // get all invMetrics of that type
                    Set<Metric> invMetrics = inventoryStorage.metricsOfType(target);

                    invMetrics.forEach(invMetric -> {
                        if (subscriptionManager.subscribes(invMetric.getPath().ids().getTenantId(),
                                invMetric.getId())) {

                            Subscription subscription = subscriptionManager
                                    .subscription(invMetric.getPath().ids().getTenantId(), invMetric.getId());
                            if (!subscription.getSubscriptionOwners().contains(Subscription.SubscriptionOwner.Metric)) {
                                subscription.forecaster().context().setForecastingHorizon(forecastingHorizon);
                            }
                            subscription.addSubscriptionOwner(Subscription.SubscriptionOwner.MetricType);
                        } else {
                            // convert and subscribe
                            org.hawkular.datamining.api.model.Metric dataminingMetric =
                                    InventoryUtil.convertMetric(invMetric, forecastingHorizon);
                            final Subscription subscription = new DataMiningSubscription(
                                    new DataMiningForecaster(dataminingMetric),
                                    new HashSet<>(Arrays.asList(Subscription.SubscriptionOwner.MetricType)));
                            subscriptionManager.subscribe(subscription);
                        }
                    });
                } else {
                    // tenant
                    CanonicalPath tenant = relationship.getTarget();
                    Set<Metric> metricsUnderTenant = inventoryStorage.metricsUnderTenant(tenant);

                    metricsUnderTenant.forEach(invMetric -> {
                        if (subscriptionManager.subscribes(invMetric.getPath().ids().getTenantId(),
                                invMetric.getId())) {

                            Subscription subscription = subscriptionManager
                                    .subscription(invMetric.getPath().ids().getTenantId(), invMetric.getId());
                            if (!subscription.getSubscriptionOwners().containsAll(Arrays.asList(
                                    Subscription.SubscriptionOwner.Metric,
                                    Subscription.SubscriptionOwner.MetricType))) {
                                subscription.forecaster().context().setForecastingHorizon(forecastingHorizon);
                            }

                            subscription.addSubscriptionOwner(Subscription.SubscriptionOwner.Tenant);
                        } else {
                            org.hawkular.datamining.api.model.Metric dataminingMetric =
                                    InventoryUtil.convertMetric(invMetric, forecastingHorizon);
                            final Subscription subscription = new DataMiningSubscription(
                                    new DataMiningForecaster(dataminingMetric),
                                    new HashSet<>(Arrays.asList(Subscription.SubscriptionOwner.Tenant)));
                            subscriptionManager.subscribe(subscription);
                        }
                    });
                }
            }
                break;
            case DELETED: {
                inventoryStorage.removePredictionRelationship(relationship);

                Set<Metric> metricsToRemove;
                Subscription.SubscriptionOwner owner;

                if (target.getSegment().getElementType().equals(Metric.class)) {
                    metricsToRemove = new HashSet<>(Arrays.asList(inventoryStorage.metric(target)));
                    owner = Subscription.SubscriptionOwner.Metric;
                } else if (target.getSegment().getElementType().equals(MetricType.class)) {
                    metricsToRemove = inventoryStorage.metricsOfType(target);
                    owner = Subscription.SubscriptionOwner.MetricType;
                } else {
                    metricsToRemove = inventoryStorage.metricsUnderTenant(target);
                    owner = Subscription.SubscriptionOwner.Tenant;
                }

                metricsToRemove.forEach(invMetric -> {
                    Set<Relationship> relationships = inventoryStorage
                            .predictionRelationships(invMetric.getPath(), invMetric.getType().getPath(),
                                    invMetric.getPath().getRoot());
                    Long horizon = InventoryUtil.closestForecastingHorizon(relationships);

                    subscriptionManager.subscription(invMetric.getPath().ids().getTenantId(), invMetric.getId())
                            .forecaster().context().setForecastingHorizon(horizon);
                    subscriptionManager.unSubscribe(invMetric.getPath().ids().getTenantId(), invMetric.getId(), owner);
                });
                break;
            }
        }
    }

    private void metricEvent(final Metric metric, Action.Enumerated action) {
        CanonicalPath tenant = metric.getPath().getRoot();

        Set<Relationship> predictionRelationships =
                inventoryStorage.predictionRelationships(metric.getPath(), metric.getType().getPath(), tenant);

        if (predictionRelationships.isEmpty()) {
            return;
        }

        switch (action) {
            case CREATED: {
                final Long forecastingHorizon = InventoryUtil.closestForecastingHorizon(predictionRelationships);

                org.hawkular.datamining.api.model.Metric dataminingMetric =
                        InventoryUtil.convertMetric(metric, forecastingHorizon);

                final Subscription subscription = new DataMiningSubscription(
                        new DataMiningForecaster(dataminingMetric),
                        InventoryUtil.predictionRelationshipsToOwners(predictionRelationships));

                subscriptionManager.subscribe(subscription);
            }
            break;

            case UPDATED: {
                Subscription subscription = subscriptionManager.subscription(metric.getPath().ids().getTenantId(),
                        metric.getId());
                ((org.hawkular.datamining.api.model.Metric)subscription.getMetric())
                        .getMetricType().setCollectionInterval(metric.getCollectionInterval());
            }
            break;

            case DELETED: {
                subscriptionManager.unSubscribe(metric.getPath().ids().getTenantId(), metric.getId(),
                        Subscription.SubscriptionOwner.Metric);
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
                inventoryStorage.predictionRelationships(metricType.getPath(), tenant);

        if (predictionRelationships.isEmpty()) {
            // no predicted metrics
            return;
        }

        switch (action) {
            case UPDATED: {

                Set<Metric> metricsOfType = inventoryStorage.metricsOfType(metricType.getPath());
                metricsOfType.forEach(metric -> {
                    if (subscriptionManager.subscribes(metric.getPath().ids().getTenantId(), metric.getId())) {
                        org.hawkular.datamining.api.model.Metric metricc = (org.hawkular.datamining.api.model.Metric)
                                 subscriptionManager.subscription(metric.getPath().ids().getTenantId(), metric.getId())
                                .getMetric();
                        metricc.setCollectionInterval(metric.getType().getCollectionInterval());
                    }
                });
            }
                break;
            case DELETED: {
                Set<Metric> metrics = inventoryStorage.metricsOfType(metricType.getPath());
                metrics.forEach(metric ->
                    subscriptionManager.unSubscribeAll(metric.getPath().ids().getTenantId(), metric.getId()));
            }
                break;
        }
    }
}
