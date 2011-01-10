/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.sample;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
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
    private final JRadioButton m_absoluteButton =
            new JRadioButton("Absolute   ");

    private final JRadioButton m_relativeButton =
            new JRadioButton("Relative[%]   ");

    private final JSpinner m_absoluteSpinner =
            new JSpinner(new SpinnerNumberModel(100, 1, Integer.MAX_VALUE, 50));

    private final JSpinner m_relativeSpinner =
            new JSpinner(new SpinnerNumberModel(10.0, 0.0, 100.0, 2.0));

    private final JCheckBox m_useSeedChecker = new JCheckBox("Use random seed");

    private final JFormattedTextField m_seedField;

    private final JRadioButton m_randomSampling =
            new JRadioButton("Draw randomly");

    private final JRadioButton m_linearSampling =
            new JRadioButton("Linear sampling");

    private final JRadioButton m_firstSampling =
            new JRadioButton("Take from top");

    private final JRadioButton m_stratifiedSampling =
            new JRadioButton("Stratified sampling");

    private final ColumnSelectionComboxBox m_classColumn;

    /**
     * Creates new panel, inits fields. Nothing else.
     */
    @SuppressWarnings("unchecked")
    public SamplingNodeDialogPanel() {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        final int width = 8;
        JSpinner.DefaultEditor editor =
                (JSpinner.DefaultEditor)m_absoluteSpinner.getEditor();
        editor.getTextField().setColumns(width);
        editor = (JSpinner.DefaultEditor)m_relativeSpinner.getEditor();
        editor.getTextField().setColumns(width);

        m_useSeedChecker
                .setToolTipText("Check this to allow deterministic sampling.");
        m_useSeedChecker.setHorizontalTextPosition(SwingConstants.RIGHT);
        m_seedField =
                new JFormattedTextField(new Long(System.currentTimeMillis()));
        m_seedField.setColumns(10);
        m_useSeedChecker.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_seedField
                        .setEnabled((m_randomSampling.isSelected() || m_stratifiedSampling
                                .isSelected())
                                && m_useSeedChecker.isSelected());
            }
        });
        m_randomSampling.setToolTipText("If selected, chooses samples randomly"
                + "; otherwise takes from top.");
        m_randomSampling.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_useSeedChecker.setEnabled(m_randomSampling.isSelected());
                m_seedField.setEnabled(m_randomSampling.isSelected()
                        && m_useSeedChecker.isSelected());
            }
        });

        m_stratifiedSampling.setToolTipText("Check this to retain the "
                + "distribution in the class column.");
        m_stratifiedSampling.setHorizontalTextPosition(SwingConstants.RIGHT);
        m_classColumn =
                new ColumnSelectionComboxBox((Border)null, NominalValue.class);

        m_stratifiedSampling.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                m_classColumn.setEnabled(m_stratifiedSampling.isSelected());
                m_useSeedChecker.setEnabled(m_stratifiedSampling.isSelected());
                m_seedField.setEnabled(m_stratifiedSampling.isSelected()
                        && m_useSeedChecker.isSelected());
            }
        });

        m_linearSampling.setToolTipText("Check this to select samples "
                + "linearly over the whole table");

        m_firstSampling.setToolTipText("Check this to select samples "
                + "consecutively from the beginning");

        m_useSeedChecker.setEnabled(m_randomSampling.isSelected()
                || m_stratifiedSampling.isSelected());
        m_seedField
                .setEnabled((m_randomSampling.isSelected() || m_stratifiedSampling
                        .isSelected())
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

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 1, 2, 1);
        add(m_absoluteButton, c);
        c.gridx++;
        add(m_absoluteSpinner, c);

        c.gridx = 0;
        c.gridy++;
        add(m_relativeButton, c);
        c.gridx++;
        add(m_relativeSpinner, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        add(new JSeparator(), c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        add(m_firstSampling, c);

        c.gridy++;
        add(m_linearSampling, c);

        c.gridy++;
        add(m_randomSampling, c);

        c.gridy++;
        add(m_stratifiedSampling, c);
        c.gridx++;
        add(m_classColumn, c);

        c.gridx = 0;
        c.gridy++;
        c.insets = new Insets(10, 1, 2, 1);
        add(m_useSeedChecker, c);

        c.gridx++;
        add(m_seedField, c);
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
}
