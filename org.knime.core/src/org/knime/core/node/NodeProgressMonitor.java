/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.core.node;

import org.knime.core.node.workflow.NodeProgressListener;


/**
 * Implement this interface if you want to get informed about progress change
 * events and if you want to can ask for cancelation.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public interface NodeProgressMonitor {

    /**
     * @throws CanceledExecutionException If the execution of the
     *         <code>NodeModel</code> has been canceled during execute.
     */
    void checkCanceled() throws CanceledExecutionException;

    /**
     * Sets a new progress value. If the value is not in range, the old value is
     * kept.
     * @param progress The value between 0 and 1.
     */
    void setProgress(final double progress);

    /**
     * The current progress value or <code>null</code> if no progress available.
     * @return Progress value between 0 and 1, or <code>null</code>.
     */
    Double getProgress();

    /**
     * Sets a new progress value. If the value is not in range, the old value
     * is kept. The message is displayed.
     * @param progress The value between 0 and 1.
     * @param message A convenience message shown in the progress monitor or
     *        <code>null</code>.
     */
    void setProgress(final double progress, final String message);

    /**
     * Displays the message as given by the argument.
     * @param message A convenience message shown in the progress monitor.
     * @see #setProgress(String)
     */
    void setMessage(final String message);

    /**
     * Displays the message as given by the argument.
     * @param message A convenience message shown in the progress monitor.
     */
    void setProgress(final String message);

    /**
     * The current progress message displayed.
     * @return Progress message.
     */
    String getMessage();

    /**
     * Sets the cancel requested flag.
     */
    void setExecuteCanceled();

    /**
     * Reset progress, message and cancel flag.
     */
    void reset();

    /**
     * Adds a new listener to the list of instances which are interested in
     * receiving progress events.
     * @param l The progress listener to add.
     */
    void addProgressListener(final NodeProgressListener l);

    /**
     * Removes the given listener from the list and will therefore no longer
     * receive progress events.
     * @param l The progress listener to remove.
     */
    void removeProgressListener(final NodeProgressListener l);

    /**
     * Removes all registered progress listeners.
     */
    void removeAllProgressListener();

}   // NodeProgressMonitor
