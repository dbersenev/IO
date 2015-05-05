/*
 * Copyright 2015 Bersenev Dmitry molasdin@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.molasdin.io;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by molasdin on 3/10/15.
 */

/**
 * Incapsulates signals activities
 * Provides multithreaded processing for pins events
 */
public abstract class SignalsProcessor implements Runnable {

    private final Map<Integer, List<InputPinListener>> listeners = new HashMap<Integer, List<InputPinListener>>();
    private ExecutorService executorService;
    private int[] signals = null;
    private volatile boolean terminateFlag = true;
    private volatile boolean suspend = false;
    private Map<Integer, Boolean> changed = new HashMap<>(5);
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private ExecutorService eventsExecutor;

    /**
     * Add new listener for some pin
     * It is possible to add listener even when some events are being processed
     * @param listener
     * @param id
     */
    public void addPinListener(InputPinListener listener, Integer id) {
        synchronized (listeners) {
            awaitTasks();
            if (!listeners.containsKey(id)) {
                listeners.put(id, new LinkedList<>());
            }
            listeners.get(id).add(listener);
            rebuildSignals();
            suspend = false;
            if (!terminateFlag) {
                if (lock.isLocked()) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Same capabilities as with "addPinListener"
     * @param id
     * @param listener
     */
    public void removePinListener(Integer id, InputPinListener listener) {
        synchronized (listeners) {
            awaitTasks();
            listeners.get(id).remove(listener);
            if (listeners.get(id).isEmpty()) {
                listeners.remove(id);
            }
            if (listeners.isEmpty()) {
                terminateFlag = true;
            }
            rebuildSignals();
            suspend = false;
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    /**
     * Wait for all events to be processed
     */
    private void awaitTasks() {
        if (!terminateFlag) {
            lock.lock();
            terminateSignalsWait();
            suspend = true;
            try {
                condition.await();
            } catch (InterruptedException ex) {
                terminateFlag = true;
                throw new RuntimeException(ex);
            }
        }

    }

    private void rebuildSignals() {
        if (listeners.isEmpty()) {
            signals = null;
        } else {
            signals = new int[listeners.size()];
            int pos = 0;
            for (Integer key : listeners.keySet()) {
                signals[pos] = key;
            }
            prepareToCheckSignals(signals);
        }
    }

    protected Map<Integer, Boolean> changed() {
        return changed;
    }

    /**
     * Activates processor
     */
    public void start() {
        synchronized (listeners){
            if(!terminateFlag || listeners.isEmpty()){
                return;
            }
        }
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        if (eventsExecutor == null) {
            ThreadPoolExecutor tmp = new ThreadPoolExecutor(listeners.size() + 3, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS, new SynchronousQueue<>());
            tmp.prestartAllCoreThreads();
            eventsExecutor = tmp;
        }
        terminateFlag = false;
        executorService.submit(this);
    }

    /**
     * Terminates any processing
     */
    public void stop() {
        synchronized (listeners) {
            if (terminateFlag) {
                return;
            }
            awaitTasks();
            terminateFlag = true;
            lock.unlock();
        }

        executorService.shutdown();
        eventsExecutor.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            eventsExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            eventsExecutor.shutdownNow();
            ex.printStackTrace();
        }
        executorService = null;
        eventsExecutor = null;
        suspend = false;
    }

    @Override
    public void run() {
        while (!terminateFlag) {
            if (suspend) {
                lock.lock();
                condition.signal();
                lock.unlock();
                synchronized (listeners) {
                }
            } else {
                analyzeSignals();
                if (!changed.isEmpty()) {
                    for (Integer id : changed.keySet()) {
                        Boolean value = changed.get(id);
                        List<InputPinListener> pinListeners = listeners.get(id);
                        for(InputPinListener listener: pinListeners){
                            eventsExecutor.execute(() -> listener.pinChanged(value));
                        }
                    }
                }
            }

            if (!terminateFlag) {
                Thread.yield();
            }
        }
    }

    /**
     * Wait for signals changes.
     */
    protected abstract void analyzeSignals();

    /**
     * Prepare some signals to be analyzed
     * @param signals
     */
    protected abstract void prepareToCheckSignals(int[] signals);

    protected abstract void terminateSignalsWait();
}
