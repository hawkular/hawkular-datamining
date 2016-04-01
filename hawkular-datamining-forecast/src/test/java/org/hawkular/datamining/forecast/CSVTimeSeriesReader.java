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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * @author Pavol Loffay
 */
public class CSVTimeSeriesReader {


    public static List<DataPoint> getData(String fileName) throws IOException {

        fileName = TestDirectory.pathPrefix + fileName;
        File fileToRead = new File(fileName);
        Reader in = new FileReader(fileToRead);

        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withAllowMissingColumnNames(true)
                .withHeader("")
                .parse(in);

        List<DataPoint> dataPoints = new ArrayList<>();
        long counter = 0;
        for (CSVRecord record : records) {
            String value = record.get(0);

            Double doubleValue = null;
            try {
                doubleValue = Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                continue;
            }

            DataPoint dataPoint = new DataPoint(doubleValue, counter++);
            dataPoints.add(dataPoint);
        }

        return dataPoints;
    }
}
