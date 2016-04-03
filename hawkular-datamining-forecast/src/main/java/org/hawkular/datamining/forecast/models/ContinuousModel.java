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

package org.hawkular.datamining.forecast.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;

import com.google.common.collect.EvictingQueue;

/**
 * @author Pavol Loffay
 */
public class ContinuousModel implements TimeSeriesModel {

    private final TimeSeriesModel model;
    private EvictingQueue<DataPoint> window;
    private boolean initialized;


    public ContinuousModel(TimeSeriesModel model) {
        this.model = model;
        this.window = EvictingQueue.create(model.minimumInitSize());
    }

    @Override
    public AccuracyStatistics init(List<DataPoint> learnData) {
        return model.init(learnData);
    }

    @Override
    public void learn(DataPoint learnData) {
        model.learn(Collections.singletonList(learnData));
    }

    @Override
    public void learn(List<DataPoint> learnData) {
        if (window.remainingCapacity() - learnData.size() < 1) {
            List<DataPoint> initData = new ArrayList<>(window.size() + learnData.size());
            initData.addAll(window);
            initData.addAll(learnData);

            model.init(learnData);
            initialized = true;
        } else {
            window.addAll(learnData);
        }

        if (initialized) {
            model.learn(learnData);
        }
    }

    @Override
    public DataPoint forecast() {
        return model.forecast();
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {
        return model.forecast(nAhead);
    }

    @Override
    public AccuracyStatistics initStatistics() {
        return model.initStatistics();
    }

    @Override
    public AccuracyStatistics runStatistics() {
        return model.runStatistics();
    }

    @Override
    public String name() {
        return model.name();
    }

    @Override
    public int numberOfParams() {
        return model.numberOfParams();
    }

    @Override
    public int minimumInitSize() {
        return model.minimumInitSize();
    }
}
