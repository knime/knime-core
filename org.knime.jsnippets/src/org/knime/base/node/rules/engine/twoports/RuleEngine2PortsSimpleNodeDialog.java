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
 *   1 June 2015 (Gabor): created
 */
package org.knime.base.node.rules.engine.twoports;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.knime.base.node.rules.engine.Condition;
import org.knime.base.node.rules.engine.Rule;
import org.knime.base.node.rules.engine.RuleEngineNodeModel;
import org.knime.base.node.rules.engine.RuleFactory;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * The base of the Rule * (Dictionary) node dialogs.
 *
 * @author Gabor Bakos
 */
abstract class RuleEngine2PortsSimpleNodeDialog extends DataAwareNodeDialogPane {
    /** The rule column selector. */
    @SuppressWarnings("unchecked")
    protected final ColumnSelectionPanel m_ruleColumn = new ColumnSelectionPanel((Border)null, StringValue.class);

    /** The optional outcome column selector. */
    protected final ColumnSelectionPanel m_outcomeColumn;

    protected final JCheckBox m_treatOutcomesWithDollarAsReferences = new JCheckBox("Treat values starting with $ as references");

    /** The computed outcome type label. */
    protected final JLabel m_outcomeType = new JLabel(DataType.getMissingCell().getType().getIcon());

    /** The label when no errors were detected in the input rules. */
    protected final JLabel m_noErrors = new JLabel("No errors in ruleset detected.");

    /** The errors with 3 columns: line, rule, error message. */
    @SuppressWarnings("serial")
    private final DefaultTableModel m_errorsModel = new DefaultTableModel(
        new String[]{"line", "rule", "error message"}, 0) {
        @Override
        public boolean isCellEditable(final int row, final int column) {
            return false;
        }
    };

    private final JTable m_errors = new JTable(m_errorsModel);

    /** Read only text area for the warnings. */
    protected final JTextArea m_warnings = new JTextArea(3, 100);

    private final RuleEngine2PortsSimpleSettings m_settings;

    private BufferedDataTable m_rules;

    /** The kind of node to customize the dialog. */
    protected RuleNodeSettings m_ruleType;

    /** The ActionListener calling {@link #updateErrorsAndWarnings()}. */
    ActionListener m_updateErrorsAndWarnings;

    private JScrollPane m_errorPanel;

    /**
     * Constructs the dialog.
     *
     * @param settings The common settings.
     * @param ruleType The kind of the node.
     * @see #addAppendOrReplace(JPanel, GridBagConstraints)
     */
    RuleEngine2PortsSimpleNodeDialog(final RuleEngine2PortsSimpleSettings settings,
        final RuleNodeSettings ruleType) {
        super();
        Class<? extends DataValue>[] types;
        m_ruleType = ruleType;
        m_settings = settings;
        switch (ruleType) {
            case PMMLRule:
            case RuleEngine:
            case VariableRule:
                types = ofType(StringValue.class, DoubleValue.class, BooleanValue.class);
                break;
            case RuleFilter:
            case RuleSplitter:
                types = ofType(StringValue.class, BooleanValue.class);
                break;
            default:
                throw new IllegalStateException("Unknow rule type: " + ruleType);
        }
        m_outcomeColumn = new ColumnSelectionPanel((Border)null, new DataValueColumnFilter(types), true);
        addTab("Settings", createSettingsPanel(ruleType));
        m_updateErrorsAndWarnings = e -> updateErrorsAndWarnings();
        m_ruleColumn.addActionListener(m_updateErrorsAndWarnings);
        m_outcomeColumn.addActionListener(m_updateErrorsAndWarnings);
        m_outcomeColumn.addActionListener(e -> m_treatOutcomesWithDollarAsReferences.setEnabled(m_outcomeColumn.getSelectedColumn() != null));
    }

    /**
     * @param classes An array of classes.
     * @return The input array (in an unsafe way).
     */
    @SafeVarargs
    private final Class<? extends DataValue>[] ofType(final Class<? extends DataValue>... classes) {
        return classes;
    }

    /**
     * @return the settings
     */
    protected RuleEngine2PortsSimpleSettings getSettings() {
        return m_settings;
    }

