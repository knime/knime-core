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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   11.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;


import static org.knime.base.node.jsnippet.ui.InFieldsTableModel.COL_COLUMN;
import static org.knime.base.node.jsnippet.ui.InFieldsTableModel.COL_JAVA_FIELD;
import static org.knime.base.node.jsnippet.ui.InFieldsTableModel.COL_JAVA_TYPE;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;

import org.knime.base.node.jsnippet.JavaFieldSettings.InCol;
import org.knime.base.node.jsnippet.JavaFieldSettings.InVar;
import org.knime.base.node.jsnippet.JavaFieldSettingsList.InColList;
import org.knime.base.node.jsnippet.JavaFieldSettingsList.InVarList;
import org.knime.base.node.jsnippet.JavaSnippetFields;
import org.knime.base.node.jsnippet.type.DataValueToJava;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.util.ConfigTablePanel;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.FlowVariableTableCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

/**
 * The table where java snippet fields for input columns and flow variables
 * can be defined.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings({"rawtypes", "serial" })
public class InFieldsTable extends ConfigTablePanel {


    private InFieldsTableModel m_model;
    private DataTableSpec m_spec;
    private Map<String, FlowVariable> m_flowVars;

    /**
     * Create a new empty table.
     */
    public InFieldsTable() {
        super(new InFieldsTableModel());
        final JTable table = getTable();
        m_model = (InFieldsTableModel)getModel();
        m_model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                // The table is highly interactive. Changing the input column
                // can invalidate the java type and a editing the java
                // identifier can invalidate other java identifiers (duplicate
                // names). Therefore, force a repaint of the whole, most likely
                // small table.
                table.tableChanged(new TableModelEvent(m_model,
                        0, m_model.getRowCount()));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ActionListener createAddButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DataColumnSpec defaultColTarget = null;
                if (null != m_spec) {
                    Set<String> cols = new HashSet<String>();
                    for (int r = 0; r < m_model.getRowCount(); r++) {
                        Object value = m_model.getValueAt(r, COL_COLUMN);
                        if (value instanceof DataColumnSpec) {
                            cols.add(((DataColumnSpec)value).getName());
                        }
                    }
                    defaultColTarget = null;
                    for (DataColumnSpec colSpec : m_spec) {
                        if (null == defaultColTarget) {
                            defaultColTarget = colSpec;
                        }
                        if (!cols.contains(colSpec.getName())) {
                            // Add a row and fill it
                            addRow(colSpec);
                            return;
                        }
                    }
                }
                FlowVariable defaultVarTarget = null;
                if (null != m_flowVars) {
                    Set<String> flowVars = new HashSet<String>();
                    for (int r = 0; r < m_model.getRowCount(); r++) {
                        Object value = m_model.getValueAt(r, COL_COLUMN);
                        if (value instanceof FlowVariable) {
                            flowVars.add(((FlowVariable)value).getName());
                        }
                    }
                    defaultVarTarget = !m_flowVars.isEmpty()
                        ? m_flowVars.values().iterator().next() : null;
                    for (FlowVariable flowVar : m_flowVars.values()) {
                        if (!flowVars.contains(flowVar.getName())) {
                            // Add a row and fill it
                            addRow(flowVar);
                            return;
                        }
                    }
                }
                if (null != defaultColTarget) {
                    addRow(defaultColTarget);
                } else if (null != defaultVarTarget) {
                    addRow(defaultVarTarget);
                } else {
                    m_model.addRow();
                }

            }
        };
    }

    /**
     * Get the column index defining the java field name.
     * @return the index of the column with the java field names.
     */
    public int getFieldNameColumnIndex() {
        return COL_JAVA_FIELD;
    }


    /**
     * Adds a row for the given input column.
     *
     * @param colSpec the input column
     * @return true when the row was added successfully
     */
    public boolean addRow(final DataColumnSpec colSpec) {
        int row = m_model.getRowCount();
        m_model.addRow();

        m_model.setValueAt(colSpec, row, COL_COLUMN);
        String colName = colSpec.getName();
        Set<String> taken = new HashSet<String>();
        for (int i = 0; i < m_model.getRowCount(); i++) {
            taken.add((String)m_model.getValueAt(i, COL_JAVA_FIELD));
        }
        String fieldName = FieldsTableUtil.createUniqueJavaIdentifier(
                colName, taken, "c_");
        m_model.setValueAt(fieldName, row, COL_JAVA_FIELD);
        DataType elemType = colSpec.getType().isCollectionType()
            ? colSpec.getType().getCollectionElementType()
            : colSpec.getType();
        DataValueToJava dvToJava =
            TypeProvider.getDefault().getCompatibleTypes(elemType,
                    colSpec.getType().isCollectionType());
        Class javaType = dvToJava.getPreferredJavaType();
        m_model.setValueAt(javaType, row, COL_JAVA_TYPE);
        return true;
    }

    /**
     * Adds a row for the given flow variable.
     *
     * @param var the flow variable definition
     * @return true when the row was added successfully
     */
    public boolean addRow(final FlowVariable var) {
        int row = m_model.getRowCount();
        m_model.addRow();
        m_model.setValueAt(var, row, COL_COLUMN);
        String varName = var.getName();
        Set<String> taken = new HashSet<String>();
        for (int i = 0; i < m_model.getRowCount(); i++) {
            taken.add((String)m_model.getValueAt(i, COL_JAVA_FIELD));
        }
        String fieldName = FieldsTableUtil.createUniqueJavaIdentifier(
                varName, taken, "v_");
        m_model.setValueAt(fieldName, row, COL_JAVA_FIELD);
        Class javaType = TypeProvider.getDefault()
            .getTypeConverter(var.getType()).getPreferredJavaType();
        m_model.setValueAt(javaType, row, COL_JAVA_TYPE);
        return true;
    }

    /**
     * Set the table data.
     *
     * @param fields the fields
     * @param spec the input spec might be null
     * @param flowVars the flow variables
     */
    void updateData(final JavaSnippetFields fields,
            final DataTableSpec spec,
            final Map<String, FlowVariable> flowVars) {
        m_spec = spec;
        m_flowVars = flowVars;

        m_model.clear();
        for (int r = 0; r < fields.getInColFields().size(); r++) {
            InCol field = fields.getInColFields().get(r);
            m_model.addRow();
            String colName = field.getKnimeName();
            DataColumnSpec colSpec = spec.getColumnSpec(colName);
            Object value = null != colSpec ? colSpec : colName;
            m_model.setValueAt(value, r, COL_COLUMN);
            m_model.setValueAt(field.getJavaName(), r, COL_JAVA_FIELD);
            m_model.setValueAt(field.getJavaType(), r, COL_JAVA_TYPE);
        }
        int offset = m_model.getRowCount();
        for (int r = 0; r < fields.getInVarFields().size(); r++) {
            InVar field = fields.getInVarFields().get(r);
            m_model.addRow();
            String name = field.getKnimeName();
            FlowVariable flowVar = m_flowVars.get(name);
            Object value = null != flowVar ? flowVar : name;
            m_model.setValueAt(value, offset + r, COL_COLUMN);
            m_model.setValueAt(field.getJavaName(), offset + r, COL_JAVA_FIELD);
            m_model.setValueAt(field.getJavaType(), offset + r, COL_JAVA_TYPE);
        }

        JTable table = getTable();
        table.getColumnModel().getColumn(COL_COLUMN).setCellRenderer(
                new InputTableCellRenderer());
        table.getColumnModel().getColumn(COL_COLUMN).setCellEditor(
                createInputCellEditor());
        table.getColumnModel().getColumn(COL_JAVA_FIELD).setCellRenderer(
                FieldsTableUtil.createJavaFieldTableCellRenderer());
        table.getColumnModel().getColumn(COL_JAVA_TYPE).setCellRenderer(
                FieldsTableUtil.createJavaTypeTableCellRenderer());
        table.getColumnModel().getColumn(COL_JAVA_TYPE).setCellEditor(
                FieldsTableUtil.createJavaTypeTableCellEditor());
    }

    /** Create cell editor for for the input columns / flow variables. */
    private TableCellEditor createInputCellEditor() {
        JComboBox comboBox = new JComboBox();
        comboBox.setRenderer(new InputListCellRenderer());
        if (null != m_spec) {
            for (DataColumnSpec colSpec : m_spec) {
                comboBox.addItem(colSpec);
            }
        }
        if (null != m_flowVars) {
            for (FlowVariable flowVar : m_flowVars.values()) {
                comboBox.addItem(flowVar);
            }
        }
        return new DefaultCellEditor(comboBox);
    }


    /**
     * Get the field definitions representing input columns.
     *
     * @return fields representing input columns.
     */
    public InColList getInColFields() {
        InColList inCols = new InColList();
        for (int r = 0; r < m_model.getRowCount(); r++) {
            if (!m_model.validateValues(r)) {
                // there are errors in this row
                continue;
            }
            Object value = m_model.getValueAt(r, COL_COLUMN);
            if (value instanceof DataColumnSpec) {
                DataColumnSpec colSpec = (DataColumnSpec)value;
                InCol inCol = new InCol();
                inCol.setKnimeType(colSpec.getType());
                inCol.setKnimeName(colSpec.getName());
                inCol.setJavaName(
                        (String)m_model.getValueAt(r, COL_JAVA_FIELD));
                Object javaTypeObject = m_model.getValueAt(r, COL_JAVA_TYPE);
                if (javaTypeObject instanceof Class) {
                    inCol.setJavaType((Class)javaTypeObject);
                }
                inCols.add(inCol);
            }
        }
        return inCols;
    }

    /**
     * Get the field definitions representing input flow variables.
     *
     * @return fields representing input flow variables
     */
    public InVarList getInVarFields() {
        InVarList inCols = new InVarList();
        for (int r = 0; r < m_model.getRowCount(); r++) {
            if (!m_model.validateValues(r)) {
                // there are errors in this row
                continue;
            }
            Object value = m_model.getValueAt(r, COL_COLUMN);
            if (value instanceof FlowVariable) {
                FlowVariable colSpec = (FlowVariable)value;
                InVar inVar = new InVar();
                inVar.setKnimeType(colSpec.getType());
                inVar.setKnimeName(colSpec.getName());
                inVar.setJavaName(
                        (String)m_model.getValueAt(r, COL_JAVA_FIELD));
                inVar.setJavaType((Class)m_model.getValueAt(r, COL_JAVA_TYPE));
                inCols.add(inVar);
            }
        }
        return inCols;
    }

    /** Renders the table cells defining the input column or flow variables. */
    private static class InputListCellRenderer
            extends FlowVariableListCellRenderer {

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
            // reset values which maybe changed by previous calls of this method
            setForeground(list.getForeground());
            setBackground(list.getBackground());
            // let super class do the first step
            super.getListCellRendererComponent(list, value, index, isSelected,
                    cellHasFocus);
            if (value instanceof DataColumnSpec) {
                DataColumnSpec col = (DataColumnSpec)value;
                setIcon(col.getType().getIcon());
                setText(col.getName());
                setToolTipText(null);
            } else if (null == value || value instanceof String) {
                setIcon(null);
                setBackground(reddishBackground());
                setToolTipText("Please select an input.");
            }
            return this;
        }

        private Color reddishBackground() {
            Color b = getBackground();
            return new Color((b.getRed() + 255) / 2, b.getGreen() / 2,
                    b.getBlue() / 2);
        }
    }


    /** Renders the table cells defining the input column or flow variables. */
    private static class InputTableCellRenderer
            extends FlowVariableTableCellRenderer {

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(final JTable table,
                final Object value, final boolean isSelected,
                final boolean hasFocus, final int row,
                final int column) {
            // reset values which maybe changed by previous calls of this method
            setForeground(table.getForeground());
            setBackground(table.getBackground());
            setFont(getFont().deriveFont(Font.PLAIN));
            // let super class do the first step
            super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus,
                    row, column);
            FieldsTableModel model = (FieldsTableModel)table.getModel();
            if (model.isValidValue(row, column)) {
                if (value instanceof FlowVariable) {
                    setFont(getFont().deriveFont(Font.ITALIC));
                } else if (value instanceof DataColumnSpec) {
                    DataColumnSpec col = (DataColumnSpec)value;
                    setIcon(col.getType().getIcon());
                    setText(col.getName());
                    setToolTipText(null);
                }
            } else {
                if (value instanceof FlowVariable) {
                    setFont(getFont().deriveFont(Font.ITALIC));
                } else if (value instanceof DataColumnSpec) {
                    DataColumnSpec col = (DataColumnSpec)value;
                    setIcon(col.getType().getIcon());
                    setText(col.getName());
                } else {
                    setIcon(null);
                }
                setBackground(reddishBackground());
                setToolTipText(model.getErrorMessage(row, column));
            }
            return this;
        }

        private Color reddishBackground() {
            Color b = getBackground();
            return new Color((b.getRed() + 255) / 2, b.getGreen() / 2,
                    b.getBlue() / 2);
        }
    }
}
