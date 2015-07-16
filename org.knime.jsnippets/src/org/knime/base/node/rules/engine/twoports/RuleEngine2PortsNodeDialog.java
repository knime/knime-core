package org.knime.base.node.rules.engine.twoports;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.knime.base.node.rules.engine.RuleFactory;
import org.knime.base.node.rules.engine.RuleNodeSettings;
import org.knime.base.node.rules.engine.Util;
import org.knime.base.node.rules.engine.twoports.RuleEngine2PortsSettings.RuleSelectionMethod;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * <code>NodeDialog</code> for the "Rule Engine (Dictionary)" Node. Applies the rules from the second input port to the
 * first datatable.
 *
 * @author Gabor Bakos
 */
class RuleEngine2PortsNodeDialog extends RuleEngine2PortsSimpleNodeDialog {
    private JTextField m_appendColumn;

    private ColumnSelectionPanel m_replaceColumn;

    private JRadioButton m_append;

    private JRadioButton m_replace;

    private JCheckBox m_pmml;

    private JComboBox<RuleSelectionMethod> m_ruleSelectionMethod;

    private JCheckBox m_hasDefaultScore;

    private JTextField m_defaultScore;

    private JCheckBox m_hasDefaultConfidence;

    private JSpinner m_defaultConfidence;

    private ColumnSelectionPanel m_ruleConfidenceColumn;

    private JCheckBox m_hasDefaultWeight;

    private JSpinner m_defaultWeight;

    private ColumnSelectionPanel m_ruleWeightColumn;

    private JCheckBox m_computeConfidence;

    private JTextField m_predictionConfidenceColumn;

    private JCheckBox m_provideStatistics;

    private ColumnSelectionPanel m_validationColumn;

    private List<JLabel> m_pmmlLabels;

    private List<TitledBorder> m_pmmlBorders;

    private JLabel m_validateLabel;

    private DataTableSpec m_dataSpec;

    //TODO option to name the rule model?

