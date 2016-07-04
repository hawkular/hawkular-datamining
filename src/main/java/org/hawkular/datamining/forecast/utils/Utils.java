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

package org.hawkular.datamining.forecast.utils;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.hawkular.datamining.forecast.DataPoint;

/**
 * @author Pavol Loffay
 */
public class Utils {

    private Utils() {
    }

    /**
     * Converts list of data points to array of values
     * @return array of values
     */
    public static double[] toArray(List<DataPoint> data) {
        double[] result = new double[data.size()];

        for (int i = 0; i < data.size(); i++) {
            result[i] = data.get(i).getValue();
        }

        return result;
    }

    public static double standardDeviation(Double[] residuals) {
        double[] primitiveResiduals = ArrayUtils.toPrimitive(residuals);
        StandardDeviation standardDeviation = new StandardDeviation();
        return standardDeviation.evaluate(primitiveResiduals);
    }
}
