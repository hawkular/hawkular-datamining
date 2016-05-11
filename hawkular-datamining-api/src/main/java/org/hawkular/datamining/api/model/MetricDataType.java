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

package org.hawkular.datamining.api.model;

/**
 * @author Pavol Loffay
 */
public enum  MetricDataType {

    GAUGE("gauge"),
    AVAILABILITY("availability"),
    COUNTER("counter"),
    COUNTER_RATE("counter_rate");

    private final String displayName;

    MetricDataType(String name) {
        this.displayName = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MetricDataType fromDisplayName(String displayName) {
        for (MetricDataType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }

        throw new IllegalArgumentException("No such type: " + displayName);
    }
}