    /**
     * New pane for configuring the Rule Engine (Dictionary) node.
     */
    protected RuleEngine2PortsNodeDialog() {
        super(new RuleEngine2PortsSettings(), RuleNodeSettings.RuleEngine);
        initControls();
        addTab("PMML", createPMMLPanel());
        final ActionListener appendOrReplace = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_replaceColumn.setEnabled(m_replace.isSelected());
                m_appendColumn.setEnabled(m_append.isSelected());
            }
        };
        ButtonGroup bg = new ButtonGroup();
        m_replace.addActionListener(appendOrReplace);
        m_append.addActionListener(appendOrReplace);
        bg.add(m_replace);
        bg.add(m_append);
        m_replace.setSelected(RuleEngine2PortsSettings.DEFAULT_IS_REPLACED_COLUMN);
        m_append.setSelected(!RuleEngine2PortsSettings.DEFAULT_IS_REPLACED_COLUMN);

        final ActionListener setEnabled = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                setEnabled();
            }
        };

        m_pmml.addActionListener(setEnabled);
        //        m_ruleSelectionMethod.setRenderer(new ListCellRenderer<RuleSelectionMethod>() {
        //            @Override
        //            public Component getListCellRendererComponent(final JList<? extends RuleSelectionMethod> list,
        //                final RuleSelectionMethod value, final int index, final boolean isSelected, final boolean cellHasFocus) {
        //                JLabel ret = new JLabel(value.getStringValue());
        //                return ret;
        //            }
        //            });
        m_hasDefaultScore.addActionListener(setEnabled);
        m_hasDefaultConfidence.addActionListener(setEnabled);
        m_hasDefaultWeight.addActionListener(setEnabled);
        m_computeConfidence.addActionListener(setEnabled);
        m_provideStatistics.addActionListener(setEnabled);

        m_pmml.setSelected(RuleEngine2PortsSettings.DEFAULT_IS_PMML_RULESET);
        m_ruleSelectionMethod.setSelectedItem(RuleEngine2PortsSettings.DEFAULT_RULE_SELECTION_METHOD);
        m_hasDefaultScore.setSelected(RuleEngine2PortsSettings.DEFAULT_HAS_DEFAULT_SCORE);
        m_hasDefaultConfidence.setSelected(RuleEngine2PortsSettings.DEFAULT_HAS_DEFAULT_CONFIDENCE);
        m_hasDefaultWeight.setSelected(RuleEngine2PortsSettings.DEFAULT_HAS_DEFAULT_WEIGHT);
        m_computeConfidence.setSelected(RuleEngine2PortsSettings.DEFAULT_COMPUTE_CONFIDENCE);

        m_warnings.setEditable(false);
        setEnabled();

        m_pmml.addActionListener(m_updateErrorsAndWarnings);
    }

    /**
     * Creates and adds most of the controls to the dialog.
     */
    private void initControls() {
        m_pmml = new JCheckBox("Enable PMML RuleSet generation (fails when result would not be a valid PMML RuleSet)");
        m_ruleSelectionMethod = new JComboBox<>(new Vector<>(RuleEngine2PortsSettings.POSSIBLE_RULE_SELECTION_METHODS));
        m_hasDefaultScore = new JCheckBox("Default value (when nothing matches): ");
        m_defaultScore =
            Util.createTextFieldWithWatermark(RuleEngine2PortsSettings.DEFAULT_DEFAULT_SCORE, 22,
                "Default score/value when nothing matches");
        m_hasDefaultConfidence =
            new JCheckBox("Default confidence value (when not specified in the confidence column): ");
        m_defaultConfidence =
            new JSpinner(new SpinnerNumberModel(RuleEngine2PortsSettings.DEFAULT_DEFAULT_CONFIDENCE, 0d, 1d, .1));
        @SuppressWarnings("unchecked")
        DataValueColumnFilter doubleValueFilter = new DataValueColumnFilter(DoubleValue.class);
        m_ruleConfidenceColumn = new ColumnSelectionPanel((Border)null, doubleValueFilter, true);
        m_hasDefaultWeight = new JCheckBox("Default weight value (when not specified in the weight column): ");
        m_defaultWeight =
            new JSpinner(new SpinnerNumberModel(RuleEngine2PortsSettings.DEFAULT_DEFAULT_WEIGHT, 0d, 1e6, .1));
        m_ruleWeightColumn = new ColumnSelectionPanel((Border)null, doubleValueFilter, true);
        m_computeConfidence = new JCheckBox("Confidence column name: ");
        m_predictionConfidenceColumn =
            Util.createTextFieldWithWatermark(RuleEngine2PortsSettings.DEFAULT_PREDICTION_CONFIDENCE_COLUMN, 22,
                "Computed confidence column name");
        m_provideStatistics = new JCheckBox("Provide statistics");
        @SuppressWarnings("unchecked")
        DataValueColumnFilter validationColumnFilter =
            new DataValueColumnFilter(StringValue.class, BooleanValue.class, DoubleValue.class);
        m_validationColumn = new ColumnSelectionPanel(null, validationColumnFilter, true);
        m_pmmlLabels = new ArrayList<>();
        m_pmmlBorders = new ArrayList<>();
        m_validateLabel = new JLabel("Validation column");
    }

    /**
     * @return The panel with the PMML related controls.
     */
    private JPanel createPMMLPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        //gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.ipadx = 4;
        gbc.gridwidth = 2;
        panel.add(m_pmml, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        JLabel label = new JLabel("Hit selection:");
        m_pmmlLabels.add(label);
        panel.add(label, gbc);
        gbc.gridx++;
        panel.add(m_ruleSelectionMethod, gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        panel.add(createDefaultScorePanel(), gbc);
        gbc.gridy++;
        panel.add(createInputConfidencePanel(), gbc);
        gbc.gridy++;
        panel.add(createInputWeightPanel(), gbc);
        gbc.gridy++;
        panel.add(createOutputConfidencePanel(), gbc);
        gbc.gridy++;
        panel.add(m_provideStatistics, gbc);
        gbc.gridy++;
        panel.add(createOutputValidatePanel(), gbc);
        return panel;
    }

    /**
     * @return The validation panel (a label and a column selector).
     */
    private JPanel createOutputValidatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_validateLabel, gbc);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        panel.add(m_validationColumn, gbc);
        final TitledBorder border = new TitledBorder("Validation");
        m_pmmlBorders.add(border);
        panel.setBorder(border);
        return panel;
    }

    /**
     * @return The output confidence panel (a checkbox and the textfield for the column name).
     */
    private JPanel createOutputConfidencePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_computeConfidence, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_predictionConfidenceColumn, gbc);
        final TitledBorder border = new TitledBorder("Computed confidence");
        m_pmmlBorders.add(border);
        panel.setBorder(border);
        return panel;
    }

    /**
     * @return The input weight parameters (a checkbox and numeric spinner for the default value and a column selector
     *         for the (optional) weight column).
     */
    private JPanel createInputWeightPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        TitledBorder border = new TitledBorder("Rule weight");
        m_pmmlBorders.add(border);
        panel.setBorder(border);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(m_hasDefaultWeight, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_defaultWeight, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel label = new JLabel("Rule weight column: ");
        m_pmmlLabels.add(label);
        panel.add(label, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_ruleWeightColumn, gbc);
        return panel;
    }

    /**
     * @return The input confidence parameters (a checkbox and numeric spinner for the default value and a column
     *         selector for the (optional) confidence column).
     */
    private JPanel createInputConfidencePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        TitledBorder border = new TitledBorder("Rule confidence");
        m_pmmlBorders.add(border);
        panel.setBorder(border);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(m_hasDefaultConfidence, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_defaultConfidence, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel label = new JLabel("Rule confidence column: ");
        m_pmmlLabels.add(label);
        panel.add(label, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_ruleConfidenceColumn, gbc);
        return panel;
    }

    /**
     * @return The panel for the default score/value (a checkbox and a textfield).
     */
    private JPanel createDefaultScorePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_hasDefaultScore, gbc);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        panel.add(m_defaultScore, gbc);
        TitledBorder border = new TitledBorder("Default value/score");
        m_pmmlBorders.add(border);
        panel.setBorder(border);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuleEngine2PortsSettings getSettings() {
        return (RuleEngine2PortsSettings)super.getSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        final DataTableSpec inSpec = (DataTableSpec)specs[0], secondSpec = (DataTableSpec)specs[1];
        m_dataSpec = inSpec;
        m_appendColumn.setText(getSettings().getAppendColumn());
        m_replaceColumn.update(inSpec, getSettings().getReplaceColumn());
        m_replace.setSelected(getSettings().isReplaceColumn());
        m_pmml.setSelected(getSettings().isPMMLRuleSet());
        m_ruleSelectionMethod.setSelectedItem(getSettings().getRuleSelectionMethod());
        m_hasDefaultScore.setSelected(getSettings().isHasDefaultScore());
        m_defaultScore.setText(getSettings().getDefaultScore());
        m_hasDefaultConfidence.setSelected(getSettings().isHasDefaultConfidence());
        m_defaultConfidence.setValue(getSettings().getDefaultConfidence());
        m_ruleConfidenceColumn.update(secondSpec, getSettings().getRuleConfidenceColumn());
        m_hasDefaultWeight.setSelected(getSettings().isHasDefaultWeight());
        m_defaultWeight.setValue(getSettings().getDefaultWeight());
        m_ruleWeightColumn.update(secondSpec, getSettings().getRuleWeightColumn());
        m_computeConfidence.setSelected(getSettings().isComputeConfidence());
        m_predictionConfidenceColumn.setText(getSettings().getPredictionConfidenceColumn());

        m_provideStatistics.setSelected(getSettings().isProvideStatistics());
        m_validationColumn.update(inSpec, getSettings().getValidateColumn());
        setEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final RuleEngine2PortsSettings s = getSettings();
        s.setAppendColumn(m_appendColumn.getText());
        s.setReplaceColumn(m_replaceColumn.getSelectedColumn());
        s.setReplaceColumn(m_replace.isSelected());
        s.setPMMLRuleSet(m_pmml.isSelected());
        s.setRuleSelectionMethod((RuleSelectionMethod)m_ruleSelectionMethod.getSelectedItem());
        s.setHasDefaultScore(m_hasDefaultScore.isSelected());
        s.setDefaultScore(m_defaultScore.getText());
        s.setHasDefaultConfidence(m_hasDefaultConfidence.isSelected());
        s.setDefaultConfidence(((Number)m_defaultConfidence.getValue()).doubleValue());
        s.setRuleConfidenceColumn(m_ruleConfidenceColumn.getSelectedColumn());
        s.setHasDefaultWeight(m_hasDefaultWeight.isSelected());
        s.setDefaultWeight(((Number)m_defaultWeight.getValue()).doubleValue());
        s.setRuleWeightColumn(m_ruleWeightColumn.getSelectedColumn());
        s.setComputeConfidence(m_computeConfidence.isSelected());
        s.setPredictionConfidenceColumn(m_predictionConfidenceColumn.getText());

        s.setProvideStatistics(m_provideStatistics.isSelected());
        s.setValidateColumn(m_validationColumn.getSelectedColumn());
        s.saveSettings(settings);
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isFirstHit() {
        return !getSettings().isPMMLRuleSet() || getSettings().getRuleSelectionMethod() == RuleSelectionMethod.FirstHit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSpecAvailable() {
        return m_dataSpec != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec getDataSpec() {
        return m_dataSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuleFactory ruleFactory() {
        return RuleFactory.getInstance(getSettings().isPMMLRuleSet() ? RuleNodeSettings.PMMLRule
            : RuleNodeSettings.RuleEngine);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabled() {
        m_replaceColumn.setEnabled(m_replace.isSelected());
        m_appendColumn.setEnabled(m_append.isSelected());
        m_ruleSelectionMethod.setEnabled(m_pmml.isSelected());
        m_hasDefaultScore.setEnabled(m_pmml.isSelected());
        m_defaultScore.setEnabled(m_pmml.isSelected() && m_hasDefaultScore.isSelected());
        m_hasDefaultConfidence.setEnabled(m_pmml.isSelected());
        m_defaultConfidence.setEnabled(m_pmml.isSelected() && m_hasDefaultConfidence.isSelected());
        m_ruleConfidenceColumn.setEnabled(m_pmml.isSelected());
        m_hasDefaultWeight.setEnabled(m_pmml.isSelected());
        m_defaultWeight.setEnabled(m_pmml.isSelected() && m_hasDefaultWeight.isSelected());
        m_ruleWeightColumn.setEnabled(m_pmml.isSelected());
        m_computeConfidence.setEnabled(m_pmml.isSelected());
        m_predictionConfidenceColumn.setEnabled(m_pmml.isSelected() && m_computeConfidence.isSelected());
        m_provideStatistics.setEnabled(m_pmml.isSelected());
        m_validateLabel.setEnabled(m_pmml.isSelected() && m_provideStatistics.isSelected());
        m_validationColumn.setEnabled(m_pmml.isSelected() && m_provideStatistics.isSelected());
        for (final JLabel label : m_pmmlLabels) {
            label.setEnabled(m_pmml.isSelected());
        }
    }

    /**
     * {@inheritDoc} it adds the replace or append controls (radiobuttons, textfield and column selector).
     */
    @Override
    protected void addAppendOrReplace(final JPanel panel, final GridBagConstraints gbc) {
        m_append = new JRadioButton("Append column");
        panel.add(m_append, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.gridwidth = 4;
        m_appendColumn = Util.createTextFieldWithWatermark("Prediction", 22, "Computed column name");
        panel.add(m_appendColumn, gbc);
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        m_replace = new JRadioButton("Replace column");
        panel.add(m_replace, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.gridwidth = 4;
        m_replaceColumn = new ColumnSelectionPanel((String)null);
        panel.add(m_replaceColumn, gbc);
        gbc.gridy++;
    }
}
