/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

import java.util.HashMap;
import java.util.Map;

import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.type.data.DataValueToJava;
import org.knime.base.node.jsnippet.type.flowvar.TypeConverter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;

/**
 * The table model for the input fields table. It provides validation.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public class InFieldsTableModel extends FieldsTableModel {
//    /** The index of the input column / flow variable column. */
//    public static final int COLUMN = 0;
//    /** The index of the java type column. */
//    public static final int JAVA_TYPE = 1;
//    /** The index of the java field name column. */
//    public static final int JAVA_FIELD = 2;

    /**
     * Create a new instance.
     */
    public InFieldsTableModel() {
        super(new String[]{
                "Column / Flow variable", "Java Type", "Java Field"});
        Map<Column, Integer> columns = new HashMap<Column, Integer>();
        columns.put(Column.COLUMN, 0);
        columns.put(Column.JAVA_TYPE, 1);
        columns.put(Column.JAVA_FIELD, 2);
        setColumnsMap(columns);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    boolean isValidValue(final int row, final Column column) {
        switch (column) {
        case COLUMN:
            return validateColColumn(row) == null;
        case JAVA_TYPE:
            return validateJavaTypeColumn(row) == null;
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
        case COLUMN:
            return validateColColumn(row);
        case JAVA_TYPE:
            return validateJavaTypeColumn(row);
        case JAVA_FIELD:
            return validateJavaFieldColumn(row);
        default:
            throw new IllegalStateException("Unknown column.");
        }
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
        Object input = this.getValueAt(row, Column.COLUMN);
        if (input instanceof DataColumnSpec) {
            DataColumnSpec colSpec = (DataColumnSpec)input;
            DataType elemType = colSpec.getType().isCollectionType()
                ? colSpec.getType().getCollectionElementType()
                : colSpec.getType();
            DataValueToJava dvToJava =
                TypeProvider.getDefault().getDataValueToJava(elemType,
                        colSpec.getType().isCollectionType());
            return dvToJava.canProvideJavaType(javaType) ? null
                    : "The java type \"" + javaType.getSimpleName()
                      + "\" is not supported.";
        } else if (input instanceof FlowVariable) {
            FlowVariable flowVar = (FlowVariable)input;
            TypeConverter typeConversion =
                TypeProvider.getDefault().getTypeConverter(
                    flowVar.getType());
            return typeConversion.canProvideJavaType(javaType) ? null
                    : "The java type \"" + javaType.getSimpleName()
                      + "\" is not supported.";
        } else {
            return null;
        }
    }

    /**
     * Validate column / flow variable name in the given row.
     * @param row the row to check
     * @return error message if error occurs
     */
    private String validateColColumn(final int row) {
        Object value = getValueAt(row, Column.COLUMN);
        if (null == value || value instanceof String) {
            return ("Please select an input.");
        } else {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getAllowedJavaTypes(final int row) {
        Object input = getValueAt(row, Column.COLUMN);
        if (input instanceof DataColumnSpec) {
            DataColumnSpec colSpec = (DataColumnSpec)input;
            DataType elemType = colSpec.getType().isCollectionType()
                ? colSpec.getType().getCollectionElementType()
                : colSpec.getType();
            DataValueToJava dvToJava =
                TypeProvider.getDefault().getDataValueToJava(elemType,
                        colSpec.getType().isCollectionType());
            return dvToJava.canProvideJavaTypes();
        } else if (input instanceof FlowVariable) {
            FlowVariable flowVar = (FlowVariable)input;
            TypeConverter typeConversion =
                TypeProvider.getDefault().getTypeConverter(
                    flowVar.getType());
            return typeConversion.canProvideJavaTypes();
        } else {
            return new Class[0];
        }
    }


}
