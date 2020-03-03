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
 *   Oct 27, 2008 (wiswedel): created
 */
package org.knime.core.node.exec.dataexchange.in;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.exec.dataexchange.PortObjectIDSettings;
import org.knime.core.node.exec.dataexchange.PortObjectIDSettings.ReferenceType;
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;

/**
 * Model for the pass-on node.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 * @since 3.1
 */
public final class PortObjectInNodeModel extends NodeModel {

    private PortObjectIDSettings m_portObjectIDSettings;

    /** Set no input, one specified output.
     * @param type to represent.
     */
    PortObjectInNodeModel(final PortType type) {
        super(new PortType[0], new PortType[]{type});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_portObjectIDSettings == null || m_portObjectIDSettings.getId() == null) {
            // don't throw exception here as it will be printed to the log
            // assume we have settings upon execution.
            return new PortObjectSpec[1];
        }
        pushFlowVariables();
        int id = m_portObjectIDSettings.getId();
        PortObject obj = PortObjectRepository.get(id);
        if (obj != null) {
            return new PortObjectSpec[]{obj.getSpec()};
        }
        // let's assume that we have a table upon execution
        return new PortObjectSpec[1];
    }

    /** Push flow variables onto stack. */
    private void pushFlowVariables() {
        for (FlowVariable fv : m_portObjectIDSettings.getFlowVariables()) {
            Node.invokePushFlowVariable(this, fv);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        pushFlowVariables();
        return new PortObject[]{getPortObject(exec)};
    }

    // TODO we might have to revisit this when implementing AP-13335
    private PortObject getPortObject(final ExecutionContext exec)
        throws IOException, CanceledExecutionException, URISyntaxException {
        switch (m_portObjectIDSettings.getReferenceType()) {
            case NODE:
                WorkflowManager wfm = NodeContext.getContext().getWorkflowManager();
                if (wfm != null) {
                    return wfm.findNodeContainer(m_portObjectIDSettings.getNodeIDSuffix().prependParent(wfm.getID()))
                        .getOutPort(m_portObjectIDSettings.getPortIdx()).getPortObject();
                } else {
                    throw new IllegalStateException("Not a local workflow");
                }
            case FILE:
                return readPOFromURI(exec);
            case REPOSITORY:
            default:
                int id = m_portObjectIDSettings.getId();
                PortObject obj = PortObjectRepository.get(id);
                if (obj == null) {
                    throw new RuntimeException("No port object for id " + id);
                }
                PortObject cloneOrSelf =
                    m_portObjectIDSettings.isCopyData() ? PortObjectRepository.copy(obj, exec, exec) : obj;
                return cloneOrSelf;
        }
    }

    /**
     * The port object if the reference type is {@link ReferenceType#NODE}, otherwise an empty optional.
     *
     * @return the port object of an other referenced node
     */
    public Optional<PortObject> getPortObject() {
        if (m_portObjectIDSettings.getReferenceType() == ReferenceType.NODE) {
            try {
                return Optional.of(getPortObject(null));
            } catch (IOException | CanceledExecutionException | URISyntaxException ex) {
                //should never happen
                throw new RuntimeException(ex);
            }
        } else {
            return Optional.empty();
        }
    }

    private PortObject readPOFromURI(final ExecutionContext exec)
        throws IOException, URISyntaxException, CanceledExecutionException {
        assert m_portObjectIDSettings.getReferenceType() == ReferenceType.FILE;
        File portFile = FileUtil.resolveToPath(m_portObjectIDSettings.getUri().toURL()).toFile();
        PortObject po;
        if (m_portObjectIDSettings.isTable()) {
            ContainerTable t = DataContainer.readFromZip(portFile);
            po = exec.createBufferedDataTable(t, exec);
            t.clear();

        } else {
            po = PortUtil.readObjectFromFile(portFile, exec);
        }
        return po;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // empty
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        PortObjectIDSettings s = new PortObjectIDSettings();
        s.setCredentialsProvider(getCredentialsProvider());
        s.loadSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        PortObjectIDSettings s = new PortObjectIDSettings();
        s.setCredentialsProvider(getCredentialsProvider());
        s.loadSettings(settings);
        m_portObjectIDSettings = s;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_portObjectIDSettings != null) {
            m_portObjectIDSettings.saveSettings(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
    }

    /**
     * Injects the port object ID and flow variables into the settings of
     * this node. If this node reads back the settings, it will read the port
     * object with the specified ID and variables (during execute)
     *
     * @param s settings object with settings of this node.
     * @param portObjectIDSettings the settings to be set
     * @throws InvalidSettingsException if the settings are invalid.
     */
    public static void setInputNodeSettings(final NodeSettings s, final PortObjectIDSettings portObjectIDSettings)
        throws InvalidSettingsException {
        if (!s.containsKey("model")) {
            s.addNodeSettings("model");
        }
        NodeSettings modelSettings = s.getNodeSettings("model");
        portObjectIDSettings.saveSettings(modelSettings);
    }

    /**
     * @return a copy of the settings representing how the provided port object is referenced
     */
    public PortObjectIDSettings getInputNodeSettingsCopy() {
        final NodeSettings settings = new NodeSettings("copy");
        m_portObjectIDSettings.saveSettings(settings);
        final PortObjectIDSettings copy = new PortObjectIDSettings();
        try {
            copy.loadSettings(settings);
        } catch (InvalidSettingsException ex) {
            throw new RuntimeException(ex);
        }
        return copy;
    }

}
