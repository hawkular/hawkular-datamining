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

package org.hawkular.dataminig.api.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavol Loffay
 */
public class PredictionRequest implements Serializable {

    private String metricId;
    private List<Double> features;


    public PredictionRequest() {
    }

    public PredictionRequest(String metricId, List<Double> features) {
        this.metricId = metricId;
        this.features = features != null ? features : Collections.EMPTY_LIST;
    }

    public String getMetricId() {
        return metricId;
    }

    public List<Double> getFeatures() {
        return features;
    }

    public void setMetricId(String metricId) {
        this.metricId = metricId;
    }

    public void setFeatures(List<Double> features) {
        this.features = features;
    }

    @Override
    public String toString() {
        return "Prediction data [metricId=" + metricId + ", features=" + features.toString() + "]";
    }
}
