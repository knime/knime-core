/*
 * ------------------------------------------------------------------ *
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
 *   07.01.2008 (ohl): created
 */
package org.knime.core.node.util;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

/**
 * Provides helper methods mostly useful when implementing NodeViews.
 * 
 * @author ohl, University of Konstanz
 */
public final class ViewUtils {

    private ViewUtils() {

    }

    /**
     * Executes the specified runnable in the Swing Event Dispatch Thread. If
     * the caller is already running in the EDT, it just executes the
     * <code>run</code> method of the runnable, otherwise it passes the
     * runnable to the EDT and waits until its <code>run</code> method
     * returns.
     * 
     * @param runMe the <code>run</code> method of this will be executed.
     * @throws InvocationTargetRuntimeException if the executed code throws an
     *             exception (the cause of it is set to the exception thrown by
     *             the executed code then), or if the execution was interrupted
     *             in the EDT.
     * @see SwingUtilities#invokeAndWait(Runnable)
     */
    public static void invokeAndWaitInEDT(final Runnable runMe)
            throws InvocationTargetRuntimeException {

        // if already in event dispatch thread, run immediately
        if (SwingUtilities.isEventDispatchThread()) {
            runMe.run();
        } else {
            try {
                // otherwise queue into event dispatch thread
                SwingUtilities.invokeAndWait(runMe);
            } catch (InvocationTargetException ite) {
                Throwable c = ite.getCause();
                if (c == null) {
                    c = ite;
                }
                throw new InvocationTargetRuntimeException(
                        "Exception during execution in Event Dispatch Thread",
                        c);
            } catch (InterruptedException ie) {
                Throwable c = ie.getCause();
                if (c == null) {
                    c = ie;
                }
                throw new InvocationTargetRuntimeException(Thread
                        .currentThread()
                        + " was interrupted", c);
            }
        }

    }

    /**
     * Executes the specified runnable some time in the Swing Event Dispatch
     * Thread. If the caller is already running in the EDT, it immediately
     * executes the <code>run</code> method and does not return until it
     * finishes. Otherwise it queues the argument for execution in the EDT and
     * returns (not waiting for the <code>run</code> method to finish).
     * 
     * @param runMe the <code>run</code> method of this will be executed.
     * @see SwingUtilities#invokeLater(Runnable)
     */
    public static void runOrInvokeLaterInEDT(final Runnable runMe) {

        if (SwingUtilities.isEventDispatchThread()) {
            runMe.run();
        } else {
            SwingUtilities.invokeLater(runMe);
        }

    }

}
