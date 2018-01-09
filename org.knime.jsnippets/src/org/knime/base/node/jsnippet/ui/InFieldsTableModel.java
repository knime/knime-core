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
 *   14.03.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.base.node.jsnippet.JavaSnippet;
import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.type.flowvar.TypeConverter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.node.workflow.FlowVariable;

/**
 * The table model for the input fields table. It provides validation.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
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
        super(new String[]{"Name", "Java Type", "Java Field"});
        Map<Column, Integer> columns = new HashMap<>();
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
    private String validateJavaTypeColumn(final int row) {
        Object value = this.getValueAt(row, Column.JAVA_TYPE);
        if (null == value) {
            return "Please select a value";
        }

        Object input = this.getValueAt(row, Column.COLUMN);

        if (value instanceof DataCellToJavaConverterFactory) {
            if (input instanceof FlowVariable) {
                return "DataCell converters do not work for flow variables.";
            } else if (input instanceof DataColumnSpec) {
                final DataType dataType = ((DataColumnSpec)input).getType();
                if (!dataType.getValueClasses().contains(((DataCellToJavaConverterFactory<?, ?>)value).getSourceType())) {
                    return "Input DataCell is incompatible with this java type.";
                }
            } // otherwise user has to fix column/flowvar first.
        } else if (value instanceof String) {
            // converter factory id
            final String id = (String)value;

            if (input instanceof DataColumnSpec) {
                final DataColumnSpec colSpec = (DataColumnSpec)input;
                final Optional<DataCellToJavaConverterFactory<?, ?>> factory =
                    ConverterUtil.getDataCellToJavaConverterFactory(id);
                if (!factory.isPresent()) {
                    return "Converter with id " + id + " could not be found.";
                }
                if (!colSpec.getType().getValueClasses().contains(factory.get().getSourceType())) {
                    // Should not happen, factory should have been cleared instead.
                    // But just in case it does, this will make sure we know.
                    return "Selected factory does not support the input type.";
                }
                return null;
            } else if (input instanceof FlowVariable) {
                return "DataCell converters do not work for flow variables.";
            }
        } else if (value instanceof Class){
            Class<?> javaType = (Class<?>)value;

            if (input instanceof DataColumnSpec) {
                DataColumnSpec colSpec = (DataColumnSpec)input;
                final Collection<?> factories = ConverterUtil.getFactoriesForSourceType(colSpec.getType());
                return factories.isEmpty() ? "The java type \"" + javaType.getSimpleName() + "\" is not supported." : null;
            } else if (input instanceof FlowVariable) {
                FlowVariable flowVar = (FlowVariable)input;
                TypeConverter typeConversion =
                    TypeProvider.getDefault().getTypeConverter(
                        flowVar.getType());
                return typeConversion.canProvideJavaType(javaType) ? null
                        : "The java type \"" + javaType.getSimpleName()
                          + "\" is not supported.";
            }
        } else {
            return "Cannot find type or converter " + value.toString();
        }
        return null;
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
    @Override
    public Object[] getAllowedJavaTypes(final int row) {
        Object input = getValueAt(row, Column.COLUMN);
        if (input instanceof DataColumnSpec) {
            DataColumnSpec colSpec = (DataColumnSpec)input;
            final Collection<DataCellToJavaConverterFactory<?, ?>> factories =
                ConverterUtil.getFactoriesForSourceType(colSpec.getType()).stream()
                    .filter(factory -> JavaSnippet.getBuildPathFromCache(factory.getIdentifier()) != null)
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())).collect(Collectors.toList());
            final Object[] array = factories.toArray(new Object[factories.size()]);
            return array;
        } else if (input instanceof FlowVariable) {
            FlowVariable flowVar = (FlowVariable)input;
            TypeConverter typeConversion = TypeProvider.getDefault().getTypeConverter(flowVar.getType());
            return typeConversion.canProvideJavaTypes();
        } else {
            return new Class[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int row, final Column column) {
        super.setValueAt(aValue, row, column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int row, final int column) {
        // make sure setValue(Object, int, Column) is always called.
        Column col = getColumnForIndex(column);
        if (col == Column.COLUMN) {
            if (aValue instanceof FlowVariable) {
                // make sure we do not keep a ConverterFactory in the JavaType column
                // when changing from DataColumnSpec.
                Object type = getValueAt(row, Column.JAVA_TYPE);

                if (type instanceof DataCellToJavaConverterFactory) {
                    // set java type to dest type of converter factory
                    setValueAt(((DataCellToJavaConverterFactory<?, ?>)type).getDestinationType(), row,
                        Column.JAVA_TYPE);
                }
            } else if (aValue instanceof DataColumnSpec) {
                // make sure we do not keep a Java class in the JavaType column for Columns
                Object type = getValueAt(row, Column.JAVA_TYPE);

                if (type instanceof Class) {
                    // find a DataCell converter which is able to convert from the new column type to the current java type
                    final Optional<?> factory = ConverterUtil.getConverterFactory(((DataColumnSpec)aValue).getType(), (Class<?>)type);

                    if (factory.isPresent()) {
                        setValueAt(factory.get(), row, Column.JAVA_TYPE);
                    }
                }
            }
        }
        super.setValueAt(aValue, row, column);
    }
}
