/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   25 Oct 2016 (albrecht): created
 */
package org.knime.core.node.util.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.node.util.ConfigTablePanel;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.FlowVariableTableCellRenderer;
import org.knime.core.node.util.dialog.FieldsTableModel.Column;
import org.knime.core.node.util.dialog.OutFieldsTableModel.FieldType;
import org.knime.core.node.util.dialog.field.AbstractField;
import org.knime.core.node.util.dialog.field.DefaultOutFlowVariableField;
import org.knime.core.node.util.dialog.field.FieldCollection;
import org.knime.core.node.util.dialog.field.FieldList.OutColumnList;
import org.knime.core.node.util.dialog.field.FieldList.OutFlowVariableList;
import org.knime.core.node.util.dialog.field.OutColumnField;
import org.knime.core.node.util.dialog.field.OutFlowVariableField;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * The table where fields for output columns and flow variables can be defined.
 *
 * <p>This class might change and is not meant as public API.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @author Heiko Hofer
 * @since 3.3
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("serial")
public class OutFieldsTable extends ConfigTablePanel {

    /**
     * Property fired when a row is manually added by the user.
     */
    public static final String PROP_FIELD_ADDED = "prop_field_added";

    private OutFieldsTableModel m_model;
    private DataTableSpec m_spec;
    private Map<String, FlowVariable> m_flowVars;
    private boolean m_flowVarsOnly;
    private boolean m_defineDefaultValues;

