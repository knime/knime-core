/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   11.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;

import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.ui.FieldsTableModel.Column;
import org.knime.base.node.jsnippet.util.JavaFieldList.InColList;
import org.knime.base.node.jsnippet.util.JavaFieldList.InVarList;
import org.knime.base.node.jsnippet.util.JavaSnippetFields;
import org.knime.base.node.jsnippet.util.field.InCol;
import org.knime.base.node.jsnippet.util.field.InVar;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.node.util.ConfigTablePanel;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.FlowVariableTableCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

/**
 * The table where java snippet fields for input columns and flow variables
 * <p>
 * This class might change and is not meant as public API. can be defined.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings({"rawtypes", "serial"})
public class InFieldsTable extends ConfigTablePanel {
    /**
     * Property fired when a row is manually added by the user.
     */
    public static final String PROP_FIELD_ADDED = "prop_field_added";

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
                table.tableChanged(new TableModelEvent(m_model, 0, m_model.getRowCount()));
            }
        });

        // commit editor on focus lost
        getTable().putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
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
                    Set<String> cols = new HashSet<>();
                    for (int r = 0; r < m_model.getRowCount(); r++) {
                        Object value = m_model.getValueAt(r, Column.COLUMN);
                        if (value instanceof DataColumnSpec) {
                            cols.add(((DataColumnSpec)value).getName());
                        }
                    }
                    for (DataColumnSpec colSpec : m_spec) {
                        if (null == defaultColTarget) {
                            defaultColTarget = colSpec;
                        }
                        if (!cols.contains(colSpec.getName())) {
                            // Add a row and fill it
                            boolean rowAdded = addRow(colSpec);
                            if (rowAdded) {
                                firePropertyChange(PROP_FIELD_ADDED, m_model.getRowCount() - 1, m_model.getRowCount());
                            }
                            return;
                        }
                    }
                }
                FlowVariable defaultVarTarget = null;
                if (null != m_flowVars) {
                    Set<String> flowVars = new HashSet<>();
                    for (int r = 0; r < m_model.getRowCount(); r++) {
                        Object value = m_model.getValueAt(r, Column.COLUMN);
                        if (value instanceof FlowVariable) {
                            flowVars.add(((FlowVariable)value).getName());
                        }
                    }
                    defaultVarTarget = !m_flowVars.isEmpty() ? m_flowVars.values().iterator().next() : null;
                    for (FlowVariable flowVar : m_flowVars.values()) {
                        if (!flowVars.contains(flowVar.getName())) {
                            // Add a row and fill it
                            boolean rowAdded = addRow(flowVar);
                            if (rowAdded) {
                                firePropertyChange(PROP_FIELD_ADDED, m_model.getRowCount() - 1, m_model.getRowCount());
                            }
                            return;
                        }
                    }
                }
                boolean rowAdded = false;
                if (null != defaultColTarget) {
                    rowAdded = addRow(defaultColTarget);
                } else if (null != defaultVarTarget) {
                    rowAdded = addRow(defaultVarTarget);
                } else {
                    m_model.addRow();
                    rowAdded = true;
                }
                if (rowAdded) {
                    firePropertyChange(PROP_FIELD_ADDED, m_model.getRowCount() - 1, m_model.getRowCount());
                }
            }
        };
    }

    /**
     * Adds a row for the given input column.
     *
     * @param colSpec the input column
     * @return true when the row was added successfully
     */
    public boolean addRow(final DataColumnSpec colSpec) {
        int r = m_model.getRowCount();
        m_model.addRow();

        m_model.setValueAt(colSpec, r, Column.COLUMN);
        String colName = colSpec.getName();
        Set<String> taken = new HashSet<>();
        for (int i = 0; i < m_model.getRowCount(); i++) {
            taken.add((String)m_model.getValueAt(i, Column.JAVA_FIELD));
        }
        String fieldName = FieldsTableUtil.createUniqueJavaIdentifier(colName, taken, "c_");
        m_model.setValueAt(fieldName, r, Column.JAVA_FIELD);
        final Optional<DataCellToJavaConverterFactory<?, ?>> first =
            ConverterUtil.getFactoriesForSourceType(colSpec.getType()).stream().findFirst();
        if (first.isPresent()) {
            m_model.setValueAt(first.get(), r, Column.JAVA_TYPE);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Adds a row for the given flow variable.
     *
     * @param var the flow variable definition
     * @return true when the row was added successfully
     */
    public boolean addRow(final FlowVariable var) {
        int r = m_model.getRowCount();
        m_model.addRow();
        m_model.setValueAt(var, r, Column.COLUMN);
        String varName = var.getName();
        Set<String> taken = new HashSet<>();
        for (int i = 0; i < m_model.getRowCount(); i++) {
            taken.add((String)m_model.getValueAt(i, Column.JAVA_FIELD));
        }
        String fieldName = FieldsTableUtil.createUniqueJavaIdentifier(varName, taken, "v_");
        m_model.setValueAt(fieldName, r, Column.JAVA_FIELD);
        Class javaType = TypeProvider.getDefault().getTypeConverter(var.getType()).getPreferredJavaType();
        m_model.setValueAt(javaType, r, Column.JAVA_TYPE);
        return true;
    }

    /**
     * Set the table data.
     *
     * @param fields the fields
     * @param spec the input spec might be null
     * @param flowVars the flow variables
     */
    void updateData(final JavaSnippetFields fields, final DataTableSpec spec,
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
            m_model.setValueAt(value, r, Column.COLUMN);
            m_model.setValueAt(field.getJavaName(), r, Column.JAVA_FIELD);

            Optional<?> factory = ConverterUtil.getDataCellToJavaConverterFactory(field.getConverterFactoryId());
            if (!factory.isPresent()) {
                // try to find another converter for the source and dest types. The one with for the stored id
                // seems to be missing.
                if (field.getJavaType() != null) {
                    factory = ConverterUtil.getConverterFactory(field.getDataType(), field.getJavaType());
                }
            }

            if (factory.isPresent()) {
                m_model.setValueAt(factory.get(), r, Column.JAVA_TYPE);
            } else {
                final Class<?> type = field.getJavaType();
                if (type != null) {
                    m_model.setValueAt(type, r, Column.JAVA_TYPE);
                } else {
                    m_model.setValueAt(field.getJavaTypeName(), r, Column.JAVA_TYPE);
                }
            }
        }
        int offset = m_model.getRowCount();
        for (int r = 0; r < fields.getInVarFields().size(); r++) {
            InVar field = fields.getInVarFields().get(r);
            m_model.addRow();
            String name = field.getKnimeName();
            FlowVariable flowVar = m_flowVars.get(name);
            Object value = null != flowVar ? flowVar : name;
            m_model.setValueAt(value, offset + r, Column.COLUMN);
            m_model.setValueAt(field.getJavaName(), offset + r, Column.JAVA_FIELD);
            m_model.setValueAt(field.getJavaType(), offset + r, Column.JAVA_TYPE);
        }

        JTable table = getTable();
        final TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(m_model.getIndex(Column.COLUMN)).setCellRenderer(new InputTableCellRenderer());
        columnModel.getColumn(m_model.getIndex(Column.COLUMN)).setCellEditor(createInputCellEditor());

        columnModel.getColumn(m_model.getIndex(Column.JAVA_FIELD))
            .setCellRenderer(FieldsTableUtil.createJavaFieldTableCellRenderer());

        columnModel.getColumn(m_model.getIndex(Column.JAVA_TYPE))
            .setCellRenderer(FieldsTableUtil.createJavaTypeTableCellRenderer());
        columnModel.getColumn(m_model.getIndex(Column.JAVA_TYPE))
            .setCellEditor(FieldsTableUtil.createJavaTypeTableCellEditor());
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
        DefaultCellEditor editor = new DefaultCellEditor(comboBox);
        editor.setClickCountToStart(2);
        return editor;
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
            Object value = m_model.getValueAt(r, Column.COLUMN);
            if (value instanceof DataColumnSpec) {
                DataColumnSpec colSpec = (DataColumnSpec)value;
                InCol inCol = new InCol();
                inCol.setKnimeName(colSpec.getName());
                inCol.setJavaName((String)m_model.getValueAt(r, Column.JAVA_FIELD));
                Object javaTypeObject = m_model.getValueAt(r, Column.JAVA_TYPE);
                if (javaTypeObject instanceof DataCellToJavaConverterFactory) {
                    inCol.setConverterFactory(colSpec.getType(), (DataCellToJavaConverterFactory)javaTypeObject);
                } else {
                    throw new IllegalStateException("Contents of JavaType column need to be DataCellToJavaConverterFactory instances.");
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
            Object value = m_model.getValueAt(r, Column.COLUMN);
            if (value instanceof FlowVariable) {
                FlowVariable colSpec = (FlowVariable)value;
                InVar inVar = new InVar();
                inVar.setFlowVarType(colSpec.getType());
                inVar.setKnimeName(colSpec.getName());
                inVar.setJavaName((String)m_model.getValueAt(r, Column.JAVA_FIELD));
                inVar.setJavaType((Class)m_model.getValueAt(r, Column.JAVA_TYPE));
                inCols.add(inVar);
            }
        }
        return inCols;
    }

    /** Renders the table cells defining the input column or flow variables. */
    private static class InputListCellRenderer extends FlowVariableListCellRenderer {

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
            final boolean isSelected, final boolean cellHasFocus) {
            // reset values which maybe changed by previous calls of this method
            setForeground(list.getForeground());
            setBackground(list.getBackground());
            // let super class do the first step
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
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
            return new Color((b.getRed() + 255) / 2, b.getGreen() / 2, b.getBlue() / 2);
        }
    }

    /** Renders the table cells defining the input column or flow variables. */
    private static class InputTableCellRenderer extends FlowVariableTableCellRenderer {

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
            // reset values which maybe changed by previous calls of this method
            setForeground(table.getForeground());
            setBackground(table.getBackground());
            setFont(getFont().deriveFont(Font.PLAIN));
            // let super class do the first step
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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
            return new Color((b.getRed() + 255) / 2, b.getGreen() / 2, b.getBlue() / 2);
        }
    }
}
