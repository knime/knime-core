/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import org.knime.base.node.preproc.sample.SamplingNodeSettings.SamplingMethods;
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

    private final JCheckBox m_useSeedChecker;

    private final JFormattedTextField m_seedField;

    private final JRadioButton m_randomSampling;

    private final JRadioButton m_linearSampling;

    private final JRadioButton m_firstSampling;

    private final JRadioButton m_stratifiedSampling;

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
                new JSpinner(new SpinnerNumberModel(10.0, 0.0, 100.0, 2.0));
        final int width = 8;
        JSpinner.DefaultEditor editor =
                (JSpinner.DefaultEditor)m_absoluteSpinner.getEditor();
        editor.getTextField().setColumns(width);
        editor = (JSpinner.DefaultEditor)m_relativeSpinner.getEditor();
        editor.getTextField().setColumns(width);

        m_useSeedChecker = new JCheckBox("Seed:");
        m_useSeedChecker
                .setToolTipText("Check this to allow deterministic sampling.");
        m_useSeedChecker.setHorizontalTextPosition(SwingConstants.RIGHT);
        m_seedField =
                new JFormattedTextField(new Long(System.currentTimeMillis()));
        m_seedField.setColumns(10);
        m_useSeedChecker.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_seedField.setEnabled(m_randomSampling.isSelected()
                        && m_useSeedChecker.isSelected());
            }
        });
        m_randomSampling = new JRadioButton("Draw randomly");
        m_randomSampling.setToolTipText("If selected, chooses samples randomly"
                + "; otherwise takes from top.");
        m_randomSampling.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_useSeedChecker.setEnabled(m_randomSampling.isSelected());
                m_seedField.setEnabled(m_randomSampling.isSelected()
                        && m_useSeedChecker.isSelected());
            }
        });

        m_stratifiedSampling = new JRadioButton("Stratified sampling:");
        m_stratifiedSampling.setToolTipText(
                "Check this to retain the distribution in the class column.");
        m_stratifiedSampling.setHorizontalTextPosition(SwingConstants.RIGHT);
        m_classColumn =
                new ColumnSelectionComboxBox((Border)null, NominalValue.class);

        m_stratifiedSampling.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_classColumn.setEnabled(m_stratifiedSampling.isSelected());
            }
        });

        m_linearSampling = new JRadioButton("Linear sampling");
        m_linearSampling.setToolTipText("Check this to select samples "
                + "linearly over the whole table");

        m_firstSampling = new JRadioButton("Take from top");
        m_firstSampling.setToolTipText("Check this to select samples "
                + "consecutively from the beginning");

        m_useSeedChecker.setEnabled(m_randomSampling.isSelected());
        m_seedField.setEnabled(m_randomSampling.isSelected()
                && m_useSeedChecker.isSelected());

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_absoluteButton);
        buttonGroup.add(m_relativeButton);

        buttonGroup = new ButtonGroup();
        buttonGroup.add(m_randomSampling);
        buttonGroup.add(m_stratifiedSampling);
        buttonGroup.add(m_linearSampling);
        buttonGroup.add(m_firstSampling);

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

        add(new JLabel());
        add(new JLabel());

        add(getInFlowLayout(m_randomSampling));
        add(getInFlowLayout(m_useSeedChecker, m_seedField));
        
        add(getInFlowLayout(m_firstSampling));
        add(new JLabel());

        add(getInFlowLayout(m_stratifiedSampling));
        add(getInFlowLayout(m_classColumn));

        add(getInFlowLayout(m_linearSampling));
        add(new JLabel());
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

        if (sets.countMethod() == SamplingNodeSettings.CountMethods.Relative) {
            m_relativeButton.doClick();
        } else {
            m_absoluteButton.doClick();
        }

        m_relativeSpinner.setValue(new Double(sets.fraction() * 100));
        m_absoluteSpinner.setValue(new Integer(sets.count()));
        m_randomSampling.setSelected(sets.samplingMethod().equals(
                SamplingMethods.Random));
        m_stratifiedSampling.setSelected(sets.samplingMethod().equals(
                SamplingMethods.Stratified));
        m_linearSampling.setSelected(sets.samplingMethod().equals(
                SamplingMethods.Linear));
        m_firstSampling.setSelected(sets.samplingMethod().equals(
                SamplingMethods.First));

        if (sets.seed() != null) {
            m_useSeedChecker.setSelected(true);
            m_seedField.setText(sets.seed().toString());
        } else {
            m_useSeedChecker.setSelected(false);
        }

        try {
            m_classColumn.update(spec, sets.classColumn());
            m_stratifiedSampling.setEnabled(true);
            m_classColumn.setEnabled(sets.samplingMethod().equals(
                    SamplingMethods.Stratified));
        } catch (NotConfigurableException ex) {
            // no nominal value column, so disable stratified sampling
            m_classColumn.setEnabled(false);
            m_stratifiedSampling.setSelected(false);
            m_stratifiedSampling.setEnabled(false);
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
            sets.countMethod(SamplingNodeSettings.CountMethods.Relative);
        } else {
            sets.countMethod(SamplingNodeSettings.CountMethods.Absolute);
        }

        sets
                .fraction(((Double)m_relativeSpinner.getValue()).doubleValue() / 100);
        sets.count(((Integer)m_absoluteSpinner.getValue()).intValue());
        if (m_randomSampling.isSelected()) {
            sets.samplingMethod(SamplingMethods.Random);
        } else if (m_stratifiedSampling.isSelected()) {
            sets.samplingMethod(SamplingMethods.Stratified);
        } else if (m_linearSampling.isSelected()) {
            sets.samplingMethod(SamplingMethods.Linear);
        } else if (m_firstSampling.isSelected()) {
            sets.samplingMethod(SamplingMethods.First);
        }

        if (m_useSeedChecker.isSelected()) {
            sets.seed(new Long(m_seedField.getValue().toString()));
        } else {
            sets.seed(null);
        }

        sets.classColumn(m_classColumn.getSelectedColumn());

        sets.saveSettingsTo(settings);
    }

    /* Convenience method to get an component in a JPanel with FlowLayout. */
    private static JPanel getInFlowLayout(final JComponent... c) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent comp : c) {
            panel.add(comp);
        }
        return panel;
    }
}
