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

package org.hawkular.datamining.engine;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Pavol Loffay
 */
public class MetricFilter {


    private static final Set<String> subscriptions = new HashSet<>();

    static {
        subscriptions.add("MI~R~[dhcp130-144~Local~/]~MT~WildFly Memory Metrics~Heap Used");
//        subscriptions.add("MI~R~[dhcp130-144~Local~/]~MT~WildFly Memory Metrics~NonHeap Used");

        subscriptions.forEach(x -> EngineLogger.LOGGER.infof("Metric %s is being watched", x));
    }


    public static boolean subscribe(String key) {
        return subscriptions.contains(key);
    }

    public static boolean unSubscribe(String key) {
        return subscriptions.remove(key);
    }

    public static boolean contains(String key) {
        return subscriptions.contains(key);
    }
}
