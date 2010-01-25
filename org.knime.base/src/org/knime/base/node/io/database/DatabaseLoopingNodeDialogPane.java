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
