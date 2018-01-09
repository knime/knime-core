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
package org.knime.base.node.rules.engine.twoports;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableLong;
import org.knime.base.node.rules.engine.Condition.MatchOutcome.MatchState;
import org.knime.base.node.rules.engine.RowAppenderRowOutput;
import org.knime.base.node.rules.engine.Rule;
import org.knime.base.node.rules.engine.RuleEngineNodeModel;
import org.knime.base.node.rules.engine.RuleFactory;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.base.node.rules.engine.Util;
import org.knime.base.node.rules.engine.VariableProvider;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.RowAppender;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;

/**
 * This is the model for the Rule-based Row Splitter (Dictionary) node. It takes the user-defined rules and assigns the
 * row to the first or the second outport (the second can be dummy).
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Gabor Bakos
 */
class RuleEngineFilter2PortsNodeModel extends RuleEngineNodeModel {
    /** Configuration key for include on match parameter. */
    static final String CFGKEY_INCLUDE_ON_MATCH = "include";

    /** Default value for the include on match parameter. */
    static final boolean DEFAULT_INCLUDE_ON_MATCH = true;

    /** Index of the data port. */
    static final int DATA_PORT = 0;

    /** Index of the rule port. */
    static final int RULE_PORT = 1;

    private boolean m_includeOnMatch = DEFAULT_INCLUDE_ON_MATCH;

    private final RuleEngine2PortsSimpleSettings m_settings = new RuleEngine2PortsSimpleSettings();

    private final List<String> m_rulesList = new ArrayList<>();

