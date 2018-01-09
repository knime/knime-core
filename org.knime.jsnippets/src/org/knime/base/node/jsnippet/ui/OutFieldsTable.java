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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.knime.base.node.jsnippet.JavaSnippet;
import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.ui.FieldsTableModel.Column;
import org.knime.base.node.jsnippet.util.JavaFieldList.OutColList;
import org.knime.base.node.jsnippet.util.JavaFieldList.OutVarList;
import org.knime.base.node.jsnippet.util.JavaSnippetFields;
import org.knime.base.node.jsnippet.util.field.JavaField;
import org.knime.base.node.jsnippet.util.field.JavaField.FieldType;
import org.knime.base.node.jsnippet.util.field.OutCol;
import org.knime.base.node.jsnippet.util.field.OutVar;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.node.util.ConfigTablePanel;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.FlowVariableTableCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * The table where java snippet fields for output columns and flow variables
 * can be defined.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings({"rawtypes", "serial" })
public class OutFieldsTable extends ConfigTablePanel {
    /**
     * Property fired when a row is manually added by the user.
     */
    public static final String PROP_FIELD_ADDED = "prop_field_added";

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
        super(new OutFieldsTableModel(flowVarsOnly));
        final JTable table = getTable();
        m_flowVarsOnly = flowVarsOnly;
        m_model = (OutFieldsTableModel)getModel();
        m_model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                checkColColumn(e);
                autoUpdateIsArray(e);
                autoUpdateJavaType(e);

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
                    Object value = m_model.getValueAt(r, Column.COLUMN);
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
                                r, Column.COLUMN);
                    }
                    if (getFieldType(r).equals(FieldType.FlowVariable)
                            && m_flowVars.containsKey(str)) {
                        m_model.setValueAt(m_flowVars.get(str),
                                r, Column.COLUMN);
                    }
                }
            }

            private void autoUpdateIsArray(final TableModelEvent e) {
                if (e.getType() == TableModelEvent.DELETE) {
                    return;
                }
                // if Column.FIELD_TYPE is not in the table
                if (-1 == m_model.getIndex(Column.FIELD_TYPE)) {
                    return;
                }
                if (e.getColumn() != m_model.getIndex(Column.FIELD_TYPE)) {
                    return;
                }
                for (int r = e.getFirstRow(); r <= e.getLastRow(); r++) {
                    boolean isVariable = m_model.getValueAt(r,
                            Column.FIELD_TYPE).equals(FieldType.FlowVariable);
                    if (isVariable) {
                        Object isArray = m_model.getValueAt(r,
                                Column.IS_COLLECTION);
                        if (null != isArray && (Boolean)isArray) {
                            m_model.setValueAt(Boolean.FALSE, r,
                                    Column.IS_COLLECTION);
                        }
                        // update the java type
                        Object javaTypeObject = m_model.getValueAt(r,
                                Column.JAVA_TYPE);
                        if (javaTypeObject == null
                                || !(javaTypeObject instanceof Class)) {
                            return;
                        }
                        Class javaType = (Class)javaTypeObject;
                        if (javaType.isArray()) {
                            m_model.setValueAt(javaType.getComponentType(), r,
                                Column.JAVA_TYPE);
                        }
                    }
                }
            }

            private void autoUpdateJavaType(final TableModelEvent e) {
                if (e.getType() == TableModelEvent.DELETE) {
                    return;
                }
                // if Column.IS_COLLECTION is not in the table
                if (-1 == m_model.getIndex(Column.IS_COLLECTION)) {
                    return;
                }
                if (e.getColumn() != m_model.getIndex(Column.IS_COLLECTION)) {
                    return;
                }
                for (int r = e.getFirstRow(); r <= e.getLastRow(); r++) {
                    boolean isCollection = (Boolean)m_model.getValueAt(r, Column.IS_COLLECTION);
                    // update the java type
                    Object javaTypeObject = m_model.getValueAt(r, Column.JAVA_TYPE);
                    if (!(javaTypeObject instanceof Class)) {
                        return;
                    }
                    Class javaType = (Class)javaTypeObject;
                    if (javaType.isArray() && !isCollection) {
                        m_model.setValueAt(javaType.getComponentType(), r, Column.JAVA_TYPE);
                    }
                    if (!javaType.isArray() && isCollection) {
                        Class arrayType = Array.newInstance(javaType, 0).getClass();
                        m_model.setValueAt(arrayType, r, Column.JAVA_TYPE);
                    }
                }
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
                                firePropertyChange(PROP_FIELD_ADDED,
                                        m_model.getRowCount() - 1 ,
                                        m_model.getRowCount());
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
                                boolean rowAdded = addRow(flowVar);
                                if (rowAdded) {
                                    firePropertyChange(PROP_FIELD_ADDED,
                                            m_model.getRowCount() - 1 ,
                                            m_model.getRowCount());
                                }
                                return;
                            }
                        }

                    }
                }
                boolean rowAdded = false;
                if (null != defaultColTarget) {
                    rowAdded = addRow(defaultColTarget);
                } else if (null != defaultVarTarget) {
                    rowAdded = addRow(defaultVarTarget);
                } else {
                    rowAdded = addRow("var", Type.STRING);
                }
                if (rowAdded) {
                    firePropertyChange(PROP_FIELD_ADDED,
                        m_model.getRowCount() - 1 ,
                        m_model.getRowCount());
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
        return (Boolean)m_model.getValueAt(row, Column.REPLACE_EXISTING);
    }

    /**
     * Returns the field type for the given column.
     *
     * @param row the row to check
     * @return the field type of the given column
     */
    public FieldType getFieldType(final int row) {
        if (m_flowVarsOnly) {
            return FieldType.FlowVariable;
        } else {
            return (FieldType)m_model.getValueAt(row, Column.FIELD_TYPE);
        }
    }

    /**
     * Adds a row using the give colSpec as a hint for the new row.
     *
     * @param colSpec the input column
     * @return true when the row was added successfully
     */
    public boolean addRow(final DataColumnSpec colSpec) {
        Frame parent = (Frame)SwingUtilities.getAncestorOfClass(
                Frame.class, this);
        JavaField newRow = AddOutFieldDialog.openUserDialog(parent,
                m_model, m_spec, m_flowVars, m_flowVarsOnly);
        if (null != newRow) {
            return addRow(newRow);
        } else {
            return false;
        }
    }

    /**
     * Adds a row to the output table.
     * @param newRow the new row
     * @return true when the row was added succesfully
     */
    private boolean addRow(final JavaField newRow) {
        if (newRow instanceof OutCol) {
            return addRow((OutCol)newRow);
        } else if (newRow instanceof OutVar) {
            return addRow((OutVar)newRow);
        } else {
            return false;
        }
    }

    /**
     * Adds a row using the given flow variable as a hint.
     *
     * @param var the flow variable definition
     * @return true when the row was added successfully
     */
    public boolean addRow(final FlowVariable var) {
        Frame parent = (Frame)SwingUtilities.getAncestorOfClass(
                Frame.class, this);
        JavaField newRow = AddOutFieldDialog.openUserDialog(parent,
                m_model, m_spec, m_flowVars, m_flowVarsOnly);
        if (null != newRow) {
            return addRow(newRow);
        } else {
            return false;
        }
    }

    /**
     * Adds a row using the give values as a hint.
     * @param name the knime name
     * @param type the Type
     * @return true when the row was added successfully
     */
    public boolean addRow(final String name, final Type type) {
        Frame parent = (Frame)SwingUtilities.getAncestorOfClass(
                Frame.class, this);
        JavaField newRow = AddOutFieldDialog.openUserDialog(parent,
                m_model, m_spec, m_flowVars, m_flowVarsOnly);
        if (null != newRow) {
            return addRow(newRow);
        } else {
            return false;
        }
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
        fields.getOutColFields().forEach(field -> addRow(field));
        fields.getOutVarFields().forEach(field -> addRow(field));

        /*
         * There is no simple way of disabling a boolean cell renderer if the cell is not editable.
         *
         * Hence, here we wrap the hidden Default BooleanRenderer and disable if not editable.
         */
        final TableCellRenderer boolRenderer = new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                final Component c = table.getDefaultRenderer(Boolean.class).getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
                c.setEnabled(table.isCellEditable(row, column));
                return c;
            }
        };

        final TableColumnModel columnModel = getTable().getColumnModel();
        columnModel.getColumn(m_model.getIndex(Column.REPLACE_EXISTING))
            .setCellEditor(new DefaultCellEditor(new JCheckBox()));
        columnModel.getColumn(m_model.getIndex(Column.REPLACE_EXISTING)).setCellRenderer(boolRenderer);

        if (!m_flowVarsOnly) {
            // Field type column does not exist for flowVarsOnly
            columnModel.getColumn(m_model.getIndex(Column.FIELD_TYPE)).setCellEditor(createFieldTypeCellEditor());
        }

        columnModel.getColumn(m_model.getIndex(Column.COLUMN)).setCellRenderer(new InputTableCellRenderer());
        columnModel.getColumn(m_model.getIndex(Column.COLUMN))
            .setCellEditor(new InputTableCellEditor(this, m_spec, m_flowVars));

        columnModel.getColumn(m_model.getIndex(Column.DATA_TYPE)).setCellRenderer(new DataTypeTableCellRenderer());
        columnModel.getColumn(m_model.getIndex(Column.DATA_TYPE))
            .setCellEditor(new DataTypeTableCellEditor(this, m_spec, m_flowVars));

        if (!m_flowVarsOnly) {
            // Field type column does not exist for flowVarsOnly
            columnModel.getColumn(m_model.getIndex(Column.IS_COLLECTION))
                .setCellEditor(new DefaultCellEditor(new JCheckBox()));
            columnModel.getColumn(m_model.getIndex(Column.IS_COLLECTION)).setCellRenderer(boolRenderer);
        }

        columnModel.getColumn(m_model.getIndex(Column.JAVA_TYPE))
            .setCellRenderer(FieldsTableUtil.createJavaTypeTableCellRenderer());
        columnModel.getColumn(m_model.getIndex(Column.JAVA_TYPE))
            .setCellEditor(FieldsTableUtil.createJavaTypeTableCellEditor());

        columnModel.getColumn(m_model.getIndex(Column.JAVA_FIELD))
            .setCellRenderer(FieldsTableUtil.createJavaFieldTableCellRenderer());
    }

    /**
     * Adds a row using the values of the given output column.
     *
     * @param outCol the output column definition
     * @return true when the row was added successfully
     */
    public boolean addRow(final OutCol outCol) {
        int r = m_model.getRowCount();
        m_model.addRow();
        m_model.setValueAt(outCol.getReplaceExisting(),
                r, Column.REPLACE_EXISTING);
        m_model.setValueAt(FieldType.Column, r, Column.FIELD_TYPE);

        String colName = outCol.getKnimeName();
        DataColumnSpec colSpec = m_spec.getColumnSpec(colName);
        Object value = null != colSpec ? colSpec : colName;
        m_model.setValueAt(value, r, Column.COLUMN);
        DataType type = outCol.getDataType();
        boolean isCollection = type.isCollectionType();
        m_model.setValueAt(isCollection ? type.getCollectionElementType() : type, r, Column.DATA_TYPE);
        m_model.setValueAt(isCollection, r, Column.IS_COLLECTION);
        m_model.setValueAt(outCol.getJavaName(), r, Column.JAVA_FIELD);

        Optional<?> factory = ConverterUtil.getJavaToDataCellConverterFactory(outCol.getConverterFactoryId());
        if (!factory.isPresent()) {
            // try to find another converter for the source and dest types. The one with for the stored id
            // seems to be missing.
            if (outCol.getJavaType() != null) {
                factory = ConverterUtil.getConverterFactory(outCol.getJavaType(), outCol.getDataType());
            }
        }

        if (factory.isPresent()) {
            m_model.setValueAt(factory.get(), r, Column.JAVA_TYPE);
        } else {
            if (outCol.getJavaType() != null) {
                m_model.setValueAt(outCol.getJavaType(), r, Column.JAVA_TYPE);
            } else {
                m_model.setValueAt(outCol.getJavaTypeName(), r, Column.JAVA_TYPE);
            }
        }

        return true;
    }

    /**
     * Adds a row using the values of the given output variable.
     *
     * @param outVar the output variable definition
     * @return true when the row was added successfully
     */
    public boolean addRow(final OutVar outVar) {
        int r = m_model.getRowCount();
        m_model.addRow();
        m_model.setValueAt(outVar.getReplaceExisting(),
                r, Column.REPLACE_EXISTING);
        if (!m_flowVarsOnly) {
            m_model.setValueAt(FieldType.FlowVariable,  r,
                    Column.FIELD_TYPE);
        }
        String name = outVar.getKnimeName();
        FlowVariable flowVar = m_flowVars.get(name);
        Object value = null != flowVar ? flowVar : name;
        m_model.setValueAt(value, r, Column.COLUMN);
        m_model.setValueAt(outVar.getFlowVarType(), r, Column.DATA_TYPE);
        if (!m_flowVarsOnly) {
            m_model.setValueAt(false, r, Column.IS_COLLECTION);
        }

        m_model.setValueAt(outVar.getJavaName(), r, Column.JAVA_FIELD);
        m_model.setValueAt(outVar.getJavaType(), r, Column.JAVA_TYPE);
        return true;
    }

    /** Create cell editor for for the input columns / flow variables. */
    private TableCellEditor createFieldTypeCellEditor() {
        JComboBox comboBox = new JComboBox();
        comboBox.addItem(FieldType.Column);
        comboBox.addItem(FieldType.FlowVariable);
        DefaultCellEditor editor = new DefaultCellEditor(comboBox);
        editor.setClickCountToStart(2);
        return editor;
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
            Object fieldTypeValue = getFieldType(r);
            if (null == fieldTypeValue) {
                continue;
            }
            boolean isColumn = fieldTypeValue.equals(FieldType.Column);
            if (isColumn) {
                OutCol outCol = new OutCol();
                outCol.setReplaceExisting((Boolean)m_model.getValueAt(r,
                                Column.REPLACE_EXISTING));
                Object colColValue = m_model.getValueAt(r, Column.COLUMN);
                if (colColValue instanceof DataColumnSpec) {
                    DataColumnSpec colSpec = (DataColumnSpec)colColValue;
                    outCol.setKnimeName(colSpec.getName());
                } else if (colColValue instanceof String) {
                    outCol.setKnimeName(colColValue.toString());
                } else {
                    continue;
                }
                final Object dataTypeValue = m_model.getValueAt(r, Column.DATA_TYPE);
                if (!(dataTypeValue instanceof DataType)) {
                    continue;
                }
                outCol.setJavaName(
                        (String)m_model.getValueAt(r, Column.JAVA_FIELD));
                Object javaTypeObject = m_model.getValueAt(r,
                        Column.JAVA_TYPE);
                if (javaTypeObject instanceof JavaToDataCellConverterFactory) {
                    outCol.setConverterFactory((JavaToDataCellConverterFactory)javaTypeObject);
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
            Object fieldTypeValue = getFieldType(r);
            if (null == fieldTypeValue) {
                continue;
            }
            boolean isFlowVar = fieldTypeValue.equals(FieldType.FlowVariable);
            if (isFlowVar) {
                OutVar outVar = new OutVar();
                outVar.setReplaceExisting((Boolean)m_model.getValueAt(r,
                                Column.REPLACE_EXISTING));
                Object colColValue = m_model.getValueAt(r, Column.COLUMN);
                if (colColValue instanceof FlowVariable) {
                    FlowVariable flowVar = (FlowVariable)colColValue;
                    outVar.setKnimeName(flowVar.getName());
                } else if (colColValue instanceof String) {
                    outVar.setKnimeName(colColValue.toString());
                } else {
                    continue;
                }
                Object dataTypeValue = m_model.getValueAt(r, Column.DATA_TYPE);
                if (dataTypeValue instanceof Type) {
                    Type type = (Type)dataTypeValue;
                    outVar.setFlowVarType(type);
                } else {
                    continue;
                }
                outVar.setJavaName(
                        (String)m_model.getValueAt(r, Column.JAVA_FIELD));
                Object javaTypeObject = m_model.getValueAt(r,
                        Column.JAVA_TYPE);
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

    /** Renders the table cells defining the input column or flow variables. */
    private static class InputListCellRenderer
            extends FlowVariableListCellRenderer {
        private Map<Type, FlowVariable> m_flowVars;

        public InputListCellRenderer() {
            m_flowVars = new HashMap<>();
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
            if (value instanceof JavaToDataCellConverterFactory) {
                final JavaToDataCellConverterFactory<?> factory = (JavaToDataCellConverterFactory<?>)value;
                setText(factory.getName());
                setToolTipText(getText());
                setIcon(factory.getDestinationType().getIcon());
            } else if (v instanceof DataColumnSpec) {
                DataColumnSpec col = (DataColumnSpec)v;
                setIcon(col.getType().getIcon());
                setText(col.getName());
                setToolTipText(getText());
            } else if (v instanceof DataType) {
                DataType type = (DataType)v;
                setIcon(type.getIcon());
                setText(type.toString());
                setToolTipText(getText());
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
                if (value instanceof JavaToDataCellConverterFactory) {
                    final JavaToDataCellConverterFactory<?> factory = (JavaToDataCellConverterFactory<?>)value;
                    setText(factory.getName());
                    setIcon(factory.getDestinationType().getIcon());
                } else if (value instanceof DataColumnSpec) {
                    DataColumnSpec col = (DataColumnSpec)value;
                    setIcon(col.getType().getIcon());
                    setText(col.getName());
                }
            } else {
                if (value instanceof JavaToDataCellConverterFactory) {
                    final JavaToDataCellConverterFactory<?> factory = (JavaToDataCellConverterFactory<?>)value;
                    setText(factory.getName());
                    setIcon(factory.getDestinationType().getIcon());
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

    /** Renders the table cells defining the data type. */
    private static class DataTypeTableCellRenderer
            extends FlowVariableTableCellRenderer {
        private Map<Type, FlowVariable> m_flowVars;

        public DataTypeTableCellRenderer() {
            m_flowVars = new HashMap<>();
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
                if (value instanceof JavaToDataCellConverterFactory) {
                    final JavaToDataCellConverterFactory<?> factory = (JavaToDataCellConverterFactory<?>)value;
                    setText(factory.getDestinationType().toString() + " (" + factory.getName() + ")");
                    setIcon(factory.getDestinationType().getIcon());
                } else if (value instanceof DataType) {
                    DataType type = (DataType)value;
                    setIcon(type.getIcon());
                    setText(type.toString());
                }
                setToolTipText(getText());
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
            setClickCountToStart(2);
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
            setClickCountToStart(2);
        }

        /** Create an editor component for the input columns. */
        private JComboBox createDataTypeComboBox() {
            JComboBox comboBox = new JComboBox();
            if (null != m_spec) {
                for (DataType type : ConverterUtil.getAllDestinationDataTypes()) {
                    if (ConverterUtil.getFactoriesForDestinationType(type).stream()
                        .filter(factory -> JavaSnippet.getBuildPathFromCache(factory.getIdentifier()) != null).findAny()
                        .isPresent()) {
                        comboBox.addItem(type);
                    }
                    // skip collection types, there is now a separate column
                    // for this.
                    // comboBox.addItem(ListCell.getCollectionType(type));
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
