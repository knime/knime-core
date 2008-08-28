/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 */
package org.knime.base.node.io.database;

import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.DatabasePortObjectSpec;
import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBRowFilterNodeDialogPane extends GenericNodeDialogPane {
    
    private final DBConnectionDialogPanel m_tableOptions =
        new DBConnectionDialogPanel();

    @SuppressWarnings("unchecked")
    private final DialogComponentColumnNameSelection m_column
        = new DialogComponentColumnNameSelection(createColumnModel(),
                "", 0, DataValue.class);
    
    private final DialogComponentStringSelection m_operator
        = new DialogComponentStringSelection(createOperatorModel(), 
                "", Arrays.asList("=", "<>", "!=", ">", "<", ">=", "<=", 
                        "BETWEEN", "LIKE", "IN"));
    
    private final DialogComponentString m_value = new DialogComponentString(
            createValueModel(), "");
    
    /**
     * Create query dialog with text box to enter table name.
     */
    DBRowFilterNodeDialogPane() {
        Box optionPanel = Box.createVerticalBox();
        JPanel column = m_column.getComponentPanel();
        column.setBorder(BorderFactory.createTitledBorder(" Column "));
        optionPanel.add(column);
        JPanel operator = m_operator.getComponentPanel();
        operator.setBorder(BorderFactory.createTitledBorder(" Operator "));
        optionPanel.add(operator);
        JPanel value = m_value.getComponentPanel();
        value.setBorder(BorderFactory.createTitledBorder(" Value "));
        optionPanel.add(value);
        super.addTab("Row Filter", optionPanel);
        super.addTab("Table Options", m_tableOptions);      
    }
    
    /**
     * @return string settings model for operator selection
     */
    static final SettingsModelString createOperatorModel() {
        return new SettingsModelString("operator", null);
    }

    /**
     * @return string settings model for column selection
     */
    static final SettingsModelString createColumnModel() {
        return new SettingsModelString("column", null);
    }
    
    /**
     * @return string settings model for the filter value
     */
    static final SettingsModelString createValueModel() {
        return new SettingsModelString("value", "");
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        DataTableSpec spec = 
            ((DatabasePortObjectSpec) specs[0]).getDataTableSpec();
        m_column.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        m_operator.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        m_value.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        m_tableOptions.loadSettingsFrom(settings, new DataTableSpec[]{spec});
    }
    
    /**
     * @return new settings model for column filter
     */
    static final SettingsModelFilterString createColumnFilterModel() {
        return new SettingsModelFilterString("column_filter");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_column.saveSettingsTo(settings);
        m_operator.saveSettingsTo(settings);
        m_value.saveSettingsTo(settings);
        m_tableOptions.saveSettingsTo(settings);
    }
}
