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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class ModelReaderTest {

    @Test
    public void testAttributes() throws IOException {
        ModelData modelData = ModelReader.read("sineLowVarMedium");

        Assert.assertNotNull(modelData.getModel());
        Assert.assertNotNull(modelData.getMse());
        Assert.assertNotNull(modelData.getLevel());
        Assert.assertNotNull(modelData.getTrend());
        Assert.assertNotNull(modelData.getAic());
        Assert.assertNotNull(modelData.getAicc());
        Assert.assertNotNull(modelData.getBic());
        Assert.assertNotNull(modelData.getPeriods());
    }
}
