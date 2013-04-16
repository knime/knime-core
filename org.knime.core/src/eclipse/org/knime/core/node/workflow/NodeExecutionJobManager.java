/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   Apr 12, 2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.net.URL;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;

/**
 * Main entry point for compute intensive jobs. Controls resource (thread)
 * allocation...
 *
 * <p>Subclasses should extend from {@link AbstractNodeExecutionJobManager} (which has one more method
 * {@link AbstractNodeExecutionJobManager#getIconForWorkflow()}).
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
     * {@inheritDoc}
     */
    @Override
    String toString();

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

    /**
     * Returns true if this job manager provides a view.
     *
     * @return true if this job manager provides a view.
     */
    public boolean hasView();

    /**
     * Return a new instance of a node view for the job of the provided node.
     *
     * @param nc the corresponding node container
     * @return the view for the job if the node
     */
    public NodeView<NodeModel> getView(final NodeContainer nc);

    /**
     * Creates a title for the corresponding view.
     *
     * @param nc the corresponding node container.
     * @return the title for the view.
     */
    public String getViewName(final NodeContainer nc);

    /**
     * Called when the underlying node is reset. Clear all open views.
     */
    public void resetAllViews();

    /**
     * Close all open views.
     */
    public void closeAllViews();

    /**
     * @return whether this job manager has meaningful internals, for instance
     *         log files of remote jobs that were run.
     * @see #saveInternals(ReferencedFile)
     */
    public boolean canSaveInternals();

    /**
     * Save the internals of this instance to the target directory. This method
     * is only called if {@link #canSaveInternals()} returns true.
     *
     * @param directory To save to (guaranteed to be empty)
     * @throws IOException If that fails for any reason.
     */
    public void saveInternals(final ReferencedFile directory)
            throws IOException;

    /**
     * Restore the internals from a directory. This is the reverse operation to
     * {@link #saveInternals(ReferencedFile)}. Implementations should consider
     * to keep a pointer to the referenced file and load the internals on demand
     * (in order to speed up the load routines).
     *
     * @param directory To load from.
     * @throws IOException If that fails for any reason.
     */
    public void loadInternals(final ReferencedFile directory)
            throws IOException;

}
