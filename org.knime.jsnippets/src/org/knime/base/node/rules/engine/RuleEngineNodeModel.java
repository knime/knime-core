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
import org.knime.base.node.rules.engine.Rule.OutcomeKind;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
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
     * @param allowNoOutcome Whether to allow no outcome ({@code true}), or is it required ({@code false}).
     * @return a list of parsed rules
     * @throws ParseException if a rule cannot be parsed
     * @since 2.8
     */
    protected List<Rule> parseRules(final DataTableSpec spec, final boolean allowNoOutcome) throws ParseException {
        ArrayList<Rule> rules = new ArrayList<Rule>();

        final Map<String, FlowVariable> availableFlowVariables = getAvailableFlowVariables();
        //SimpleRuleParser ruleParser = new SimpleRuleParser(spec, availableFlowVariables);
        RuleFactory factory = allowNoOutcome ? RuleFactory.getFilterInstance() : RuleFactory.getInstance();
        for (String s : m_settings.rules()) {
            final Rule rule = factory.parse(s, spec, availableFlowVariables);
            if (rule.getCondition().isEnabled()) {
                rules.add(rule);
            }
        }

        return rules;
    }

    private ColumnRearranger createRearranger(final DataTableSpec inSpec, final List<Rule> rules)
            throws InvalidSettingsException {
        ColumnRearranger crea = new ColumnRearranger(inSpec);

        String newColName = DataTableSpec.getUniqueColumnName(inSpec, m_settings.getNewColName());

        final String defaultLabelRaw = m_settings.getDefaultLabel();
        final String defaultLabel;
        DataType preferredDefaultDataType = null;
        switch (m_settings.getDefaultOutputType()) {
            case FlowVariable:
                if (defaultLabelRaw.length() < 5 || !defaultLabelRaw.startsWith("$${") || !defaultLabelRaw.endsWith("}$$")) {
                    throw new InvalidSettingsException("Not a valid flow variable reference: " + defaultLabelRaw);
                }
                final String flowVarName = defaultLabelRaw.substring(4, defaultLabelRaw.length() - 3);
                final FlowVariable flowVariable = getAvailableFlowVariables().get(flowVarName);
                if (flowVariable == null) {
                    throw new InvalidSettingsException("No flow variable is avaialable with name: " + flowVarName);
                }
                defaultLabel = flowVariable.getValueAsString();
                preferredDefaultDataType = Util.toDataType(flowVariable.getType());
                break;
            case Column:
                if (defaultLabelRaw.length() < 3 || defaultLabelRaw.charAt(0) != '$' || !defaultLabelRaw.endsWith("$")) {
                    throw new InvalidSettingsException("Not a column reference: " + defaultLabelRaw);
                }
                defaultLabel = defaultLabelRaw;
                break;
            case PlainText:
                defaultLabel = defaultLabelRaw;
                break;
            case StringInterpolation:
                throw new InvalidSettingsException("String interpolation is not supported yet.");
            default:
                throw new InvalidSettingsException("Not supported outcome type: " + m_settings.getDefaultOutputType());
        }
        final int defaultLabelColumnIndex =
                findDefaultLabelColumnIndex(inSpec, m_settings.getDefaultOutputType(), defaultLabel);
        final DataType outType = computeOutputType(inSpec, rules, defaultLabelColumnIndex, defaultLabel, m_settings.getDefaultOutputType() == OutcomeKind.FlowVariable ? preferredDefaultDataType : null);

        DataColumnSpec cs = new DataColumnSpecCreator(newColName, outType).createSpec();

        crea.append(new VariableProvider.SingleCellFactoryProto(cs) {
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

                if (defaultLabelColumnIndex >= 0) {
                    DataCell cell = row.getCell(defaultLabelColumnIndex);
                    if (outType.equals(StringCell.TYPE) && !cell.getType().equals(StringCell.TYPE)) {
                        return new StringCell(cell.toString());
                    } else {
                        return cell;
                    }
                } else if (m_settings.getDefaultLabel().length() > 0) {
                    String l = defaultLabel;
                    if (outType.equals(StringCell.TYPE)) {
                        return new StringCell(l);
                    }

                    try {
                        int i = Integer.parseInt(l);
                        return new IntCell(i);
                    } catch (NumberFormatException ex) {
                        try {
                            double d = Double.parseDouble(l);
                            return new DoubleCell(d);
                        } catch (NumberFormatException ex1) {
                            return new StringCell(l);
                        }
                    }
                } else {
                    return DataType.getMissingCell();
                }
            }
            @Override
            public Object readVariable(final String name, final Class<?> type) {
                return RuleEngineNodeModel.this.readVariable(name, type);
            }
            @Override
            public int getRowCount() {
                return RuleEngineNodeModel.this.getRowCount();
            }
        });

        return crea;
    }

    /**
     * Finds the column index for the default label.
     *
     * @param inSpec The input table specification.
     * @param defaultOutcomeType How to interpret the {@code defaultLabel} parameter.
     * @param defaultLabel {@link String} representation of the default value, can be empty, but cannot be {@code null}.
     * @return The index of the column, or {@code -1} when it is not requested {@code defaultOutcomeType} is not {@value OutcomeKind#Column}.
     * @throws InvalidSettingsException When {@code defaultLabel} is inconsistent, eg. not properly escaped column name.
     * @see OutcomeKind
     */
    static int findDefaultLabelColumnIndex(final DataTableSpec inSpec, final Rule.OutcomeKind defaultOutcomeType,
                                           final String defaultLabel) throws InvalidSettingsException {
        if (defaultOutcomeType == null) {
            return -1;
        }
        final int defaultLabelColumnIndex;
        switch (defaultOutcomeType) {
            case Column:
                if (defaultLabel.length() < 3) {
                    throw new InvalidSettingsException("Default label is not a column reference");
                }

                if (!defaultLabel.startsWith("$") || !defaultLabel.endsWith("$")) {
                    throw new InvalidSettingsException("Column references in default label must be enclosed in $");
                }
                String colRef = defaultLabel.substring(1, defaultLabel.length() - 1);
                defaultLabelColumnIndex = inSpec.findColumnIndex(colRef/*defaultLabel*/);
                if (defaultLabelColumnIndex == -1) {
                    throw new InvalidSettingsException("Column '" + defaultLabel
                            + "' for default label does not exist in input table");
                }
                break;
            case PlainText:
                defaultLabelColumnIndex = -1;
                break;
            case FlowVariable:
                defaultLabelColumnIndex = -1;
                break;
            case StringInterpolation:
                defaultLabelColumnIndex = -1;
                break;
            default:
                throw new UnsupportedOperationException("Not supported outcome type: " + defaultOutcomeType);
        }
        return defaultLabelColumnIndex;
    }

    /**
     * Computes the result type based on the default value and the rules (and the column types).
     *
     * @param inSpec The input table specification.
     * @param rules The {@link Rule}s.
     * @param defaultLabelColumnIndex The index of column referred by the default label, or a negative number (eg. {@code -1}).
     * @return The {@link DataType} representing the output's type.
     */
    static DataType computeOutputType(final DataTableSpec inSpec, final List<Rule> rules,
                                      final int defaultLabelColumnIndex, final String defaultLabel, final DataType flowVarType) {
        // determine output type
        List<DataType> types = new ArrayList<DataType>();
        // add outcome column types
        for (Rule r : rules) {
            types.add(r.getOutcome().getType());
        }

        if (defaultLabelColumnIndex >= 0) {
            types.add(inSpec.getColumnSpec(defaultLabelColumnIndex).getType());
        } else {
            if (defaultLabel != null && defaultLabel.length() > 0) {
                if (flowVarType != null) {
                    types.add(flowVarType);
                } else {
                    try {
                        Integer.parseInt(defaultLabel);
                        types.add(IntCell.TYPE);
                    } catch (NumberFormatException ex) {
                        try {
                            Double.parseDouble(defaultLabel);
                            types.add(DoubleCell.TYPE);
                        } catch (NumberFormatException ex1) {
                            types.add(StringCell.TYPE);
                        }
                    }
                }
            }
        }
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
            outType = temp;
        } else {
            outType = StringCell.TYPE;
        }
        return outType;
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
            ColumnRearranger crea = createRearranger(inSpecs[0], parseRules(inSpecs[0], false));
            return new DataTableSpec[]{crea.createSpec()};
        } catch (ParseException ex) {
            throw new InvalidSettingsException(ex);
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
            List<Rule> rules = parseRules(inData[0].getDataTableSpec(), false);
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
    }
}
