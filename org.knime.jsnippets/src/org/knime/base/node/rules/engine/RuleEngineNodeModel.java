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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.rules.engine.Condition.MatchOutcome.MatchState;
import org.knime.base.node.rules.engine.Rule.Outcome;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;

/**
 * This is the model for the business rule node. It takes the user-defined rules and assigns the outcome of the first
 * matching rule to the new cell.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.8
 */
public class RuleEngineNodeModel extends NodeModel implements FlowVariableProvider {
//    private static final String CFGKEY_MISSINGS_AND_NANS_MATCH = "missingsAndNaNsMatch";
//
//    private static final boolean DEFAULT_MISSINGS_AND_NANS_MATCH = true;

    /** Config key for row counts. */
    protected static final String CFG_ROW_COUNT = "ROWCOUNT";

    private final RuleEngineSettings m_settings = new RuleEngineSettings();

    private long m_rowCount;

    /**
     * Creates a new model.
     */
    public RuleEngineNodeModel() {
        this(1, 1);
    }

    /**
     * Creates a new model.
     *
     * @param nrInDataPorts number of input data ports
     * @param nrOutDataPorts number of output data ports
     * @throws NegativeArraySizeException If the number of in- or outputs is smaller than zero.
     * @since 2.8
     */
    protected RuleEngineNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    /**
     * Parses all rules in the settings object.
     *
     * @param spec the spec of the table on which the rules are applied.
     * @param nodeType The type of the node from this method is called.
     * @return a list of parsed rules
     * @throws ParseException if a rule cannot be parsed
     * @since 2.9
     */
    protected List<Rule> parseRules(final DataTableSpec spec, final RuleNodeSettings nodeType) throws ParseException {
        ArrayList<Rule> rules = new ArrayList<Rule>();

        final Map<String, FlowVariable> availableFlowVariables = getAvailableFlowVariables();
        //SimpleRuleParser ruleParser = new SimpleRuleParser(spec, availableFlowVariables);
        RuleFactory factory = RuleFactory.getInstance(nodeType).cloned();
        factory.disableMissingComparisons();
        factory.disableNaNComparisons();
        int line = 0;
        for (String s : m_settings.rules()) {
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

    private ColumnRearranger createRearranger(final DataTableSpec inSpec, final List<Rule> rules, final long rowCount, final boolean updateColSpec)
            throws InvalidSettingsException {
        if (m_settings.isAppendColumn() && m_settings.getNewColName().isEmpty()) {
            throw new InvalidSettingsException("No name for prediction column provided");
        }

        ColumnRearranger crea = new ColumnRearranger(inSpec);

        String newColName =
            m_settings.isAppendColumn() ? DataTableSpec.getUniqueColumnName(inSpec, m_settings.getNewColName())
                : m_settings.getReplaceColumn();

        final DataType outType = computeOutputType(
            rules, RuleNodeSettings.RuleEngine, m_settings.isDisallowLongOutputForCompatibility());

        DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator(newColName, outType);
        if (updateColSpec) {//only update in configure, execute will compute properly
            updateColSpec(rules, outType, colSpecCreator, this);
        }
        DataColumnSpec cs = colSpecCreator.createSpec();

        final boolean disallowLongOutputForCompatibility = m_settings.isDisallowLongOutputForCompatibility();
        VariableProvider.SingleCellFactoryProto cellFactory = new VariableProvider.SingleCellFactoryProto(cs) {
            private long m_rowIndex = -1L;
            @Override
            public DataCell getCell(final DataRow row) {
                m_rowIndex++;
                return getRulesOutcome(outType, row, rules, disallowLongOutputForCompatibility, this);
            }

            @Override
            public Object readVariable(final String name, final Class<?> type) {
                return RuleEngineNodeModel.this.readVariable(name, type);
            }

            @Deprecated
            @Override
            public int getRowIndex() {
                return (int)m_rowIndex;
            }

            @Override
            public long getRowIndexLong() {
                return m_rowIndex;
            }

            @Deprecated
            @Override
            public int getRowCount() {
                return (int)rowCount;
            }

            @Override
            public long getRowCountLong() {
                return rowCount;
            }
        };
        if (m_settings.isAppendColumn()) {
            crea.append(cellFactory);
        } else {
            crea.replace(cellFactory, m_settings.getReplaceColumn());
        }

        return crea;
    }

    /**
     * Updates the prediction column specification if the rule outcomes are computable in advance.
     * <br/>
     * This will add all outcomes, not just the possibles.
     * <br/>
     * Sorry for the high complexity.
     *
     * @param rules The {@link Rule}s we want to analyse.
     * @param outType The output data type.
     * @param colSpecCreator The column creator.
     */
    private static void updateColSpec(final List<Rule> rules, final DataType outType, final DataColumnSpecCreator colSpecCreator, final FlowVariableProvider nm) {
        List<DataValue> results = new ArrayList<DataValue>(rules.size());
        for (Rule rule : rules) {
            try {
                DataValue result =
                    rule.getOutcome().getComputedResult(new DefaultRow("", new double[0]), new VariableProvider() {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        @Deprecated
                        public int getRowCount() {
                            throw new IllegalStateException("We will catch this.");
                        }
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public long getRowCountLong() {
                            throw new IllegalStateException("We will catch this.");
                        }
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        @Deprecated
                        public int getRowIndex() {
                            throw new IllegalStateException("We will catch this.");
                        }
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public long getRowIndexLong() {
                            throw new IllegalStateException("We will catch this.");
                        }
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public Object readVariable(final String arg0, final Class<?> arg1) {
                            return nm.readVariable(arg0, arg1);
                        }
                    });
                results.add(result);
            } catch (RuntimeException e) {
                //We stop, cannot update properly
                return;
            }
        }
        Set<DataCell> values = new LinkedHashSet<DataCell>(results.size());
        if (outType.equals(StringCell.TYPE)) {
            for (DataValue dataValue : results) {
                if (dataValue instanceof StringCell) {
                    values.add((StringCell)dataValue);
                } else if (dataValue instanceof StringValue) {
                    StringValue sv = (StringValue)dataValue;
                    values.add(new StringCell(sv.getStringValue()));
                } else {
                    values.add(new StringCell(dataValue.toString()));
                }
            }
            colSpecCreator.setDomain(new DataColumnDomainCreator(values).createDomain());
        } else if (outType.isCompatible(DoubleValue.class)) {
            DataCell min = new DoubleCell(Double.POSITIVE_INFINITY), max = new DoubleCell(Double.NEGATIVE_INFINITY);
            for (DataValue dataValue : results) {
                if (dataValue instanceof DoubleValue) {
                    DoubleValue dv = (DoubleValue)dataValue;
                    double d = dv.getDoubleValue();
                    min = d < ((DoubleValue)min).getDoubleValue() ? (DataCell)dv : min;
                    max = d > ((DoubleValue)max).getDoubleValue() ? (DataCell)dv : max;
                    values.add((DataCell)dv);
                }
            }
            DataColumnDomainCreator dcdc = new DataColumnDomainCreator(/*values*/);
            if (min instanceof DoubleValue && max instanceof DoubleValue) {
                double mi = ((DoubleValue)min).getDoubleValue(), ma = ((DoubleValue)max).getDoubleValue();
                if (mi != Double.POSITIVE_INFINITY && ma != Double.NEGATIVE_INFINITY && !Double.isNaN(mi)
                    && !Double.isNaN(ma)) {
                    dcdc.setLowerBound(min);
                    dcdc.setUpperBound(max);
                }
            }
            colSpecCreator.setDomain(dcdc.createDomain());
        }
    }

    /**
     * Computes the output's type based on the types of the outcomes and on the option of allowed boolean outcome.
     * <br/>
     * Its usage is discouraged (outcome type defaults to {@link StringCell#TYPE}), please consider {@link #computeOutputType(List, DataType, boolean, boolean)} instead.
     *
     * @param types The type of outcomes.
     * @param allowBooleanOutcome Allow or not boolean results in outcome?
     * @return The {@link DataType} specifying the result's type.
     */
    public static DataType computeOutputType(final List<DataType> types, final boolean allowBooleanOutcome) {
        return computeOutputType(types, StringCell.TYPE, allowBooleanOutcome, false);
    }

    /**
     * Computes the output's type based on the types of the outcomes and on the option of allowed boolean outcome. In
     * case there are no enabled (non-commented out) rules, the type {@code outcomeType} will be used.
     *
     * @param types The type of outcomes.
     * @param outcomeType The default outcome type if no enabled rules were present.
     * @param allowBooleanOutcome Allow or not boolean results in outcome?
     * @param disallowLongOutputForCompatibility see {@link RuleEngineSettings#isDisallowLongOutputForCompatibility()}
     * @return The {@link DataType} specifying the result's type.
     * @since 2.12
     */
    public static DataType computeOutputType(final List<DataType> types, final DataType outcomeType,
        final boolean allowBooleanOutcome, final boolean disallowLongOutputForCompatibility) {
        final DataType outType;
        if (disallowLongOutputForCompatibility) {
            types.replaceAll(t -> LongCell.TYPE.equals(t) ? IntCell.TYPE : t);
        }
        if (types.size() > 0) {
            DataType temp = types.get(0);
            for (int i = 1; i < types.size(); i++) {
                temp = DataType.getCommonSuperType(temp, types.get(i));
            }
            if ((temp.getValueClasses().size() == 1) && temp.getValueClasses().contains(DataValue.class)) {
                // a non-native type, we replace it with string
                temp = StringCell.TYPE;
            }
            outType = allowBooleanOutcome ? temp : BooleanCell.TYPE.isASuperTypeOf(temp) ? IntCell.TYPE : temp;
        } else {
            outType = outcomeType;
        }
        return outType;
    }

    /**
     * Computes the outcome's type.
     * <br/>
     * Its usage is discouraged. Please consider {@link #computeOutputType(List, DataType, RuleNodeSettings, boolean)}
     *
     * @param rules The {@link Rule}s.
     * @param nodeType The {@link RuleNodeSettings}.
     * @param disallowLongOutputForCompatibility TODO
     * @return The type of the output according to {@code rules} and {@code nodeType}.
     */
    public static DataType computeOutputType(final List<Rule> rules, final RuleNodeSettings nodeType,
        final boolean disallowLongOutputForCompatibility) {
        return computeOutputType(rules, StringCell.TYPE, nodeType, disallowLongOutputForCompatibility);
    }
    /**
     * Computes the outcome's type.
     *
     * @param rules The {@link Rule}s.
     * @param outcomeType The outcome column's type.
     * @param nodeType The {@link RuleNodeSettings}.
     * @param disallowLongOutputForCompatibility see {@link RuleNodeSettings}
     * @return The type of the output according to {@code rules} and {@code nodeType}.
     */
    public static DataType computeOutputType(final List<Rule> rules, final DataType outcomeType,
        final RuleNodeSettings nodeType, final boolean disallowLongOutputForCompatibility) {
        // determine output type
        List<DataType> types = new ArrayList<DataType>();
        // add outcome column types
        for (Rule r : rules) {
            types.add(r.getOutcome().getType());
        }
        return computeOutputType(types, outcomeType, nodeType.allowBooleanOutcome(), disallowLongOutputForCompatibility);
    }

    /**
     * @param outType
     * @param row
     * @param r
     * @param isDisallowlongOutputForCompatibility TODO
     * @param variableProvider TODO
     * @return
     * @noreference This method is not intended to be referenced by clients.
     */
    public static final DataCell getRulesOutcome(final DataType outType, final DataRow row, final List<Rule> rules,
        final boolean isDisallowLongOutputForCompatibility, final VariableProvider variableProvider) {
        for (Rule r : rules) {
            if (r.getCondition().matches(row, variableProvider).getOutcome() == MatchState.matchedAndStop) {
                Outcome outcome2 = r.getOutcome();
                //                        r.getSideEffect().perform(row, this);
                DataCell cell = (DataCell)outcome2.getComputedResult(row, variableProvider);
                // in versions < 3.2 the output was never long ... so casting to int
                // (instanceof check for LongCELL as this is what our code generates
                // ... don't want Booleans (also implementing Long), for instance)
                if (cell instanceof LongCell && isDisallowLongOutputForCompatibility) {
                    long l = ((LongValue)cell).getLongValue();
                    if (l > Integer.MAX_VALUE) {
                        throw new RuntimeException("Values larger than " + Integer.MAX_VALUE
                            + " not supported in old instances of the node -- recreate the node "
                            + "(node was created using an KNIME version < 3.2");
                    }
                    cell = new IntCell((int)l);
                }
                if (outType.equals(StringCell.TYPE) && !cell.isMissing()
                        && !cell.getType().equals(StringCell.TYPE)) {
                    return new StringCell(cell.toString());
                } else {
                    return cell;
                }
            }
        }
        return DataType.getMissingCell();
    }

    /** {@inheritDoc} */
    @Override
    public Object readVariable(final String name, final Class<?> type) {
        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return peekFlowVariableInt(name);
        } else if (Double.class.equals(type) || Double.TYPE.equals(type)) {
            return peekFlowVariableDouble(name);
        } else if (String.class.equals(type)) {
            return peekFlowVariableString(name);
        } else {
            throw new IllegalArgumentException("Invalid variable class: " + type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public int getRowCount() {
        return (int)m_rowCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRowCountLong() {
        return m_rowCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        try {
            m_rowCount = -1;
            final List<Rule> rules = parseRules(inSpecs[0], RuleNodeSettings.RuleEngine);
            ColumnRearranger crea = createRearranger(inSpecs[0], rules, -1, true);
            return new DataTableSpec[]{crea.createSpec()};
        } catch (ParseException ex) {
            throw new InvalidSettingsException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        m_rowCount = inData[0].size();
        try {
            List<Rule> rules = parseRules(inData[0].getDataTableSpec(), RuleNodeSettings.RuleEngine);
            ColumnRearranger crea = createRearranger(inData[0].getDataTableSpec(), rules, inData[0].size(), false);
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], crea, exec)};
        } finally {
            m_rowCount = -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //No internal state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        RuleEngineSettings s = new RuleEngineSettings();
        s.loadSettings(settings);
        validateRules(s.rules());
    }

    /**
     * @param rules
     * @throws InvalidSettingsException
     */
    protected void validateRules(final Iterable<String> rules) throws InvalidSettingsException {
        RuleFactory ruleFactory = RuleFactory.getInstance(RuleNodeSettings.RuleEngine).cloned();
        ruleFactory.disableColumnChecks();
        ruleFactory.disableFlowVariableChecks();
        for (String rule : rules) {
            try {
                ruleFactory.parse(rule, null, getAvailableInputFlowVariables());
            } catch (ParseException e) {
                throw new InvalidSettingsException(e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] inputPortRoles = super.getInputPortRoles();
        final RuleFactory ruleFactory = RuleFactory.getInstance(RuleNodeSettings.RuleEngine);
        //hasNonStreamingRule(ruleFactory);
        final boolean hasNonDistributable = hasNonDistributableRule(ruleFactory);
        inputPortRoles[0] = hasNonDistributable ?
            InputPortRole.NONDISTRIBUTED_STREAMABLE : InputPortRole.DISTRIBUTED_STREAMABLE;
        return inputPortRoles;
    }

    /**
     * @param ruleFactory The appropriate {@link RuleFactory}.
     * @return {@code true} iff at least one of the rules has non-distributable component.
     */
    protected boolean hasNonDistributableRule(final RuleFactory ruleFactory) {
        return StreamingUtil.hasNonDistributableRule(ruleFactory, m_settings.rules());
    }

    /**
     * @param ruleFactory The appropriate {@link RuleFactory}.
     * @return {@code true} iff at least one of the rules has non-streamable component.
     */
    protected boolean hasNonStreamingRule(final RuleFactory ruleFactory) {
        return StreamingUtil.hasNonStreamableRule(ruleFactory, m_settings.rules());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        final OutputPortRole[] outputPortRoles = super.getOutputPortRoles();
        final RuleFactory ruleFactory = RuleFactory.getInstance(RuleNodeSettings.RuleEngine);
        outputPortRoles[0] = hasNonDistributableRule(ruleFactory)
            ? OutputPortRole.NONDISTRIBUTED : OutputPortRole.DISTRIBUTED;
        return outputPortRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec spec = (DataTableSpec)inSpecs[0];
        final List<Rule> parsedRules;
        try {
            parsedRules = parseRules(spec, RuleNodeSettings.RuleEngine);
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
                RowInput rowInput = (RowInput) inputs[0];
                while(rowInput.poll()!=null) {
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
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                long rowCount = -1L;
                if (m_internals.getConfig().containsKey(CFG_ROW_COUNT)) {
                    rowCount = m_internals.getConfig().getLong(CFG_ROW_COUNT);
                }

                createRearranger(((RowInput)inputs[0]).getDataTableSpec(), parsedRules, rowCount, false).createStreamableFunction(0, 0).runFinal(inputs, outputs, exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        return new SimpleStreamableOperatorInternals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        if (hasNonStreamingRule(RuleFactory.getInstance(RuleNodeSettings.RuleEngine))) {
            SimpleStreamableOperatorInternals simpleInternals = (SimpleStreamableOperatorInternals) internals;
            if (simpleInternals.getConfig().containsKey(CFG_ROW_COUNT)) {
                //already iterated
                return false;
            } else {
                //needs one iteration to determine the row count
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MergeOperator createMergeOperator() {
        return new MergeOperator() {

            /**
             * {@inheritDoc}
             */
            @Override
            public StreamableOperatorInternals mergeIntermediate(final StreamableOperatorInternals[] operators) {
                //sum up the row counts if necessary
                long count = 0;
                for (int i = 0; i < operators.length; i++) {
                    SimpleStreamableOperatorInternals simpleInternals = (SimpleStreamableOperatorInternals)operators[i];
                    CheckUtils.checkState(simpleInternals.getConfig().containsKey(CFG_ROW_COUNT),
                        "Config for key " + CFG_ROW_COUNT + " isn't set.");
                    try {
                        count += simpleInternals.getConfig().getLong(CFG_ROW_COUNT);
                    } catch (InvalidSettingsException e) {
                        // should not happen since we checked already
                        throw new RuntimeException(e);
                    }
                }

                SimpleStreamableOperatorInternals res = new SimpleStreamableOperatorInternals();
                if(count > 0) {
                    res.getConfig().addLong(CFG_ROW_COUNT, count);
                }
                return res;
            }

            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
                //nothing to do here
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        // nothing to do here
    }

//    /**
//     * @return Creates a {@link SettingsModelBoolean} that allows matching of missing and {@link Double#NaN} values in
//     *         comparisons.
//     */
//    public static SettingsModelBoolean createMissingsAndNaNsMatch() {
//        return new SettingsModelBoolean(CFGKEY_MISSINGS_AND_NANS_MATCH, DEFAULT_MISSINGS_AND_NANS_MATCH);
//    }
}
