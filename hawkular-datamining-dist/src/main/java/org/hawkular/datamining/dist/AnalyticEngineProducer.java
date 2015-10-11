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

package org.hawkular.datamining.dist;

import java.io.IOException;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.hawkular.dataminig.api.AnalyticEngine;
import org.hawkular.datamining.bus.BusLogger;
import org.hawkular.datamining.engine.SparkEngine;
import org.hawkular.datamining.engine.receiver.MetricDataReceiver;
import org.hawkular.datamining.engine.receiver.PredictionRequestReceiver;


/**
 * @author Pavol Loffay
 */
@Singleton
public class AnalyticEngineProducer {

    private AnalyticEngine analyticEngine;

    @Official
    @Produces
    @Singleton
    public AnalyticEngine getAnalyticEngine() {

        MetricDataReceiver metricDataReceiver = new MetricDataReceiver();
        PredictionRequestReceiver predictionRequestReceiver = new PredictionRequestReceiver();

        try {
            SparkEngine sparkEngine = new SparkEngine(metricDataReceiver, predictionRequestReceiver);
            this.analyticEngine = sparkEngine;

            this.analyticEngine.start();

        } catch (IOException ex)  {
            BusLogger.LOGGER.initializedFailedError(ex);
            ex.printStackTrace();
        }

        return analyticEngine;
    }
}
