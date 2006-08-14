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
 *   Aug 12, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.domain;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.node.util.FilterColumnPanel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class DomainNodeDialogPane extends NodeDialogPane {
    
    private final FilterColumnPanel m_possValuesPanel;
    private final FilterColumnPanel m_minMaxPanel;
    
    /** Inits members, does nothing else. */
    public DomainNodeDialogPane() {
        m_possValuesPanel = new FilterColumnPanel();
        m_minMaxPanel = new FilterColumnPanel();
        JPanel allPanel = new JPanel(new GridLayout(0, 1));
        JPanel possValPanel = new JPanel(new BorderLayout());
        possValPanel.setBorder(BorderFactory.createMatteBorder(
                0, 0, 2, 0, Color.BLACK));
        possValPanel.add(new JLabel("Possible Values"), BorderLayout.NORTH);
        possValPanel.add(m_possValuesPanel, BorderLayout.CENTER);
        allPanel.add(possValPanel);
        JPanel minMaxPanel = new JPanel(new BorderLayout());
        minMaxPanel.setBorder(BorderFactory.createMatteBorder(
                2, 0, 0, 0, Color.BLACK));
        minMaxPanel.add(new JLabel("Min & Max"), BorderLayout.NORTH);
        minMaxPanel.add(m_minMaxPanel, BorderLayout.CENTER);
        allPanel.add(minMaxPanel);
        addTab("Column Selector", allPanel);
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException("No data at input.");
        }
        String[] stringCols = getAllCols(StringValue.class, specs[0]); 
        String[] possCols = settings.getStringArray(
                DomainNodeModel.CFG_POSSVAL_COLS, stringCols);
        
        String[] dblCols = getAllCols(DoubleValue.class, specs[0]);
        String[] minMaxCols = settings.getStringArray(
                DomainNodeModel.CFG_MIN_MAX_COLS, dblCols);
        m_possValuesPanel.update(specs[0], false, possCols);
        m_minMaxPanel.update(specs[0], false, minMaxCols);
    }
    
    private static String[] getAllCols(
            final Class<? extends DataValue> cl, final DataTableSpec spec) {
        ArrayList<String> result = new ArrayList<String>();
        for (DataColumnSpec c : spec) {
            if (c.getType().isCompatible(cl)) {
                result.add(c.getName());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Set<String> possCols = m_possValuesPanel.getIncludedColumnList();
        Set<String> minMaxCols = m_minMaxPanel.getIncludedColumnList();
        settings.addStringArray(DomainNodeModel.CFG_POSSVAL_COLS, 
                possCols.toArray(new String[possCols.size()]));
        settings.addStringArray(DomainNodeModel.CFG_MIN_MAX_COLS, 
                minMaxCols.toArray(new String[minMaxCols.size()]));
    }

}
