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