    /**
     * @param settingsType The kind of the dialog.
     * @return The "Settings" panel.
     */
    protected JPanel createSettingsPanel(final RuleNodeSettings settingsType) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.ipadx = 4;
        gbc.ipady = 4;
        gbc.insets = new Insets(1, 2, 0, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Rules column:"), gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_ruleColumn, gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        panel.add(new JLabel("=>"), gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_outcomeColumn, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        panel.add(m_outcomeType, gbc);
        gbc.gridy++;
        gbc.gridx--;
        panel.add(m_treatOutcomesWithDollarAsReferences, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        addAppendOrReplace(panel, gbc);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        gbc.gridwidth = 5;
        m_errorPanel = new JScrollPane(m_errors);
        m_errorPanel.setBorder(new TitledBorder("Errors"));
        panel.add(m_errorPanel, gbc);
        panel.add(m_noErrors, gbc);
        m_errors.getColumnModel().getColumn(0).setMaxWidth(1000);
        m_errors.getColumnModel().getColumn(0).setPreferredWidth(44);
        final TableColumn contentColumn = m_errors.getColumnModel().getColumn(1);
        final DefaultTableCellRenderer origRenderer = new DefaultTableCellRenderer();
        contentColumn.setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                Component ret =
                    origRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof MissingValue) {
                    MissingValue mv = (MissingValue)value;
                    if (ret instanceof JLabel) {
                        JLabel retLabel = (JLabel)ret;
                        retLabel.setText(mv.getError());
                    }
                    ret.setBackground(Color.RED);
                }
                return ret;
            }
        });

        gbc.gridy++;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JScrollPane warningPanel = new JScrollPane(m_warnings);
        warningPanel.setBorder(new TitledBorder("Warnings"));
        m_warnings.setFont(UIManager.getFont("TextField.font"));
        panel.add(warningPanel, gbc);
        return panel;
    }

    /**
     * Adds additional controls after the rule and outcome controls, like variable name, TRUE outcome handling or output
     * column name/selection.
     *
     * @param panel The panel where the controls should be added.
     * @param gbc The {@link GridBagConstraints} to use and modify.
     */
    protected abstract void addAppendOrReplace(final JPanel panel, final GridBagConstraints gbc);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setRuleColumn(m_ruleColumn.getSelectedColumn());
        getSettings().setOutcomeColumn(m_outcomeColumn.getSelectedColumn());
        getSettings().setTreatOutcomesAsReferences(m_treatOutcomesWithDollarAsReferences.isSelected());
        getSettings().saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        setRules(null);
        final DataTableSpec inSpec = (DataTableSpec)specs[RuleEngine2PortsNodeModel.DATA_PORT], secondSpec =
            (DataTableSpec)specs[RuleEngine2PortsNodeModel.RULE_PORT];
        getSettings().loadSettingsDialog(settings, inSpec, secondSpec);
        m_ruleColumn.update(secondSpec, getSettings().getRuleColumn());
        m_outcomeColumn.update(secondSpec, getSettings().getOutcomeColumn());
        m_treatOutcomesWithDollarAsReferences.setSelected(getSettings().isTreatOutcomesAsReferences());
        m_errorsModel.setRowCount(0);
        m_warnings.setText("");
        setEnabled();
    }

    /**
     * Sets enabledness of the controls.
     */
    protected void setEnabled() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
        throws NotConfigurableException {
        PortObject inputPort = input[RuleEngine2PortsNodeModel.DATA_PORT];
        BufferedDataTable data = inputPort instanceof BufferedDataTable ? (BufferedDataTable)inputPort : null;
        BufferedDataTable rules = (BufferedDataTable)input[RuleEngine2PortsNodeModel.RULE_PORT];
        loadSettingsFrom(settings, new PortObjectSpec[]{data == null ? null : data.getDataTableSpec(),
            rules == null ? null : rules.getDataTableSpec()});
        setRules(rules);
        updateErrorsAndWarnings();
    }

    /**
     * Updates the errors table, the warning text area and the computed outcome type.
     */
    protected void updateErrorsAndWarnings() {
        m_errorsModel.setRowCount(0);
        hideErrors();
        m_warnings.setText("");
        m_outcomeType.setIcon(DataType.getMissingCell().getType().getIcon());
        //Checking data from second input port
        final int ruleIdx =
            getRules() == null ? -1 : getRules().getSpec().findColumnIndex(m_ruleColumn.getSelectedColumn());
        final int outcomeIdx =
            getRules() == null ? -1 : getRules().getSpec().findColumnIndex(m_outcomeColumn.getSelectedColumn());
        if (getRules() != null && isSpecAvailable() && ruleIdx >= 0) {
            RuleFactory factory = ruleFactory();
            long lineNo = 0;
            boolean wasCatchAll = false;
            final boolean firstHit = isFirstHit();
            List<Rule> rules = new ArrayList<>();
            for (DataRow dataRow : getRules()) {
                ++lineNo;
                DataCell ruleCell = dataRow.getCell(ruleIdx);
                if (ruleCell.isMissing()) {
                    //                    String cellValue = "?";
                    //                    if (ruleCell instanceof MissingValue) {
                    //                        cellValue += " (" + ((MissingValue)ruleCell).getError() + ")";
                    //                    }
                    m_errorsModel.addRow(new Object[]{dataRow.getKey(), ruleCell, "Missing cell"});
                    showErrors();
                }
                if (ruleCell instanceof StringValue) {
                    StringValue ruleSV = (StringValue)ruleCell;
                    String ruleText = ruleSV.getStringValue().replaceAll("[\r\n]+", " ");
                    if (outcomeIdx >= 0) {
                        DataCell outcome = dataRow.getCell(outcomeIdx);
                        String outcomeString;
                        try {
                            outcomeString = m_settings.asStringFailForMissing(outcome);
                        } catch (InvalidSettingsException e) {
                            outcomeString = "?";
                        }
                        if (m_ruleType.onlyBooleanOutcome()) {
                            if ("\"TRUE\"".equalsIgnoreCase(outcomeString)) {
                                outcomeString = "TRUE";
                            } else if ("\"FALSE\"".equalsIgnoreCase(outcomeString)) {
                                outcomeString = "FALSE";
                            }
                        }
                        ruleText += " => " + outcomeString;
                    }
                    try {
                        Rule rule = factory.parse(ruleText, getDataSpec(), getAvailableFlowVariables());
                        rules.add(rule);
                        String origWarning = !m_warnings.getText().isEmpty() ? m_warnings.getText() + "\n" : "";
                        Condition cond = rule.getCondition();
                        if (cond.isEnabled()) {//not comment
                            if (cond.isCatchAll() && !wasCatchAll && firstHit && lineNo < getRules().size()) {
                                m_warnings.setText(origWarning + "No rules will match after line " + lineNo + " ("
                                    + dataRow.getKey() + "). Because of rule: " + ruleText);
                            }
                            wasCatchAll |= cond.isCatchAll() && firstHit;
                            if (!wasCatchAll && cond.isConstantFalse()) {
                                m_warnings.setText(origWarning + "The rule in line " + lineNo + " (" + dataRow.getKey()
                                    + ") will never match: " + ruleText);
                            }
                        }
                    } catch (ParseException e) {
                        m_errorsModel.addRow(new Object[]{dataRow.getKey(), ruleText, e.getMessage()});
                        showErrors();
                    }
                } else {
                    //Missings were handled previously
                    if (!ruleCell.isMissing()) {
                        m_errorsModel.addRow(new Object[]{dataRow.getKey(), ruleCell.toString(),
                            "Wrong type: " + ruleCell.getType()});
                    }
                }
            }
            final DataColumnSpec outcomeSpec = m_outcomeColumn.getSelectedColumnAsSpec();
            DataType dataType = RuleEngineNodeModel.computeOutputType(rules,
                outcomeSpec == null ? StringCell.TYPE : outcomeSpec.getType(), m_ruleType,
                    getSettings().isDisallowLongOutputForCompatibility());
            if (dataType != null) {
                m_outcomeType.setIcon(dataType.getIcon());
            }
        }
    }

    /**
     * Hides errors panel.
     */
    protected void hideErrors() {
        m_errorPanel.setVisible(false);
        m_noErrors.setVisible(true);
    }

    /**
     * Shows errors panel.
     */
    private void showErrors() {
        m_errorPanel.setVisible(true);
        m_noErrors.setVisible(false);
    }

    /**
     * @return The input data's {@link DataTableSpec}. Can be {@code null}!
     */
    protected DataTableSpec getDataSpec() {
        return null;
    }

    /**
     * @return Whether input data's {@link DataTableSpec} is available (should be {@code false} for variable input port).
     */
    protected boolean isSpecAvailable() {
        return true;
    }

    /**
     * @return Do we use the {@link RuleEngine2PortsSettings.RuleSelectionMethod#FirstHit} (even implicitly) or not.
     */
    boolean isFirstHit() {
        return true;//!m_settings.isPMMLRuleSet() || m_settings.getRuleSelectionMethod() == RuleSelectionMethod.FirstHit;
    }

    /**
     * @return The {@link RuleFactory} belonging to {@link #m_ruleType}.
     */
    abstract RuleFactory ruleFactory();

    /**
     * @return the rules
     */
    public BufferedDataTable getRules() {
        return m_rules;
    }

    /**
     * @param rules the rules to set
     */
    public void setRules(final BufferedDataTable rules) {
        this.m_rules = rules;
    }

}