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
 * History
 *   Oct 25, 2018 (marcel): created
 */
package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.convert.java.DataCellToBooleanConverter;
import org.knime.core.data.convert.java.DataCellToByteConverter;
import org.knime.core.data.convert.java.DataCellToCharConverter;
import org.knime.core.data.convert.java.DataCellToDoubleConverter;
import org.knime.core.data.convert.java.DataCellToFloatConverter;
import org.knime.core.data.convert.java.DataCellToIntConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToLongConverter;
import org.knime.core.data.convert.java.DataCellToShortConverter;
import org.knime.core.data.convert.java.TypedDataCellToJavaConverterFactory;
import org.knime.core.data.convert.map.Destination.ConsumerParameters;

/**
 * Default implementation of {@link DataRowConsumer} that writes data to a {@link Destination} using a set of
 * {@link ConsumptionPath consumption paths}.
 * <P>
 * Internally, each consumption path is translated to an executable mapper. There are mapper implementations for all
 * Java primitive types (to avoid autoboxing) as well as a common one for all object types.
 *
 * @param <D> Type of the {@link Destination} to which to write the data rows.
 * @param <CP> Subtype of {@link ConsumerParameters} that can be used to configure the consumers per call to
 *            {@link #consumeDataRow(DataRow, ConsumerParameters[])}.
 * @since 3.7
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultDataRowConsumer<D extends Destination<?>, CP extends ConsumerParameters<D>>
    implements DataRowConsumer<CP> {

    private final D m_destination;

    private final Mapper<CP, ?, ?>[] m_mappers;

    /**
     * Creates a new data row consumer for the given destination and the given mapping.
     *
     * @param destination The destination to which to write data rows.
     * @param mapping Consumption paths that describe the mapping from {@link DataCell data cells} to destination. The
     *            number and order of the passed paths must match the ones of the parameters and data cells later passed
     *            to {@link #consumeDataRow(DataRow, ConsumerParameters[])}.
     */
    public DefaultDataRowConsumer(final D destination, final ConsumptionPath[] mapping) {
        m_destination = destination;
        @SuppressWarnings("unchecked")
        final Mapper<CP, ?, ?>[] mappers = new Mapper[mapping.length];
        for (int i = 0; i < mapping.length; i++) {
            final ConsumptionPath path = mapping[i];
            final DataCellToJavaConverterFactory<?, ?> converterFactory = path.m_converterFactory;
            final CellValueConsumerFactory<?, ?, ?, ?> consumerFactory = path.m_consumerFactory;
            mappers[i] = createMapper(converterFactory, consumerFactory);
        }
        m_mappers = mappers;
    }

    private Mapper<CP, ?, ?> createMapper(final DataCellToJavaConverterFactory<?, ?> converterFactory,
        final CellValueConsumerFactory<?, ?, ?, ?> consumerFactory) {
        Mapper<CP, ?, ?> mapper = null;
        // Check if both value converter and consumer are applicable to the same primitive type.
        // This allows us to create a mapper that avoids autoboxing.
        if (converterFactory.getDestinationType().isPrimitive()
            && converterFactory instanceof TypedDataCellToJavaConverterFactory
            && consumerFactory.getSourceType().isPrimitive()
            && consumerFactory instanceof TypedCellValueConsumerFactory) {
            final Class<?> converterType =
                ((TypedDataCellToJavaConverterFactory<?, ?, ?>)converterFactory).getConverterType();
            final Class<?> consumerType =
                ((TypedCellValueConsumerFactory<?, ?, ?, ?, ?>)consumerFactory).getConsumerType();
            // double:
            if (DataCellToDoubleConverter.class.isAssignableFrom(converterType)
                && DoubleCellValueConsumer.class.isAssignableFrom(consumerType)) {
                mapper = new DoubleMapper(
                    (TypedDataCellToJavaConverterFactory<?, ?, DataCellToDoubleConverter<?>>)converterFactory,
                    (TypedCellValueConsumerFactory<D, ?, ?, CP, DoubleCellValueConsumer<D, CP>>)consumerFactory);
            }
            // int:
            else if (DataCellToIntConverter.class.isAssignableFrom(converterType)
                && IntCellValueConsumer.class.isAssignableFrom(consumerType)) {
                mapper = new IntMapper(
                    (TypedDataCellToJavaConverterFactory<?, ?, DataCellToIntConverter<?>>)converterFactory,
                    (TypedCellValueConsumerFactory<D, ?, ?, CP, IntCellValueConsumer<D, CP>>)consumerFactory);
            }
            // long:
            else if (DataCellToLongConverter.class.isAssignableFrom(converterType)
                && LongCellValueConsumer.class.isAssignableFrom(consumerType)) {
                mapper = new LongMapper(
                    (TypedDataCellToJavaConverterFactory<?, ?, DataCellToLongConverter<?>>)converterFactory,
                    (TypedCellValueConsumerFactory<D, ?, ?, CP, LongCellValueConsumer<D, CP>>)consumerFactory);
            }
            // boolean:
            else if (DataCellToBooleanConverter.class.isAssignableFrom(converterType)
                && BooleanCellValueConsumer.class.isAssignableFrom(consumerType)) {
                mapper = new BooleanMapper(
                    (TypedDataCellToJavaConverterFactory<?, ?, DataCellToBooleanConverter<?>>)converterFactory,
                    (TypedCellValueConsumerFactory<D, ?, ?, CP, BooleanCellValueConsumer<D, CP>>)consumerFactory);
            }
            // float:
            else if (DataCellToFloatConverter.class.isAssignableFrom(converterType)
                && FloatCellValueConsumer.class.isAssignableFrom(consumerType)) {
                mapper = new FloatMapper(
                    (TypedDataCellToJavaConverterFactory<?, ?, DataCellToFloatConverter<?>>)converterFactory,
                    (TypedCellValueConsumerFactory<D, ?, ?, CP, FloatCellValueConsumer<D, CP>>)consumerFactory);
            }
            // byte:
            else if (DataCellToByteConverter.class.isAssignableFrom(converterType)
                && ByteCellValueConsumer.class.isAssignableFrom(consumerType)) {
                mapper = new ByteMapper(
                    (TypedDataCellToJavaConverterFactory<?, ?, DataCellToByteConverter<?>>)converterFactory,
                    (TypedCellValueConsumerFactory<D, ?, ?, CP, ByteCellValueConsumer<D, CP>>)consumerFactory);
            }
            // short:
            else if (DataCellToShortConverter.class.isAssignableFrom(converterType)
                && ShortCellValueConsumer.class.isAssignableFrom(consumerType)) {
                mapper = new ShortMapper(
                    (TypedDataCellToJavaConverterFactory<?, ?, DataCellToShortConverter<?>>)converterFactory,
                    (TypedCellValueConsumerFactory<D, ?, ?, CP, ShortCellValueConsumer<D, CP>>)consumerFactory);
            }
            // char:
            else if (DataCellToCharConverter.class.isAssignableFrom(converterType)
                && CharCellValueConsumer.class.isAssignableFrom(consumerType)) {
                mapper = new CharMapper(
                    (TypedDataCellToJavaConverterFactory<?, ?, DataCellToCharConverter<?>>)converterFactory,
                    (TypedCellValueConsumerFactory<D, ?, ?, CP, CharCellValueConsumer<D, CP>>)consumerFactory);
            }
        }
        if (mapper == null) {
            // Fall-through.
            mapper = new ObjectMapper(converterFactory, (CellValueConsumerFactory<D, ?, ?, CP>)consumerFactory);
        }
        return mapper;
    }

    @Override
    public void consumeDataRow(final DataRow row, final CP[] params) throws Exception {
        for (int i = 0; i < m_mappers.length; i++) {
            m_mappers[i].map(row.getCell(i), params[i]);
        }
    }

    // Mapper implementations:

    private final class DoubleMapper extends Mapper<CP, //
            TypedDataCellToJavaConverterFactory<?, ?, DataCellToDoubleConverter<?>>, //
            TypedCellValueConsumerFactory<D, ?, ?, CP, DoubleCellValueConsumer<D, CP>>> {

        private DoubleMapper(
            final TypedDataCellToJavaConverterFactory<?, ?, DataCellToDoubleConverter<?>> converterFactory,
            final TypedCellValueConsumerFactory<D, ?, ?, CP, DoubleCellValueConsumer<D, CP>> consumerFactory) {
            super(converterFactory, consumerFactory);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            @SuppressWarnings("rawtypes")
            final DataCellToDoubleConverter converter = m_converterFactory.create();
            final DoubleCellValueConsumer<D, CP> consumer = m_consumerFactory.create();
            if (cell.isMissing()) {
                consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final double value = converter.convertIntoDouble(cell);
                consumer.consumeDoubleCellValue(m_destination, value, params);
            }
        }
    }

    private final class IntMapper extends Mapper<CP, //
            TypedDataCellToJavaConverterFactory<?, ?, DataCellToIntConverter<?>>, //
            TypedCellValueConsumerFactory<D, ?, ?, CP, IntCellValueConsumer<D, CP>>> {

        private IntMapper(final TypedDataCellToJavaConverterFactory<?, ?, DataCellToIntConverter<?>> converterFactory,
            final TypedCellValueConsumerFactory<D, ?, ?, CP, IntCellValueConsumer<D, CP>> consumerFactory) {
            super(converterFactory, consumerFactory);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            @SuppressWarnings("rawtypes")
            final DataCellToIntConverter converter = m_converterFactory.create();
            final IntCellValueConsumer<D, CP> consumer = m_consumerFactory.create();
            if (cell.isMissing()) {
                consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final int value = converter.convertIntoInt(cell);
                consumer.consumeIntCellValue(m_destination, value, params);
            }
        }
    }

    private final class LongMapper extends Mapper<CP, //
            TypedDataCellToJavaConverterFactory<?, ?, DataCellToLongConverter<?>>, //
            TypedCellValueConsumerFactory<D, ?, ?, CP, LongCellValueConsumer<D, CP>>> {

        private LongMapper(final TypedDataCellToJavaConverterFactory<?, ?, DataCellToLongConverter<?>> converterFactory,
            final TypedCellValueConsumerFactory<D, ?, ?, CP, LongCellValueConsumer<D, CP>> consumerFactory) {
            super(converterFactory, consumerFactory);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            @SuppressWarnings("rawtypes")
            final DataCellToLongConverter converter = m_converterFactory.create();
            final LongCellValueConsumer<D, CP> consumer = m_consumerFactory.create();
            if (cell.isMissing()) {
                consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final long value = converter.convertIntoLong(cell);
                consumer.consumeLongCellValue(m_destination, value, params);
            }
        }
    }

    private final class BooleanMapper extends Mapper<CP, //
            TypedDataCellToJavaConverterFactory<?, ?, DataCellToBooleanConverter<?>>, //
            TypedCellValueConsumerFactory<D, ?, ?, CP, BooleanCellValueConsumer<D, CP>>> {

        private BooleanMapper(
            final TypedDataCellToJavaConverterFactory<?, ?, DataCellToBooleanConverter<?>> converterFactory,
            final TypedCellValueConsumerFactory<D, ?, ?, CP, BooleanCellValueConsumer<D, CP>> consumerFactory) {
            super(converterFactory, consumerFactory);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            @SuppressWarnings("rawtypes")
            final DataCellToBooleanConverter converter = m_converterFactory.create();
            final BooleanCellValueConsumer<D, CP> consumer = m_consumerFactory.create();
            if (cell.isMissing()) {
                consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final boolean value = converter.convertIntoBoolean(cell);
                consumer.consumeBooleanCellValue(m_destination, value, params);
            }
        }
    }

    private final class FloatMapper extends Mapper<CP, //
            TypedDataCellToJavaConverterFactory<?, ?, DataCellToFloatConverter<?>>, //
            TypedCellValueConsumerFactory<D, ?, ?, CP, FloatCellValueConsumer<D, CP>>> {

        private FloatMapper(
            final TypedDataCellToJavaConverterFactory<?, ?, DataCellToFloatConverter<?>> converterFactory,
            final TypedCellValueConsumerFactory<D, ?, ?, CP, FloatCellValueConsumer<D, CP>> consumerFactory) {
            super(converterFactory, consumerFactory);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            @SuppressWarnings("rawtypes")
            final DataCellToFloatConverter converter = m_converterFactory.create();
            final FloatCellValueConsumer<D, CP> consumer = m_consumerFactory.create();
            if (cell.isMissing()) {
                consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final float value = converter.convertIntoFloat(cell);
                consumer.consumeFloatCellValue(m_destination, value, params);
            }
        }
    }

    private final class ByteMapper extends Mapper<CP, //
            TypedDataCellToJavaConverterFactory<?, ?, DataCellToByteConverter<?>>, //
            TypedCellValueConsumerFactory<D, ?, ?, CP, ByteCellValueConsumer<D, CP>>> {

        private ByteMapper(final TypedDataCellToJavaConverterFactory<?, ?, DataCellToByteConverter<?>> converterFactory,
            final TypedCellValueConsumerFactory<D, ?, ?, CP, ByteCellValueConsumer<D, CP>> consumerFactory) {
            super(converterFactory, consumerFactory);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            @SuppressWarnings("rawtypes")
            final DataCellToByteConverter converter = m_converterFactory.create();
            final ByteCellValueConsumer<D, CP> consumer = m_consumerFactory.create();
            if (cell.isMissing()) {
                consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final byte value = converter.convertIntoByte(cell);
                consumer.consumeByteCellValue(m_destination, value, params);
            }
        }
    }

    private final class ShortMapper extends Mapper<CP, //
            TypedDataCellToJavaConverterFactory<?, ?, DataCellToShortConverter<?>>, //
            TypedCellValueConsumerFactory<D, ?, ?, CP, ShortCellValueConsumer<D, CP>>> {

        private ShortMapper(
            final TypedDataCellToJavaConverterFactory<?, ?, DataCellToShortConverter<?>> converterFactory,
            final TypedCellValueConsumerFactory<D, ?, ?, CP, ShortCellValueConsumer<D, CP>> consumerFactory) {
            super(converterFactory, consumerFactory);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            @SuppressWarnings("rawtypes")
            final DataCellToShortConverter converter = m_converterFactory.create();
            final ShortCellValueConsumer<D, CP> consumer = m_consumerFactory.create();
            if (cell.isMissing()) {
                consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final short value = converter.convertIntoShort(cell);
                consumer.consumeShortCellValue(m_destination, value, params);
            }
        }
    }

    private final class CharMapper extends Mapper<CP, //
            TypedDataCellToJavaConverterFactory<?, ?, DataCellToCharConverter<?>>, //
            TypedCellValueConsumerFactory<D, ?, ?, CP, CharCellValueConsumer<D, CP>>> {

        private CharMapper(final TypedDataCellToJavaConverterFactory<?, ?, DataCellToCharConverter<?>> converterFactory,
            final TypedCellValueConsumerFactory<D, ?, ?, CP, CharCellValueConsumer<D, CP>> consumerFactory) {
            super(converterFactory, consumerFactory);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            @SuppressWarnings("rawtypes")
            final DataCellToCharConverter converter = m_converterFactory.create();
            final CharCellValueConsumer<D, CP> consumer = m_consumerFactory.create();
            if (cell.isMissing()) {
                consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final char value = converter.convertIntoChar(cell);
                consumer.consumeCharCellValue(m_destination, value, params);
            }
        }
    }

    private final class ObjectMapper extends Mapper<CP, //
            DataCellToJavaConverterFactory<?, ?>, //
            CellValueConsumerFactory<D, ?, ?, CP>> {

        private ObjectMapper(final DataCellToJavaConverterFactory<?, ?> converterFactory,
            final CellValueConsumerFactory<D, ?, ?, CP> consumerFactory) {
            super(converterFactory, consumerFactory);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            final DataCellToJavaConverter<?, ?> converter = m_converterFactory.create();
            @SuppressWarnings("unchecked")
            final CellValueConsumer<D, Object, CP> consumer =
                (CellValueConsumer<D, Object, CP>)m_consumerFactory.create();
            final Object cellValue = cell.isMissing() ? null : converter.convertUnsafe(cell);
            consumer.consumeCellValue(m_destination, cellValue, params);
        }
    }

    private abstract static class Mapper<CP extends ConsumerParameters<?>, //
            DC extends DataCellToJavaConverterFactory<?, ?>, //
            CC extends CellValueConsumerFactory<?, ?, ?, ?>> {

        protected final DC m_converterFactory;

        protected final CC m_consumerFactory;

        private Mapper(final DC converterFactory, final CC consumerFactory) {
            m_converterFactory = converterFactory;
            m_consumerFactory = consumerFactory;
        }

        protected abstract void map(DataCell cell, final CP params) throws Exception;
    }
}
