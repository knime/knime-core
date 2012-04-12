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

import static org.knime.base.node.jsnippet.ui.OutFieldsTableModel.COL_COLUMN;
import static org.knime.base.node.jsnippet.ui.OutFieldsTableModel.COL_DATA_TYPE;
import static org.knime.base.node.jsnippet.ui.OutFieldsTableModel.COL_FIELD_TYPE;
import static org.knime.base.node.jsnippet.ui.OutFieldsTableModel.COL_JAVA_FIELD;
import static org.knime.base.node.jsnippet.ui.OutFieldsTableModel.COL_JAVA_TYPE;
import static org.knime.base.node.jsnippet.ui.OutFieldsTableModel.COL_REPLACE_EXISTING;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.knime.base.node.jsnippet.JavaFieldSettings.OutCol;
import org.knime.base.node.jsnippet.JavaFieldSettings.OutVar;
import org.knime.base.node.jsnippet.JavaFieldSettingsList.OutColList;
import org.knime.base.node.jsnippet.JavaFieldSettingsList.OutVarList;
import org.knime.base.node.jsnippet.JavaSnippetFields;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.util.ConfigTablePanel;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.FlowVariableTableCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * The table where java snippet fields for output columns and flow variables
 * can be defined.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings({"rawtypes", "serial" })
public class OutFieldsTable extends ConfigTablePanel {
    /**
     * The KNIME type of an output field.
     * @author Heiko Hofer
     */
    enum FieldType {
        /** The field defines a column in a DataTable. */
        Column,
        /** The field defines a Flow Variable. */
        FlowVariable
    }

    private OutFieldsTableModel m_model;
    private DataTableSpec m_spec;
    private Map<String, FlowVariable> m_flowVars;
    private boolean m_flowVarsOnly;

    /**
     * Create a new component. The flowVarsOnly allow to switch between a
     * table where columns and flow variables can be defined and where flow
     * variables can be defined, only.
     *
     * @param flowVarsOnly true when only flow variables and no columns can
     * be defined.
     */
    public OutFieldsTable(final boolean flowVarsOnly) {
        super(new OutFieldsTableModel());
        final JTable table = getTable();
        m_flowVarsOnly = flowVarsOnly;
        m_model = (OutFieldsTableModel)getModel();
        m_model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                checkColColumn(e);
                // The table is highly interactive. Changing the input column
                // can invalidate the java type and a editing the java
                // identifier can invalidate other java identifiers (duplicate
                // names). Therefore, force a repaint of the whole, most likely
                // small table.
                table.tableChanged(new TableModelEvent(m_model,
                        0, m_model.getRowCount()));
            }

