/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
