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

package org.hawkular.datamining.forecast.model.r;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.model.DoubleExponentialSmoothing;
import org.hawkular.datamining.forecast.model.SimpleExponentialSmoothing;
import org.hawkular.datamining.forecast.model.TripleExponentialSmoothing;

/**
 * @author Pavol Loffay
 */
public class ModelReader {
    private static final Pattern MSE_PATTERN = Pattern.compile("(MSE: )(\\d*\\.?\\d*)");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("( l: )(-?\\d*\\.?\\d*)");
    private static final Pattern TREND_PATTERN = Pattern.compile("(b: )(-?\\d*\\.?\\d*)");
    private static final Pattern AIC_PATTERN = Pattern.compile("(aic: )(-?\\d*\\.?\\d*)");
    private static final Pattern BIC_PATTERN = Pattern.compile("(bic: )(-?\\d*\\.?\\d*)");
    private static final Pattern AICC_PATTERN = Pattern.compile("(aicc: )(-?\\d*\\.?\\d*)");
    private static final Pattern ALPHA_PATTERN = Pattern.compile("(alpha: )(-?\\d*\\.?\\d*)");
    private static final Pattern BETA_PATTERN = Pattern.compile("(beta: )(-?\\d*\\.?\\d*)");
    private static final Pattern GAMMA_PATTERN = Pattern.compile("(gamma: )(-?\\d*\\.?\\d*)");


    /**
     * Call without any file extension
     */
    public static ModelData read(String fileName) throws IOException {
        String path = TestDirectory.pathPrefix + fileName;

        byte[] encoded = Files.readAllBytes(Paths.get(path + ".model"));
        final String content = new String(encoded, StandardCharsets.UTF_8);

        List<DataPoint> data = CSVTimeSeriesReader.getData(fileName + ".csv");
        Class<?> model = parseModel(content);
        Double mse = patternParse(content, MSE_PATTERN);
        Double aic = patternParse(content, AIC_PATTERN);
        Double bic = patternParse(content, BIC_PATTERN);
        Double aicc = patternParse(content, AICC_PATTERN);
        Double trend = patternParse(content, TREND_PATTERN);
        Double level = patternParse(content, LEVEL_PATTERN);
        Double alpha = patternParse(content, ALPHA_PATTERN);
        Double beta = patternParse(content, BETA_PATTERN);
        Double gamma = patternParse(content, GAMMA_PATTERN);

        ModelData result = new ModelData(model, fileName, level, trend, mse, aic, bic, aicc);
        result.setData(data);
        result.setAlpha(alpha);
        result.setBeta(beta);
        result.setGamma(gamma);

        return result;
    }

    private static Class<?> parseModel(final String fileContent) {

        Class<?> model = null;
        if (fileContent.contains("ETS(A,N,N)")) {
            model = SimpleExponentialSmoothing.class;
        } else if (fileContent.contains("ETS(A,A,N)")) {
            model = DoubleExponentialSmoothing.class;
        } else if (fileContent.contains("ETS(A,A,A)")) {
            model = TripleExponentialSmoothing.class;
        }

        return model;
    }

    private static Double patternParse(final String fileContent, Pattern regex) {
        Matcher m = regex.matcher(fileContent);
        String str = null;
        if (m.find()) {
            str = m.group(2);
        }

        return str != null && !str.isEmpty() ? Double.parseDouble(str) : null;
    }
}
