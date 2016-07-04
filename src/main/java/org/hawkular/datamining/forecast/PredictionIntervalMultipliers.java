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

package org.hawkular.datamining.forecast;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * @author Pavol Loffay
 */
public class PredictionIntervalMultipliers {

    private static final Map<Integer, Double> cashedMultipliers;
    static {
        Map<Integer, Double> multipliers = new HashMap<>();
        multipliers.put(50, 0.67);
        multipliers.put(85, 1.44);
        multipliers.put(95, 1.96);

        cashedMultipliers = new ConcurrentHashMap<>(multipliers);
    }

    private PredictionIntervalMultipliers() {
    }

    public static double multiplier(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException();
        }

        Double multiplier = cashedMultipliers.get(percentage);
        if (multiplier == null) {
            NormalDistribution normalDistribution = new NormalDistribution(0, 1);
            multiplier = normalDistribution.inverseCumulativeProbability(0.5 + (percentage*0.01)/2);
            cashedMultipliers.put(percentage, multiplier);
        }

        return multiplier;
    }
}
