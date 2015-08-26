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

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.spark.SparkConf;

/**
 * @author Pavol Loffay
 */
public class Configuration {

    public static final String CONF_FILE = "hawkular-datamining.properties";

    private SparkConf sparkConf = new SparkConf();
    private Properties properties = new Properties();

    public Configuration() throws IOException {
        properties.load(getConfigurationFile().openStream());
        parseSparkProperties(properties);
    }

    public SparkConf getSparkConf() {
        return sparkConf;
    }

    private void parseSparkProperties(Properties properties) {

        for (Map.Entry<Object, Object> entry: properties.entrySet()) {

            String key = String.valueOf(entry.getKey());
            String value = String.valueOf(entry.getValue());

            if (key.startsWith("spark.")) {
                sparkConf.set(key, value);
            }
        }
    }

    private URL getConfigurationFile() {
        return this.getClass().getClassLoader().getResource(CONF_FILE);
    }
}
