/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
package org.knime.base.node.mine.smote;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;


/**
 * Dialog for smote. What you can enter:
 * <ul>
 * <li>The target column (the class of interest)</li>
 * <li>The kNN value</li>
 * <li>Your choice of oversampling:
 * <ul>
 * <li>Oversample all classes equally, determine amount</li>
 * <li>Oversample only minority classes such that they appear as often as the
 * minority class</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SmoteNodeDialog extends NodeDialogPane {
    private final JSpinner m_kNNSpinner;

    private final ColumnSelectionPanel m_selectionPanel;

    private final JRadioButton m_smoteAllButton;

    private final JRadioButton m_smoteMinorityButton;

    private final JSpinner m_rateSpinner;
    
    private final JFormattedTextField m_seedField;
    private final JButton m_drawNewSeedButton;
    private final JCheckBox m_enableStaticSeedChecker;
    
    /**
     * Builds up the dialog.
     */
    @SuppressWarnings("unchecked")
    public SmoteNodeDialog() {
        super();
        m_kNNSpinner = new JSpinner(new SpinnerNumberModel(5, 1,
                Integer.MAX_VALUE, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)m_kNNSpinner
                .getEditor();
        editor.getTextField().setColumns(8);
        m_rateSpinner = new JSpinner(new SpinnerNumberModel(2.0, 0.0,
                Double.MAX_VALUE, 1.0));
        editor = (JSpinner.DefaultEditor)m_rateSpinner.getEditor();
        editor.getTextField().setColumns(8);
        ButtonGroup buttonGroup = new ButtonGroup();
        m_smoteAllButton = new JRadioButton("Oversample by: ");
        m_smoteAllButton.setToolTipText(
                "Oversample all classes equally but this rate.");
        m_smoteMinorityButton = new JRadioButton("Oversample minority classes");
        m_smoteMinorityButton
                .setToolTipText("Oversample only minority classes "
                        + "(all classes occur equally often)");
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_rateSpinner.setEnabled(e.getSource() == m_smoteAllButton);
            }
        };
        buttonGroup.add(m_smoteAllButton);
        buttonGroup.add(m_smoteMinorityButton);
        m_smoteAllButton.addActionListener(actionListener);
        m_smoteMinorityButton.addActionListener(actionListener);
        m_smoteAllButton.setSelected(true);
        m_selectionPanel = new ColumnSelectionPanel((Border)null,
                StringValue.class);
        m_seedField = new JFormattedTextField(new AbstractFormatter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Object stringToValue(
                    final String text) throws ParseException {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException nfe) {
                    throw new ParseException("Contains non-numeric chars", 0);
                }
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public String valueToString(
                    final Object value) throws ParseException {
                return value == null ? null : value.toString();
            }
        });
        m_seedField.setColumns(8);
        
        m_drawNewSeedButton = new JButton("Draw new seed");
        m_drawNewSeedButton.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
               long l = Double.doubleToLongBits(Math.random());
               m_seedField.setText(Long.toString(l));
            } 
        });
        
        m_enableStaticSeedChecker = new JCheckBox("Enable static seed");
        m_enableStaticSeedChecker.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                checkEnableState();
            }
        });

        JPanel tab = new JPanel(new GridLayout(0, 2));
        tab.add(getInFlowLayout(new JLabel("Class column: ")));
        tab.add(getInFlowLayout(m_selectionPanel));
        tab.add(getInFlowLayout(new JLabel("# Nearest neighbor: ")));
        tab.add(getInFlowLayout(m_kNNSpinner));
        tab.add(getInFlowLayout(m_smoteAllButton));
        tab.add(getInFlowLayout(m_rateSpinner));
        tab.add(getInFlowLayout(m_smoteMinorityButton));
        tab.add(getInFlowLayout(new JLabel()));
        tab.add(getInFlowLayout(m_enableStaticSeedChecker));
        tab.add(getInFlowLayout(m_seedField, m_drawNewSeedButton));
        addTab("Settings", tab);
    }
    
    private void checkEnableState() {
        boolean enabled = m_enableStaticSeedChecker.isSelected();
        m_drawNewSeedButton.setEnabled(enabled);
        m_seedField.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        int kNN = settings.getInt(SmoteNodeModel.CFG_KNN, 5);
        String method = settings.getString(SmoteNodeModel.CFG_METHOD,
                SmoteNodeModel.METHOD_ALL);
        double rate = settings.getDouble(SmoteNodeModel.CFG_RATE, 2.0);
        String clas = settings.getString(SmoteNodeModel.CFG_CLASS, null);
        m_selectionPanel.update(specs[0], clas);
        m_kNNSpinner.setValue(new Integer(kNN));
        m_rateSpinner.setValue(new Double(rate));
        if (SmoteNodeModel.METHOD_MAJORITY.equals(method)) {
            m_smoteMinorityButton.doClick();
        } else {
            m_smoteAllButton.doClick();
        }
        String seed = settings.getString(SmoteNodeModel.CFG_SEED, null);
        Long lSeed = null;
        if (seed != null) {
            try {
                lSeed = Long.parseLong(seed);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        m_enableStaticSeedChecker.setSelected(lSeed != null);
        if (lSeed != null) {
            m_seedField.setText(Long.toString(lSeed));
        }
        checkEnableState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        int kNN = ((Integer)m_kNNSpinner.getValue()).intValue();
        double rate = ((Double)m_rateSpinner.getValue()).doubleValue();
        String method = SmoteNodeModel.METHOD_ALL;
        if (m_smoteMinorityButton.isSelected()) {
            method = SmoteNodeModel.METHOD_MAJORITY;
        }
        String clas = m_selectionPanel.getSelectedColumn();
        String seed = null;
        if (m_enableStaticSeedChecker.isSelected()) {
            try {
                String t = m_seedField.getText();
                Long.parseLong(t);
                seed = t;
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException(
                        "Can't parse seed as number.");
            }
        }
        settings.addInt(SmoteNodeModel.CFG_KNN, kNN);
        settings.addString(SmoteNodeModel.CFG_METHOD, method);
        settings.addDouble(SmoteNodeModel.CFG_RATE, rate);
        settings.addString(SmoteNodeModel.CFG_CLASS, clas);
        settings.addString(SmoteNodeModel.CFG_SEED, seed);        
    }

    private static JPanel getInFlowLayout(final JComponent... comps) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            panel.add(c);
        }
        return panel;
    }
}