    /**
     * Create a new component. The flowVarsOnly allow to switch between a
     * table where columns and flow variables can be defined and where flow
     * variables can be defined, only.
     *
     * @param flowVarsOnly true when only flow variables and no columns can
     * be defined.
     * @param defineDefaultValues true, when default values are supposed to be given
     */
    public OutFieldsTable(final boolean flowVarsOnly, final boolean defineDefaultValues) {
        super(new OutFieldsTableModel(flowVarsOnly, defineDefaultValues));
        final JTable table = getTable();
        m_flowVarsOnly = flowVarsOnly;
        m_defineDefaultValues = defineDefaultValues;
        m_model = (OutFieldsTableModel)getModel();

        m_model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                checkColColumn(e);
                autoUpdateIsArray(e);
                autoUpdateTargetName(e);

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

                    }
                }
            }

            private void autoUpdateTargetName(final TableModelEvent e) {
                if (e.getType() == TableModelEvent.DELETE) {
                    return;
                }
                for (int r = e.getFirstRow(); r <= e.getLastRow(); r++) {
                    Object value = m_model.getValueAt(r, Column.COLUMN);
                    if (null == value) {
                        return;
                    } else {
                        String targetName = getTargetFieldName(r);
                        if (!targetName.equals(m_model.getValueAt(r, Column.TARGET_FIELD))) {
                            m_model.setValueAt(targetName, r, Column.TARGET_FIELD);
                        }
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
                    defaultColTarget = null;
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
                    defaultVarTarget = null;
                    for (FlowVariable flowVar : m_flowVars.values()) {
                        // test if a flow variable of this name might be
                        // created.
                        if (OutFieldsTableModel.verifyNameOfFlowVariable(
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
     * Returns a name under which the output column or flow variable can be accessed or defined in the target script
     *
     * @param row the row to check
     * @return the target name
     */
    public String getTargetFieldName(final int row) {

        //TODO make abstract, implement in concrete class in JS view

        boolean isVariable = -1 == m_model.getIndex(Column.FIELD_TYPE) ||
                m_model.getValueAt(row, Column.FIELD_TYPE).equals(FieldType.FlowVariable);
        Object v = m_model.getValueAt(row, Column.COLUMN);
        String knimeName;
        if (v instanceof FlowVariable) {
            knimeName = ((FlowVariable)v).getName();
        } else if (v instanceof String) {
            knimeName = (String)v;
        } else {
            return null;
        }
        if (isVariable) {
            return "FLOW_VARIABLES[\"" + knimeName + "\"]";
        } else {
            return "OUT_COLUMNS[\"" + knimeName + "\"]";
        }
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
        AbstractField newRow = AddOutFieldDialog.openUserDialog(parent,
                m_model, m_spec, m_flowVars, m_flowVarsOnly, m_defineDefaultValues);
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
    private boolean addRow(final AbstractField newRow) {
        if (newRow instanceof OutColumnField) {
            return addRow((OutColumnField)newRow);
        } else if (newRow instanceof OutFlowVariableField) {
            return addRow((OutFlowVariableField)newRow);
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
        AbstractField newRow = AddOutFieldDialog.openUserDialog(parent,
                m_model, m_spec, m_flowVars, m_flowVarsOnly, m_defineDefaultValues);
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
        AbstractField newRow = AddOutFieldDialog.openUserDialog(parent,
                m_model, m_spec, m_flowVars, m_flowVarsOnly, m_defineDefaultValues);
        if (null != newRow) {
            return addRow(newRow);
        } else {
            return false;
        }
    }

    /**
     * Adds a row using the values of the given output column.
     *
     * @param outCol the output column definition
     * @return true when the row was added successfully
     */
    public boolean addRow(final OutColumnField outCol) {
        int r = m_model.getRowCount();
        m_model.addRow();
        m_model.setValueAt(outCol.getReplaceExisting(),
                r, Column.REPLACE_EXISTING);
        m_model.setValueAt(FieldType.Column, r, Column.FIELD_TYPE);

        String colName = outCol.getKnimeName();
        DataColumnSpec colSpec = m_spec.getColumnSpec(colName);
        Object value = null != colSpec ? colSpec : colName;
        m_model.setValueAt(value, r, Column.COLUMN);
        DataType type = outCol.getKnimeType();
        boolean isCollection = type.isCollectionType();
        m_model.setValueAt(
                isCollection ? type.getCollectionElementType() : type,
                r, Column.DATA_TYPE);
        m_model.setValueAt(isCollection, r, Column.IS_COLLECTION);
        if (m_defineDefaultValues) {
            //TODO default values for columns
        }
        return true;
    }

    /**
     * Adds a row using the values of the given output variable.
     *
     * @param outVar the output variable definition
     * @return true when the row was added successfully
     */
    public boolean addRow(final OutFlowVariableField outVar) {
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
        m_model.setValueAt(outVar.getKnimeType(), r, Column.DATA_TYPE);
        if (!m_flowVarsOnly) {
            m_model.setValueAt(false, r, Column.IS_COLLECTION);
        }
        if (m_defineDefaultValues && outVar instanceof DefaultOutFlowVariableField) {
            DefaultOutFlowVariableField doV = (DefaultOutFlowVariableField)outVar;
            m_model.setValueAt(doV.getDefaultValue(), r, Column.DEFAULT_VALUE);
        }

        return true;
    }

    /**
     * Set the table data.
     *
     * @param fields the fields
     * @param spec the input spec
     * @param flowVars the flow variables
     */
    public void updateData(final FieldCollection fields,
            final DataTableSpec spec,
            final Map<String, FlowVariable> flowVars) {
        m_spec = spec;
        m_flowVars = flowVars;

        m_model.clear();
        for (int r = 0; r < fields.getOutColumnList().size(); r++) {
            OutColumnField field = fields.getOutColumnList().get(r);
            addRow(field);
        }
        for (int r = 0; r < fields.getOutFlowVariableList().size(); r++) {
            OutFlowVariableField field = fields.getOutFlowVariableList().get(r);
            addRow(field);
        }

        JTable table = getTable();
        table.getColumnModel().getColumn(
                m_model.getIndex(Column.REPLACE_EXISTING)).setCellRenderer(
                new BooleanRenderer());
        table.getColumnModel().getColumn(
                m_model.getIndex(Column.REPLACE_EXISTING)).setCellEditor(
                new DefaultCellEditor(new JCheckBox()));
        if (!m_flowVarsOnly) {
            table.getColumnModel().getColumn(
                    m_model.getIndex(Column.FIELD_TYPE)).setCellEditor(
                    createFieldTypeCellEditor());
        }

        table.getColumnModel().getColumn(
                m_model.getIndex(Column.COLUMN)).setCellRenderer(
                new InputTableCellRenderer());
        table.getColumnModel().getColumn(
                m_model.getIndex(Column.COLUMN)).setCellEditor(
                new InputTableCellEditor(this, m_spec, m_flowVars));
        table.getColumnModel().getColumn(
                m_model.getIndex(Column.DATA_TYPE)).setCellRenderer(
                new DataTypeTableCellRenderer());
        table.getColumnModel().getColumn(
                m_model.getIndex(Column.DATA_TYPE)).setCellEditor(
                new DataTypeTableCellEditor(this, m_spec, m_flowVars));
        if (!m_flowVarsOnly) {
            table.getColumnModel().getColumn(
                    m_model.getIndex(Column.IS_COLLECTION)).setCellRenderer(
                    new BooleanRenderer() {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public Component getTableCellRendererComponent(
                                final JTable fooTable, final Object value,
                                final boolean isSelected,
                                final boolean hasFocus, final int row,
                                final int column) {
                            Component comp =
                                super.getTableCellRendererComponent(
                                    fooTable, value, isSelected, hasFocus,
                                    row, column);
                            FieldType fieldType = getFieldType(row);
                            comp.setEnabled(fieldType.equals(FieldType.Column));
                            return comp;
                        }
                    });
            table.getColumnModel().getColumn(
                    m_model.getIndex(Column.IS_COLLECTION)).setCellEditor(
                    new DefaultCellEditor(new JCheckBox()) {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public boolean isCellEditable(
                                final EventObject anEvent) {
                            int row = getTable().rowAtPoint(
                                    ((MouseEvent)anEvent).getPoint());
                            FieldType fieldType = getFieldType(row);
                            return fieldType.equals(FieldType.Column);
                        }
                    });
        }

        if (m_defineDefaultValues) {
            table.getColumnModel().getColumn(
                m_model.getIndex(Column.DEFAULT_VALUE))
                .setCellRenderer(new DefaultFieldTableCellRenderer());
        }
    }

    /**
     * Get the field definitions representing output columns.
     *
     * @return fields representing output columns.
     */
    public OutColumnList getOutColFields() {
        OutColumnList outCols = new OutColumnList();
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
                OutColumnField outCol = new OutColumnField();
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
                Object dataTypeValue = m_model.getValueAt(r,
                        Column.DATA_TYPE);
                boolean isArray = (Boolean)m_model.getValueAt(r,
                        Column.IS_COLLECTION);
                if (dataTypeValue instanceof DataType) {
                    DataType type = (DataType)dataTypeValue;
                    if (isArray) {
                        type = ListCell.getCollectionType(type);
                    }
                    outCol.setKnimeType(type);
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
    public OutFlowVariableList getOutVarFields() {
        OutFlowVariableList outVars = new OutFlowVariableList(m_defineDefaultValues);
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
                OutFlowVariableField outVar = m_defineDefaultValues ? new DefaultOutFlowVariableField() : new OutFlowVariableField();
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
                    outVar.setKnimeType(type);
                } else {
                    continue;
                }
                if (m_defineDefaultValues) {
                    DefaultOutFlowVariableField dOVar = (DefaultOutFlowVariableField)outVar;
                    Object defaultValue = m_model.getValueAt(r, Column.DEFAULT_VALUE);
                    switch ((Type)dataTypeValue) {
                        case INTEGER:
                            int defInt = defaultValue instanceof String ? Integer.parseInt((String)defaultValue) : (Integer)defaultValue;
                            dOVar.setDefaultValue(defInt);
                            break;
                        case DOUBLE:
                            double defDouble = defaultValue instanceof String ? Double.parseDouble((String)defaultValue) : (Double)defaultValue;
                            dOVar.setDefaultValue(defDouble);
                            break;
                        default:
                            dOVar.setDefaultValue((String)defaultValue);
                    }
                }
                outVars.add(outVar);
            }
        }
        return outVars;
    }

    // CELL RENDERERS

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
            setHorizontalAlignment(SwingConstants.CENTER);
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
    private static class InputTableCellRenderer extends FlowVariableTableCellRenderer {

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

    /** Renders the table cells defining the default value fields.
    * Paints erroneous default values in red.
    */
   static class DefaultFieldTableCellRenderer extends DefaultTableCellRenderer {
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

           // let super class do the first step
           super.getTableCellRendererComponent(table, value,
                   isSelected, hasFocus,
                   row, column);

           FieldsTableModel model = (FieldsTableModel)table.getModel();
           if (model.isValidValue(row, column)) {
               setToolTipText(null);
           } else {
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

    // CELL EDITORS

    /**
     * Create cell editor for for the input columns / flow variables.
     * @return an editor for the type field
     */
    protected TableCellEditor createFieldTypeCellEditor() {
        JComboBox<FieldType> comboBox = new JComboBox<FieldType>();
        comboBox.addItem(FieldType.Column);
        comboBox.addItem(FieldType.FlowVariable);
        DefaultCellEditor editor = new DefaultCellEditor(comboBox);
        editor.setClickCountToStart(2);
        return editor;
    }

    /**
     * A cell editor to define the name of the output column or flow variable.
     * Note, that the output may replace an input.
     *
     * @author Heiko Hofer
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
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
                    if (OutFieldsTableModel.verifyNameOfFlowVariable(flowVar.getName())) {
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
    @SuppressWarnings({"unchecked", "rawtypes"})
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
                TypeProvider typeProvider = TypeProvider.getDefault();
                for (DataType type : typeProvider.getColumnTypes()) {
                    comboBox.addItem(type);
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
                for (Type type : typeProvider.getFlowVarTypes().keySet()) {
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
