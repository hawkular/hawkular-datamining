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

import java.util.List;

/**
 * @author Pavol Loffay
 */
public class ModelData {

    private String name;
    private List<DataPoint> data;

    private Class<?> model;
    private Double mse;
    private Double aic;
    private Double bic;
    private Double aicc;

    private Double level;
    private Double trend;

    private Double alpha;
    private Double beta;
    private Double gamma;

    private int periods;


    public ModelData(Class<?> model, String name, Double level, Double trend, Double mse, Double aic, Double bic,
                     Double aicc, int periods) {
        this.model = model;
        this.name = name;

        this.level = level;
        this.trend = trend;

        this.mse = mse;
        this.aic = aic;
        this.bic = bic;
        this.aicc = aicc;

        this.periods = periods;
    }


    public void setData(List<DataPoint> data) {
        this.data = data;
    }

    public Double getLevel() {
        return level;
    }

    public Double getTrend() {
        return trend;
    }

    public Double getMse() {
        return mse;
    }

    public Class<?> getModel() {
        return model;
    }

    public List<DataPoint> getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public Double getAic() {
        return aic;
    }

    public Double getBic() {
        return bic;
    }

    public Double getAicc() {
        return aicc;
    }

    public Double getAlpha() {
        return alpha;
    }

    public void setAlpha(Double alpha) {
        this.alpha = alpha;
    }

    public Double getBeta() {
        return beta;
    }

    public void setBeta(Double beta) {
        this.beta = beta;
    }

    public Double getGamma() {
        return gamma;
    }

    public void setGamma(Double gamma) {
        this.gamma = gamma;
    }

    public int getPeriods() {
        return periods;
    }

    @Override
    public String toString() {
        return "ModelData{" +
                "model=" + model +
                ", mse=" + mse +
                ", AIC=" + aic +
                ", BIC=" + bic +
                ", AICc=" + aicc +
                ", level=" + level +
                ", trend=" + trend +
                ", alpha=" + alpha +
                ", beta=" + beta +
                ", gamma=" + gamma +
                ", name='" + name + '\'' +
                '}';
    }
}
