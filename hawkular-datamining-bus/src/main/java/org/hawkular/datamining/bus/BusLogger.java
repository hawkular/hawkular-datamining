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

package org.hawkular.datamining.bus;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Pavol Loffay
 */
@MessageLogger(projectCode = "HAWKDMING")
public interface BusLogger extends BasicLogger {

    BusLogger LOGGER = Logger.getMessageLogger(BusLogger.class, "org.hawkular.datamining.bus");

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Datamining bus successfully connected")
    void initializedInfo();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 10011, value = "Dataminig bus failed to conenct to bus")
    void initializedFailedError(@Cause Throwable t);
}
