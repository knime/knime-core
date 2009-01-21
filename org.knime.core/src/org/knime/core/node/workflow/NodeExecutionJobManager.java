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

import java.net.URL;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;

/**
 * Main entry point for compute intensive jobs. Controls resource (thread)
 * allocation...
 *
 * @author M. Berthold & B. Wiswedel, University of Konstanz
 */
public interface NodeExecutionJobManager {

    /**
     * Executes the given node container with this job manager.
     *
     * @param nc The node to execute (may be a single node container or a meta
     *            node)
     * @param data The input data for that node.
     * @return A job representing the pending execution.
     * @throws IllegalStateException If this job manager is not able to execute
     *             the given argument node container (for instance a local
     *             thread executor will not allow an execution of a meta node).
     */
    public NodeExecutionJob submitJob(final NodeContainer nc,
            final PortObject[] data);

    /**
     * Creates a new instance of a panel that holds components to display the
     * job manager's settings and to allow the user to enter new values. A new
     * instance must be created every time this method is called.<br />
     * Returns null if this job manager has no settings to adjust. The framework
     * transfers the settings of the job manager in the panel. The returned
     * panel can be un-initialized.
     *
     * @param nodeSplitType type of splitting permitted by the underlying node
     * @return a new instance of the dialog component for this job manager
     */
    NodeExecutionJobManagerPanel getSettingsPanelComponent(
            final SplitType nodeSplitType);

    /**
     * Returns a unique ID of this job manager implementations. Preferably this
     * is the fully qualifying name of its package. <br />
     * For a user readable label, see {@link #toString()}
     *
     * @return a unique ID of this job manager implementations
     */
    String getID();

    /**
     *
     * @return the URL of the decorating image for the implementing manager
     */
    public URL getIcon();

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
     * @see #loadFromReconnectSettings( NodeSettingsRO, PortObject[],
     *      NodeContainer)
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
     * @param nc Node whose remote executing is to be continued.
     * @return a new job restored and representing the running job
     * @throws InvalidSettingsException if the information in the settings
     *             object is invalid
     * @throws NodeExecutionJobReconnectException if reconnect failed.
     * @see #saveReconnectSettings(NodeExecutionJob, NodeSettingsWO)
     */
    public NodeExecutionJob loadFromReconnectSettings(
            final NodeSettingsRO settings, final PortObject[] inports,
            final NodeContainer nc) throws InvalidSettingsException,
            NodeExecutionJobReconnectException;

    /**
     * Disconnects the running job.
     *
     * @param job The job to cancel.
     */
    public void disconnect(final NodeExecutionJob job);

    /**
     * Saves parameters that customize this instance. It does not save the
     * general job manager ID (that happens elsewhere). Job managers that are
     * represented by a singleton, leave this method empty.
     *
     * @param settings to save to.
     */
    public void save(final NodeSettingsWO settings);

    /**
     * Restores the properties of the specific job manager. This is the reverse
     * operation to {@link #save(NodeSettingsWO)}.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If that fails.
     */
    public void load(final NodeSettingsRO settings)
            throws InvalidSettingsException;

    /**
     * In case the executor modifies the result of the underlying node it can
     * create the appropriate output specs. Trivial implementations just return
     * the <code>nodeModelOutSpecs</code>.
     *
     * @param inSpecs port object specs from predecessor node(s)
     * @param nodeModelOutSpecs the output specs created by the underlying node
     * @return the output specs actually delivered at the node's output ports
     * @throws InvalidSettingsException if the node can't be executed.
     */
    public PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
            PortObjectSpec[] nodeModelOutSpecs) throws InvalidSettingsException;

}
