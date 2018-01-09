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
 * Created on 2013.08.17. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.pmml;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the "PMMLRuleSetPredictor" Node. Applies the rules to the input table.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Gabor Bakos
 */
public class PMMLRuleSetPredictorNodeDialog extends NodeDialogPane {
    private final SettingsModelBoolean m_replace = PMMLRuleSetPredictorNodeModel.createDoReplace();
    private final JRadioButton m_newColumn = new JRadioButton("Append Column: ");
    private final JRadioButton m_replaceColumn = new JRadioButton("Replace Column: ");
    private final ButtonGroup m_group = new ButtonGroup();
    private DialogComponentString m_outputColumn;
    private DialogComponentColumnNameSelection m_replacedColumn;
    private DialogComponentBoolean m_computeConfidence;
    private DialogComponentString m_confidenceColumn;
    /**
     * New pane for configuring the PMMLRuleSetPredictor node.
     */
    protected PMMLRuleSetPredictorNodeDialog() {
        super();
        JPanel mainPanel = new JPanel(new BorderLayout(0, 1));
        addTab("Options", mainPanel);
        Box outputBox = Box.createVerticalBox();
        TitledBorder outputBorder = new TitledBorder("Output");
        outputBox.setBorder(outputBorder);
        m_outputColumn = new DialogComponentString(new SettingsModelString(
            PMMLRuleSetPredictorNodeModel.CFGKEY_OUTPUT_COLUMN, PMMLRuleSetPredictorNodeModel.DEFAULT_OUTPUT_COLUMN),
                "");
        @SuppressWarnings("unchecked")
        DialogComponentColumnNameSelection colSelection =
            new DialogComponentColumnNameSelection(PMMLRuleSetPredictorNodeModel.createReplaceColumn(),
                "", 1, DoubleValue.class, StringValue.class, BooleanValue.class);
        m_replacedColumn = colSelection;
        JPanel newColumnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        newColumnPanel.add(m_newColumn);
        newColumnPanel.add(m_outputColumn.getComponentPanel());
        outputBox.add(newColumnPanel);
        JPanel replacedColumnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replacedColumnPanel.add(m_replaceColumn);
        replacedColumnPanel.add(m_replacedColumn.getComponentPanel());
        outputBox.add(replacedColumnPanel);

        mainPanel.add(outputBox, BorderLayout.CENTER);
        Box confidenceBox = Box.createHorizontalBox();
        confidenceBox.setBorder(new TitledBorder("Confidence"));
        mainPanel.add(confidenceBox, BorderLayout.SOUTH);
        final SettingsModelBoolean computeConfidenceModel =
            new SettingsModelBoolean(PMMLRuleSetPredictorNodeModel.CFGKEY_ADD_CONFIDENCE,
                PMMLRuleSetPredictorNodeModel.DEFAULT_ADD_CONFIDENCE);
        m_computeConfidence = new DialogComponentBoolean(computeConfidenceModel, "Compute confidence?");
        confidenceBox.add(m_computeConfidence.getComponentPanel());
        final SettingsModelString confidenceColumnModel =
            new SettingsModelString(PMMLRuleSetPredictorNodeModel.CFGKEY_CONFIDENCE_COLUMN,
                PMMLRuleSetPredictorNodeModel.DEFAULT_CONFIDENCE_COLUN);
        ChangeListener listener = new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                confidenceColumnModel.setEnabled(computeConfidenceModel.getBooleanValue());
            }
        };
        computeConfidenceModel.addChangeListener(listener);
        m_confidenceColumn = new DialogComponentString(confidenceColumnModel, "Confidence column");
        confidenceBox.add(m_confidenceColumn.getComponentPanel());
        listener.stateChanged(null);
        m_group.add(m_newColumn);
        m_group.add(m_replaceColumn);
        ActionListener selectionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_outputColumn.getModel().setEnabled(m_newColumn.isSelected());
                m_replacedColumn.getModel().setEnabled(!m_newColumn.isSelected());
            }
        };
        m_newColumn.addActionListener(selectionListener);
        m_replaceColumn.addActionListener(selectionListener);
        //TODO add option to select the rule selection method from the possible options.
        //TODO disable output column when not isScorable.
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_replace.setBooleanValue(m_replaceColumn.isSelected());
        m_replace.saveSettingsTo(settings);
        m_outputColumn.saveSettingsTo(settings);
        m_replacedColumn.saveSettingsTo(settings);
        m_computeConfidence.saveSettingsTo(settings);
        m_confidenceColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            m_replace.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_replace.setBooleanValue(PMMLRuleSetPredictorNodeModel.DEFAULT_DO_REPLACE_COLUMN);
        }
        m_newColumn.setSelected(!m_replace.getBooleanValue());
        m_replaceColumn.setSelected(m_replace.getBooleanValue());
        m_outputColumn.loadSettingsFrom(settings, specs);
        m_replacedColumn.loadSettingsFrom(settings, specs);
        m_computeConfidence.loadSettingsFrom(settings, specs);
        m_confidenceColumn.loadSettingsFrom(settings, specs);
        m_confidenceColumn.getModel().setEnabled(
            ((SettingsModelBoolean)m_computeConfidence.getModel()).getBooleanValue());
        for (ActionListener listener : m_newColumn.getActionListeners()) {
            listener.actionPerformed(null);
        }
    }
}
