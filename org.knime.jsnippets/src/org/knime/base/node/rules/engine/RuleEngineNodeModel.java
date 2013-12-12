/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * History
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules.engine;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.base.node.rules.engine.Condition.MatchOutcome.MatchState;
import org.knime.base.node.rules.engine.Rule.Outcome;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
    private final RuleEngineSettings m_settings = new RuleEngineSettings();

    private int m_rowCount;

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
        RuleFactory factory = RuleFactory.getInstance(nodeType);
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

    private ColumnRearranger createRearranger(final DataTableSpec inSpec, final List<Rule> rules)
            throws InvalidSettingsException {
        ColumnRearranger crea = new ColumnRearranger(inSpec);

        String newColName =
            m_settings.isAppendColumn() ? DataTableSpec.getUniqueColumnName(inSpec, m_settings.getNewColName())
                : m_settings.getReplaceColumn();

        final DataType outType =
                computeOutputType(rules, RuleNodeSettings.RuleEngine);

        DataColumnSpec cs = new DataColumnSpecCreator(newColName, outType).createSpec();


        VariableProvider.SingleCellFactoryProto cellFactory = new VariableProvider.SingleCellFactoryProto(cs) {
            @Override
            public DataCell getCell(final DataRow row) {
                for (Rule r : rules) {
                    if (r.getCondition().matches(row, this).getOutcome() == MatchState.matchedAndStop) {
                        Outcome outcome2 = r.getOutcome();
                        //                        r.getSideEffect().perform(row, this);
                        final DataCell cell = (DataCell)outcome2.getComputedResult(row, this);
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

            @Override
            public Object readVariable(final String name, final Class<?> type) {
                return RuleEngineNodeModel.this.readVariable(name, type);
            }

            @Override
            public int getRowCount() {
                return RuleEngineNodeModel.this.getRowCount();
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
     * Computes the output's type based on the types of the outcomes and on the option of allowed boolean outcome.
     *
     * @param types The type of outcomes.
     * @param allowBooleanOutcome Allow or not boolean results in outcome?
     * @return The {@link DataType} specifying the result's type.
     */
    public static DataType computeOutputType(final List<DataType> types, final boolean allowBooleanOutcome) {
        final DataType outType;
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
            outType = StringCell.TYPE;
        }
        return outType;
    }

    /**
     * Computes the outcome's type.
     *
     * @param rules The {@link Rule}s.
     * @param nodeType The {@link RuleNodeSettings}.
     * @return The type of the output according to {@code rules} and {@code nodeType}.
     */
    public static DataType computeOutputType(final List<Rule> rules, final RuleNodeSettings nodeType) {
        // determine output type
        List<DataType> types = new ArrayList<DataType>();
        // add outcome column types
        for (Rule r : rules) {
            types.add(r.getOutcome().getType());
        }
        return computeOutputType(types, nodeType.allowBooleanOutcome());
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
    public int getRowCount() {
        return m_rowCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        try {
            m_rowCount = -1;
            ColumnRearranger crea = createRearranger(inSpecs[0], parseRules(inSpecs[0], RuleNodeSettings.RuleEngine));
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
        m_rowCount = inData[0].getRowCount();
        try {
            List<Rule> rules = parseRules(inData[0].getDataTableSpec(), RuleNodeSettings.RuleEngine);
            ColumnRearranger crea = createRearranger(inData[0].getDataTableSpec(), rules);

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
}
