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
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;

/**
 * This is the model for the business rule node. It takes the user-defined rules and assigns the outcome of the first
 * matching rule to the new cell.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.8
 */
public class RuleEngineVariableNodeModel extends NodeModel implements FlowVariableProvider {
    private final RuleEngineSettings m_settings = new RuleEngineSettings();

    /**
     * Creates a new model.
     *
     * @since 2.8
     */
    protected RuleEngineVariableNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL}, new PortType[]{FlowVariablePortObject.TYPE});
    }

    /**
     * Parses all rules in the settings object.
     *
     * @return a list of parsed rules
     * @throws ParseException if a rule cannot be parsed
     * @since 2.8
     */
    protected List<Rule> parseRules() throws ParseException {
        ArrayList<Rule> rules = new ArrayList<Rule>();

        final Map<String, FlowVariable> availableFlowVariables = getAvailableFlowVariables();
        //SimpleRuleParser ruleParser = new SimpleRuleParser(spec, availableFlowVariables);
        final RuleFactory factory = RuleFactory.getVariableInstance();
        final DataTableSpec spec = new DataTableSpec();
        for (String s : m_settings.rules()) {
            final Rule rule = factory.parse(s, spec, availableFlowVariables);
            if (rule.getCondition().isEnabled()) {
                rules.add(rule);
            }
        }

        return rules;
    }

    /**
     * Creates the flow variable according to the computed value.
     *
     * @param rules The rules to check for match.
     * @throws InvalidSettingsException When there is an error in the settings.
     */
    private void performExecute(final List<Rule> rules) throws InvalidSettingsException {
        String newFlowVar = m_settings.getNewColName();

        final String defaultLabelRaw = m_settings.getDefaultLabel();
        final String defaultLabel;
        DataType preferredDefaultDataType = null;
        switch (m_settings.getDefaultOutputType()) {
            case FlowVariable:
                if (defaultLabelRaw.length() < 5 || !defaultLabelRaw.startsWith("$${")
                        || !defaultLabelRaw.endsWith("}$$")) {
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
                throw new InvalidSettingsException("Columns are not supported!");
                //                if (defaultLabelRaw.length() < 3 || defaultLabelRaw.charAt(0) != '$' || !defaultLabelRaw.endsWith("$")) {
                //                    throw new InvalidSettingsException("Not a column reference: " + defaultLabelRaw);
                //                }
                //                defaultLabel = defaultLabelRaw;
                //                break;
            case PlainText:
                defaultLabel = defaultLabelRaw;
                break;
            case StringInterpolation:
                throw new InvalidSettingsException("String interpolation is not supported yet.");
            default:
                throw new InvalidSettingsException("Not supported outcome type: " + m_settings.getDefaultOutputType());
        }
        final DataType outType =
                computeOutputType(rules, defaultLabel, m_settings.getDefaultOutputType() == OutcomeKind.FlowVariable
                        ? preferredDefaultDataType : null);

        final VariableProvider provider = new VariableProvider() {
            @Override
            public Object readVariable(final String name, final Class<?> type) {
                return RuleEngineVariableNodeModel.this.readVariable(name, type);
            }

            @Override
            public int getRowCount() {
                throw new IllegalStateException("Row count is not available.");
            }

            @Override
            public int getRowIndex() {
                throw new IllegalStateException("Row index is not available.");
            }
        };
        boolean wasMatch = false;
        for (Rule r : rules) {
            if (r.getCondition().matches(null, provider).getOutcome() == MatchState.matchedAndStop) {
                Outcome outcome2 = r.getOutcome();
                //                        r.getSideEffect().perform(row, this);
                final DataCell cell = (DataCell)outcome2.getComputedResult(null, provider);
                wasMatch = true;
                if (outType.equals(StringCell.TYPE) && !cell.isMissing() && !cell.getType().equals(StringCell.TYPE)) {
                    pushFlowVariableString(newFlowVar, cell.toString());
                    break;
                } else {
                    //return cell;
                    if (cell.isMissing()) {
                        throw new UnsupportedOperationException("Missing result, TODO");
                    }
                    if (outType.equals(IntCell.TYPE)) {
                        pushFlowVariableInt(newFlowVar, ((IntValue)cell).getIntValue());
                        break;
                    } else if (outType.equals(DoubleCell.TYPE)) {
                        pushFlowVariableDouble(newFlowVar, ((DoubleValue)cell).getDoubleValue());
                        break;
                    } else if (outType.equals(StringCell.TYPE)) {
                        pushFlowVariableString(newFlowVar, ((StringValue)cell).getStringValue());
                        break;
                    } else {
                        //TODO
                        throw new UnsupportedOperationException("Wrong type: " + cell.getClass());
                        //break;
                    }
                }
            }
        }
        if (!wasMatch) {
            if (m_settings.getDefaultLabel().length() > 0) {
                String l = defaultLabel;
                if (outType.equals(StringCell.TYPE)) {
                    pushFlowVariableString(newFlowVar, l);
                } else {

                    try {
                        int i = Integer.parseInt(l);
                        pushFlowVariableInt(newFlowVar, i);
                        //return new IntCell(i);
                    } catch (NumberFormatException ex) {
                        try {
                            double d = Double.parseDouble(l);
                            //return new DoubleCell(d);
                            pushFlowVariableDouble(newFlowVar, d);
                        } catch (NumberFormatException ex1) {
                            //return new StringCell(l);
                            pushFlowVariableString(newFlowVar, l);
                        }
                    }
                }
            } else {
                //return DataType.getMissingCell();
                throw new UnsupportedOperationException("Missing value!");
            }
        }
    }

    /**
     * Computes the result's {@link DataType}.
     *
     * @param rules The rules.
     * @param defaultLabel The value for default value.
     * @param flowVarType The data type of flow variable if it is selected in the default value. Can be {@code null}.
     * @return The common base type of the outcomes and the default value.
     */
    static DataType computeOutputType(final List<Rule> rules, final String defaultLabel, final DataType flowVarType) {
        // determine output type
        List<DataType> types = new ArrayList<DataType>();
        // add outcome column types
        for (Rule r : rules) {
            types.add(r.getOutcome().getType());
        }

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
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        try {
            parseRules();
            //            ColumnRearranger crea = createRearranger(inSpecs[0], parseRules());
            return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
        } catch (ParseException ex) {
            throw new InvalidSettingsException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        List<Rule> rules = parseRules();
        //            ColumnRearranger crea = createRearranger(inData[0].getDataTableSpec(), rules);
        //exec.createColumnRearrangeTable(inData[0], crea, exec);
        performExecute(rules);
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
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
