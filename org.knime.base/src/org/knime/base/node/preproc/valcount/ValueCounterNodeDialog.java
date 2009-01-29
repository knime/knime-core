/* Created on 27.03.2007 14:55:11 by thor
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.preproc.valcount;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This class is the dialog for the value counter dialog that lets the user
 * selected an arbitrary column.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ValueCounterNodeDialog extends NodeDialogPane {
    private final ColumnSelectionComboxBox m_columnName =
            new ColumnSelectionComboxBox((Border)null, DataValue.class);
    private final JCheckBox m_hiliting = new JCheckBox();
    
    private final ValueCounterSettings m_settings = new ValueCounterSettings();
    
    /**
     * Creates a new dialog for the value counter node.
     */
    public ValueCounterNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Column with values to count "), c);
        c.gridx++;
        p.add(m_columnName, c);
        
        c.gridx = 0;
        c.gridy = 1;
        p.add(new JLabel("Enable hiliting  "), c);
        c.gridx++;
        p.add(m_hiliting, c);
        addTab("Standard settings", p);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            m_settings.loadSettings(settings);
        } catch (InvalidSettingsException ex) {
            // ignore it and use defaults
        }
        m_columnName.update(specs[0], m_settings.columnName());
        m_hiliting.setSelected(m_settings.hiliting());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.columnName(m_columnName.getSelectedColumn());
        m_settings.hiliting(m_hiliting.isSelected());
        m_settings.saveSettings(settings);
    }
}
