/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Oct 27, 2008 (wiswedel): created
 */
package org.knime.core.node.exec.dataexchange.in;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Model for the pass-on node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
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
            switch (fv.getType()) {
            case DOUBLE:
                pushFlowVariableDouble(fv.getName(), fv.getDoubleValue());
                break;
            case INTEGER:
                pushFlowVariableInt(fv.getName(), fv.getIntValue());
                break;
            case STRING:
                pushFlowVariableString(fv.getName(), fv.getStringValue());
                break;
            case CREDENTIALS:
                Node.invokePushFlowVariable(this, fv);
                break;
            default:
                throw new RuntimeException("Unknown variable type: "
                        + fv.getType() + " (" + fv + ")");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        int id = m_portObjectIDSettings.getId();
        PortObject obj = PortObjectRepository.get(id);
        if (obj == null) {
            throw new RuntimeException("No port object for id " + id);
        }
        pushFlowVariables();
        PortObject cloneOrSelf = m_portObjectIDSettings.isCopyData() ? PortObjectRepository.copy(obj, exec, exec) : obj;
        return new PortObject[]{cloneOrSelf};
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
     * @param portObjectID the ID of the port object to inject
     * @param flowVariables The flow variables the node should expose
     * @param copyData Wether to deep-clone data (context switch)
     * @throws InvalidSettingsException if the settings are invalid.
     */
    public static void setInputNodeSettings(final NodeSettings s,
            final int portObjectID, final List<FlowVariable> flowVariables, final boolean copyData)
        throws InvalidSettingsException {
        PortObjectIDSettings poSettings = new PortObjectIDSettings();
        poSettings.setId(portObjectID);
        poSettings.setCopyData(copyData);
        poSettings.setFlowVariables(flowVariables);
        if (!s.containsKey("model")) {
            s.addNodeSettings("model");
        }
        NodeSettings modelSettings = s.getNodeSettings("model");
        poSettings.saveSettings(modelSettings);
    }

}
