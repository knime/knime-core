/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   18.06.2007 (gabriel): created
 */
package org.knime.base.node.preproc.rowsplit;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class NumericRowSplitterNodeDialogPane extends NodeDialogPane {
    
    private static final String[] LOWER_BOUNDS = new String[]{">=", ">"};
    private static final String[] UPPER_BOUNDS = new String[]{"<=", "<"};
    
    private final DialogComponentColumnNameSelection m_columnSelection;
    private final DialogComponentBoolean m_lowerBoundCheck;
    private final DialogComponentNumberEdit m_lowerBoundValue;
    private final DialogComponentStringSelection m_lowerBound;
    private final DialogComponentBoolean m_upperBoundCheck;
    private final DialogComponentNumberEdit m_upperBoundValue;
    private final DialogComponentStringSelection m_upperBound;

    /**
     * Creates a new dialog pane with a field for numeric column selection,
     * as well as components to define lower and upper bound optionally.
     */
    @SuppressWarnings("unchecked")
    public NumericRowSplitterNodeDialogPane() {
        JPanel columnSelectionPanel = new JPanel(new BorderLayout());
        columnSelectionPanel.setBorder(BorderFactory.createTitledBorder(
                " Column selection "));
        m_columnSelection = new DialogComponentColumnNameSelection(
                createColumnSelectionModel(), "", 0, DoubleValue.class);
        columnSelectionPanel.add(m_columnSelection.getComponentPanel(), 
                BorderLayout.CENTER);
        
        final SettingsModelBoolean lowerBoundCheck = 
            createLowerBoundCheckBoxModel();
        final SettingsModelDouble lowerBoundValue =
            createLowerBoundTextfieldModel();
        final SettingsModelString lowerBound =
            createLowerBoundModel();
        lowerBoundCheck.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                boolean isSelected = lowerBoundCheck.getBooleanValue();
                lowerBoundValue.setEnabled(isSelected);
                lowerBound.setEnabled(isSelected);
            }
        });
        
        JPanel lowerBoundPanel = new JPanel(new BorderLayout());
        lowerBoundPanel.setBorder(BorderFactory.createTitledBorder(
                " Lower bound "));
        m_lowerBoundCheck = new DialogComponentBoolean(lowerBoundCheck, "");
        lowerBoundPanel.add(m_lowerBoundCheck.getComponentPanel(), BorderLayout.WEST);
        m_lowerBoundValue = 
            new DialogComponentNumberEdit(lowerBoundValue, "Value: ", 10);
        lowerBoundPanel.add(m_lowerBoundValue.getComponentPanel(), 
                BorderLayout.CENTER);
        m_lowerBound = new DialogComponentStringSelection(
                lowerBound, "", LOWER_BOUNDS);
        lowerBoundPanel.add(m_lowerBound.getComponentPanel(), 
                BorderLayout.EAST);

        final SettingsModelBoolean upperBoundCheck = 
            createUpperBoundCheckBoxModel();
        final SettingsModelDouble upperBoundValue =
            createUpperBoundTextfieldModel();
        final SettingsModelString upperBound =
            createUpperBoundModel();
        upperBoundCheck.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                boolean isSelected = upperBoundCheck.getBooleanValue();
                upperBoundValue.setEnabled(isSelected);
                upperBound.setEnabled(isSelected);
            }
        });
        
        JPanel upperBoundPanel = new JPanel(new BorderLayout());
        upperBoundPanel.setBorder(BorderFactory.createTitledBorder(
                " Upper bound "));
        m_upperBoundCheck = new DialogComponentBoolean(upperBoundCheck, "");
        upperBoundPanel.add(m_upperBoundCheck.getComponentPanel(), 
                BorderLayout.WEST);
        m_upperBoundValue = 
            new DialogComponentNumberEdit(upperBoundValue, "Value: ", 10);
        upperBoundPanel.add(m_upperBoundValue.getComponentPanel(), 
                BorderLayout.CENTER);
        m_upperBound = 
            new DialogComponentStringSelection(upperBound, "", UPPER_BOUNDS);
        upperBoundPanel.add(m_upperBound.getComponentPanel(), 
                BorderLayout.EAST);
        
        JPanel comp = new JPanel(new GridLayout(3, 1));
        comp.add(columnSelectionPanel);
        comp.add(lowerBoundPanel);
        comp.add(upperBoundPanel);
        super.addTab("Settings", comp);
    }
    
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, 
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_columnSelection.loadSettingsFrom(settings, specs);
        m_lowerBound.loadSettingsFrom(settings, specs);
        m_lowerBoundCheck.loadSettingsFrom(settings, specs);
        m_lowerBoundValue.loadSettingsFrom(settings, specs);
        m_upperBound.loadSettingsFrom(settings, specs);
        m_upperBoundCheck.loadSettingsFrom(settings, specs);
        m_upperBoundValue.loadSettingsFrom(settings, specs);
    }
    
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) 
            throws InvalidSettingsException {
        m_columnSelection.saveSettingsTo(settings);
        m_lowerBound.saveSettingsTo(settings);
        m_lowerBoundCheck.saveSettingsTo(settings);
        m_lowerBoundValue.saveSettingsTo(settings);
        m_upperBound.saveSettingsTo(settings);
        m_upperBoundCheck.saveSettingsTo(settings);
        m_upperBoundValue.saveSettingsTo(settings);
    }
    
    /**
     * @return model used for numeric column selection
     */
    static final SettingsModelString createColumnSelectionModel() {
        return new SettingsModelString("column_selection", null);
    }
    
    /**
     * @return model to check if lower bound is defined
     */
    static final SettingsModelBoolean createLowerBoundCheckBoxModel() {
        return new SettingsModelBoolean("lower_bound_defined", true);
    }
    
    /**
     * @return a new model to specify lower bound value
     */
    static final SettingsModelDouble createLowerBoundTextfieldModel() {
        return new SettingsModelDouble("lower_bound_value", 0.0);
    }
    
    /**
     * 
     * @return a new string model to select lower bound property
     */
    static final SettingsModelString createLowerBoundModel() {
        return new SettingsModelString("lower_bound", LOWER_BOUNDS[0]);
    }
    
    /**
     * 
     * @return model to check if upper bound is defined
     */
    static final SettingsModelBoolean createUpperBoundCheckBoxModel() {
        return new SettingsModelBoolean("upper_bound_defined", true);
    }
    
    /**
     * @return a new model to specify a upper value
     */
    static final SettingsModelDouble createUpperBoundTextfieldModel() {
        return new SettingsModelDouble("upper_bound_value", 0.0);
    }
    
    /**
     * @return a new string model to select the upper bound property
     */
    static final SettingsModelString createUpperBoundModel() {
        return new SettingsModelString("upper_bound", UPPER_BOUNDS[0]);
    }
    
    /**
     * 
     * @param model contains the selected lower bound property
     * @return true if the belongs to the interval, otherwise false
     */
    static final boolean includeLowerBound(final SettingsModelString model) {
        return model.getStringValue().equals(LOWER_BOUNDS[0]);
    }

    /**
     * 
     * @param model contains the selected upper bound property
     * @return true if the belongs to the interval, otherwise false
     */
    static final boolean includeUpperBound(final SettingsModelString model) {
        return model.getStringValue().equals(UPPER_BOUNDS[0]);
    }
}
