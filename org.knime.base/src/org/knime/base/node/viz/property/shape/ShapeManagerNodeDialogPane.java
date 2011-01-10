/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */
package org.knime.base.node.viz.property.shape;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ShapeFactory;
import org.knime.core.data.property.ShapeSelectionComboBox;
import org.knime.core.data.property.ShapeSelectionComboBoxRenderer;
import org.knime.core.data.property.ShapeFactory.Shape;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * 
 * @see ShapeManagerNodeModel
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ShapeManagerNodeDialogPane extends NodeDialogPane implements
        ItemListener {

    /** Keeps all columns. */
    private final JComboBox m_columns = new JComboBox();
    
    /** Keeps the all possible column values. */
    private final JTable m_valueTable;
    
    /** Keeps mapping from data cell name to shape. */
    private final Map<String, LinkedHashMap<DataCell, Shape>> m_map;

    /**
     * Creates a new shape manager dialog.
     */
    ShapeManagerNodeDialogPane() {
        super();
        
        // table with column value to shape mapping
        m_valueTable = new JTable();
        m_valueTable.setRowHeight(Math.max(m_valueTable.getRowHeight(), 
                m_columns.getPreferredSize().height));
        m_valueTable.getTableHeader().setReorderingAllowed(false);
        
        // map for key to shape mapping
        m_map = new LinkedHashMap<String, LinkedHashMap<DataCell, Shape>>();

        m_columns.setRenderer(new DataColumnSpecListCellRenderer());
        JPanel columnPanel = new JPanel(new BorderLayout());
        columnPanel.setBorder(BorderFactory
                .createTitledBorder(" Select nominal column: "));
        columnPanel.add(m_columns);

        // panel keep the table with the column value to  shape mapping
        JPanel nominalPanel = new JPanel(new BorderLayout());
        nominalPanel.setBorder(BorderFactory.createTitledBorder(
                " Shape Mapping "));
        nominalPanel.add(new JScrollPane(m_valueTable));

        // center panel that is added to the dialog pane's tabs
        JPanel center = new JPanel(new BorderLayout());
        center.add(columnPanel, BorderLayout.NORTH);
        center.add(nominalPanel, BorderLayout.CENTER);
        super.addTab(" Shape Settings ", center);
    }

    /**
     * Updates this dialog by refreshing all components in the shape settings 
     * tab. Inits the column name combo box and sets the values for the default
     * selected one.
     * 
     * @param settings the settings to load
     * @param specs the input table specs
     * @throws NotConfigurableException if no column contains domain values
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // remove all columns and column value to shape mappings
        m_columns.removeAllItems();
        m_map.clear();
        // read settings and write into the map
        String target = settings.getString(
                ShapeManagerNodeModel.SELECTED_COLUMN, null);

        // add columns and domain value mapping
        int cols = specs[0].getNumColumns();
        Shape[] shapes = ShapeFactory.getShapes().toArray(new Shape[]{});
        for (int i = 0; i < cols; i++) {
            DataColumnSpec cspec = specs[0].getColumnSpec(i);
            DataColumnDomain domain = cspec.getDomain();
            if (domain.hasValues()) {
                LinkedHashMap<DataCell, Shape> domMap = 
                    new LinkedHashMap<DataCell, Shape>();
                int j = 0;
                for (DataCell value : domain.getValues()) {
                    if (value != null) {
                        String shape = settings.getString(value.toString(),
                                // no settings -> assign different shapes
                                null);
                        if (shape == null) {
                            // bugfix 1283
                            domMap.put(value, shapes[j++ % shapes.length]);
                        } else {
                            domMap.put(value, ShapeFactory.getShape(shape));
                        }
                    }
                }
                m_map.put(cspec.getName(), domMap);
            } else  {
                continue;
            }
            m_columns.addItem(cspec);
            if (cspec.getName().equals(target)) {
                m_columns.setSelectedItem(cspec);
            }
        }
        if (m_map.size() == 0) {
            throw new NotConfigurableException("No column in data contains"
                    + " domain values.");
        }
        columnChanged(getSelectedColumn());
        m_columns.addItemListener(this);
    }

    /**
     * Method is invoked by the super class in order to force the dialog to
     * apply its changes.
     * 
     * @param settings the object to write the settings into
     * @throws InvalidSettingsException if either nominal or range selection
     *             could not be saved
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        String cell = getSelectedColumn();
        settings.addString(ShapeManagerNodeModel.SELECTED_COLUMN, cell);
        if (cell != null) {
            Map<DataCell, Shape> domMap = m_map.get(cell);
            DataCell[] vals = new DataCell[domMap.size()];
            int idx = 0;
            for (DataCell value : domMap.keySet()) {
                vals[idx] = value;
                String domValue = vals[idx].toString();
                settings.addString(domValue, domMap.get(vals[idx]).toString());
                idx++;
            }
            settings.addDataCellArray(ShapeManagerNodeModel.VALUES, vals);
        }
    }

    /**
     * @param e the source event.
     * @see ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    public void itemStateChanged(final ItemEvent e) {
        Object o = e.getItem();
        if (o == null) {
            return;
        }
        String cell = ((DataColumnSpec)o).getName();
        columnChanged(cell);
    }
    
    private void columnChanged(final String column) {
        final LinkedHashMap<DataCell, Shape> domMap = m_map.get(column);
        TableModel tableModel = new DefaultTableModel() {
            private final DataCell[] m_valueNames = new DataCell[domMap.size()];
            {
                int row = 0;
                for (DataCell cell : domMap.keySet()) {
                    m_valueNames[row] = cell;
                    row++;
                }
            }
            @Override
            public String getColumnName(final int columnIdx) {
                if (columnIdx == 1) {
                    return "Shapes";
                } else {
                    return "Values of " + column;
                }
            }
            @Override
            public boolean isCellEditable(final int row, final int columnIdx) {
                return (columnIdx == 1);
            }
            @Override
            public int getRowCount() {
                return domMap.size();
            }
            @Override
            public int getColumnCount() {
                return 2;
            }
            @Override
            public Class<?> getColumnClass(final int columnIndex) {
                if (columnIndex == 1) {
                    return Shape.class;
                } else {
                    return DataCell.class;
                }
            }
            @Override
            public Object getValueAt(final int row, final int columnIndex) {
                if (columnIndex == 1) {
                    return domMap.get(m_valueNames[row]);
                } else {
                    return m_valueNames[row]; 
                }   
            }
            @Override
            public void setValueAt(final Object aValue, final int row, 
                    final int columnIdx) {
                assert aValue instanceof Shape;
                assert columnIdx == 1;
                domMap.put(m_valueNames[row], (Shape)aValue);
            }
        };
        m_valueTable.setModel(tableModel);
        m_valueTable.getColumnModel().getColumn(1).setCellEditor(
                new DefaultCellEditor(new ShapeSelectionComboBox()));
        m_valueTable.getColumnModel().getColumn(1).setCellRenderer(
                new ShapeSelectionComboBoxRenderer());
    }
    
    /* Return select column name or String from combo box. */
    private String getSelectedColumn() {
        Object o = m_columns.getSelectedItem();
        if (o == null) {
            return null;
        }
        return ((DataColumnSpec)o).getName();
    }
    
}
