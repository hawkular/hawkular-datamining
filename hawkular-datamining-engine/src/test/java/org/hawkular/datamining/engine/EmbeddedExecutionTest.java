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
import java.io.Serializable;
import java.net.URL;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class EmbeddedExecutionTest implements Serializable {

    private static SparkConf sparkConf;
    private static URL testFile;

    @BeforeClass
    public static void initBeforeClass() throws IOException {
        Configuration configuration = new Configuration();
        sparkConf = configuration.getSparkConf();

        ClassLoader classLoader = EmbeddedExecutionTest.class.getClassLoader();
        testFile = classLoader.getResource(Configuration.CONF_FILE);
    }

    @Test
    public void testEmbeddedExecution() {

        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);
        JavaRDD<String> logData = sparkContext.textFile(testFile.getPath()).cache();

        long numAs = logData.filter(new Function<String, Boolean>() {
            public Boolean call(String s) {
                return s.startsWith("s");
            }
        }).count();

        long numBs = logData.filter(new Function<String, Boolean>() {
            public Boolean call(String s) {
                return s.contains("b");
            }
        }).count();

        sparkContext.stop();

        System.out.println("Lines starts with s: " + numAs + ", lines with b: " + numBs + "\n\n\n");
    }
}
