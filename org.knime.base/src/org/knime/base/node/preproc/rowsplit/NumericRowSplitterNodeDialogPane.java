/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
        lowerBoundPanel.add(
                m_lowerBoundCheck.getComponentPanel(), BorderLayout.WEST);
        m_lowerBoundValue = 
            new DialogComponentNumberEdit(lowerBoundValue, "Value: ", 10, 
                    createFlowVariableModel(lowerBoundValue));
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
            new DialogComponentNumberEdit(upperBoundValue, "Value: ", 10,
                    createFlowVariableModel(upperBoundValue));
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
