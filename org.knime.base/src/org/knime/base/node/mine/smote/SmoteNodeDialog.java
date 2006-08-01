/*
 * -------------------------------------------------------------------
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.mine.smote;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
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

    /**
     * Builds up the dialog.
     */
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
        m_smoteAllButton
                .setToolTipText("Oversample all classes equally but this rate.");
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
        JPanel tab = new JPanel(new GridLayout(0, 2));
        tab.add(getInFlowLayout(new JLabel("Class column: ")));
        tab.add(getInFlowLayout(m_selectionPanel));
        tab.add(getInFlowLayout(new JLabel("# Nearest neighbor: ")));
        tab.add(getInFlowLayout(m_kNNSpinner));
        tab.add(getInFlowLayout(m_smoteAllButton));
        tab.add(getInFlowLayout(m_rateSpinner));
        tab.add(getInFlowLayout(m_smoteMinorityButton));
        addTab("Settings", tab);
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
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
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
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
        settings.addInt(SmoteNodeModel.CFG_KNN, kNN);
        settings.addString(SmoteNodeModel.CFG_METHOD, method);
        settings.addDouble(SmoteNodeModel.CFG_RATE, rate);
        settings.addString(SmoteNodeModel.CFG_CLASS, clas);
    }

    private static JPanel getInFlowLayout(final JComponent comp) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(comp);
        return panel;
    }
}
