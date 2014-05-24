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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 30, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.subnode;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ObjectUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;


/** NodeModel to subnode virtual output node.
 * <p>No API.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class VirtualSubNodeOutputNodeModel extends NodeModel {

    /** Holds the data (specs, objects, flow vars), gets update on reset, configure, execute. */
    private int m_numberOfPorts;
    private VirtualSubNodeOutputConfiguration m_configuration;
    private VirtualSubNodeExchange m_outputExchange;

    /** @param outTypes Output types of subnode (which are input to this node) */
    public VirtualSubNodeOutputNodeModel(final PortType[] outTypes) {
        super(outTypes, new PortType[0]);
        m_numberOfPorts = outTypes.length;
        m_configuration = VirtualSubNodeOutputConfiguration.newDefault(m_numberOfPorts);
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        setNewExchange(new VirtualSubNodeExchange(inSpecs, getAvailableFlowVariables().values()));
        if (m_configuration == null) {
            setWarningMessage("Guessing defaults (excluding all variables)");
            m_configuration = VirtualSubNodeOutputConfiguration.newDefault(m_numberOfPorts);
        }
        return new PortObjectSpec[0];
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec)
            throws Exception {
        setNewExchange(new VirtualSubNodeExchange(inObjects, getAvailableFlowVariables().values()));
        return new PortObject[0];
    }

    /** Called when workflow is loaded to fill the exchange field.
     * @param inObjects Input objects, excluding flow var port. */
    public void postLoadExecute(final PortObject[] inObjects) {
        setNewExchange(new VirtualSubNodeExchange(inObjects, getAvailableFlowVariables().values()));
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        setNewExchange(null);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.saveConfiguration(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new VirtualSubNodeOutputConfiguration(m_numberOfPorts).loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        VirtualSubNodeOutputConfiguration config = new VirtualSubNodeOutputConfiguration(m_numberOfPorts);
        config.loadConfigurationInModel(settings);
        m_configuration = config;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
        final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
        final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no internals
    }

    /** @return the outputExchange, last data seen in configure, execute, (reset). */
    public VirtualSubNodeExchange getOutputExchange() {
        return m_outputExchange;
    }

    /**
     * @param exchange the outputExchange to set
     * @return if changed
     */
    public boolean setNewExchange(final VirtualSubNodeExchange exchange) {
        if (ObjectUtils.notEqual(m_outputExchange, exchange)) {
            m_outputExchange = exchange;
            return true;
        }
        return false;
    }

    /**
     * @return Names of the ports
     */
    public String[] getPortNames() {
        return m_configuration.getPortNames();
    }

    /**
     * @return Descriptions of the ports
     */
    public String[] getPortDescriptions() {
        return m_configuration.getPortDescriptions();
    }

}
