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
package org.hawkular.datamining.api;

import java.util.Set;

import org.hawkular.datamining.api.model.Metric;

/**
 * @author Pavol Loffay
 */
public interface TimeSeriesLinkedModel extends TimeSeriesModel {

    Metric getLinkedMetric();

    void addSubscriptionOwner(ModelManager.ModelOwner owner);

    void removeSubscriptionOwner(ModelManager.ModelOwner owner);

    void addAllSubscriptionOwners(Set<ModelManager.ModelOwner> owners);

    Long getPredictionInterval();

    Long getCollectionInterval();

    Set<ModelManager.ModelOwner> getModelOwners();
}
