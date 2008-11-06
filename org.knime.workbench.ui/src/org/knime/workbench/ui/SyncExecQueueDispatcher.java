/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   Nov 5, 2008 (wiswedel): created
 */
package org.knime.workbench.ui;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;

/**
 * A static class offering functionality that should be used instead of the
 * usual <code>Display.getDefault().asyncExec(Runnable)</code> procedure.
 * 
 * <p>
 * The method {@link Display#asyncExec(Runnable) asyncExec} offered by the
 * {@link Display} class has performance problems if requests come in faster
 * than they are processed; reason being the class
 * {@link org.eclipse.swt.widgets.Synchronizer} (specifically the method
 * addLast), which keeps an array of to-be-processed runnables. This array is
 * virtually resized by a fixed size (currently 4) as more runnables come in.
 * There are three expensive operations: a static synchronization, the memory
 * allocation for the new array, and the array copy.
 * 
 * <p>This class uses a {@link java.util.concurrent.ThreadPoolExecutor} to 
 * queue runnables (which are then processed using 
 * {@link Display#syncExec(Runnable)}. 
 * 
 * <p>The use of this class fixes bug #1551 (NodeFigure update events block UI),
 * i.e. the update events that are sent by a looping workflow block the UI.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class SyncExecQueueDispatcher {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(SyncExecQueueDispatcher.class);
    
    private static final ExecutorService EXECUTOR = 
        Executors.newSingleThreadExecutor(new ThreadFactory() {
            private AtomicInteger m_threadCounter = new AtomicInteger();
            @Override
            public Thread newThread(final Runnable r) {
                Thread t = new Thread(r, "KNIME Sync Exec Dispatcher-" 
                        + m_threadCounter.incrementAndGet());
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(
                            final Thread thread, final Throwable e) {
                        LOGGER.error("Uncaught exception while queuing events "
                                + "into main thread", e);
                    }
                });
                t.setDaemon(true);
                return t;
            }
        });
    
    /** Queues a runnable in a local {@link ExecutorService}, that will hand off
     * the runnable to the {@link Display#getDefault()} using the 
     * {@link Display#syncExec(Runnable)} method.
     * @param runnable the runnable to be processed.
     */
    public static final void asyncExec(final Runnable runnable) {
        if (runnable == null) {
            LOGGER.coding("Can't execute null runnable.");
            return;
        }
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Display display = Display.getDefault();
                if (!display.isDisposed()) {
                    display.syncExec(runnable);
                } else {
                    LOGGER.error("Ignoring async execution of runnable " 
                            + "(full class name \"" 
                            + runnable.getClass().getName() 
                            + "\" since device is disposed.");
                }
            }
        });
    }
    
    private SyncExecQueueDispatcher() {
    }
    
}
