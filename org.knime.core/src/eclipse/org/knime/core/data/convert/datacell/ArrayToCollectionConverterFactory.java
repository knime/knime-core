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

package org.knime.core.data.convert.datacell;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.node.ExecutionContext;

/**
 * {@link JavaToDataCellConverterFactory} for converting arrays of a type to a collection of a data cell type by
 * converting each element using a given converter.
 *
 * @author Jonathan Hale
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 *
 * @param <S> {@link JavaToDataCellConverter} subclass which can be created by this factory.
 * @param <SE> Element factory type
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class ArrayToCollectionConverterFactory<S, SE> implements JavaToDataCellConverterFactory<S> {

    private final JavaToDataCellConverterFactory<SE> m_elementFactory;

    /**
     * @param elementFactory Factory to convert the components of the input array into components of the output array
     */
    ArrayToCollectionConverterFactory(final JavaToDataCellConverterFactory<SE> elementFactory) {
        m_elementFactory = elementFactory;
    }

    @Override
    public JavaToDataCellConverter<S> create(final ExecutionContext context) {
        final JavaToDataCellConverter<SE> elementConverter = m_elementFactory.create(context);
        if (m_elementFactory.getSourceType().isPrimitive()) {
            // Implement for each primitive type explicitly. That's the most performant way.
            // double:
            if (elementConverter instanceof DoubleToDataCellConverter) {
                return createDoubleConverter(elementConverter);
            }
            // int:
            else if (elementConverter instanceof IntToDataCellConverter) {
                return createIntConverter(elementConverter);
            }
            // long:
            else if (elementConverter instanceof LongToDataCellConverter) {
                return createLongConverter(elementConverter);
            }
            // boolean:
            else if (elementConverter instanceof BooleanToDataCellConverter) {
                return createBooleanConverter(elementConverter);
            }
            // float:
            else if (elementConverter instanceof FloatToDataCellConverter) {
                return createFloatConverter(elementConverter);
            }
            // byte:
            else if (elementConverter instanceof ByteToDataCellConverter) {
                return createByteConverter(elementConverter);
            }
            // short:
            else if (elementConverter instanceof ShortToDataCellConverter) {
                return createShortConverter(elementConverter);
            }
            // char:
            else if (elementConverter instanceof CharToDataCellConverter) {
                return createCharConverter(elementConverter);
            }
        }
        // Otherwise it's an object type. / Fall-through.
        return createObjectConverter(elementConverter);
    }

    private JavaToDataCellConverter<S> createDoubleConverter(final JavaToDataCellConverter<SE> elementConverter) {
        return new AbstractArrayToCollectionConverter<S, DoubleToDataCellConverter>(
            (DoubleToDataCellConverter)elementConverter) {

            @Override
            protected DataCell[] convertSourceArrayToDataCellArray(final Object sourceArray) throws Exception {
                final double[] array = (double[])sourceArray;
                final DataCell[] dataCellArray = new DataCell[array.length];
                for (int i = 0; i < array.length; i++) {
                    dataCellArray[i] = m_elementConverter.convertDouble(array[i]);
                }
                return dataCellArray;
            }
        };
    }

    private JavaToDataCellConverter<S> createIntConverter(final JavaToDataCellConverter<SE> elementConverter) {
        return new AbstractArrayToCollectionConverter<S, IntToDataCellConverter>(
            (IntToDataCellConverter)elementConverter) {

            @Override
            protected DataCell[] convertSourceArrayToDataCellArray(final Object sourceArray) throws Exception {
                final int[] array = (int[])sourceArray;
                final DataCell[] dataCellArray = new DataCell[array.length];
                for (int i = 0; i < array.length; i++) {
                    dataCellArray[i] = m_elementConverter.convertInt(array[i]);
                }
                return dataCellArray;
            }
        };
    }

    private JavaToDataCellConverter<S> createLongConverter(final JavaToDataCellConverter<SE> elementConverter) {
        return new AbstractArrayToCollectionConverter<S, LongToDataCellConverter>(
            (LongToDataCellConverter)elementConverter) {

            @Override
            protected DataCell[] convertSourceArrayToDataCellArray(final Object sourceArray) throws Exception {
                final long[] array = (long[])sourceArray;
                final DataCell[] dataCellArray = new DataCell[array.length];
                for (int i = 0; i < array.length; i++) {
                    dataCellArray[i] = m_elementConverter.convertLong(array[i]);
                }
                return dataCellArray;
            }
        };
    }

    private JavaToDataCellConverter<S> createBooleanConverter(final JavaToDataCellConverter<SE> elementConverter) {
        return new AbstractArrayToCollectionConverter<S, BooleanToDataCellConverter>(
            (BooleanToDataCellConverter)elementConverter) {

            @Override
            protected DataCell[] convertSourceArrayToDataCellArray(final Object sourceArray) throws Exception {
                final boolean[] array = (boolean[])sourceArray;
                final DataCell[] dataCellArray = new DataCell[array.length];
                for (int i = 0; i < array.length; i++) {
                    dataCellArray[i] = m_elementConverter.convertBoolean(array[i]);
                }
                return dataCellArray;
            }
        };
    }

    private JavaToDataCellConverter<S> createFloatConverter(final JavaToDataCellConverter<SE> elementConverter) {
        return new AbstractArrayToCollectionConverter<S, FloatToDataCellConverter>(
            (FloatToDataCellConverter)elementConverter) {

            @Override
            protected DataCell[] convertSourceArrayToDataCellArray(final Object sourceArray) throws Exception {
                final float[] array = (float[])sourceArray;
                final DataCell[] dataCellArray = new DataCell[array.length];
                for (int i = 0; i < array.length; i++) {
                    dataCellArray[i] = m_elementConverter.convertFloat(array[i]);
                }
                return dataCellArray;
            }
        };
    }

    private JavaToDataCellConverter<S> createByteConverter(final JavaToDataCellConverter<SE> elementConverter) {
        return new AbstractArrayToCollectionConverter<S, ByteToDataCellConverter>(
            (ByteToDataCellConverter)elementConverter) {

            @Override
            protected DataCell[] convertSourceArrayToDataCellArray(final Object sourceArray) throws Exception {
                final byte[] array = (byte[])sourceArray;
                final DataCell[] dataCellArray = new DataCell[array.length];
                for (int i = 0; i < array.length; i++) {
                    dataCellArray[i] = m_elementConverter.convertByte(array[i]);
                }
                return dataCellArray;
            }
        };
    }

    private JavaToDataCellConverter<S> createShortConverter(final JavaToDataCellConverter<SE> elementConverter) {
        return new AbstractArrayToCollectionConverter<S, ShortToDataCellConverter>(
            (ShortToDataCellConverter)elementConverter) {

            @Override
            protected DataCell[] convertSourceArrayToDataCellArray(final Object sourceArray) throws Exception {
                final short[] array = (short[])sourceArray;
                final DataCell[] dataCellArray = new DataCell[array.length];
                for (int i = 0; i < array.length; i++) {
                    dataCellArray[i] = m_elementConverter.convertShort(array[i]);
                }
                return dataCellArray;
            }
        };
    }

    private JavaToDataCellConverter<S> createCharConverter(final JavaToDataCellConverter<SE> elementConverter) {
        return new AbstractArrayToCollectionConverter<S, CharToDataCellConverter>(
            (CharToDataCellConverter)elementConverter) {

            @Override
            protected DataCell[] convertSourceArrayToDataCellArray(final Object sourceArray) throws Exception {
                final char[] array = (char[])sourceArray;
                final DataCell[] dataCellArray = new DataCell[array.length];
                for (int i = 0; i < array.length; i++) {
                    dataCellArray[i] = m_elementConverter.convertChar(array[i]);
                }
                return dataCellArray;
            }
        };
    }

    private JavaToDataCellConverter<S> createObjectConverter(final JavaToDataCellConverter<SE> elementConverter) {
        return new AbstractArrayToCollectionConverter<S, JavaToDataCellConverter<SE>>(elementConverter) {

            @Override
            protected DataCell[] convertSourceArrayToDataCellArray(final Object sourceArray) throws Exception {
                @SuppressWarnings("unchecked")
                final SE[] array = (SE[])sourceArray;
                final DataCell[] dataCellArray = new DataCell[array.length];
                for (int i = 0; i < array.length; i++) {
                    dataCellArray[i] = m_elementConverter.convert(array[i]);
                }
                return dataCellArray;
            }
        };
    }

    @Override
    public Class<S> getSourceType() {
        return (Class<S>)Array.newInstance(m_elementFactory.getSourceType(), 0).getClass();
    }

    @Override
    public DataType getDestinationType() {
        return ListCell.getCollectionType(m_elementFactory.getDestinationType());
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_elementFactory.getIdentifier() + ")";
    }

    @Override
    public String getName() {
        return "Array of " + m_elementFactory.getName();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ArrayToCollectionConverterFactory<?, ?> other = (ArrayToCollectionConverterFactory<?, ?>)obj;
        return other.m_elementFactory.equals(m_elementFactory);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_elementFactory == null) ? 0 : m_elementFactory.hashCode());
        return result;
    }

    private abstract static class AbstractArrayToCollectionConverter<S, EC extends JavaToDataCellConverter<?>>
        implements JavaToDataCellConverter<S> {

        final EC m_elementConverter;

        public AbstractArrayToCollectionConverter(final EC elementConverter) {
            m_elementConverter = elementConverter;
        }

        protected abstract DataCell[] convertSourceArrayToDataCellArray(Object sourceArray) throws Exception;

        @Override
        public DataCell convert(final S source) throws Exception {
            if (source == null) {
                return new MissingCell("Value was null.");
            }
            return CollectionCellFactory.createListCell(Arrays.asList(convertSourceArrayToDataCellArray(source)));
        }
    }
}
