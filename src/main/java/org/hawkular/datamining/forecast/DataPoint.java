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

import java.io.Serializable;

/**
 * Represents point at specific time
 *
 * @author Pavol Loffay
 */
public class DataPoint implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private Long timestamp;
    private Double value;

    private Double min;
    private Double max;
    
    public DataPoint(Double value, Long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public DataPoint(Double value, Long timestamp, Double max, Double min) {
        this.value = value;
        this.timestamp = timestamp;
        this.min = min;
        this.max = max;
    }

    public Double getValue() {
        return value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataPoint)) return false;

        DataPoint dataPoint = (DataPoint) o;

        if (!timestamp.equals(dataPoint.timestamp)) return false;
        return value.equals(dataPoint.value);

    }

    @Override
    public int hashCode() {
        int result = timestamp.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " { value = " + value + " , timestamp=" + timestamp + " }";
    }
}
