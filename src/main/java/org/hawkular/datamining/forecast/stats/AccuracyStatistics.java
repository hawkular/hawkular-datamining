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

package org.hawkular.datamining.forecast.stats;

/**
 * Holds statistics of the model
 *
 * @author Pavol Loffay
 */
public class AccuracyStatistics {

    private final double sse;
    private final double mse;
    private final double mae;


    public AccuracyStatistics(double sse, double mse, double mae) {
        this.sse = sse;
        this.mse = mse;
        this.mae = mae;
    }

    /**
     * @return mean squared error
     */
    public double getMse() {
        return mse;
    }

    /**
     * @return mean absolute error
     */
    public double getMae() {
        return mae;
    }

    /**
     * @return sum of squared error
     */
    public double getSse() {
        return sse;
    }

    /**
     * @return root mean squared error
     */
    public double getRmse() {
        return Math.sqrt(mse);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccuracyStatistics)) return false;

        AccuracyStatistics that = (AccuracyStatistics) o;

        if (Double.compare(that.sse, sse) != 0) return false;
        if (Double.compare(that.mse, mse) != 0) return false;
        return Double.compare(that.mae, mae) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(sse);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mse);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mae);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "AccuracyStatistics{" +
                "sse=" + sse +
                ", mse=" + mse +
                ", mae=" + mae +
                '}';
    }
}
