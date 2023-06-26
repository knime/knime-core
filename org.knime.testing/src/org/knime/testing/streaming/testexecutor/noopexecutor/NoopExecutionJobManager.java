/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Jun 26, 2023 (wiswedel): created
 */
package org.knime.testing.streaming.testexecutor.noopexecutor;

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
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeExecutionJob;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManagerPanel;
import org.knime.core.node.workflow.NodeExecutionJobReconnectException;

/**
 * Job manager set on nodes, "runs" executing by just setting inactive outputs.
 *
 * @author wiswedel
 */
public final class NoopExecutionJobManager implements NodeExecutionJobManager {

    /** Singleton to be used. */
    public static final NoopExecutionJobManager INSTANCE = new NoopExecutionJobManager();

    private NoopExecutionJobManager() {
    }

    @Override
    public NodeExecutionJob submitJob(final NodeContainer nc, final PortObject[] data) {
        final var job = new NoopExecutionJob(nc, data);
        job.run();
        return job;
    }

    @Override
    public NodeExecutionJobManagerPanel getSettingsPanelComponent(final SplitType nodeSplitType) {
        return null;
    }

    @Override
    public String getID() {
        return NoopExecutionJobManagerFactory.class.getName();
    }

    @Override
    public URL getIcon() {
        return null;
    }

    @Override
    public boolean canDisconnect(final NodeExecutionJob job) {
        return false;
    }

    @Override
    public void saveReconnectSettings(final NodeExecutionJob job, final NodeSettingsWO settings) {
        throw new IllegalStateException();
    }

    @Override
    public NodeExecutionJob loadFromReconnectSettings(final NodeSettingsRO settings, final PortObject[] inports,
        final NodeContainer nc) throws InvalidSettingsException, NodeExecutionJobReconnectException {
        throw new IllegalStateException();
    }

    @Override
    public void disconnect(final NodeExecutionJob job) {
        throw new IllegalStateException();
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        // nothing to save
    }

    @Override
    public void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to load
    }

    @Override
    public PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final PortObjectSpec[] nodeModelOutSpecs)
        throws InvalidSettingsException {
        return nodeModelOutSpecs;
    }

    @Override
    public boolean hasView() {
        return false;
    }

    @Override
    public NodeView<NodeModel> getView(final NodeContainer nc) {
        throw new IllegalStateException();
    }

    @Override
    public String getViewName(final NodeContainer nc) {
        throw new IllegalStateException();
    }

    @Override
    public void resetAllViews() {
        // no views
    }

    @Override
    public void closeAllViews() {
        // no views
    }

    @Override
    public boolean canSaveInternals() {
        return false;
    }

    @Override
    public String toString() {
        return "Noop Job Manager";
    }

    @Override
    public void saveInternals(final ReferencedFile directory) throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public void loadInternals(final ReferencedFile directory) throws IOException {
        throw new IllegalStateException();
    }

}
