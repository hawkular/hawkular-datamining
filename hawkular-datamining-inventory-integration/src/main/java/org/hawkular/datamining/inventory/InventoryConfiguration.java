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

/**
 * @author Pavol Loffay
 */
public final class InventoryConfiguration {
    public static final String BROKER_URL = "tcp://localhost:62626";
    public static final String TOPIC_INVENTORY_CHANGES = "HawkularInventoryChanges";
    public static final String QUEUE_INVENTORY_QUERY = "HawkularInventoryQuery";

    public static final String PREDICTION_RELATIONSHIP = "__inPrediction";
    public static final String PREDICTION_INTERVAL_PROP = "forecastingHorizon";
}
