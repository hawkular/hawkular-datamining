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

import java.util.LinkedList;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;

/**
 * @author Pavol Loffay
 */
public class StreamingJMSReceiver extends Receiver<String> {

    private Thread receiver;

    private LinkedList<String> incomingMessages = new LinkedList<>();

    public StreamingJMSReceiver(StorageLevel storageLevel) {
        super(storageLevel);
    }

    public void addMessage(String message) {
        incomingMessages.add(message);
    }

    @Override
    public StorageLevel storageLevel() {
        return null;
    }

    @Override
    public void onStart() {
        receiver =  new Thread()  {
            @Override public void run() {
                receive();
            }
        };

        receiver.start();
    }

    @Override
    public void onStop() {

    }

    private void receive() {

        while (true) {

            for (String message: incomingMessages) {
                store(message);
            }
        }
    }
}
