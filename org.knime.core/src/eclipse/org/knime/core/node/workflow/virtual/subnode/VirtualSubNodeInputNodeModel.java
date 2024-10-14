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
 *   Apr 7, 2014 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.subnode;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
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
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.report.IReportPortObject;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.workflow.ExecutionEnvironment;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 * NodeModel of the subnode virtual source node.
 * <p>No API.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.10
 */
public final class VirtualSubNodeInputNodeModel extends ExtendedScopeNodeModel implements VirtualSubNodeInOut {

    /** Needed to fetch data and flow object stack. */
    private int m_numberOfPorts;

    /**
     * Used to fetch input data.
     */
    private SubNodeContainer m_subNodeContainer;
    private VirtualSubNodeInputConfiguration m_configuration;
    private IReportPortObject m_inputReportPortObject;

    /**
     * @param subnodeContainer
     * @param outPortTypes
     */
    VirtualSubNodeInputNodeModel(final SubNodeContainer subnodeContainer, final PortType[] outPortTypes) {
        super(new PortType[0], outPortTypes);
        m_numberOfPorts = outPortTypes.length;
        m_subNodeContainer = subnodeContainer;
        m_configuration = VirtualSubNodeInputConfiguration.newDefault(m_numberOfPorts);
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
            final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new VirtualSubNodeInputOperator();
    }