            private void checkColColumn(final TableModelEvent e) {
                if (e.getType() == TableModelEvent.DELETE) {
                    return;
                }
                for (int r = e.getFirstRow(); r <= e.getLastRow(); r++) {
                    if (r < 0) {
                        continue;
                    }
                    Object value = m_model.getValueAt(r, COL_COLUMN);
                    String str;
                    if (null == value) {
                        return;
                    } else if (value instanceof DataColumnSpec) {
                        if (getFieldType(r).equals(FieldType.Column)) {
                            return;
                        }
                        str = ((DataColumnSpec)value).getName();
                    } else if (value instanceof FlowVariable) {
                        if (getFieldType(r).equals(FieldType.FlowVariable)) {
                            return;
                        }
                        str = ((FlowVariable)value).getName();
                    } else {
                        str = value.toString();
                    }
                    if (getFieldType(r).equals(FieldType.Column)
                            && m_spec.containsName(str)) {
                        m_model.setValueAt(m_spec.getColumnSpec(str),
                                r, COL_COLUMN);
                    }
                    if (getFieldType(r).equals(FieldType.FlowVariable)
                            && m_flowVars.containsKey(str)) {
                        m_model.setValueAt(m_flowVars.get(str),
                                r, COL_COLUMN);
                    }
                }
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
                    defaultVarTarget = null;
                    for (FlowVariable flowVar : m_flowVars.values()) {
                        // test if a flow variable of this name might be
                        // created.
                        if (FieldsTableUtil.verifyNameOfFlowVariable(
                                flowVar.getName())) {
                            if (null == defaultVarTarget) {
                                defaultVarTarget = flowVar;
                            }
                            if (!flowVars.contains(flowVar.getName())) {
                                // Add a row and fill it
                                addRow(flowVar);
                                return;
                            }
                        }

                    }
                }
                if (null != defaultColTarget) {
                    addRow(defaultColTarget);
                } else if (null != defaultVarTarget) {
                    addRow(defaultVarTarget);
                } else {
                    addRow("var", Type.STRING);
                }


            }
        };
    }

    /**
     * True when the given row represents a output that replaces an existing
     * input.
     *
     * @param row the row to check
     * @return true when the replace flag is set on the given row
     */
    public boolean getReplaceExisting(final int row) {
        return (Boolean)m_model.getValueAt(row, COL_REPLACE_EXISTING);
    }

    /**
     * Returns the field type for the given column.
     *
     * @param row the row to check
     * @return the field type of the given column
     */
    public FieldType getFieldType(final int row) {
        return (FieldType)m_model.getValueAt(row, COL_FIELD_TYPE);
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

        m_model.setValueAt(true, row, COL_REPLACE_EXISTING);
        m_model.setValueAt(FieldType.Column, row, COL_FIELD_TYPE);
        m_model.setValueAt(colSpec, row, COL_COLUMN);
        String colName = colSpec.getName();
        Set<String> taken = new HashSet<String>();
        for (int i = 0; i < m_model.getRowCount(); i++) {
            taken.add((String)m_model.getValueAt(i, COL_JAVA_FIELD));
        }

        DataType dataType = colSpec.getType();
        DataType elemType = dataType.isCollectionType()
            ? dataType.getCollectionElementType() : dataType;
        boolean isCollection = dataType.isCollectionType();
        TypeProvider typeProvider = TypeProvider.getDefault();
        if (!typeProvider.getOutputDataTypes().contains(elemType)) {
            elemType = new StringCell("").getType();
            isCollection = false;
        }

        String fieldName = FieldsTableUtil.createUniqueJavaIdentifier(
                    colName, taken, "out_");
        m_model.setValueAt(elemType, row, COL_DATA_TYPE);

        Class javaType = typeProvider.getJavaToDataCell(elemType, isCollection)
                .getPreferredJavaType();

        m_model.setValueAt(javaType, row, COL_JAVA_TYPE);
        m_model.setValueAt(fieldName, row, COL_JAVA_FIELD);

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

        m_model.setValueAt(true, row, COL_REPLACE_EXISTING);
        m_model.setValueAt(FieldType.FlowVariable, row, COL_FIELD_TYPE);
        m_model.setValueAt(var, row, COL_COLUMN);
        String varName = var.getName();
        Set<String> taken = new HashSet<String>();
        for (int i = 0; i < m_model.getRowCount(); i++) {
            taken.add((String)m_model.getValueAt(i, COL_JAVA_FIELD));
        }
        String fieldName = FieldsTableUtil.createUniqueJavaIdentifier(
                varName, taken, "out_");
        m_model.setValueAt(var.getType(), row, COL_DATA_TYPE);
        Class javaType = TypeProvider.getDefault()
            .getTypeConverter(var.getType()).getPreferredJavaType();
        m_model.setValueAt(javaType, row, COL_JAVA_TYPE);
        m_model.setValueAt(fieldName, row, COL_JAVA_FIELD);

        return true;
    }

    /**
     * Adds a row for a new flow variable with given values.
     * @param name the knime name
     * @param type the Type
     * @return true when the row was added successfully
     */
    public boolean addRow(final String name, final Type type) {
        int row = m_model.getRowCount();
        m_model.addRow();

        m_model.setValueAt(false, row, COL_REPLACE_EXISTING);
        m_model.setValueAt(FieldType.FlowVariable, row, COL_FIELD_TYPE);
        m_model.setValueAt(name, row, COL_COLUMN);
        Set<String> taken = new HashSet<String>();
        for (int i = 0; i < m_model.getRowCount(); i++) {
            taken.add((String)m_model.getValueAt(i, COL_JAVA_FIELD));
        }
        String fieldName = FieldsTableUtil.createUniqueJavaIdentifier(
                name, taken, "out_");
        m_model.setValueAt(type, row, COL_DATA_TYPE);
        Class javaType = TypeProvider.getDefault()
            .getTypeConverter(type).getPreferredJavaType();
        m_model.setValueAt(javaType, row, COL_JAVA_TYPE);
        m_model.setValueAt(fieldName, row, COL_JAVA_FIELD);

        return true;
    }

    /**
     * Set the table data.
     *
     * @param fields the fields
     * @param spec the input spec
     * @param flowVars the flow variables
     */
    void updateData(final JavaSnippetFields fields,
            final DataTableSpec spec,
            final Map<String, FlowVariable> flowVars) {
        m_spec = spec;
        m_flowVars = flowVars;

        m_model.clear();
        for (int r = 0; r < fields.getOutColFields().size(); r++) {
            OutCol field = fields.getOutColFields().get(r);
            m_model.addRow();
            m_model.setValueAt(field.getReplaceExisting(),
                    r, COL_REPLACE_EXISTING);
            m_model.setValueAt(FieldType.Column, r, COL_FIELD_TYPE);
            String colName = field.getKnimeName();
            DataColumnSpec colSpec = spec.getColumnSpec(colName);
            Object value = null != colSpec ? colSpec : colName;
            m_model.setValueAt(value, r, COL_COLUMN);
            m_model.setValueAt(field.getKnimeType(), r, COL_DATA_TYPE);
            m_model.setValueAt(field.getJavaName(), r, COL_JAVA_FIELD);
            m_model.setValueAt(field.getJavaType(), r, COL_JAVA_TYPE);
        }
        int offset = m_model.getRowCount();
        for (int r = 0; r < fields.getOutVarFields().size(); r++) {
            OutVar field = fields.getOutVarFields().get(r);
            m_model.addRow();
            m_model.setValueAt(field.getReplaceExisting(),
                    offset + r, COL_REPLACE_EXISTING);
            m_model.setValueAt(FieldType.FlowVariable, offset + r,
                    COL_FIELD_TYPE);
            String name = field.getKnimeName();
            FlowVariable flowVar = m_flowVars.get(name);
            Object value = null != flowVar ? flowVar : name;
            m_model.setValueAt(value, offset + r, COL_COLUMN);
            m_model.setValueAt(field.getKnimeType(), offset + r, COL_DATA_TYPE);
            m_model.setValueAt(field.getJavaName(), offset + r, COL_JAVA_FIELD);
            m_model.setValueAt(field.getJavaType(), offset + r, COL_JAVA_TYPE);
        }

        JTable table = getTable();
        table.getColumnModel().getColumn(COL_REPLACE_EXISTING).setCellRenderer(
                new BooleanRenderer());
        table.getColumnModel().getColumn(COL_REPLACE_EXISTING).setCellEditor(
                new DefaultCellEditor(new JCheckBox()));
        if (!m_flowVarsOnly) {
            table.getColumnModel().getColumn(COL_FIELD_TYPE).setCellEditor(
                    createFieldTypeCellEditor());
        } else {
            table.getColumnModel().getColumn(COL_FIELD_TYPE).setCellEditor(
                new DefaultCellEditor(new JTextField()) {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public boolean isCellEditable(final EventObject anEvent) {
                        return false;
                    }
                });
        }
        table.getColumnModel().getColumn(COL_COLUMN).setCellRenderer(
                new InputTableCellRenderer());
        table.getColumnModel().getColumn(COL_COLUMN).setCellEditor(
                new InputTableCellEditor(this, m_spec, m_flowVars));
        table.getColumnModel().getColumn(COL_DATA_TYPE).setCellRenderer(
                new DataTypeTableCellRenderer());
        table.getColumnModel().getColumn(COL_DATA_TYPE).setCellEditor(
                new DataTypeTableCellEditor(this, m_spec, m_flowVars));
        table.getColumnModel().getColumn(COL_JAVA_TYPE).setCellRenderer(
                FieldsTableUtil.createJavaTypeTableCellRenderer());
        table.getColumnModel().getColumn(COL_JAVA_TYPE).setCellEditor(
                FieldsTableUtil.createJavaTypeTableCellEditor());
        table.getColumnModel().getColumn(COL_JAVA_FIELD).setCellRenderer(
                FieldsTableUtil.createJavaFieldTableCellRenderer());

    }

    /** Create cell editor for for the input columns / flow variables. */
    private TableCellEditor createFieldTypeCellEditor() {
        JComboBox comboBox = new JComboBox();
        comboBox.addItem(FieldType.Column);
        comboBox.addItem(FieldType.FlowVariable);
        return new DefaultCellEditor(comboBox);
    }


    /**
     * Get the field definitions representing output columns.
     *
     * @return fields representing output columns.
     */
    public OutColList getOutColFields() {
        OutColList outCols = new OutColList();
        for (int r = 0; r < m_model.getRowCount(); r++) {
            if (!m_model.validateValues(r)) {
                // there are errors in this row
                continue;
            }
            Object fieldTypeValue = m_model.getValueAt(r, COL_FIELD_TYPE);
            if (null == fieldTypeValue) {
                continue;
            }
            boolean isColumn = fieldTypeValue.equals(FieldType.Column);
            if (isColumn) {
                OutCol outCol = new OutCol();
                outCol.setReplaceExisting(
                        (Boolean)m_model.getValueAt(r, COL_REPLACE_EXISTING));
                Object colColValue = m_model.getValueAt(r, COL_COLUMN);
                if (colColValue instanceof DataColumnSpec) {
                    DataColumnSpec colSpec = (DataColumnSpec)colColValue;
                    outCol.setKnimeName(colSpec.getName());
                } else if (colColValue instanceof String) {
                    outCol.setKnimeName(colColValue.toString());
                } else {
                    continue;
                }
                Object dataTypeValue = m_model.getValueAt(r, COL_DATA_TYPE);
                if (dataTypeValue instanceof DataType) {
                    DataType type = (DataType)dataTypeValue;
                    outCol.setKnimeType(type);
                } else {
                    continue;
                }
                outCol.setJavaName(
                        (String)m_model.getValueAt(r, COL_JAVA_FIELD));
                Object javaTypeObject = m_model.getValueAt(r, COL_JAVA_TYPE);
                if (javaTypeObject instanceof Class) {
                    outCol.setJavaType((Class)javaTypeObject);
                } else {
                    continue;
                }
                outCols.add(outCol);
            }
        }
        return outCols;
    }

    /**
     * Get the field definitions representing output flow variables.
     *
     * @return fields representing output flow variables
     */
    public OutVarList getOutVarFields() {
        OutVarList outVars = new OutVarList();
        for (int r = 0; r < m_model.getRowCount(); r++) {
            if (!m_model.validateValues(r)) {
                // there are errors in this row
                continue;
            }
            Object fieldTypeValue = m_model.getValueAt(r, COL_FIELD_TYPE);
            if (null == fieldTypeValue) {
                continue;
            }
            boolean isFlowVar = fieldTypeValue.equals(FieldType.FlowVariable);
            if (isFlowVar) {
                OutVar outVar = new OutVar();
                outVar.setReplaceExisting(
                        (Boolean)m_model.getValueAt(r, COL_REPLACE_EXISTING));
                Object colColValue = m_model.getValueAt(r, COL_COLUMN);
                if (colColValue instanceof FlowVariable) {
                    FlowVariable flowVar = (FlowVariable)colColValue;
                    outVar.setKnimeName(flowVar.getName());
                } else if (colColValue instanceof String) {
                    outVar.setKnimeName(colColValue.toString());
                } else {
                    continue;
                }
                Object dataTypeValue = m_model.getValueAt(r, COL_DATA_TYPE);
                if (dataTypeValue instanceof Type) {
                    Type type = (Type)dataTypeValue;
                    outVar.setKnimeType(type);
                } else {
                    continue;
                }
                outVar.setJavaName(
                        (String)m_model.getValueAt(r, COL_JAVA_FIELD));
                Object javaTypeObject = m_model.getValueAt(r, COL_JAVA_TYPE);
                if (javaTypeObject instanceof Class) {
                    outVar.setJavaType((Class)javaTypeObject);
                } else {
                    continue;
                }
                outVars.add(outVar);
            }
        }
        return outVars;
    }


    /**
     * Renders a boolean as a checkbox.
     *
     * @author Heiko Hofer
     */
    static class BooleanRenderer extends JCheckBox
        implements TableCellRenderer, UIResource {
        private static final Border NO_FOCUS_BORDER =
            new EmptyBorder(1, 1, 1, 1);

        /**
         * Create a new instance.
         */
        public BooleanRenderer() {
            super();
            setHorizontalAlignment(JLabel.CENTER);
            setBorderPainted(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(
                final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus,
                final int row, final int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            setSelected((value != null && ((Boolean)value).booleanValue()));

            if (hasFocus) {
                setBorder(UIManager.getBorder(
                        "Table.focusCellHighlightBorder"));
            } else {
                setBorder(NO_FOCUS_BORDER);
            }

            return this;
        }
    }


    /** Renders the table cells defining the input column or flow variables. */
    private static class InputListCellRenderer
            extends FlowVariableListCellRenderer {
        private Map<Type, FlowVariable> m_flowVars;

        public InputListCellRenderer() {
            m_flowVars = new HashMap<FlowVariable.Type, FlowVariable>();
            m_flowVars.put(Type.DOUBLE, new FlowVariable("double", 1.0));
            m_flowVars.put(Type.INTEGER, new FlowVariable("int", 1));
            m_flowVars.put(Type.STRING, new FlowVariable("string", "1.0"));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
            Object v = value instanceof Type ? m_flowVars.get(value) : value;
            // reset values which maybe changed by previous calls of this method
            setForeground(list.getForeground());
            setBackground(list.getBackground());
            // let super class do the first step
            super.getListCellRendererComponent(list, v, index, isSelected,
                    cellHasFocus);
            if (v instanceof DataColumnSpec) {
                DataColumnSpec col = (DataColumnSpec)v;
                setIcon(col.getType().getIcon());
                setText(col.getName());
                setToolTipText(null);
            } else if (v instanceof DataType) {
                DataType type = (DataType)v;
                setIcon(type.getIcon());
                setText(type.toString());
                setToolTipText(null);
            } else if (null == v || v instanceof String) {
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

    /** Renders the table cells defining the output column or flow variables. */
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
                if (value instanceof DataColumnSpec) {
                    DataColumnSpec col = (DataColumnSpec)value;
                    setIcon(col.getType().getIcon());
                    setText(col.getName());
                }
            } else {
                if (value instanceof DataColumnSpec) {
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

    /** Renders the table cells defining the data type. */
    private static class DataTypeTableCellRenderer
            extends FlowVariableTableCellRenderer {
        private Map<Type, FlowVariable> m_flowVars;

        public DataTypeTableCellRenderer() {
            m_flowVars = new HashMap<FlowVariable.Type, FlowVariable>();
            m_flowVars.put(Type.DOUBLE, new FlowVariable("double", 1.0));
            m_flowVars.put(Type.INTEGER, new FlowVariable("int", 1));
            m_flowVars.put(Type.STRING, new FlowVariable("string", "1.0"));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(final JTable table,
                final Object value, final boolean isSelected,
                final boolean hasFocus, final int row,
                final int column) {
            Object v = value instanceof Type ? m_flowVars.get(value) : value;
            // reset values which maybe changed by previous calls of this method
            setForeground(table.getForeground());
            setBackground(table.getBackground());
            setFont(getFont().deriveFont(Font.PLAIN));
            // let super class do the first step
            super.getTableCellRendererComponent(table, v,
                    isSelected, hasFocus,
                    row, column);
            FieldsTableModel model = (FieldsTableModel)table.getModel();
            if (model.isValidValue(row, column)) {
                if (value instanceof DataType) {
                    DataType type = (DataType)value;
                    setIcon(type.getIcon());
                    setText(type.toString());
                    setToolTipText(null);
                }
            } else {
                if (value instanceof DataType) {
                    DataType type = (DataType)value;
                    setIcon(type.getIcon());
                    setText(type.toString());
                    setToolTipText(null);
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

    /**
     * A cell editor to define the name of the output column or flow variable.
     * Note, that the output may replace an input.
     *
     * @author Heiko Hofer
     */
    static class InputTableCellEditor extends DefaultCellEditor {
        private JComboBox m_editor;
        private EditorDelegate m_delegate;

        private JTextField m_stringEditor;
        private EditorDelegate m_stringDelegate;
        private OutFieldsTable m_fieldsTable;
        private JComboBox m_colComboBox;
        private JComboBox m_flowVarComboBox;
        private DataTableSpec m_spec;
        private Map<String, FlowVariable> m_flowVars;


        /**
         * Create a new instance.
         *
         * @param fieldsTable The table this editor is used for
         * @param spec the input spec
         * @param flowVars the flow variables on the input
         */
        public InputTableCellEditor(final OutFieldsTable fieldsTable,
                final DataTableSpec spec,
                final Map<String, FlowVariable> flowVars) {
            super(new JComboBox());
            m_spec = spec;
            m_flowVars = flowVars;
            m_fieldsTable = fieldsTable;
            m_editor = (JComboBox)editorComponent;
            m_editor.setRenderer(new InputListCellRenderer());
            m_delegate = delegate;
            m_colComboBox = createInputColumnComboBox();
            m_flowVarComboBox = createFlowVariablesComboBox();


            m_stringEditor = new JTextField();
            m_stringDelegate = new EditorDelegate() {
                @Override
                public void setValue(final Object val) {
                    if (null == val) {
                        m_stringEditor.setText("");
                    } else if (val instanceof DataColumnSpec) {
                        m_stringEditor.setText(
                                ((DataColumnSpec)val).getName());
                    } else if (val instanceof FlowVariable) {
                        m_stringEditor.setText(((FlowVariable)val).getName());
                    } else {
                        m_stringEditor.setText(val.toString());
                    }
                }

                @Override
                public Object getCellEditorValue() {
                    return m_stringEditor.getText();
                }
            };
            m_stringEditor.addActionListener(delegate);
        }

        /** Create an editor component for the input columns. */
        private JComboBox createInputColumnComboBox() {
            JComboBox comboBox = new JComboBox();
            if (null != m_spec) {
                for (DataColumnSpec colSpec : m_spec) {
                    comboBox.addItem(colSpec);
                }
            }
            return comboBox;
        }

        /** Create an editor component for the flow variables. */
        private JComboBox createFlowVariablesComboBox() {
            JComboBox comboBox = new JComboBox();
            if (null != m_flowVars) {
                for (FlowVariable flowVar : m_flowVars.values()) {
                    // test if a flow variable of this name might be
                    // created.
                    if (FieldsTableUtil.verifyNameOfFlowVariable(
                            flowVar.getName())) {
                        comboBox.addItem(flowVar);
                    }
                }
            }
            return comboBox;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellEditorComponent(final JTable table,
                final Object value, final boolean isSelected, final int row,
                final int column) {
            if (m_fieldsTable.getReplaceExisting(row)) {
                editorComponent = m_editor;
                delegate = m_delegate;
                if (m_fieldsTable.getFieldType(row).equals(FieldType.Column)) {
                    m_editor.setModel(m_colComboBox.getModel());
                } else {
                    m_editor.setModel(m_flowVarComboBox.getModel());
                }
            } else {
                editorComponent = m_stringEditor;
                delegate = m_stringDelegate;
            }
            return super.getTableCellEditorComponent(table, value,
                    isSelected, row, column);
        }
    }

    /**
     * A cell editor to define the type of the output column or flow variable.
     *
     * @author Heiko Hofer
     */
    static class DataTypeTableCellEditor extends DefaultCellEditor {
        private JComboBox m_editor;

        private OutFieldsTable m_fieldsTable;
        private JComboBox m_colComboBox;
        private JComboBox m_flowVarComboBox;
        private DataTableSpec m_spec;
        private Map<String, FlowVariable> m_flowVars;


        /**
         * Create a new instance.
         *
         * @param fieldsTable The table this editor is used for
         * @param spec the input spec
         * @param flowVars the flow variables on the input
         */
        public DataTypeTableCellEditor(final OutFieldsTable fieldsTable,
                final DataTableSpec spec,
                final Map<String, FlowVariable> flowVars) {
            super(new JComboBox());
            m_spec = spec;
            m_flowVars = flowVars;
            m_fieldsTable = fieldsTable;
            m_editor = (JComboBox)editorComponent;
            m_editor.setRenderer(new InputListCellRenderer());
            m_colComboBox = createDataTypeComboBox();
            m_flowVarComboBox = createTypeComboBox();
        }

        /** Create an editor component for the input columns. */
        private JComboBox createDataTypeComboBox() {
            JComboBox comboBox = new JComboBox();
            if (null != m_spec) {
                TypeProvider typeProvider = TypeProvider.getDefault();
                for (DataType type : typeProvider.getOutputDataTypes()) {
                    comboBox.addItem(type);
                    comboBox.addItem(ListCell.getCollectionType(type));
                }
            }
            return comboBox;
        }

        /** Create an editor component for the flow variables. */
        private JComboBox createTypeComboBox() {
            JComboBox comboBox = new JComboBox();
            if (null != m_flowVars) {
                TypeProvider typeProvider = TypeProvider.getDefault();
                for (Type type : typeProvider.getTypes()) {
                    comboBox.addItem(type);
                }
            }
            return comboBox;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellEditorComponent(final JTable table,
                final Object value, final boolean isSelected, final int row,
                final int column) {
            if (m_fieldsTable.getFieldType(row).equals(FieldType.Column)) {
                m_editor.setModel(m_colComboBox.getModel());
            } else {
                m_editor.setModel(m_flowVarComboBox.getModel());
            }

            return super.getTableCellEditorComponent(table, value,
                    isSelected, row, column);
        }
    }

}
