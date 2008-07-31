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
 * History
 *   25.02.2007 (wiswedel): created
 */
package org.knime.base.node.preproc.lowvarfilter;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * Dialog for low variance filter node. Shows a double value chooser and a 
 * filter panel.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LowVarFilterNodeDialogPane extends NodeDialogPane {
    
    private final JSpinner m_varianceSpinner;
    private final ColumnFilterPanel m_colFilterPanel;

    /** Inits GUI. */
    @SuppressWarnings("unchecked")
    public LowVarFilterNodeDialogPane() {
        m_varianceSpinner = new JSpinner(new SpinnerNumberModel(
                0.0, 0.0, Double.POSITIVE_INFINITY, 0.1));
        m_varianceSpinner.setEditor(new JSpinner.NumberEditor(m_varianceSpinner,
                "0.0#########"));        
        JSpinner.DefaultEditor editor =
            (JSpinner.DefaultEditor)m_varianceSpinner.getEditor();
        editor.getTextField().setColumns(10);
        m_colFilterPanel = new ColumnFilterPanel(DoubleValue.class);
        JPanel p = new JPanel(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout());
        northPanel.add(new JLabel("Variance Upper Bound"));
        northPanel.add(m_varianceSpinner);
        p.add(northPanel, BorderLayout.NORTH);
        p.add(m_colFilterPanel, BorderLayout.CENTER);
        addTab("Options", p);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, 
            final DataTableSpec[] specs) throws NotConfigurableException {
        List<String> defIncludes = new ArrayList<String>();
        for (DataColumnSpec s : specs[0]) {
            if (s.getType().isCompatible(DoubleValue.class)) {
                defIncludes.add(s.getName());
            }
        }
        double threshold = settings.getDouble(
                LowVarFilterNodeModel.CFG_KEY_MAX_VARIANCE, 0.0);
        String[] includes = settings.getStringArray(
                LowVarFilterNodeModel.CFG_KEY_COL_FILTER, 
                defIncludes.toArray(new String[defIncludes.size()]));
        m_varianceSpinner.setValue(threshold);
        m_colFilterPanel.update(specs[0], false, includes);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) 
        throws InvalidSettingsException {
        double threshold = ((Number)m_varianceSpinner.getValue()).doubleValue();
        Set<String> includes = m_colFilterPanel.getIncludedColumnSet();
        settings.addStringArray(LowVarFilterNodeModel.CFG_KEY_COL_FILTER, 
                includes.toArray(new String[includes.size()]));
        settings.addDouble(
                LowVarFilterNodeModel.CFG_KEY_MAX_VARIANCE, threshold);
    }

}
