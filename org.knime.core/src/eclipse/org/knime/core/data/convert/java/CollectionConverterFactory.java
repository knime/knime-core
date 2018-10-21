/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 */

package org.knime.core.data.convert.java;

import java.lang.reflect.Array;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.convert.util.ClassUtil;

/**
 * A {@link DataCellToJavaConverterFactory} which creates converters for converting {@link CollectionDataValue} subtypes
 * to Array subtypes.
 *
 * Warning: Incorrect use of this class results in undefined behavior. Handle with care.
 *
 * @author Jonathan Hale
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 *
 * @param <D> An array class so that <code> D == DE[] </code>
 * @param <SE> Type of elements of the source {@link CollectionDataValue}
 * @param <DE> Type of elements of {@code <DE>}
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class CollectionConverterFactory<D, SE extends DataValue, DE>
    implements DataCellToJavaConverterFactory<CollectionDataValue, D> {

    private final Class<D> m_destType;

    private final DataCellToJavaConverterFactory<SE, DE> m_elementConverterFactory;

    /**
     * Constructor.
     *
     * @param elementConverter Implementation (possibly a Lambda expression) of the {@link DataCellToJavaConverter}
     *            defining the single instance returned by {@link #create()}.
     */
    CollectionConverterFactory(final DataCellToJavaConverterFactory<SE, DE> elementConverterFactory) {
        m_destType = ClassUtil.getArrayType(elementConverterFactory.getDestinationType());
        m_elementConverterFactory = elementConverterFactory;
    }

    @Override
    public DataCellToJavaConverter<CollectionDataValue, D> create() {
        final DataCellToJavaConverter<SE, DE> elementConverter = m_elementConverterFactory.create();
        if (m_elementConverterFactory.getDestinationType().isPrimitive()) {
            // Implement for each primitive type explicitly. That's the most performant way.
            // double:
            if (elementConverter instanceof DataCellToDoubleConverter) {
                return createToDoubleConverter(elementConverter);
            }
            // int:
            else if (elementConverter instanceof DataCellToIntConverter) {
                return createToIntConverter(elementConverter);
            }
            // long:
            else if (elementConverter instanceof DataCellToLongConverter) {
                return createToLongConverter(elementConverter);
            }
            // boolean:
            else if (elementConverter instanceof DataCellToBooleanConverter) {
                return createToBooleanConverter(elementConverter);
            }
            // float:
            else if (elementConverter instanceof DataCellToFloatConverter) {
                return createToFloatConverter(elementConverter);
            }
            // byte:
            else if (elementConverter instanceof DataCellToByteConverter) {
                return createToByteConverter(elementConverter);
            }
            // short:
            else if (elementConverter instanceof DataCellToShortConverter) {
                return createToShortConverter(elementConverter);
            }
            // char:
            else if (elementConverter instanceof DataCellToCharConverter) {
                return createToCharConverter(elementConverter);
            }
        }
        // Otherwise it's an object type. / Fall-through.
        return createToObjectConverter(elementConverter);
    }

    private DataCellToJavaConverter<CollectionDataValue, D>
        createToDoubleConverter(final DataCellToJavaConverter<SE, DE> elementConverter) {
        @SuppressWarnings("unchecked")
        final DataCellToDoubleConverter<SE> converter = (DataCellToDoubleConverter<SE>)elementConverter;
        return source -> {
            final double[] array = new double[source.size()];
            int i = 0;
            for (final DataCell element : source) {
                if (element.isMissing()) {
                    // TODO: Primitive converters don't support missing values at the moment.
                    throw new IllegalStateException("Collection cell contains a missing value. This "
                        + "cannot be converted to a Java primitive type.");
                } else {
                    @SuppressWarnings("unchecked")
                    final SE elementValue = (SE)element;
                    array[i] = converter.convertIntoDouble(elementValue);
                }
                i++;
            }
            @SuppressWarnings("unchecked")
            final D destination = (D)array;
            return destination;
        };
    }

    private DataCellToJavaConverter<CollectionDataValue, D>
        createToIntConverter(final DataCellToJavaConverter<SE, DE> elementConverter) {
        @SuppressWarnings("unchecked")
        final DataCellToIntConverter<SE> converter = (DataCellToIntConverter<SE>)elementConverter;
        return source -> {
            final int[] array = new int[source.size()];
            int i = 0;
            for (final DataCell element : source) {
                if (element.isMissing()) {
                    // TODO: Primitive converters don't support missing values at the moment.
                    throw new IllegalStateException("Collection cell contains a missing value. This "
                        + "cannot be converted to a Java primitive type.");
                } else {
                    @SuppressWarnings("unchecked")
                    final SE elementValue = (SE)element;
                    array[i] = converter.convertIntoInt(elementValue);
                }
                i++;
            }
            @SuppressWarnings("unchecked")
            final D destination = (D)array;
            return destination;
        };
    }

    private DataCellToJavaConverter<CollectionDataValue, D>
        createToLongConverter(final DataCellToJavaConverter<SE, DE> elementConverter) {
        @SuppressWarnings("unchecked")
        final DataCellToLongConverter<SE> converter = (DataCellToLongConverter<SE>)elementConverter;
        return source -> {
            final long[] array = new long[source.size()];
            int i = 0;
            for (final DataCell element : source) {
                if (element.isMissing()) {
                    // TODO: Primitive converters don't support missing values at the moment.
                    throw new IllegalStateException("Collection cell contains a missing value. This "
                        + "cannot be converted to a Java primitive type.");
                } else {
                    @SuppressWarnings("unchecked")
                    final SE elementValue = (SE)element;
                    array[i] = converter.convertIntoLong(elementValue);
                }
                i++;
            }
            @SuppressWarnings("unchecked")
            final D destination = (D)array;
            return destination;
        };
    }

    private DataCellToJavaConverter<CollectionDataValue, D>
        createToBooleanConverter(final DataCellToJavaConverter<SE, DE> elementConverter) {
        @SuppressWarnings("unchecked")
        final DataCellToBooleanConverter<SE> converter = (DataCellToBooleanConverter<SE>)elementConverter;
        return source -> {
            final boolean[] array = new boolean[source.size()];
            int i = 0;
            for (final DataCell element : source) {
                if (element.isMissing()) {
                    // TODO: Primitive converters don't support missing values at the moment.
                    throw new IllegalStateException("Collection cell contains a missing value. This "
                        + "cannot be converted to a Java primitive type.");
                } else {
                    @SuppressWarnings("unchecked")
                    final SE elementValue = (SE)element;
                    array[i] = converter.convertIntoBoolean(elementValue);
                }
                i++;
            }
            @SuppressWarnings("unchecked")
            final D destination = (D)array;
            return destination;
        };
    }

    private DataCellToJavaConverter<CollectionDataValue, D>
        createToFloatConverter(final DataCellToJavaConverter<SE, DE> elementConverter) {
        @SuppressWarnings("unchecked")
        final DataCellToFloatConverter<SE> converter = (DataCellToFloatConverter<SE>)elementConverter;
        return source -> {
            final float[] array = new float[source.size()];
            int i = 0;
            for (final DataCell element : source) {
                if (element.isMissing()) {
                    // TODO: Primitive converters don't support missing values at the moment.
                    throw new IllegalStateException("Collection cell contains a missing value. This "
                        + "cannot be converted to a Java primitive type.");
                } else {
                    @SuppressWarnings("unchecked")
                    final SE elementValue = (SE)element;
                    array[i] = converter.convertIntoFloat(elementValue);
                }
                i++;
            }
            @SuppressWarnings("unchecked")
            final D destination = (D)array;
            return destination;
        };
    }

    private DataCellToJavaConverter<CollectionDataValue, D>
        createToByteConverter(final DataCellToJavaConverter<SE, DE> elementConverter) {
        @SuppressWarnings("unchecked")
        final DataCellToByteConverter<SE> converter = (DataCellToByteConverter<SE>)elementConverter;
        return source -> {
            final byte[] array = new byte[source.size()];
            int i = 0;
            for (final DataCell element : source) {
                if (element.isMissing()) {
                    // TODO: Primitive converters don't support missing values at the moment.
                    throw new IllegalStateException("Collection cell contains a missing value. This "
                        + "cannot be converted to a Java primitive type.");
                } else {
                    @SuppressWarnings("unchecked")
                    final SE elementValue = (SE)element;
                    array[i] = converter.convertIntoByte(elementValue);
                }
                i++;
            }
            @SuppressWarnings("unchecked")
            final D destination = (D)array;
            return destination;
        };
    }

    private DataCellToJavaConverter<CollectionDataValue, D>
        createToShortConverter(final DataCellToJavaConverter<SE, DE> elementConverter) {
        @SuppressWarnings("unchecked")
        final DataCellToShortConverter<SE> converter = (DataCellToShortConverter<SE>)elementConverter;
        return source -> {
            final short[] array = new short[source.size()];
            int i = 0;
            for (final DataCell element : source) {
                if (element.isMissing()) {
                    // TODO: Primitive converters don't support missing values at the moment.
                    throw new IllegalStateException("Collection cell contains a missing value. This "
                        + "cannot be converted to a Java primitive type.");
                } else {
                    @SuppressWarnings("unchecked")
                    final SE elementValue = (SE)element;
                    array[i] = converter.convertIntoShort(elementValue);
                }
                i++;
            }
            @SuppressWarnings("unchecked")
            final D destination = (D)array;
            return destination;
        };
    }

    private DataCellToJavaConverter<CollectionDataValue, D>
        createToCharConverter(final DataCellToJavaConverter<SE, DE> elementConverter) {
        @SuppressWarnings("unchecked")
        final DataCellToCharConverter<SE> converter = (DataCellToCharConverter<SE>)elementConverter;
        return source -> {
            final char[] array = new char[source.size()];
            int i = 0;
            for (final DataCell element : source) {
                if (element.isMissing()) {
                    // TODO: Primitive converters don't support missing values at the moment.
                    throw new IllegalStateException("Collection cell contains a missing value. This "
                        + "cannot be converted to a Java primitive type.");
                } else {
                    @SuppressWarnings("unchecked")
                    final SE elementValue = (SE)element;
                    array[i] = converter.convertIntoChar(elementValue);
                }
                i++;
            }
            @SuppressWarnings("unchecked")
            final D destination = (D)array;
            return destination;
        };
    }

    private DataCellToJavaConverter<CollectionDataValue, D>
        createToObjectConverter(final DataCellToJavaConverter<SE, DE> elementConverter) {
        return source -> {
            final Object array = Array.newInstance(m_destType.getComponentType(), source.size());
            int i = 0;
            for (final DataCell element : source) {
                // Need to handle missing values.
                if (element.isMissing()) {
                    Array.set(array, i, null);
                } else {
                    @SuppressWarnings("unchecked")
                    final SE elementValue = (SE)element;
                    Array.set(array, i, elementConverter.convert(elementValue));
                }
                i++;
            }
            @SuppressWarnings("unchecked")
            final D destination = (D)array;
            return destination;
        };
    }

    @Override
    public Class<CollectionDataValue> getSourceType() {
        return CollectionDataValue.class;
    }

    @Override
    public Class<D> getDestinationType() {
        return m_destType;
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_elementConverterFactory.getIdentifier() + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Array of " + m_elementConverterFactory.getName();
    }
}
