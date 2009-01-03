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
 * History
 *   19.06.2007 (gabriel): created
 */
package org.knime.base.node.io.database;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabaseLoopingNodeDialogPane extends NodeDialogPane {
    
    private final DBReaderDialogPane m_dialog;
    
    private final DialogComponentColumnNameSelection m_columns;
    
    private final DialogComponentBoolean m_aggregatebyRow;
    
    private final DialogComponentBoolean m_appendGridColumn;
    
    private final DialogComponentNumber m_noValues;

    /**
     * 
     *
     */
    DatabaseLoopingNodeDialogPane() {
        m_dialog = new DBReaderDialogPane();
        m_columns = new DialogComponentColumnNameSelection(createColumnModel(), 
                "Column selection: ", 0, NominalValue.class);
        m_aggregatebyRow = new DialogComponentBoolean(createAggregateModel(), 
                "Aggregate by row");
        m_appendGridColumn = new DialogComponentBoolean(createGridColumnModel(),
            "Append grid column");
        m_noValues = new DialogComponentNumber(
                createNoValuesModel(), "No of Values per Query", 1);
        JPanel columnPanel = new JPanel(new BorderLayout());
        columnPanel.add(m_dialog.getPanel(), BorderLayout.CENTER);
        JPanel southPanel = new JPanel(new GridLayout(4, 1));
        southPanel.add(m_columns.getComponentPanel());
        southPanel.add(m_aggregatebyRow.getComponentPanel());
        southPanel.add(m_appendGridColumn.getComponentPanel());
        southPanel.add(m_noValues.getComponentPanel());
        columnPanel.add(southPanel, BorderLayout.SOUTH);
        addTab("Database Query", columnPanel);
    }
    
    /** @return string model for column selection */
    static SettingsModelString createColumnModel() {
        return new SettingsModelString("column_selection", null);
    }
    
    /** @return aggregation model */
    static SettingsModelBoolean createAggregateModel() {
        return new SettingsModelBoolean("aggregate_by_row", false);
    }
    
    /** @return append grid column model */
    static SettingsModelBoolean createGridColumnModel() {
        return new SettingsModelBoolean("append_grid_column", true);
    }
    
    /** @return append grid column model */
    static SettingsModelIntegerBounded createNoValuesModel() {
        return new SettingsModelIntegerBounded("values_per_query", 
                1, 1, Integer.MAX_VALUE);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_dialog.loadSettingsFrom(settings, specs);
        m_columns.loadSettingsFrom(settings, specs);
        m_aggregatebyRow.loadSettingsFrom(settings, specs);
        m_appendGridColumn.loadSettingsFrom(settings, specs);
        m_noValues.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_dialog.saveSettingsTo(settings);
        m_columns.saveSettingsTo(settings);
        m_aggregatebyRow.saveSettingsTo(settings);
        m_appendGridColumn.saveSettingsTo(settings);
        m_noValues.saveSettingsTo(settings);
    }

}
