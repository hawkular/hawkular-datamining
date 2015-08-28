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

package org.hawkular.datamining.engine.receiver;

import java.io.Serializable;
import java.util.LinkedList;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import org.hawkular.datamining.engine.EngineLogger;


/**
 * @author Pavol Loffay
 */
public class StreamingJMSReceiver extends Receiver<String> {

    private Thread workerThread;

    private LinkedList<String> incomingMessages = new LinkedList<>();


    public StreamingJMSReceiver() {
        super(StorageLevel.MEMORY_ONLY());
    }

    public void addMessage(String message) {
        incomingMessages.push(message);
    }

    @Override
    public StorageLevel storageLevel() {
        return StorageLevel.MEMORY_ONLY();
    }

    @Override
    public void onStart() {
        workerThread = new Thread(new StoreDataWorker());
        workerThread.start();
    }

    @Override
    public void onStop() {
        workerThread.stop();
    }


    private class StoreDataWorker implements Runnable, Serializable {

        @Override
        public void run() {
            while (true) {
                while (!incomingMessages.isEmpty()) {
                    String newMessage = incomingMessages.pop();

                    EngineLogger.LOGGER.debugf("Receiving %s", newMessage);
                    store(newMessage);
                }

                if (shouldEnd()) {
                    break;
                }
            }
        }

        private boolean shouldEnd() {
            return Thread.currentThread().isInterrupted();
        }
    }
}
