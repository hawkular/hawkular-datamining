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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractTest {

    protected List<String> seasonalModels = Collections.unmodifiableList(Arrays.asList(
            "sineLowVarShort", "sineLowVarMedium", "sineLowVarLong",
            "sineTrendLowVar", "austourists"));

    protected List<String> nonSeasonalModels = Collections.unmodifiableList(Arrays.asList(
            "wnLowVariance", "wnHighVariance",
            "trendStatUpwardLowVar", "trendStatUpwardHighVar",
            "trendStatDownwardLowVar", "trendStatDownwardHighVar"));

    static {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.DEBUG);
    }
}
