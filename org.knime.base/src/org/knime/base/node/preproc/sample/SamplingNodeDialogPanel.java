/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.preproc.sample;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * Panel to be used in the dialog of the sampling node. It allows to set if the
 * sampling method is absolute (how many rows) or relative (what percentage) and
 * also if to choose the rows random or take from top.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SamplingNodeDialogPanel extends JPanel {
    private final JRadioButton m_absoluteButton;

    private final JRadioButton m_relativeButton;

    private final JSpinner m_absoluteSpinner;

    private final JSpinner m_relativeSpinner;

    private final JCheckBox m_chooseRandom;

    private final JCheckBox m_useSeedChecker;

    private final JFormattedTextField m_seedField;

    private final JCheckBox m_stratified;

    private final ColumnSelectionComboxBox m_classColumn;

    /**
     * Creates new panel, inits fields. Nothing else.
     */
    @SuppressWarnings("unchecked")
    public SamplingNodeDialogPanel() {
        super(new GridLayout(0, 2));
        m_absoluteButton = new JRadioButton("Absolute: ");
        m_relativeButton = new JRadioButton("Relative[%]: ");
        m_absoluteSpinner =
                new JSpinner(new SpinnerNumberModel(100, 1, Integer.MAX_VALUE,
                        50));
        m_relativeSpinner =
                new JSpinner(new SpinnerNumberModel(10.0, 0.0, 100.0, 0.1));
        final int width = 8;
        JSpinner.DefaultEditor editor =
                (JSpinner.DefaultEditor)m_absoluteSpinner.getEditor();
        editor.getTextField().setColumns(width);
        editor = (JSpinner.DefaultEditor)m_relativeSpinner.getEditor();
        editor.getTextField().setColumns(width);

        m_useSeedChecker = new JCheckBox("Use seed:");
        m_useSeedChecker
                .setToolTipText("Check this to allow deterministic sampling.");
        m_useSeedChecker.setHorizontalTextPosition(SwingConstants.RIGHT);
        m_seedField =
                new JFormattedTextField(new Long(System.currentTimeMillis()));
        m_seedField.setColumns(8);
        m_useSeedChecker.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_seedField.setEnabled(m_chooseRandom.isSelected()
                        && m_useSeedChecker.isSelected());
            }
        });
        m_chooseRandom = new JCheckBox("Draw randomly");
        m_chooseRandom.setToolTipText("If selected, chooses samples randomly"
                + "; otherwise takes from top.");
        m_chooseRandom.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_useSeedChecker.setEnabled(m_chooseRandom.isSelected());
                m_seedField.setEnabled(m_chooseRandom.isSelected()
                        && m_useSeedChecker.isSelected());
                if (!m_chooseRandom.isSelected()) {
                    m_stratified.setSelected(false);
                }
            }
        });
        m_useSeedChecker.setEnabled(m_chooseRandom.isSelected());
        m_seedField.setEnabled(m_chooseRandom.isSelected()
                && m_useSeedChecker.isSelected());

        m_stratified = new JCheckBox("Stratified sampling:");
        m_stratified.setToolTipText(
                "Check this to retain the distribution in the class column.");
        m_stratified.setHorizontalTextPosition(SwingConstants.RIGHT);
        m_classColumn = new ColumnSelectionComboxBox(
                (Border)null, NominalValue.class);

        m_stratified.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_classColumn.setEnabled(m_stratified.isSelected());
                if (m_stratified.isSelected()) {
                    m_chooseRandom.setSelected(true);
                }
            }
        });

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_absoluteButton);
        buttonGroup.add(m_relativeButton);

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_relativeSpinner.setEnabled(m_relativeButton.isSelected());
                m_absoluteSpinner.setEnabled(m_absoluteButton.isSelected());
            }
        };
        m_relativeButton.addActionListener(actionListener);
        m_absoluteButton.addActionListener(actionListener);
        m_absoluteButton.doClick();

        add(getInFlowLayout(m_absoluteButton));
        add(getInFlowLayout(m_absoluteSpinner));

        add(getInFlowLayout(m_relativeButton));
        add(getInFlowLayout(m_relativeSpinner));

        add(getInFlowLayout(m_chooseRandom));
        add(new JLabel());

        add(getInFlowLayout(m_useSeedChecker));
        add(getInFlowLayout(m_seedField));

        add(getInFlowLayout(m_stratified));
        add(getInFlowLayout(m_classColumn));
    }

    /**
     * Method that is called from the dialog to load settings.
     * 
     * @param settings the settings from the NodeModel
     * @param spec the spec of the input table
     * @throws NotConfigurableException if the node cannot be configured (yet)
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec spec) throws NotConfigurableException {
        SamplingNodeSettings sets = new SamplingNodeSettings();
        try {
            sets.loadSettingsFrom(settings, true);
        } catch (InvalidSettingsException ise) {
            assert false : "The method should not throw an exception.";
        }

        if (sets.method() == SamplingNodeSettings.Methods.Relative) {
            m_relativeButton.doClick();
        } else {
            m_absoluteButton.doClick();
        }

        m_relativeSpinner.setValue(new Double(sets.fraction() * 100));
        m_absoluteSpinner.setValue(new Integer(sets.count()));
        m_chooseRandom.setSelected(sets.random());

        if (sets.seed() != null) {
            m_useSeedChecker.setSelected(true);
            m_seedField.setText(sets.seed().toString());
        } else {
            m_useSeedChecker.setSelected(false);
        }
        
        try {
            m_classColumn.update(spec, sets.classColumn());
            m_stratified.setSelected(sets.stratifiedSampling());
            m_classColumn.setEnabled(sets.stratifiedSampling());
        } catch (NotConfigurableException ex) {
            // no nominal value column, so disable stratified sampling
            m_classColumn.setEnabled(false);
            m_stratified.setSelected(false);
            m_stratified.setEnabled(false);
        }
    }

    /**
     * Method that is called from the dialog to save settings.
     * 
     * @param settings the object to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        SamplingNodeSettings sets = new SamplingNodeSettings();
        if (m_relativeButton.isSelected()) {
            sets.method(SamplingNodeSettings.Methods.Relative);
        } else {
            sets.method(SamplingNodeSettings.Methods.Absolute);
        }

        sets.fraction(
                ((Double)m_relativeSpinner.getValue()).doubleValue() / 100);
        sets.count(((Integer)m_absoluteSpinner.getValue()).intValue());
        sets.random(m_chooseRandom.isSelected());

        if (m_useSeedChecker.isSelected()) {
            sets.seed(new Long(m_seedField.getValue().toString()));
        } else {
            sets.seed(null);
        }

        sets.stratifiedSampling(m_stratified.isSelected());
        sets.classColumn(m_classColumn.getSelectedColumn());

        sets.saveSettingsTo(settings);
    }

    /* Convenience method to get an component in a JPanel with FlowLayout. */
    private static JPanel getInFlowLayout(final JComponent c) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(c);
        return panel;
    }
}
