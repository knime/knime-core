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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.def.node.workflow.NodeStateEvent;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.ExtendedScopeNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectHolder;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.CredentialsStore.CredentialsNode;
import org.knime.core.node.workflow.ExecutionEnvironment;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowLoadHelper;



/** NodeModel to subnode virtual output node.
 * <p>No API.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public final class VirtualSubNodeOutputNodeModel extends ExtendedScopeNodeModel
    implements InactiveBranchConsumer, PortObjectHolder, CredentialsNode {

    /** Holds the data (specs, objects, flow vars), gets update on reset, configure, execute. */
    private int m_numberOfPorts;
    private VirtualSubNodeOutputConfiguration m_configuration;
    private VirtualSubNodeExchange m_outputExchange;
    private SubNodeContainer m_subNodeContainer;

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
        setNewExchange(new VirtualSubNodeExchange(inSpecs, getVisibleFlowVariables()));
        if (m_configuration == null) {
            setWarningMessage("Guessing defaults (excluding all variables)");
            m_configuration = VirtualSubNodeOutputConfiguration.newDefault(m_numberOfPorts);
        }
        return new PortObjectSpec[0];
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] executeModel(final PortObject[] rawData, final ExecutionEnvironment exEnv,
        final ExecutionContext exec) throws Exception {
        PortObject[] result;
        // if optional input is connected and inactive the entire output is inactive
        if (rawData[0] instanceof InactiveBranchPortObject) {
            PortObject[] exchange = new PortObject[getNrOutPorts()];
            Arrays.fill(exchange, InactiveBranchPortObject.INSTANCE);
            setNewExchange(new VirtualSubNodeExchange(exchange, Collections.<FlowVariable>emptyList()));
            result = new PortObject[] {InactiveBranchPortObject.INSTANCE};
        } else {
            result = super.executeModel(rawData, exEnv, exec);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec)
            throws Exception {
        setNewExchange(new VirtualSubNodeExchange(inObjects, getVisibleFlowVariables()));
        return new PortObject[0];
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        InputPortRole[] result = new InputPortRole[getNrInPorts()];
        for (int i = 0; i < result.length; i++) {
            PortType inPortType = getInPortType(i);
            if (BufferedDataTable.TYPE.equals(inPortType)) {
                result[i] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
            } else {
                result[i] = InputPortRole.NONDISTRIBUTED_NONSTREAMABLE;
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(
        final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                    throws Exception {
                PortObject[] inObjects = new PortObject[getNrInPorts()];
                for (int i = 0; i < inObjects.length; i++) {
                    PortType inPortType = getInPortType(i);
                    if (BufferedDataTable.TYPE.equals(inPortType)) {
                        BufferedDataContainer container = exec.createDataContainer((DataTableSpec)inSpecs[i]);
                        DataRow r;
                        while ((r = ((RowInput)inputs[i]).poll()) != null) {
                            container.addRowToTable(r);
                        }
                        container.close();
                        inObjects[i] = container.getTable();
                    } else {
                        inObjects[i] = ((PortObjectInput)inputs[i]).getPortObject();
                    }
                }
                setNewExchange(new VirtualSubNodeExchange(inObjects, getVisibleFlowVariables()));
            }
        };
    }

    /**
     * @return
     */
    private Collection<FlowVariable> getVisibleFlowVariables() {
        Map<String, FlowVariable> filter = new LinkedHashMap<>(
                Node.invokeGetAvailableFlowVariables(this, Type.values()));
        FilterResult result = m_configuration.getFilterConfiguration().applyTo(filter);
        filter.keySet().retainAll(Arrays.asList(result.getIncludes()));
        return filter.values().stream().filter(e -> !e.isGlobalConstant()).collect(Collectors.toList());
    }

    /** @return the configuration - used in test framework, no API.*/
    VirtualSubNodeOutputConfiguration getConfiguration() {
        return m_configuration;
    }

    /** Called when workflow is loaded to fill the exchange field.
     * @param inObjects Input objects, excluding flow var port. */
    public void postLoadExecute(final PortObject[] inObjects) {
        setNewExchange(new VirtualSubNodeExchange(inObjects, getVisibleFlowVariables()));
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
        String[] portNames = config.getPortNames();

        //propagate the port names to the containing subnode container
        //(if sub node container has been set)
        if (m_subNodeContainer != null) {
            for (int i = 0; i < portNames.length; i++) {
                m_subNodeContainer.getOutPort(i + 1).setPortName(portNames[i]);
                m_subNodeContainer.getOutPort(i + 1).stateChanged(new NodeStateEvent(m_subNodeContainer));
            }
        }
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
     * Sets the sub node container this virtual output node is part of.
     *
     * @param subNodeContainer the sub node container
     * @since 3.3
     */
    public void setSubNodeContainer(final SubNodeContainer subNodeContainer) {
        m_subNodeContainer = subNodeContainer;
    }

    /** Called by testing framework to force all available flow variables into output.
     * @since 3.1 */
    public void updateConfigIncludeAllFlowVariables() {
        CheckUtils.checkState(m_configuration != null, "No configuration available");
        m_configuration.getFilterConfiguration().loadDefaults(getAvailableFlowVariables(), true);
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

    /** {@inheritDoc}
     * @since 3.1*/
    @Override
    public PortObject[] getInternalPortObjects() {
        return m_outputExchange.getPortObjects();
    }

    /** {@inheritDoc}
     * @since 3.1*/
    @Override
    public void setInternalPortObjects(final PortObject[] portObjects) {
        setNewExchange(new VirtualSubNodeExchange(portObjects, getVisibleFlowVariables()));
    }

    /** {@inheritDoc}
     * @since 3.2*/
    @Override
    public void doAfterLoadFromDisc(final WorkflowLoadHelper loadHelper,
        final CredentialsProvider credProvider, final boolean isExecuted, final boolean isInactive) {
        // before 3.1 it didn't implement POHolder so node output exchange set although executed
        // otherwise we could assert isExecute --> m_outputExchange != null
        if (isExecuted && m_outputExchange != null) {
            setNewExchange(new VirtualSubNodeExchange(m_outputExchange.getPortObjects(), getVisibleFlowVariables()));
        }
    }

}
