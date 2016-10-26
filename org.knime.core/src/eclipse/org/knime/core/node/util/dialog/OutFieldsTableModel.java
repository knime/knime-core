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
 *   24 Oct 2016 (albrecht): created
 */
package org.knime.core.node.util.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * Extends the table model by validation methods.
 * <p>This class might change and is not meant as public API.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz Germany
 * @author Heiko Hofer
 * @since 3.3
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("serial")
public class OutFieldsTableModel extends FieldsTableModel {

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

    private static String[] getColumns(final boolean flowVarsOnly, final boolean defaultColumn) {
        List<String> colList = new ArrayList<String>();
        if (!flowVarsOnly) {
            colList.add("Field Type");
        }
        colList.add("Replace");
        colList.add((flowVarsOnly ? "" : "Column / ") + "Flow Variable");
        colList.add("Output Type");
        if (!flowVarsOnly) {
            colList.add("Array");
        }
        colList.add("Script Field");
        if (defaultColumn) {
            colList.add("Default Value");
        }
        return colList.toArray(new String[0]);
    }


    private boolean m_flowVarsOnly;

    /**
     * Create a new instance.
     * @param flowVarsOnly true when only flow variables and no columns can
     * be defined.
     * @param defaultColumn true, when a column for default values can be defined
     */
    public OutFieldsTableModel(final boolean flowVarsOnly, final boolean defaultColumn) {
        super(getColumns(flowVarsOnly, defaultColumn));
        m_flowVarsOnly = flowVarsOnly;
        Map<Column, Integer> columns = new HashMap<>();
        if (flowVarsOnly) {
            columns.put(Column.REPLACE_EXISTING, 0);
            columns.put(Column.COLUMN, 1);
            columns.put(Column.DATA_TYPE, 2);
            columns.put(Column.TARGET_FIELD, 3);
        } else {
            columns.put(Column.FIELD_TYPE, 0);
            columns.put(Column.REPLACE_EXISTING, 1);
            columns.put(Column.COLUMN, 2);
            columns.put(Column.DATA_TYPE, 3);
            columns.put(Column.IS_COLLECTION, 4);
            columns.put(Column.TARGET_FIELD, 5);
        }
        if (defaultColumn) {
            columns.put(Column.DEFAULT_VALUE, flowVarsOnly ? 4 : 6);
        }
        setColumnsMap(columns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        if (columnIndex == getIndex(Column.TARGET_FIELD)) {
            return false;
        }
        return super.isCellEditable(rowIndex, columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidValue(final int row, final Column column) {
        switch (column) {
        case REPLACE_EXISTING:
            return validateColReplaceExisting(row) == null;
        case FIELD_TYPE:
            return validateColFieldType(row) == null;
        case COLUMN:
            return validateColColumn(row) == null;
        case DATA_TYPE:
            return validateColDataType(row) == null;
        case TARGET_FIELD:
            return validateTargetFieldColumn(row) == null;
        case IS_COLLECTION:
            return true;
        case DEFAULT_VALUE:
            return validateDefaultValueColumn(row) == null;
        default:
            throw new IllegalStateException("Unknown column.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorMessage(final int row, final Column column) {
        switch (column) {
        case REPLACE_EXISTING:
            return validateColReplaceExisting(row);
        case FIELD_TYPE:
            return validateColFieldType(row);
        case COLUMN:
            return validateColColumn(row);
        case DATA_TYPE:
            return validateColDataType(row);
        case IS_COLLECTION:
            return null;
        case TARGET_FIELD:
            return validateTargetFieldColumn(row);
        case DEFAULT_VALUE:
            return validateDefaultValueColumn(row);
        default:
            throw new IllegalStateException("Unknown column.");
        }
    }

    /**
     * Returns the field type for the given column.
     *
     * @param row the row to check
     * @return the field type of the given column
     */
    private FieldType getFieldType(final int row) {
        if (m_flowVarsOnly) {
            return FieldType.FlowVariable;
        } else {
            return (FieldType)getValueAt(row, Column.FIELD_TYPE);
        }
    }

    /**
     * Validate property "Replace Existing" in the given row.
     * @param row the row to check
     * @return error message if error occurs
     */
    private String validateColReplaceExisting(final int row) {
        // always valid
        return null;
    }

    /**
     * Validate field type in the given row.
     * @param row the row to check
     * @return error message if error occurs
     */
    private String validateColFieldType(final int row) {
        Object value = getFieldType(row);
        if (null == value) {
            return "Please select a value.";
        }
        return value.equals(FieldType.Column)
            || value.equals(FieldType.FlowVariable) ? null
                    : "No valid field type: " + value.toString();
    }

    /**
     * Validate column / flow variable name in the given row.
     * @param row the row to check
     * @return error message if error occurs
     */
    private String validateColColumn(final int row) {
        Object value = getValueAt(row, Column.COLUMN);
        if (null == value) {
            return "Please select a value";
        }
        boolean isDataColumn = getFieldType(row).equals(FieldType.Column);
        boolean isReplacing = (Boolean)getValueAt(row, Column.REPLACE_EXISTING);
        if (value instanceof String) {
            if (isReplacing) {
                return "Please select an input.";
            }
            if (((String)value).trim().isEmpty()) {
                return "Please define a name.";
            }
            if (!verifyNameOfFlowVariable((String)value)) {
                return "Flow variable has reserved prefix. "
                    + "Please change the name.";
            }
        } else if (value instanceof FlowVariable) {
            if (!isReplacing) {
                return "This flow variable does already exist.";
            }
            if (isDataColumn) {
                return "Please select a column.";
            }
            if (!verifyNameOfFlowVariable(((FlowVariable)value).getName())) {
                return "Flow variable has reserved prefix. "
                    + "Please change the name.";
            }
        } else if (value instanceof DataColumnSpec) {
            if (!isReplacing) {
                return "This column does already exist.";
            }
            if (!isDataColumn) {
                return "Please select a flow variable.";
            }
        }
        // check if data column is unique
        boolean isUnique = isUnique(value, row, Column.COLUMN, m_flowVarsOnly ? null : Column.FIELD_TYPE);
        if (!isUnique) {
            if (isDataColumn) {
                return "Duplicated data column.";
            } else {
                return "Duplicated flow variable.";
            }
        }
        // no errors found
        return null;
    }

    /**
     * Validate knime type in the given row.
     * @param row the row to check
     * @return error message if error occurs
     */
    private String validateColDataType(final int row) {
        Object value = getValueAt(row, Column.DATA_TYPE);
        if (null == value) {
            return "Please select a value";
        }
        if (value instanceof Type) {
            if (getFieldType(row).equals(FieldType.Column)) {
                return "Please select a data type.";
            }
        } else if (value instanceof DataType) {
            if (getFieldType(row).equals(FieldType.FlowVariable)) {
                return "Please select a flow variable type.";
            }
        } else if (value instanceof String) {
            return "Please select a valid type.";
        }
        // no errors found
        return null;
    }

    /**
     * Validate target field in the given row.
     * @param row the row to check
     * @return error message if error occurs
     */
    private String validateTargetFieldColumn(final int row) {
        // should always be valid (not editable)
        return null;
    }

    /**
     * Validate default value in the given row.
     * @param row the row to check
     * @return error message if error occurs
     */
    private String validateDefaultValueColumn(final int row) {
        FieldType fieldType = getFieldType(row);
        Object oType = getValueAt(row, Column.DATA_TYPE);
        Object defaultValue = getValueAt(row, Column.DEFAULT_VALUE);
        if (FieldType.FlowVariable.equals(fieldType)) {
            if (oType instanceof Type) {
                switch ((Type)oType) {
                    case INTEGER:
                        if (!(defaultValue instanceof Integer)) {
                            try {
                                Integer.parseInt((String)defaultValue);
                            } catch (NumberFormatException e) {
                                return "Please define a valid integer default value.";
                            }
                        }
                        break;
                    case DOUBLE:
                        if (!(defaultValue instanceof Double)) {
                            try {
                                Double.parseDouble((String)defaultValue);
                            } catch (NumberFormatException e) {
                                return "Please define a valid double default value.";
                            }
                        }
                        break;
                    default:
                        return null;
                }

            }
        } else {
            // TODO check default value according to given column type
        }
        return null;
    }

    /**
     * Test whether the given name is allowed for flow variables. Flow variables
     * are i.e. not allowed to start with "knime."
     *
     * @param name the name the name
     * @return true when give name is valid
     */
    static boolean verifyNameOfFlowVariable(final String name) {
        try {
            // test if a flow variable of this name might be
            // created. verifyName throws the package private
            // exception: IllegalFlowObjectStackException
            new FlowVariable(name, "").getScope().verifyName(name);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

}
