/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
