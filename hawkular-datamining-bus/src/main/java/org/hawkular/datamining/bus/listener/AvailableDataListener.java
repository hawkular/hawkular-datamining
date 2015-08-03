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
package org.hawkular.datamining.bus.listener;

import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.datamining.bus.model.AvailDataMessage;
import org.jboss.logging.Logger;

/**
 * Created by pavol on 27.7.2015.
 */
public class AvailableDataListener extends BasicMessageListener<AvailDataMessage> {

    private static final Logger LOG = Logger.getLogger(AvailableDataListener.class);

    @Override
    protected void onBasicMessage(AvailDataMessage availDataMessage) {
        LOG.debug(availDataMessage.toJSON());
    }
}
