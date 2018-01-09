/*
 * ------------------------------------------------------------------------
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
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules.engine;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.knime.base.node.rules.engine.Condition.MatchOutcome.MatchState;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;

/**
 * This is the model for the business rule node. It takes the user-defined rules and assigns the row to the first or the
 * second outport (the second can be dummy).
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.8
 */
public class RuleEngineFilterNodeModel extends RuleEngineNodeModel {
    /** Configuration key for include on match parameter. */
    static final String CFGKEY_INCLUDE_ON_MATCH = "include";

    /** Default value for the include on match parameter. */
    static final boolean DEFAULT_INCLUDE_ON_MATCH = true;

    private SettingsModelBoolean m_includeOnMatch = new SettingsModelBoolean(CFGKEY_INCLUDE_ON_MATCH,
            DEFAULT_INCLUDE_ON_MATCH);

    /**
     * Creates a new model.
     *
     * @param filter Filter or splitter? {@code true} -> filter, {@code false} -> splitter.
     */
    public RuleEngineFilterNodeModel(final boolean filter) {
        super(1, filter ? 1 : 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        try {
            parseRules(inSpecs[0], RuleNodeSettings.RuleFilter);
        } catch (ParseException ex) {
            throw new InvalidSettingsException(ex.getMessage(), ex);
        }
        final DataTableSpec[] ret = new DataTableSpec[getNrOutPorts()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = inSpecs[0];
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        RowInput input = new DataTableRowInput(inData[0]);
        final BufferedDataContainer first = exec.createDataContainer(inData[0].getDataTableSpec(), true);
        final int nrOutPorts = getNrOutPorts();
        final BufferedDataContainer second = exec.createDataContainer(inData[0].getDataTableSpec(), true);
        BufferedDataTableRowOutput[] outputs = new BufferedDataTableRowOutput[] {new BufferedDataTableRowOutput(first), new BufferedDataTableRowOutput(second)};
        execute(input , outputs, inData[0].size(), exec);
        return nrOutPorts == 2 ? new BufferedDataTable[] {outputs[0].getDataTable(), outputs[1].getDataTable()} : new BufferedDataTable[] {outputs[0].getDataTable()};
    }
    /**
     * The real worker.
     * @param inData The input data as {@link RowInput}.
     * @param outputs The output tables.
     * @param rowCount The row count (if available, else {@code -1} is fine).
     * @param exec The {@link ExecutionContext}.
     * @throws ParseException Parsing of rules failed.
     * @throws CanceledExecutionException Execution cancelled.
     * @throws InterruptedException Streaming failed.
     */
    private void execute(final RowInput inData, final RowOutput[] outputs, final long rowCount,
        final ExecutionContext exec) throws ParseException, CanceledExecutionException, InterruptedException {
        final List<Rule> rules = parseRules(inData.getDataTableSpec(), RuleNodeSettings.RuleFilter);
        final int matchIndex = m_includeOnMatch.getBooleanValue() ? 0 : 1;
        final int otherIndex = 1 - matchIndex;

        try {
            final long[] rowIdx = new long[]{0L};
            final long rows = rowCount;
            final VariableProvider provider = new VariableProvider() {
                @Override
                public Object readVariable(final String name, final Class<?> type) {
                    return RuleEngineFilterNodeModel.this.readVariable(name, type);
                }

                @Override
                public long getRowCountLong() {
                    return rows;
                }

                @Deprecated
                @Override
                public int getRowCount() {
                    return KnowsRowCountTable.checkRowCount(rows);
                }

                @Override
                @Deprecated
                public int getRowIndex() {
                    return (int)rowIdx[0];
                }

                @Override
                public long getRowIndexLong() {
                    return rowIdx[0];
                }
            };
            DataRow row;
            while ((row = inData.poll()) != null) {
                rowIdx[0]++;
                exec.setProgress(rowIdx[0] / (double)rows, () -> "Adding row " + rowIdx[0] + " of " + rows);
                exec.checkCanceled();
                boolean wasMatch = false;
                for (Rule r : rules) {
                    if (r.getCondition().matches(row, provider).getOutcome() == MatchState.matchedAndStop) {
                        //                        r.getSideEffect().perform(row, provider);
                        DataValue value = r.getOutcome().getComputedResult(row, provider);
                        final int index;
                        if (value instanceof BooleanValue) {
                            final BooleanValue bv = (BooleanValue)value;
                            index = bv.getBooleanValue() ? matchIndex : otherIndex;
                        } else {
                            index = matchIndex;
                        }
                        if (index < outputs.length) {
                            outputs[index].push(row);
                        }
                        wasMatch = true;
                        break;
                    }
                }
                if (!wasMatch) {
                    if (otherIndex < outputs.length) {
                        outputs[otherIndex].push(row);
                    }
                }
            }
        } finally {
            outputs[0].close();
            if (outputs.length > 1) {
                outputs[1].close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_includeOnMatch.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_includeOnMatch.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_includeOnMatch.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] inputPortRoles = super.getInputPortRoles();
        final RuleFactory ruleFactory = RuleFactory.getInstance(RuleNodeSettings.RuleFilter);
        final boolean isDistributable = !hasNonDistributableRule(ruleFactory);
        //!hasNonStreamingRule(ruleFactory);
        inputPortRoles[0] = isDistributable 
                ? InputPortRole.DISTRIBUTED_STREAMABLE : InputPortRole.NONDISTRIBUTED_STREAMABLE;
        return inputPortRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        final OutputPortRole[] outputPortRoles = super.getOutputPortRoles();
        final RuleFactory ruleFactory = RuleFactory.getInstance(RuleNodeSettings.RuleFilter);
        final boolean isDistributable = !hasNonDistributableRule(ruleFactory);
        Arrays.fill(outputPortRoles, isDistributable ? OutputPortRole.DISTRIBUTED : OutputPortRole.NONDISTRIBUTED);
        return outputPortRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        final DataTableSpec spec = (DataTableSpec)inSpecs[0];
        try {
            parseRules(spec, RuleNodeSettings.RuleFilter);
        } catch (final ParseException e) {
            throw new InvalidSettingsException(e);
        }
        return new StreamableOperator() {

            private SimpleStreamableOperatorInternals m_internals;

            /**
             * {@inheritDoc}
             */
            @Override
            public void loadInternals(final StreamableOperatorInternals internals) {
                m_internals = (SimpleStreamableOperatorInternals) internals;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
                //count number of rows
                long count = 0;
                if (inputs[0] instanceof RowInput) {
                    final RowInput rowInput = (RowInput)inputs[0];
                    while(rowInput.poll()!=null) {
                        count++;
                    }
                } else if (inputs[0] instanceof PortObjectInput) {
                    final PortObjectInput portObjectInput = (PortObjectInput)inputs[0];
                    count += ((BufferedDataTable)portObjectInput.getPortObject()).size();
                }
                m_internals.getConfig().addLong(CFG_ROW_COUNT, count);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public StreamableOperatorInternals saveInternals() {
                return m_internals;
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                long rowCount = -1L;
                if (m_internals.getConfig().containsKey(CFG_ROW_COUNT)) {
                    rowCount = m_internals.getConfig().getLong(CFG_ROW_COUNT);
                }
                RowOutput[] rowOutputs = (outputs instanceof RowOutput[]) ? (RowOutput[])outputs
                    : outputs.length > 1 ? new RowOutput[]{(RowOutput)outputs[0], (RowOutput)outputs[1]}
                        : new RowOutput[]{(RowOutput)outputs[0]};
                execute((RowInput)inputs[0], rowOutputs, rowCount, exec);
            }
        };
    }
}
