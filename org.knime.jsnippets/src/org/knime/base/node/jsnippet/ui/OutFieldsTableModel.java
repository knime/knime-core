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
 *   14.03.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.type.data.JavaToDataCell;
import org.knime.base.node.jsnippet.type.flowvar.TypeConverter;
import org.knime.base.node.jsnippet.ui.OutFieldsTable.FieldType;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * The table model for the OutFieldsTable. It provides validation.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public class OutFieldsTableModel extends FieldsTableModel {
    private static String[] getColumns(final boolean flowVarsOnly) {
        if (flowVarsOnly) {
            return new String[]{"Replace", "Column / Flow Variable",
                    "Output Type", "Java Type",  "Java Field"};
        } else {
            return new String[]{"Field Type", "Replace",
                    "Column / Flow Variable",
                    "Output Type", "Array", "Java Type",  "Java Field"};
        }
    }


    private boolean m_flowVarsOnly;

    /**
     * Create a new instance.
     * @param flowVarsOnly true when only flow variables and no columns can
     * be defined.
     */
    public OutFieldsTableModel(final boolean flowVarsOnly) {
        super(getColumns(flowVarsOnly));
        m_flowVarsOnly = flowVarsOnly;
        Map<Column, Integer> columns = new HashMap<Column, Integer>();
        if (flowVarsOnly) {
            columns.put(Column.REPLACE_EXISTING, 0);
            columns.put(Column.COLUMN, 1);
            columns.put(Column.DATA_TYPE, 2);
            columns.put(Column.JAVA_TYPE, 3);
            columns.put(Column.JAVA_FIELD, 4);
        } else {
            columns.put(Column.FIELD_TYPE, 0);
            columns.put(Column.REPLACE_EXISTING, 1);
            columns.put(Column.COLUMN, 2);
            columns.put(Column.DATA_TYPE, 3);
            columns.put(Column.IS_COLLECTION, 4);
            columns.put(Column.JAVA_TYPE, 5);
            columns.put(Column.JAVA_FIELD, 6);
        }
        setColumnsMap(columns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean isValidValue(final int row, final Column column) {
        switch (column) {
        case REPLACE_EXISTING:
            return validateColReplaceExisting(row) == null;
        case FIELD_TYPE:
            return validateColFieldType(row) == null;
        case COLUMN:
            return validateColColumn(row) == null;
        case DATA_TYPE:
            return validateColDataType(row) == null;
        case JAVA_TYPE:
            return validateJavaTypeColumn(row) == null;
        case IS_COLLECTION:
            return true;
        case JAVA_FIELD:
            return validateJavaFieldColumn(row) == null;
        default:
            throw new IllegalStateException("Unknown column.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getErrorMessage(final int row, final Column column) {
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
        case JAVA_TYPE:
            return validateJavaTypeColumn(row);
        case JAVA_FIELD:
            return validateJavaFieldColumn(row);
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
            if (!FieldsTableUtil.verifyNameOfFlowVariable((String)value)) {
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
            if (!FieldsTableUtil.verifyNameOfFlowVariable(
                    ((FlowVariable)value).getName())) {
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
        boolean isUnique = isUnique(value, row, Column.COLUMN);
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
        } else if (null == value || value instanceof String) {
            return "Please select a valid type.";
        }
        // no errors found
        return null;
    }


    /**
     * Validate java type in the given row.
     * @param row the row to check
     * @return error message if error occurs
     */
    @SuppressWarnings("rawtypes")
    private String validateJavaTypeColumn(final int row) {
        Object value = this.getValueAt(row, Column.JAVA_TYPE);
        if (null == value) {
            return "Please select a value";
        }
        if (!(value instanceof Class)) {
            return "Cannot find class " + value.toString();
        }

        Class javaType = (Class)value;

        Object outType = getValueAt(row, Column.DATA_TYPE);
        if (outType instanceof DataType) {
            DataType elemType = (DataType)outType;
            boolean isCollection = (Boolean)getValueAt(row,
                    Column.IS_COLLECTION);
            DataType dataType = isCollection
                ? ListCell.getCollectionType(elemType) : elemType;
            JavaToDataCell javaToDataCell =
                TypeProvider.getDefault().getJavaToDataCell(elemType,
                        isCollection);
            Collection<Class> canTypes =
                Arrays.asList(javaToDataCell.canJavaTypes());
            if (!canTypes.contains(javaType)) {
                return "The java type \"" + javaType.getSimpleName()
                      + "\" is not supported for output columns.";
            }
            if (dataType.isCollectionType() && !javaType.isArray()) {
                return "Please choose an java array for collection types.";
            }
            if (!dataType.isCollectionType() && javaType.isArray()) {
                return "An array cannot be written to a non collection column";
            }

        } else if (outType instanceof Type) {
            Type type = (Type)outType;
            TypeConverter typeConversion =
                TypeProvider.getDefault().getTypeConverter(type);
            if (!typeConversion.canProvideJavaType(javaType)) {
                return "The java type \"" + javaType.getSimpleName()
                      + "\" is not supported.";
            }
        }
        // no errror found
        return null;
    }


    /**
     * Validate java field name in the given row.
     * @param row the row to check
     * @return error message if error occurs
     */
    private String validateJavaFieldColumn(final int row) {
        Object value = getValueAt(row, Column.JAVA_FIELD);
        if (null == value) {
            return "Please select a value";
        }
        // check if a field with the same name exists
        boolean isUnique = isUnique(value, row, Column.JAVA_FIELD);
        // check if value is a valid java identifier
        boolean isValid = FieldsTableUtil.isValidJavaIdentifier((String)value);
        if (isUnique && isValid) {
            return null;
        } else {
            if (!isUnique) {
                return "Duplicated field name";
            } else { // case: !isValid
                return "This is not a valid java identifier";
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getAllowedJavaTypes(final int row) {
        Object input = getValueAt(row, Column.DATA_TYPE);
        if (input instanceof DataType) {
            DataType elemType = (DataType)input;
            boolean isCollection = (Boolean)getValueAt(row,
                    Column.IS_COLLECTION);
            JavaToDataCell javaToDataCell = TypeProvider.getDefault()
                .getJavaToDataCell(elemType, isCollection);
            return javaToDataCell.canJavaTypes();
        } else if (input instanceof Type) {
            Type type = (Type)input;
            TypeConverter typeConversion =
                TypeProvider.getDefault().getTypeConverter(type);
            return typeConversion.canCreatedFromJavaTypes();
        } else {
            return new Class[]{String.class};
        }
    }
}
