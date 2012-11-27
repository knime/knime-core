/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * Created on Nov 26, 2012 by wiswedel
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
 * Default implementation of an node execution job manager. All canXYZ methods return false
 * (no save, no reconnect, no view ...). The corresponding method (e.g. {@link #disconnect(NodeExecutionJob)} as
 * the performing method to {@link #canDisconnect(NodeExecutionJob)}) will throw an exception as they are not called
 * by the framework.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.7
 */
public abstract class AbstractNodeExecutionJobManager implements NodeExecutionJobManager {

    /** {@inheritDoc} */
    @Override
    public NodeExecutionJobManagerPanel getSettingsPanelComponent(final SplitType nodeSplitType) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canDisconnect(final NodeExecutionJob job) {
        return false;
    }

    /** The icon that is shown in the workflow editor if the corresponding meta node or its parent meta node
     * has this job manager set.
     * @return The url of the icon (or null). Icon should be at most 200x200.
     */
    public URL getIconForWorkflow() {
        return null;
    }

    /** Called when this job manager is set on a meta node and controls the execution of the argument child node.
     * @param child The child node.
     * @return the url of the decorator icon (attached to a node container figure). If null no icon is shown for
     * the node.
     */
    public URL getIconForChild(final NodeContainer child) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void saveReconnectSettings(final NodeExecutionJob job, final NodeSettingsWO settings) {
        throw new IllegalStateException("Can't save settings - no disconnected implemented");
    }

    /** {@inheritDoc} */
    @Override
    public NodeExecutionJob loadFromReconnectSettings(final NodeSettingsRO settings, final PortObject[] inports,
                                                      final NodeContainer nc)
            throws InvalidSettingsException, NodeExecutionJobReconnectException {
        throw new NodeExecutionJobReconnectException("Can't save settings - no disconnected implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect(final NodeExecutionJob job) {
        throw new IllegalStateException("Can't disconnect");
    }

    /** {@inheritDoc} */
    @Override
    public void save(final NodeSettingsWO settings) {
    }

    /** {@inheritDoc} */
    @Override
    public void load(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    public PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final PortObjectSpec[] nodeModelOutSpecs)
            throws InvalidSettingsException {
        return nodeModelOutSpecs;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasView() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public NodeView<NodeModel> getView(final NodeContainer nc) {
        throw new IllegalStateException("No view");
    }

    /** {@inheritDoc} */
    @Override
    public String getViewName(final NodeContainer nc) {
        throw new IllegalStateException("No view");
    }

    /** {@inheritDoc} */
    @Override
    public void resetAllViews() {
    }

    /** {@inheritDoc} */
    @Override
    public void closeAllViews() {
    }

    /** {@inheritDoc} */
    @Override
    public boolean canSaveInternals() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void saveInternals(final ReferencedFile directory) throws IOException {
    }

    /** {@inheritDoc} */
    @Override
    public void loadInternals(final ReferencedFile directory) throws IOException {
    }

}
