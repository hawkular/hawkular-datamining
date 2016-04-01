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

package org.hawkular.datamining.forecast.utils;

import java.io.IOException;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Pavol Loffay
 */
public class AutomaticPeriodIdentificationTest extends AbstractTest {

    @Test
    public void testAutomaticPeriodIdentifications() throws IOException {
        for (String model: seasonalModels) {
            ModelData rModel = ModelReader.read(model);

            int periods = AutomaticPeriodIdentification.periods(rModel.getData());

            System.out.println("Model=" + model + ", periods=" + rModel.getPeriods() + ", estimated=" + periods);
            Assert.assertEquals(rModel.getPeriods(), periods);
        }
    }

    /**
     *
     * http://stackoverflow.com/questions/12239096/computing-autocorrelation-with-fft-using-jtransforms-library
     * http://dsp.stackexchange.com/questions/3337/finding-peaks-in-an-autocorrelation-function
     * @throws IOException
     */
//    @Test
    public void testFFT() throws IOException {

        ModelData rModel = ModelReader.read("austourists");
        double[] x = Utils.toArray(rModel.getData());

        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

        Complex[] forward = fft.transform(x, TransformType.FORWARD);
        Complex[] inverse = fft.transform(forward, TransformType.INVERSE);
    }
}
