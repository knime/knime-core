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
 *   Apr 12, 2007 (mb): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;

/**
 * Main entry point for compute intensive jobs. Controls resource (thread)
 * allocation...
 *
 * @author M. Berthold & B. Wiswedel, University of Konstanz
 */
public interface NodeExecutionJobManager {

    public NodeExecutionJob submitJob(final SingleNodeContainer snc,
            final PortObject[] data, final ExecutionContext exec);

    /**
     * Creates a new instance of a panel that holds components to display the
     * job manager's settings and to allow the user to enter new values. A new
     * instance must be created every time this method is called.<br />
     * Returns null if this job manager has no settings to adjust. The framework
     * transfers the settings of the job manager in the panel. The returned
     * panel can be un-initialized.
     *
     * @return a new instance of the dialog component for this job manager
     */
    NodeExecutionJobManagerPanel getSettingsPanelComponent();

    /**
     * Returns a unique ID of this job manager implementations. Preferably this
     * is the fully qualifying name of its package. <br />
     * For a user readable label, see {@link #toString()}
     *
     * @return a unique ID of this job manager implementations
     */
    String getID();

    /**
     * Returns a user readable - but still most likely unique - label. This is
     * displayed in dialogs and user messages.
     *
     * @return a user readable label for this job manager
     */
    public String toString();

    /**
     * Returns true, if a executing job continues running even after closing the
     * workflow - and if this manager can reconnect to this job after re-opening
     * the workflow again.
     *
     * @param job to check for dis/reconnect ability
     * @return true, if a executing job continues running even after closing the
     *         workflow - and if this manager can reconnect to this job after
     *         re-opening the workflow again.
     */
    public boolean canDisconnect(final NodeExecutionJob job);

    /**
     * Saves all the information necessary to reconnect to the specified job
     * after it is disconnected.
     *
     * @param job the job that is disconnected and must be restored later.
     * @param settings stores the information in here
     * @see #loadFromReconnectSettings(NodeSettingsRO, PortObject[])
     */
    public void saveReconnectSettings(final NodeExecutionJob job,
            final NodeSettingsWO settings);

    /**
     * Read the information previously stored in the settings object and restore
     * an executing job.
     *
     * @param settings reconnect information stored during
     *            {@link #saveReconnectSettings(NodeExecutionJob, NodeSettingsWO)}
     * @param inports port objects that were provided at execution start time.
     * @return a new job restored and representing the running job
     * @throws InvalidSettingsException if the information in the settings
     *             object is invalid
     * @throws NodeExecutionJobReconnectException if reconnect failed.
     * @see #saveReconnectSettings(NodeExecutionJob, NodeSettingsWO)
     */
    public NodeExecutionJob loadFromReconnectSettings(
            final NodeSettingsRO settings, final PortObject[] inports)
            throws InvalidSettingsException, NodeExecutionJobReconnectException;

}