    /**
     * Creates a new model.
     *
     * @param filter Filter or splitter? {@code true} -> filter, {@code false} -> splitter.
     */
    public RuleEngineFilter2PortsNodeModel(final boolean filter) {
        super(2, filter ? 1 : 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        String warning = RuleEngine2PortsNodeModel.autoGuessRuleColumnName(inSpecs, m_settings);
        if (warning != null) {
            setWarningMessage(warning);
        }
        CheckUtils.checkSettingNotNull(
            inSpecs[RuleEngine2PortsNodeModel.RULE_PORT].getColumnSpec(m_settings.getRuleColumn()),
            "No rule column with name: " + m_settings.getRuleColumn() + " is present in the rules table");
        CheckUtils.checkSetting(
            m_settings.getOutcomeColumn() == null
                || inSpecs[RuleEngine2PortsNodeModel.RULE_PORT].findColumnIndex(m_settings.getOutcomeColumn()) >= 0,
            "No outcome column with name: " + m_settings.getOutcomeColumn() + " is present in the rules table");
        final DataTableSpec[] ret = new DataTableSpec[getNrOutPorts()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = inSpecs[DATA_PORT];
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable ruleTable = inData[RuleEngine2PortsNodeModel.RULE_PORT];
        BufferedDataTable dataTable = inData[RuleEngine2PortsNodeModel.DATA_PORT];
        //        m_rulesList.clear();
        //        m_rulesList.addAll(RuleEngineVariable2PortsNodeModel.rules(ruleTable, m_settings, RuleNodeSettings.RuleFilter));
        //        final List<Rule> rules = parseRules(dataTable.getDataTableSpec(), RuleNodeSettings.RuleFilter);
        final BufferedDataContainer first = exec.createDataContainer(dataTable.getDataTableSpec(), true);
        final int nrOutPorts = getNrOutPorts();
        final RowAppender second =
            nrOutPorts > 1 ? exec.createDataContainer(dataTable.getDataTableSpec(), true) : new RowAppender() {
                @Override
                public void addRowToTable(final DataRow row) {
                    //do nothing
                }
            };
        //        final RowAppender[] containers = new RowAppender[]{first, second};
        //        final int matchIndex = m_includeOnMatch ? 0 : 1;
        //        final int otherIndex = 1 - matchIndex;
        //
        final BufferedDataTable[] ret = new BufferedDataTable[nrOutPorts];
        //        try {
        //            final MutableLong rowIdx = new MutableLong();
        //            final long rows = inData[DATA_PORT].size();
        //            final VariableProvider provider = new VariableProvider() {
        //                @Override
        //                public Object readVariable(final String name, final Class<?> type) {
        //                    return RuleEngineFilter2PortsNodeModel.this.readVariable(name, type);
        //                }
        //
        //                @Override
        //                @Deprecated
        //                public int getRowCount() {
        //                    throw new UnsupportedOperationException();
        //                }
        //
        //                @Override
        //                public long getRowCountLong() {
        //                    return rows;
        //                }
        //
        //                @Override
        //                @Deprecated
        //                public int getRowIndex() {
        //                    throw new UnsupportedOperationException();
        //                }
        //
        //                @Override
        //                public long getRowIndexLong() {
        //                    return rowIdx.longValue();
        //                }
        //            };
        //            for (DataRow row : inData[DATA_PORT]) {
        //                rowIdx.increment();
        //                exec.setProgress(rowIdx.longValue() / (double)rows, "Adding row " + rowIdx.longValue() + " of " + rows);
        //                exec.checkCanceled();
        //                boolean wasMatch = false;
        //                for (final Rule r : rules) {
        //                    if (r.getCondition().matches(row, provider).getOutcome() == MatchState.matchedAndStop) {
        //                        //                        r.getSideEffect().perform(row, provider);
        //                        DataValue value = r.getOutcome().getComputedResult(row, provider);
        //                        if (value instanceof BooleanValue) {
        //                            final BooleanValue bv = (BooleanValue)value;
        //                            containers[bv.getBooleanValue() ? matchIndex : otherIndex].addRowToTable(row);
        //                        } else {
        //                            containers[matchIndex].addRowToTable(row);
        //                        }
        //                        wasMatch = true;
        //                        break;
        //                    }
        //                }
        //                if (!wasMatch) {
        //                    containers[otherIndex].addRowToTable(row);
        //                }
        //            }
        //        } finally {
        //            first.close();
        //            ret[0] = first.getTable();
        //            if (second instanceof BufferedDataContainer) {
        //                BufferedDataContainer container = (BufferedDataContainer)second;
        //                container.close();
        //                ret[1] = container.getTable();
        //            }
        //        }
        final PortOutput[] outputs =
            new PortOutput[]{new BufferedDataTableRowOutput(first), new RowAppenderRowOutput(second)};
        final StreamableOperator streamableOperator = createStreamableOperator(new PartitionInfo(0, 1),
            new DataTableSpec[]{inData[0].getSpec(), inData[1].getSpec()});
        final PortInput[] inputs = new PortInput[]{new DataTableRowInput(dataTable), new DataTableRowInput(ruleTable)};
        final SimpleStreamableOperatorInternals internals = new SimpleStreamableOperatorInternals();
        internals.getConfig().addLong(CFG_ROW_COUNT, dataTable.size());
        streamableOperator.loadInternals(internals);
        streamableOperator.runFinal(inputs, outputs, exec);
        ret[0] = first.getTable();
        if (ret.length > 1) {
            ret[1] = ((BufferedDataContainer)second).getTable();
        }
        return ret;
    }

    /**
     * Parses all rules in the from the table (assuming {@link #rules()} is safe to call, like
     * {@link #createStreamableOperator(PartitionInfo, PortObjectSpec[])} or
     * {@link #execute(BufferedDataTable[], ExecutionContext)} was called before).
     *
     * @param spec the spec of the table on which the rules are applied.
     * @param nodeType The type of the node from this method is called.
     * @return a list of parsed rules
     * @throws ParseException if a rule cannot be parsed
     * @since 2.12
     */
    @Override
    protected List<Rule> parseRules(final DataTableSpec spec, final RuleNodeSettings nodeType) throws ParseException {
        ArrayList<Rule> rules = new ArrayList<Rule>();

        final Map<String, FlowVariable> availableFlowVariables = getAvailableFlowVariables();
        //SimpleRuleParser ruleParser = new SimpleRuleParser(spec, availableFlowVariables);
        RuleFactory factory = RuleFactory.getInstance(nodeType).cloned();
        factory.disableMissingComparisons();
        factory.disableNaNComparisons();
        int line = 0;
        for (String s : rules()) {
            ++line;
            try {
                final Rule rule = factory.parse(s, spec, availableFlowVariables);
                if (rule.getCondition().isEnabled()) {
                    rules.add(rule);
                }
            } catch (ParseException e) {
                throw Util.addContext(e, s, line);
            }
        }

        return rules;
    }

    /**
     * @return The currently read rules.
     */
    protected List<String> rules() {
        return m_rulesList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsModel(settings);
        m_includeOnMatch = settings.getBoolean(CFGKEY_INCLUDE_ON_MATCH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
        settings.addBoolean(CFGKEY_INCLUDE_ON_MATCH, m_includeOnMatch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RuleEngine2PortsSimpleSettings().loadSettingsModel(settings);
        settings.getBoolean(CFGKEY_INCLUDE_ON_MATCH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] inputPortRoles = super.getInputPortRoles();
        //We can always stream, maybe just need more than one iteration.
        //we cannot be sure before parsing the rules that there are no row indices in the rules, so non-distributed.
        inputPortRoles[0] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        //The rules should be collected without distributed execution.
        inputPortRoles[1] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        return inputPortRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        final OutputPortRole[] outputPortRoles = super.getOutputPortRoles();
        outputPortRoles[0] = OutputPortRole.NONDISTRIBUTED;
        if (outputPortRoles.length > 1) {
            outputPortRoles[1] = outputPortRoles[0];
        }
        return outputPortRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        SimpleStreamableOperatorInternals simpleInternals = (SimpleStreamableOperatorInternals)internals;
        if (simpleInternals.getConfig().containsKey(CFG_ROW_COUNT)) {
            //already iterated
            return false;
        } else {
            //needs one iteration to determine the row count
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            private SimpleStreamableOperatorInternals m_internals;

            /**
             * {@inheritDoc}
             */
            @Override
            public void loadInternals(final StreamableOperatorInternals internals) {
                m_internals = (SimpleStreamableOperatorInternals)internals;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
                //count number of rows
                long count = 0;
                RowInput rowInput = (RowInput)inputs[DATA_PORT];
                while (rowInput.poll() != null) {
                    count++;
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
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                long rowCount = -1L;
                if (m_internals.getConfig().containsKey(CFG_ROW_COUNT)) {
                    rowCount = m_internals.getConfig().getLong(CFG_ROW_COUNT);
                }
                m_rulesList.clear();
                final PortInput rulePort = inputs[RULE_PORT];
                if (rulePort instanceof PortObjectInput) {
                    PortObjectInput poRule = (PortObjectInput)rulePort;
                    m_rulesList.addAll(RuleEngineVariable2PortsNodeModel
                        .rules((BufferedDataTable)poRule.getPortObject(), m_settings, RuleNodeSettings.RuleFilter));
                } else if (rulePort instanceof RowInput) {
                    RowInput riRule = (RowInput)rulePort;
                    m_rulesList.addAll(
                        RuleEngineVariable2PortsNodeModel.rules(riRule, m_settings, RuleNodeSettings.RuleFilter));
                }
                final DataTableSpec spec = (DataTableSpec)inSpecs[DATA_PORT];
                try {
                    parseRules(spec, RuleNodeSettings.RuleSplitter);
                } catch (final ParseException e) {
                    throw new InvalidSettingsException(e);
                }
                final RowInput inputPartitions = (RowInput)inputs[DATA_PORT];
                final List<Rule> rules = parseRules(inputPartitions.getDataTableSpec(), RuleNodeSettings.RuleFilter);
                final RowOutput first = (RowOutput)outputs[0];
                final int nrOutPorts = getNrOutPorts();
                final RowOutput second = nrOutPorts > 1 ? (RowOutput)outputs[1] : new RowOutput() {
                    @Override
                    public void push(final DataRow row) throws InterruptedException {
                        //do nothing
                    }

                    @Override
                    public void close() throws InterruptedException {
                        //do nothing
                    }
                };
                final RowOutput[] containers = new RowOutput[]{first, second};
                final int matchIndex = m_includeOnMatch ? 0 : 1;
                final int otherIndex = 1 - matchIndex;

                try {
                    final MutableLong rowIdx = new MutableLong(0L);
                    final long rows = rowCount;
                    final VariableProvider provider = new VariableProvider() {
                        @Override
                        public Object readVariable(final String name, final Class<?> type) {
                            return RuleEngineFilter2PortsNodeModel.this.readVariable(name, type);
                        }

                        @Override
                        @Deprecated
                        public int getRowCount() {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public long getRowCountLong() {
                            return rows;
                        }

                        @Override
                        @Deprecated
                        public int getRowIndex() {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public long getRowIndexLong() {
                            return rowIdx.longValue();
                        }
                    };
                    DataRow row;
                    while ((row = inputPartitions.poll()) != null) {
                        rowIdx.increment();
                        if (rows > 0) {
                            exec.setProgress(rowIdx.longValue() / (double)rows,
                                () -> "Adding row " + rowIdx.longValue() + " of " + rows);
                        } else {
                            exec.setMessage(() -> "Adding row " + rowIdx.longValue() + " of " + rows);
                        }
                        exec.checkCanceled();
                        boolean wasMatch = false;
                        for (Rule r : rules) {
                            if (r.getCondition().matches(row, provider).getOutcome() == MatchState.matchedAndStop) {
                                //                        r.getSideEffect().perform(row, provider);
                                DataValue value = r.getOutcome().getComputedResult(row, provider);
                                if (value instanceof BooleanValue) {
                                    final BooleanValue bv = (BooleanValue)value;
                                    containers[bv.getBooleanValue() ? matchIndex : otherIndex].push(row);
                                } else {
                                    containers[matchIndex].push(row);
                                }
                                wasMatch = true;
                                break;
                            }
                        }
                        if (!wasMatch) {
                            containers[otherIndex].push(row);
                        }
                    }
                } finally {
                    try {
                        second.close();
                    } finally {
                        first.close();
                    }
                }
            }
        };
    }
}
