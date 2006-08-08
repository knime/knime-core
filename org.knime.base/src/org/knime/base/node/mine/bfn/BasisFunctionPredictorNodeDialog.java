/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * A dialog to apply data to basis functions. Can be used to set a name for the
 * new, applied column.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class BasisFunctionPredictorNodeDialog extends NodeDialogPane {
    /** Prediction column. */
    private final JTextField m_apply = new JTextField();

    private final JRadioButton m_dftButton;

    private final JRadioButton m_setButton;

    private final JSpinner m_dontKnow;

    /** Key for the applied column: <i>apply_column</i>. */
    public static final String APPLY_COLUMN = "apply_column";

    /** Key for don't know propability for the unknown class. */
    public static final String DONT_KNOW_PROP = "dont_know_prop";

    /**
     * Creates a new predictor dialog to set a name for the applied column.
     * 
     * @param title title for this dialog
     */
    public BasisFunctionPredictorNodeDialog(final String title) {
        super();
        // panel with advance settings
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setPreferredSize(new Dimension(200, 150));

        // add apply column
        m_apply.setPreferredSize(new Dimension(175, 25));
        JPanel normPanel = new JPanel();
        normPanel.setBorder(BorderFactory.createTitledBorder(" Choose Name "));
        normPanel.add(m_apply);
        p.add(normPanel);

        // add don't know propability
        m_dftButton = new JRadioButton("Default ");
        m_setButton = new JRadioButton("Use ");
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_dftButton);
        bg.add(m_setButton);
        m_dftButton.setSelected(true);
        m_dontKnow = new JSpinner();
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)m_dontKnow
                .getEditor();
        editor.getTextField().setColumns(8);
        m_dontKnow.setModel(new SpinnerNumberModel(0.0, 0.0,
                Double.POSITIVE_INFINITY, 0.1));
        m_dontKnow.setPreferredSize(new Dimension(75, 25));
        JPanel dontKnowPanel = new JPanel(new FlowLayout());
        dontKnowPanel.setBorder(BorderFactory
                .createTitledBorder(" Don't Know Class "));
        dontKnowPanel.add(m_dftButton);
        dontKnowPanel.add(m_setButton);
        dontKnowPanel.add(m_dontKnow);
        p.add(dontKnowPanel);

        m_dftButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_dontKnow.setEnabled(false);
            }
        });

        m_setButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_dontKnow.setEnabled(true);
            }
        });

        // add fuzzy learner tab
        super.addTab("Applied Column", p);
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // prediction column name
        String apply = settings.getString(APPLY_COLUMN, "");
        m_apply.setText(apply);
        // dont know class
        double value = settings.getDouble(DONT_KNOW_PROP, -1.0);
        if (value < 0.0) {
            m_dftButton.setSelected(true);
            m_dontKnow.setEnabled(false);
        } else {
            m_setButton.setSelected(true);
            m_dontKnow.setEnabled(true);
            m_dontKnow.setValue(new Double(value));
        }
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // prediction column name
        String s = m_apply.getText().trim();
        if (s.length() == 0) {
            throw new InvalidSettingsException("Empty name not allowed.");
        }
        settings.addString(APPLY_COLUMN, s);
        // dont know class
        if (m_dftButton.isSelected()) {
            settings.addDouble(DONT_KNOW_PROP, -1.0);
        } else {
            Double value = (Double)m_dontKnow.getValue();
            settings.addDouble(DONT_KNOW_PROP, value.doubleValue());
        }
    }
}
