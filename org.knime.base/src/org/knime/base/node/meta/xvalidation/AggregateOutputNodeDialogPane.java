/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 6, 2006 (wiswedel): created
 */
package org.knime.base.node.meta.xvalidation;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * Two columns: One for target, one for prediction.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AggregateOutputNodeDialogPane extends NodeDialogPane {
    
    private final ColumnSelectionComboxBox m_targetCombo;
    private final ColumnSelectionComboxBox m_predictCombo;
    
    /**
     * Inits GUI. 
     */
    @SuppressWarnings("unchecked")
    public AggregateOutputNodeDialogPane() {
        JPanel p = new JPanel(new GridLayout(0, 2));
        JLabel l1 = new JLabel("Target Column: ");
        m_targetCombo = new ColumnSelectionComboxBox(
                (Border)null, StringValue.class);
        p.add(getInFlowLayout(l1));
        p.add(getInFlowLayout(m_targetCombo));
        JLabel l2 = new JLabel("Prediction Column: ");
        m_predictCombo = new ColumnSelectionComboxBox(
                (Border)null, StringValue.class);
        p.add(getInFlowLayout(l2));
        p.add(getInFlowLayout(m_predictCombo));
        addTab("Column Selection", p);
    }
    
    private static JPanel getInFlowLayout(final JComponent c) {
        JPanel result = new JPanel(new FlowLayout());
        result.add(c);
        return result;
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        DataTableSpec in = specs[0];
        String targetCol = null;
        String predictCol = null;
        for (int i = in.getNumColumns() - 1; i >= 0; i--) {
            DataColumnSpec c = in.getColumnSpec(i);
            if (c.getType().isCompatible(StringValue.class)) {
                if (predictCol == null) {
                    predictCol = c.getName();
                } else {
                    assert targetCol == null;
                    targetCol = c.getName();
                    break; // both columns assigned
                }
            }
        }
        if (targetCol == null) {
            throw new NotConfigurableException(
                    "Invalid input: Need at least two string columns.");
        }
        String targetSettingsCol = settings.getString(
                AggregateOutputNodeModel.CFG_TARGET_COL, targetCol);
        String predictSettingsCol = settings.getString(
                AggregateOutputNodeModel.CFG_PREDICT_COL, predictCol);
        if (!in.containsName(targetSettingsCol)) {
            targetSettingsCol = targetCol;
        }
        if (!in.containsName(predictSettingsCol)) {
            predictSettingsCol = predictCol;
        }
        m_targetCombo.update(in, targetSettingsCol);
        m_predictCombo.update(in, predictSettingsCol);
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String targetCol = m_targetCombo.getSelectedColumn();
        String predCol = m_predictCombo.getSelectedColumn();
        settings.addString(AggregateOutputNodeModel.CFG_TARGET_COL, targetCol);
        settings.addString(AggregateOutputNodeModel.CFG_PREDICT_COL, predCol);
    }

}
