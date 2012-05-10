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

import java.util.Collection;

import org.knime.base.node.jsnippet.type.JavaToDataCell;
import org.knime.base.node.jsnippet.type.TypeConverter;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.ui.OutFieldsTable.FieldType;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * The table model for the OutFieldsTable. It provides validation.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public class OutFieldsTableModel extends FieldsTableModel {
    /** The index of the replace existing column. */
    public static final int COL_REPLACE_EXISTING = 0;
    /** The index of the column with the fields type. */
    public static final int COL_FIELD_TYPE = 1;
    /** The index of the input column / flow variable column. */
    public static final int COL_COLUMN = 2;
    /** The index of the column with the data type. */
    public static final int COL_DATA_TYPE = 3;
    /** The index of the java type column. */
    public static final int COL_JAVA_TYPE = 4;
    /** The index of the java field name column. */
    public static final int COL_JAVA_FIELD = 5;


    /**
     * Create a new instance.
     */
    public OutFieldsTableModel() {
        super(new String[]{"Replace", "Field Type", "Column / Flow Variable",
                        "Type", "Java Type", "Java Field"});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean isValidValue(final int row, final int column) {
        switch (column) {
        case COL_REPLACE_EXISTING:
            return validateColReplaceExisting(row) == null;
        case COL_FIELD_TYPE:
            return validateColFieldType(row) == null;
        case COL_COLUMN:
            return validateColColumn(row) == null;
        case COL_DATA_TYPE:
            return validateColDataType(row) == null;
        case COL_JAVA_TYPE:
            return validateJavaTypeColumn(row) == null;
        case COL_JAVA_FIELD:
            return validateJavaFieldColumn(row) == null;
        default:
            throw new IllegalStateException("Unknown column.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getErrorMessage(final int row, final int column) {
        switch (column) {
        case COL_REPLACE_EXISTING:
            return validateColReplaceExisting(row);
        case COL_FIELD_TYPE:
            return validateColFieldType(row);
        case COL_COLUMN:
            return validateColColumn(row);
        case COL_DATA_TYPE:
            return validateColDataType(row);
        case COL_JAVA_TYPE:
            return validateJavaTypeColumn(row);
        case COL_JAVA_FIELD:
            return validateJavaFieldColumn(row);
        default:
            throw new IllegalStateException("Unknown column.");
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
        Object value = getValueAt(row, COL_FIELD_TYPE);
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
        Object value = getValueAt(row, COL_COLUMN);
        if (null == value) {
            return "Please select a value";
        }
        boolean isDataColumn =
            getValueAt(row, COL_FIELD_TYPE).equals(FieldType.Column);
        boolean isReplacing = (Boolean)getValueAt(row, COL_REPLACE_EXISTING);
        if (value instanceof String) {
            if (isReplacing) {
                return "Please select an input.";
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
        boolean isUnique = FieldsTableUtil.isUnique(
                this, value, row, COL_COLUMN);
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
        Object value = getValueAt(row, COL_DATA_TYPE);
        if (null == value) {
            return "Please select a value";
        }
        if (value instanceof Type) {
            if (getValueAt(row, COL_FIELD_TYPE).equals(FieldType.Column)) {
                return "Please select a data type.";
            }
        } else if (value instanceof DataType) {
            if (getValueAt(row, COL_FIELD_TYPE).equals(
                    FieldType.FlowVariable)) {
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
    private String validateJavaTypeColumn(final int row) {
        Object value = this.getValueAt(row, COL_JAVA_TYPE);
        if (null == value) {
            return "Please select a value";
        }
        if (!(value instanceof Class)) {
            return "Cannot find class " + value.toString();
        }
        @SuppressWarnings("rawtypes")
        Class javaType = (Class)value;

        Object outType = getValueAt(row, COL_DATA_TYPE);
        if (outType instanceof DataType) {
            DataType dataType = (DataType)outType;
            DataType elemType = dataType.isCollectionType()
                ? dataType.getCollectionElementType() : dataType;
            Collection<DataType> outTypes =
                TypeProvider.getDefault().getOutputDataTypes();
            if (!outTypes.contains(elemType)) {
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
        Object value = getValueAt(row, COL_JAVA_FIELD);
        if (null == value) {
            return "Please select a value";
        }
        // check if a field with the same name exists
        boolean isUnique = FieldsTableUtil.isUnique(this, value,
                row, COL_JAVA_FIELD);
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
        Object input = getValueAt(row, COL_DATA_TYPE);
        if (input instanceof DataType) {
            DataType dataType = (DataType)input;
            DataType elemType = dataType.isCollectionType()
                ? dataType.getCollectionElementType() : dataType;
            JavaToDataCell javaToDataCell = TypeProvider.getDefault()
                .getJavaToDataCell(elemType, dataType.isCollectionType());
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
