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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
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
    
    private final JCheckBox m_ignButton;

    private final JSpinner m_dontKnow;
    
    private final JCheckBox m_appendProp;

    /** Key for the applied column: <i>apply_column</i>. */
    public static final String APPLY_COLUMN = "apply_column";

    /** Key for don't know probability for the unknown class. */
    public static final String DONT_KNOW_PROP = "dont_know_prop";
    
    /** Config key if don't know should be ignored. */
    public static final String CFG_DONT_KNOW_IGNORE = "ignore_dont_know";
    
    /** Config key if class probabilities should be appended to the table. */
    public static final String CFG_CLASS_PROPS = "append_class_probabilities";

    /**
     * Creates a new predictor dialog to set a name for the applied column.
     */
    public BasisFunctionPredictorNodeDialog() {
        super();
        // panel with advance settings
        JPanel p = new JPanel(new GridLayout(3, 1));

        // add apply column
        m_apply.setPreferredSize(new Dimension(175, 25));
        JPanel normPanel = new JPanel();
        normPanel.setBorder(BorderFactory.createTitledBorder(" Choose Name "));
        normPanel.add(m_apply);
        p.add(normPanel);

        // append class probabilities
        m_appendProp = new JCheckBox("Append Class Columns", true);
        m_appendProp.setPreferredSize(new Dimension(175, 25));
        JPanel propPanel = new JPanel();
        propPanel.setBorder(
                BorderFactory.createTitledBorder(" Class Probabilities "));
        propPanel.add(m_appendProp);
        p.add(propPanel);
        
        // add don't know probability
        m_dftButton = new JRadioButton("Default ", true);
        m_setButton = new JRadioButton("Use ");
        m_ignButton = new JCheckBox("Ignore ", true);
        m_ignButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                selectionChanged();
            }
        });
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_dftButton);
        bg.add(m_setButton);
        m_dontKnow = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1));
        m_dontKnow.setEditor(new JSpinner.NumberEditor(
                m_dontKnow, "#.##########"));
        m_dontKnow.setPreferredSize(new Dimension(75, 25));
        JPanel dontKnowPanel = new JPanel(new BorderLayout());
        dontKnowPanel.setBorder(BorderFactory
                .createTitledBorder(" Don't Know Class "));
        dontKnowPanel.add(m_ignButton, BorderLayout.NORTH);
        JPanel dftPanel = new JPanel(new FlowLayout());
        dftPanel.setBorder(BorderFactory
                .createTitledBorder(""));
        dftPanel.add(m_dftButton);
        dftPanel.add(m_setButton);
        dftPanel.add(m_dontKnow);
        dontKnowPanel.add(dftPanel, BorderLayout.CENTER);
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
        super.addTab("Winner Column", p);
    }
    
    private void selectionChanged() {
        if (m_ignButton.isSelected()) {
            m_dftButton.setEnabled(false);
            m_setButton.setEnabled(false);
            m_dontKnow.setEnabled(false);
        } else {
            m_dftButton.setEnabled(true);
            m_setButton.setEnabled(true);
            if (m_dftButton.isSelected()) {
                m_dontKnow.setEnabled(false);
            } else {
                m_dontKnow.setEnabled(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // prediction column name
        String apply = settings.getString(APPLY_COLUMN, "");
        m_apply.setText(apply);
        if (settings.getBoolean(CFG_DONT_KNOW_IGNORE, false)) {
            m_ignButton.setSelected(true);
            m_dontKnow.setValue(new Double(0.0));
        } else {
            m_ignButton.setSelected(false);
            double value = settings.getDouble(DONT_KNOW_PROP, -1.0);
            if (value < 0.0) {
                m_dftButton.setSelected(true);
                m_dontKnow.setValue(new Double(0.0));
            } else {
                m_setButton.setSelected(true);
                m_dontKnow.setValue(new Double(value));
            }
        }
        m_appendProp.setSelected(settings.getBoolean(CFG_CLASS_PROPS, true));
        selectionChanged();
    }

    /**
     * {@inheritDoc}
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
        if (m_ignButton.isSelected()) {
            settings.addBoolean(CFG_DONT_KNOW_IGNORE, true);
            settings.addDouble(DONT_KNOW_PROP, 0.0);
        } else {
            settings.addBoolean(CFG_DONT_KNOW_IGNORE, false);
            if (m_dftButton.isSelected()) {
                settings.addDouble(DONT_KNOW_PROP, -1.0);
            } else {
                Double value = (Double)m_dontKnow.getValue();
                settings.addDouble(DONT_KNOW_PROP, value.doubleValue());
            }
        }
        settings.addBoolean(CFG_CLASS_PROPS, m_appendProp.isSelected());
    }
}
