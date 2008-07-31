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
 *   Aug 12, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.domain;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class DomainNodeDialogPane extends NodeDialogPane {
    
    private final ColumnFilterPanel m_possValuesPanel;
    private final ColumnFilterPanel m_minMaxPanel;
    private final JCheckBox m_maxValuesChecker;
    private final JSpinner m_maxValuesSpinner;
    
    /** Inits members, does nothing else. */
    public DomainNodeDialogPane() {
        m_possValuesPanel = new ColumnFilterPanel();
        m_minMaxPanel = new ColumnFilterPanel();
        JPanel possValPanel = new JPanel(new BorderLayout());
        possValPanel.add(m_possValuesPanel, BorderLayout.CENTER);
        m_maxValuesChecker = new JCheckBox(
                "Restrict number of possible values: ");
        SpinnerModel spinModel = 
            new SpinnerNumberModel(60, 1, Integer.MAX_VALUE, 10);
        m_maxValuesSpinner = new JSpinner(spinModel);
        JSpinner.DefaultEditor editor = 
            (JSpinner.DefaultEditor) m_maxValuesSpinner.getEditor();
        editor.getTextField().setColumns(6);
        m_maxValuesChecker.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
                m_maxValuesSpinner.setEnabled(m_maxValuesChecker.isSelected());
           } 
        });
        JPanel southPanel = new JPanel(new FlowLayout());
        southPanel.add(m_maxValuesChecker);
        southPanel.add(m_maxValuesSpinner);
        possValPanel.add(southPanel, BorderLayout.SOUTH);
        addTab("Possible Values", possValPanel);
        JPanel minMaxPanel = new JPanel(new BorderLayout());
        minMaxPanel.add(m_minMaxPanel, BorderLayout.CENTER);
        addTab("Min & Max Values", minMaxPanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException("No data at input.");
        }
        String[] stringCols = 
            DomainNodeModel.getAllCols(NominalValue.class, specs[0]); 
        String[] possCols = settings.getStringArray(
                DomainNodeModel.CFG_POSSVAL_COLS, stringCols);
        
        String[] dblCols = 
            DomainNodeModel.getAllCols(BoundedValue.class, specs[0]);
        String[] minMaxCols = settings.getStringArray(
                DomainNodeModel.CFG_MIN_MAX_COLS, dblCols);
        m_possValuesPanel.update(specs[0], false, possCols);
        m_minMaxPanel.update(specs[0], false, minMaxCols);
        int maxPossValues = 
            settings.getInt(DomainNodeModel.CFG_MAX_POSS_VALUES, 60);
        if ((maxPossValues >= 0) != m_maxValuesChecker.isSelected()) {
            m_maxValuesChecker.doClick();
        }
        m_maxValuesSpinner.setValue(maxPossValues >= 0 ? maxPossValues : 60);
    }
    


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Set<String> possCols = m_possValuesPanel.getIncludedColumnSet();
        Set<String> minMaxCols = m_minMaxPanel.getIncludedColumnSet();
        settings.addStringArray(DomainNodeModel.CFG_POSSVAL_COLS, 
                possCols.toArray(new String[possCols.size()]));
        settings.addStringArray(DomainNodeModel.CFG_MIN_MAX_COLS, 
                minMaxCols.toArray(new String[minMaxCols.size()]));
        int maxPossVals = m_maxValuesChecker.isSelected() 
            ? (Integer)m_maxValuesSpinner.getValue() : -1;
        settings.addInt(DomainNodeModel.CFG_MAX_POSS_VALUES, maxPossVals);
    }

}
