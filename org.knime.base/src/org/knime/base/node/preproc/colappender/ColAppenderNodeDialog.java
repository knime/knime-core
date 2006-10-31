/* Created on Oct 26, 2006 12:41:18 PM by thor
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.preproc.colappender;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.colappender.ColAppenderSettings.DuplicateHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This is the dialog for the column appender node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColAppenderNodeDialog extends NodeDialogPane {
    private final ColAppenderSettings m_settings = new ColAppenderSettings();
    private DataTableSpec[] m_specs;
    
    @SuppressWarnings("unchecked") 
    private final ColumnSelectionComboxBox m_firstColumn =
        new ColumnSelectionComboxBox((Border) null, DataValue.class);
    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_secondColumn =
        new ColumnSelectionComboxBox((Border) null, DataValue.class);
    private final JRadioButton m_duplicateFail =
        new JRadioButton("Do not execute");
    private final JRadioButton m_duplicateFilter =
        new JRadioButton("Filter columns");
    private final JRadioButton m_duplicateSuffix =
        new JRadioButton("Append suffix");
    private final JTextField m_suffix = new JTextField(10);
    
    private final JCheckBox m_sortInMemory = new JCheckBox();
    private final JCheckBox m_innerJoin = new JCheckBox();
    
    /**
     * Creates the dialog.
     */
    public ColAppenderNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        
        c.insets = new Insets(2, 2, 2, 5);
        p.add(new JLabel("Column from left table"), c);        
        c.gridy = 1;
        p.add(new JLabel("Column from right table"), c);
        c.gridy = 2;
        p.add(new JLabel("Duplicate row name handling"), c);
        c.gridy = 5;
        c.insets = new Insets(10, 2, 2, 5);
        p.add(new JLabel("Row name suffix"), c);
        c.insets = new Insets(2, 2, 2, 5);
        c.gridy = 6;
        p.add(new JLabel("Sort tables in memory"), c);        
        c.gridy = 7;
        p.add(new JLabel("Remove rows with missing values"), c);        
        
        c.gridy = 0;
        c.gridx = 1;
        c.insets = new Insets(2, 2, 2, 2);
        p.add(m_firstColumn, c);
        c.gridy = 1;
        p.add(m_secondColumn, c);
        c.gridy = 2;
        p.add(m_duplicateFail, c);
        c.gridy = 3;
        p.add(m_duplicateFilter, c);
        c.gridy = 4;
        p.add(m_duplicateSuffix, c);
        c.gridy = 5;
        p.add(m_suffix, c);
        c.gridy = 6;
        p.add(m_sortInMemory, c);
        c.gridy = 7;
        p.add(m_innerJoin, c);
        
        
        m_suffix.setEnabled(false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_duplicateFail);
        bg.add(m_duplicateFilter);
        bg.add(m_duplicateSuffix);
        
        m_duplicateSuffix.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_suffix.setEnabled(m_duplicateSuffix.isSelected());
            }
        });
        
        addTab("Joiner settings", p);
    }
    
    /**
     * @see org.knime.core.node.NodeDialogPane#loadSettingsFrom(
     *  org.knime.core.node.NodeSettingsRO, org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettings(settings);
        
        m_firstColumn.update(specs[0], m_settings.leftColumn());
        m_secondColumn.update(specs[1], m_settings.rightColumn());
        m_specs = specs;
        
        if (m_settings.duplicateHandling() == DuplicateHandling.FAIL) {
            m_duplicateFail.setSelected(true);
        } else if (m_settings.duplicateHandling() == DuplicateHandling.FILTER) {
            m_duplicateFilter.setSelected(true);
        } else {
            m_duplicateSuffix.setSelected(true);
        }
        
        m_suffix.setText(m_settings.duplicateSuffix());
        m_sortInMemory.setSelected(m_settings.sortInMemory());
        m_innerJoin.setSelected(m_settings.innerJoin());
    }

    /**
     * @see org.knime.core.node.NodeDialogPane#saveSettingsTo(
     *  org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        if ((m_firstColumn.getSelectedColumn() != null)
                && (m_secondColumn.getSelectedColumn() != null)) {
            DataColumnSpec firstSpec = m_specs[0].getColumnSpec(
                    m_firstColumn.getSelectedColumn());
            DataColumnSpec secondSpec = m_specs[1].getColumnSpec(
                    m_secondColumn.getSelectedColumn());
            
            if (!firstSpec.getType().equals(secondSpec.getType())) {
                throw new InvalidSettingsException("The two column types must"
                        + " be equal");
            }
        } else {
            throw new InvalidSettingsException("Please select two columns");
        }
        
        m_settings.leftColumn(m_firstColumn.getSelectedColumn());
        m_settings.rightColumn(m_secondColumn.getSelectedColumn());
        if (m_duplicateFail.isSelected()) {
            m_settings.duplicateHandling(DuplicateHandling.FAIL);
        } else if (m_duplicateFilter.isSelected()) {
            m_settings.duplicateHandling(DuplicateHandling.FILTER);
        } else {
            m_settings.duplicateHandling(DuplicateHandling.APPEND_SUFFIX);
        }
        m_settings.duplicateSuffix(m_suffix.getText());
        m_settings.sortInMemory(m_sortInMemory.isSelected());
        m_settings.innerJoin(m_innerJoin.isSelected());
        
        m_settings.saveSettings(settings);
    }
}
