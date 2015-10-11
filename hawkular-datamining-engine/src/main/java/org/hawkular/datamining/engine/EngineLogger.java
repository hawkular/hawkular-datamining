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

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Pavol Loffay
 */
@MessageLogger(projectCode = "HAWKDMING")
public interface EngineLogger extends BasicLogger {

    EngineLogger LOGGER = Logger.getMessageLogger(EngineLogger.class, "org.hawkular.datamining.engine");


    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Datamining engine successfully started")
    void engineStartInfo();

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Datamining engine stopped")
    void engineStopInfo();

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Datamining engine data listener successfully started")
    void dataListenerStartInfo();

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Datamining engine data listener successfully stopped")
    void dataListenerStopInfo();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(value = "Datamining engine data listener[%s] failed to start")
    void dataListenerFailedStartError(String clazz);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(value = "Datamining engine data listener failed to stop")
    void dataListenerFailedStopError();

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Jackson Databind version: %s, should be 2.4.4")
    void jacksonDatabindVersion(String message);
}
