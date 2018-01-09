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
 *   Oct 2, 2010 (wiswedel): created
 */
package org.knime.ext.sun.nodes.script.settings;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

/**
 * Abstract class of all supported in and output types in the java snippet code.
 * @param <J> The Java type that is used within the snippet
 * @param <V> The DataValue type that can be converted to J.
 * @param <C> The DataCell type is used to represent objects of J in KNIME.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public abstract class JavaSnippetType
    <J, V extends DataValue, C extends DataCell> {

    /** Available types in an array. */
    public static final JavaSnippetType<?, ?, ?>[] TYPES =
        new JavaSnippetType<?, ?, ?>[] {
        JavaSnippetBooleanType.INSTANCE,
        JavaSnippetIntType.INSTANCE,
        JavaSnippetLongType.INSTANCE,
        JavaSnippetDoubleType.INSTANCE,
        JavaSnippetStringType.INSTANCE
    };

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());
    private final Class<J> m_javaClass;
    private final Class<V> m_knimeValueClass;
    private final Class<C> m_knimeCellClass;

    /** Create type.
     * @param javaClass Concrete class of J, used for array creating, type check
     * @param knimeValueClass Concrete class of V, used for type check, cast
     * @param knimeCellClass Concrete class of C, used for type check, cast
     */
    public JavaSnippetType(final Class<J> javaClass,
            final Class<V> knimeValueClass,
            final Class<C> knimeCellClass) {
        m_javaClass = javaClass;
        m_knimeValueClass = knimeValueClass;
        m_knimeCellClass = knimeCellClass;
    }

    /** Convert a DataValue to a Java Object.
     * @param value The value
     * @return The java object
     */
    protected abstract J toJavaClass(final V value);

    /** Convert a java object to a DataCell.
     * @param value object to put into cell.
     * @return The cell representing <code>value</code>.
     */
    protected abstract C toKNIMECell(final J value);

    /** Test whether input column is super type &lt;V&gt;.
     * @param type arg type.
     * @return True if this type is compatible to the argument.
     */
    public boolean checkCompatibility(final DataType type) {
        return type.isCompatible(m_knimeValueClass);
    }

    /** Convert input collection to java array. Missing cells in the collection
     * are represented by <code>null</code> fields in the returned array.
     * @param value to convert.
     * @return The array, put into snippet.
     */
    public J[] asJavaArray(final CollectionDataValue value) {
        @SuppressWarnings("unchecked")
        J[] result = (J[])Array.newInstance(m_javaClass, value.size());
        int i = 0;
        for (DataCell c : value) {
            result[i++] = asJavaObject(c);
        }
        return result;
    }

    /** Convert input cell object to plain java field. Missing cells are
     * represented by <code>null</code>.
     * @param cell To convert
     * @return Java object representing the cell argument.
     */
    public J asJavaObject(final DataCell cell) {
        if (cell.isMissing()) {
            return null;
        }
        Class<? extends DataCell> cellClass = cell.getClass();
        if (!m_knimeValueClass.isAssignableFrom(cellClass)) {
            throw new IllegalStateException("Unable to cast cell class "
                    + cellClass.getName() + " (object \"" + cell + "\") to "
                    + m_knimeValueClass.getName());
        }
        V value = m_knimeValueClass.cast(cell);
        return toJavaClass(value);
    }

    /** Converted java array to knime list cell. Null values in the array
     * are converted to missing cells.
     * @param obj Array to convert.
     * @return The list containing values of the argument.
     */
    public ListCell asKNIMEListCell(final Object[] obj) {
        List<DataCell> asCellColl = new ArrayList<DataCell>(obj.length);
        for (Object j : obj) {
            DataCell c = asKNIMECell(j);
            asCellColl.add(c);
        }
        return CollectionCellFactory.createListCell(asCellColl);
    }

    /** Convert java object to knime cell. If argument is null, a missing
     * cell is returned. The object is supposed to be either null or instance
     * of &lt;J&gt;
     * @param o Object to convert.
     * @return The cell representing o.
     */
    public DataCell asKNIMECell(final Object o) {
        if (o == null) {
            return DataType.getMissingCell();
        } else if (!m_javaClass.isInstance(o)) {
            m_logger.warn("Unable to cast \""
                    + o.getClass().getName() + "\" to \""
                    + m_javaClass.getName() + "\", assigning missing");
            return DataType.getMissingCell();
        } else {
            return toKNIMECell(m_javaClass.cast(o));
        }
    }

    /** Get java class represented by this type.
     * @param asArray whether or not represented as array
     * @return Class of J */
    public Class<?> getJavaClass(final boolean asArray) {
        if (asArray) {
            return Array.newInstance(m_javaClass, 0).getClass();
        }
        return m_javaClass;
    }

    /** If this type is used as output type, the returned value is the
     * DataType of the column. If used as list cell, the returned value is
     * a collection data type, whose element type is set accordingly.
     * @param isListCell If output is list cell
     * @return column type of output column
     */
    public DataType getKNIMEDataType(final boolean isListCell) {
        DataType elementType = DataType.getType(m_knimeCellClass);
        if (isListCell) {
            return DataType.getType(ListCell.class, elementType);
        }
        return elementType;
    }

    /** Find the snippet type that can represent the argument type. If it
     * is a collection type, it will look at the encapsulated element type.
     * @param type The KNIME type to lookup
     * @return The snippet type that represents the argument. If none is
     *         found, returns the string snippet type.
     */
    public static JavaSnippetType<?, ?, ?> findType(final DataType type) {
        boolean isArray = type.isCollectionType();
        DataType base = type;
        if (isArray) {
            base = type.getCollectionElementType();
        }
        for (JavaSnippetType<?, ?, ?> t : TYPES) {
            if (t.checkCompatibility(base)) {
                return t;
            }
        }
        return JavaSnippetStringType.INSTANCE;
    }

    /** Boolean type. */
    public static final class JavaSnippetBooleanType
    extends JavaSnippetType<Boolean, BooleanValue, BooleanCell> {

        /** Singleton instance. */
        public static final JavaSnippetBooleanType INSTANCE =
            new JavaSnippetBooleanType();

        /** */
        private JavaSnippetBooleanType() {
            super(Boolean.class, BooleanValue.class, BooleanCell.class);
        }


        /** {@inheritDoc} */
        @Override
        protected Boolean toJavaClass(final BooleanValue value) {
            return value.getBooleanValue();
        }

        /** {@inheritDoc} */
        @Override
        protected BooleanCell toKNIMECell(final Boolean value) {
            return value ? BooleanCell.TRUE : BooleanCell.FALSE;
        }
    }

    /** Integer type. */
    public static final class JavaSnippetIntType
    extends JavaSnippetType<Integer, IntValue, IntCell> {

        /** Singleton instance. */
        public static final JavaSnippetIntType INSTANCE =
            new JavaSnippetIntType();

        /** */
        private JavaSnippetIntType() {
            super(Integer.class, IntValue.class, IntCell.class);
        }


        /** {@inheritDoc} */
        @Override
        protected Integer toJavaClass(final IntValue value) {
            return value.getIntValue();
        }

        /** {@inheritDoc} */
        @Override
        protected IntCell toKNIMECell(final Integer value) {
            return new IntCell(value);
        }
    }

    /** Long type. */
    public static final class JavaSnippetLongType
        extends JavaSnippetType<Long, LongValue, LongCell> {

        /** Singleton instance. */
        public static final JavaSnippetLongType INSTANCE =
            new JavaSnippetLongType();

        /** */
        private JavaSnippetLongType() {
            super(Long.class, LongValue.class, LongCell.class);
        }

        /** {@inheritDoc} */
        @Override
        protected Long toJavaClass(final LongValue value) {
            return value.getLongValue();
        }

        /** {@inheritDoc} */
        @Override
        protected LongCell toKNIMECell(final Long value) {
            return new LongCell(value);
        }
    }

    /** Double type. */
    public static final class JavaSnippetDoubleType
    extends JavaSnippetType<Double, DoubleValue, DoubleCell> {

        /** Singleton instance. */
        public static final JavaSnippetDoubleType INSTANCE =
            new JavaSnippetDoubleType();

        /** */
        private JavaSnippetDoubleType() {
            super(Double.class, DoubleValue.class, DoubleCell.class);
        }

        /** {@inheritDoc} */
        @Override
        protected Double toJavaClass(final DoubleValue value) {
            return value.getDoubleValue();
        }

        /** {@inheritDoc} */
        @Override
        protected DoubleCell toKNIMECell(final Double value) {
            return new DoubleCell(value);
        }
    }

    /** String type. */
    public static final class JavaSnippetStringType
    extends JavaSnippetType<String, DataValue, StringCell> {

        /** Singleton instance. */
        public static final JavaSnippetStringType INSTANCE =
            new JavaSnippetStringType();

        /** */
        private JavaSnippetStringType() {
            super(String.class, DataValue.class, StringCell.class);
        }


        /** {@inheritDoc} */
        @Override
        protected String toJavaClass(final DataValue value) {
            if (value instanceof StringValue) {
                return ((StringValue)value).getStringValue();
            }
            return value.toString();
        }

        /** {@inheritDoc} */
        @Override
        protected StringCell toKNIMECell(final String value) {
            return new StringCell(value);
        }
    }

}