    private final class VirtualSubNodeInputOperator extends StreamableOperator {
        @Override
        public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
            assert inputs.length == 0;
            final var inputData = ArrayUtils.remove(m_subNodeContainer.fetchInputData(exec), 0);
            for (int i = 0; i < outputs.length; i++) {
                if (!BufferedDataTable.TYPE.equals(getOutPortType(i))) {
                    ((PortObjectOutput)outputs[i]).setPortObject(inputData[i]);
                    continue;
                }
                // Only stream port content if it is data
                final var rowOutput = (RowOutput)outputs[i];
                final var table = ((BufferedDataTable)inputData[i]);
                try (final var in = table.cursor();
                        final var out = rowOutput.asWriteCursor(table.getSpec())) {
                    while (in.canForward()) {
                        out.forward().setFrom(in.forward());
                    }
                } finally {
                    // these cursors can throw interrupted exceptions, so we have to make sure we close the output
                    // even if this flies past us
                    rowOutput.close();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDispose() {
        super.onDispose();
        m_subNodeContainer = null;
    }

    public FlowObjectStack getSubNodeContainerFlowObjectStack() {
        return m_subNodeContainer.getFlowObjectStack();
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] executeModel(final PortObject[] rawData,
        final ExecutionEnvironment exEnv, final ExecutionContext exec) throws Exception {
        CheckUtils.checkNotNull(m_subNodeContainer, "No Component container set");
        PortObject[] inputData = m_subNodeContainer.fetchInputData(exec);
        if (inputData == null) {
            setWarningMessage("Not all inputs available");
            Thread.currentThread().interrupt();
            return null;
        }
        // a node is marked as inactive if any of its inputs is inactive, including the flow variable port to which
        // the plain "execute" method has no access.
        boolean containsInactive = false;
        for (PortObject o : inputData) {
            if (o instanceof InactiveBranchPortObject) {
                containsInactive = true;
                break;
            }
        }
        if (containsInactive) {
            PortObject[] clone = ArrayUtils.clone(inputData);
            Arrays.fill(clone, InactiveBranchPortObject.INSTANCE);
            return clone;
        } else {
            return super.executeModel(rawData, exEnv, exec);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec)
            throws Exception {
        PortObject[] inputData = m_subNodeContainer.fetchInputData(exec);
        pushFlowVariables();
        int nodePortCount = inputData.length - /* flow var port */ 1;
        if (m_subNodeContainer.getReportConfiguration().isPresent()) {
            m_inputReportPortObject = (IReportPortObject)inputData[inputData.length - 1];
            nodePortCount -= 1;
        }
        PortObject[] resultData = new PortObject[nodePortCount];
        for (int i = 0; i < nodePortCount; i++) {
            PortObject o = inputData[i + 1];
            resultData[i] = o instanceof BufferedDataTable bdt ? exec.createWrappedTable(bdt) : o;
        }
        return resultData;
    }

    /**
     * @throws Exception
     */
    private void pushFlowVariables() throws InvalidSettingsException {
        String prefix = m_configuration.getFlowVariablePrefix() == null ? "" : m_configuration.getFlowVariablePrefix();
        FlowVariableFilterConfiguration filterConfiguration = m_configuration.getFilterConfiguration();
        Map<String, FlowVariable> availableVariables =
                getSubNodeContainerFlowObjectStack().getAvailableFlowVariables(Type.values());
        FilterResult filtered = filterConfiguration.applyTo(availableVariables);
        String[] includes = filtered.getIncludes();
        ArrayUtils.reverse(includes);
        for (String include : includes) {
            FlowVariable f = availableVariables.get(include);
            switch (f.getScope()) {
                case Global:
                    // ignore global flow vars
                    continue;
                case Flow:
                case Local:
                default:
            }
            final String name = prefix + f.getName();
            Node.invokePushFlowVariable(this, f.withNewName(name));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        CheckUtils.checkSetting(m_subNodeContainer != null, "No subnode container set");
        if (m_configuration == null) {
            setWarningMessage("Guessing defaults (excluding all variables)");
            m_configuration = VirtualSubNodeInputConfiguration.newDefault(m_numberOfPorts);
        }
        final PortObjectSpec[] inputSpecs = m_subNodeContainer.fetchInputSpec();
        final int nodePortCount = inputSpecs.length - /* flow var port */ 1
            - (m_subNodeContainer.getReportConfiguration().isPresent() ? 1 : 0);
        final PortObjectSpec[] specsNoFlowVar = new PortObjectSpec[nodePortCount];
        for (int i = 0; i < nodePortCount; i++) {
            PortObjectSpec pos = inputSpecs[i + 1]; // off by one (flow var port)
            CheckUtils.checkSettingNotNull(pos,
                "Component input port %d is not connected or doesn't have meta data", i + 1);
            specsNoFlowVar[i] = pos;
        }
        pushFlowVariables();
        return specsNoFlowVar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_inputReportPortObject = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.saveConfiguration(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new VirtualSubNodeInputConfiguration(m_numberOfPorts).loadConfigurationInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        VirtualSubNodeInputConfiguration config = new VirtualSubNodeInputConfiguration(m_numberOfPorts);
        config.loadConfigurationInModel(settings);
        m_configuration = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    @Override
    public void setSubNodeContainer(final SubNodeContainer subNodeContainer) {
        m_subNodeContainer = subNodeContainer;
    }

    /**
     * @return Description for the sub node
     * @deprecated use {@link SubNodeContainer#getMetadata()} instead
     */
    @Deprecated
    public String getSubNodeDescription() {
        return m_configuration.getSubNodeDescription();
    }

    /**
     * @return Names of the ports
     * @deprecated use {@link SubNodeContainer#getMetadata()} instead
     */
    @Deprecated
    public String[] getPortNames() {
        return m_configuration.getPortNames();
    }

    /**
     * @return Descriptions of the ports
     */
    @Deprecated
    public String[] getPortDescriptions() {
        return m_configuration.getPortDescriptions();
    }

    /**
     * @param desc
     * @deprecated use {@link SubNodeContainer#getMetadata()} instead
     * @since 4.1
     */
    @Deprecated
    public void setSubNodeDescription(final String desc) {
        m_configuration.setSubNodeDescription(desc);
   }

    /**
     * @param names
     * @since 4.1
     * @deprecated use {@link SubNodeContainer#getMetadata()}
     */
    @Deprecated
    public void setPortNames(final String[] names) {
        m_configuration.setPortNames(names);
    }

    /**
     * @param descs
     * @since 4.1
     * @deprecated use {@link SubNodeContainer#getMetadata()}
     */
    @Deprecated
    public void setPortDescriptions(final String[] descs) {
        m_configuration.setPortDescriptions(descs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (m_subNodeContainer == null) {
            return new HiLiteHandler();
        }
        return m_subNodeContainer.getInHiliteHandler(outIndex);
    }

    /**
     * Get the report port object set on the input. An empty optional if the component is not configured
     * to generate a report or this node is not executed.
     * @return That object.
     * @since 5.2
     */
    public Optional<IReportPortObject> getReportObjectFromInput() {
        return Optional.ofNullable(m_inputReportPortObject);
    }
}
