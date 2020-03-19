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

    @SuppressWarnings("unchecked")
    private Mapper<CP, ?, ?> createMapper(final DataCellToJavaConverterFactory<?, ?> converterFactory,
        final CellValueConsumerFactory<?, ?, ?, ?> consumerFactory) {
        Mapper<CP, ?, ?> mapper = null;
        // Check if both value converter and consumer are applicable to the same primitive type.
        // This allows us to create a mapper that avoids autoboxing.
        if (canAvoidAutoboxing(converterFactory, consumerFactory)) {
            mapper = createPrimitiveMapper(converterFactory, consumerFactory);
        }
        if (mapper == null) {
            // Fall-through.
            final CellValueConsumer<D, Object, CP> consumer =
                (CellValueConsumer<D, Object, CP>)((CellValueConsumerFactory<D, ?, ?, CP>)consumerFactory).create();
            mapper = new ObjectMapper(converterFactory.create(), consumer);
        }
        return mapper;
    }

    @SuppressWarnings("unchecked")
    private Mapper<CP, ?, ?> createPrimitiveMapper(final DataCellToJavaConverterFactory<?, ?> converterFactory,
        final CellValueConsumerFactory<?, ?, ?, ?> consumerFactory) {
        final Class<?> converterType =
            ((TypedDataCellToJavaConverterFactory<?, ?, ?>)converterFactory).getConverterType();
        final Class<?> consumerType = ((TypedCellValueConsumerFactory<?, ?, ?, ?, ?>)consumerFactory).getConsumerType();
        // double:
        if (DataCellToDoubleConverter.class.isAssignableFrom(converterType)
            && DoubleCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new DoubleMapper(
                ((TypedDataCellToJavaConverterFactory<?, ?, DataCellToDoubleConverter<?>>)converterFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, DoubleCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // int:
        else if (DataCellToIntConverter.class.isAssignableFrom(converterType)
            && IntCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new IntMapper(
                ((TypedDataCellToJavaConverterFactory<?, ?, DataCellToIntConverter<?>>)converterFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, IntCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // long:
        else if (DataCellToLongConverter.class.isAssignableFrom(converterType)
            && LongCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new LongMapper(
                ((TypedDataCellToJavaConverterFactory<?, ?, DataCellToLongConverter<?>>)converterFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, LongCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // boolean:
        else if (DataCellToBooleanConverter.class.isAssignableFrom(converterType)
            && BooleanCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new BooleanMapper(
                ((TypedDataCellToJavaConverterFactory<?, ?, DataCellToBooleanConverter<?>>)converterFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, BooleanCellValueConsumer<D, CP>>)consumerFactory)
                    .create());
        }
        // float:
        else if (DataCellToFloatConverter.class.isAssignableFrom(converterType)
            && FloatCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new FloatMapper(
                ((TypedDataCellToJavaConverterFactory<?, ?, DataCellToFloatConverter<?>>)converterFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, FloatCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // byte:
        else if (DataCellToByteConverter.class.isAssignableFrom(converterType)
            && ByteCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new ByteMapper(
                ((TypedDataCellToJavaConverterFactory<?, ?, DataCellToByteConverter<?>>)converterFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, ByteCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // short:
        else if (DataCellToShortConverter.class.isAssignableFrom(converterType)
            && ShortCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new ShortMapper(
                ((TypedDataCellToJavaConverterFactory<?, ?, DataCellToShortConverter<?>>)converterFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, ShortCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // char:
        else if (DataCellToCharConverter.class.isAssignableFrom(converterType)
            && CharCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new CharMapper(
                ((TypedDataCellToJavaConverterFactory<?, ?, DataCellToCharConverter<?>>)converterFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, CharCellValueConsumer<D, CP>>)consumerFactory).create());
        } else {
            // no primitive mapper matches -> return null and let caller create a meaningful default
            return null;
        }
    }

    private static boolean canAvoidAutoboxing(final DataCellToJavaConverterFactory<?, ?> converterFactory,
        final CellValueConsumerFactory<?, ?, ?, ?> consumerFactory) {
        return converterFactory.getDestinationType().isPrimitive()
            && converterFactory instanceof TypedDataCellToJavaConverterFactory
            && consumerFactory.getSourceType().isPrimitive()
            && consumerFactory instanceof TypedCellValueConsumerFactory;
    }

    @Override
    public void consumeDataRow(final DataRow row, final CP[] params) throws Exception {
        for (int i = 0; i < m_mappers.length; i++) {
            m_mappers[i].map(row.getCell(i), params[i]);
        }
    }

    // Mapper implementations:

    @SuppressWarnings("rawtypes")
    private final class DoubleMapper extends Mapper<CP, DataCellToDoubleConverter, DoubleCellValueConsumer<D, CP>> {

        private DoubleMapper(final DataCellToDoubleConverter converter, final DoubleCellValueConsumer<D, CP> consumer) {
            super(converter, consumer);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            if (cell.isMissing()) {
                m_consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final double value = m_converter.convertIntoDouble(cell);
                m_consumer.consumeDoubleCellValue(m_destination, value, params);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private final class IntMapper extends Mapper<CP, DataCellToIntConverter, IntCellValueConsumer<D, CP>> {

        private IntMapper(final DataCellToIntConverter converter, final IntCellValueConsumer<D, CP> consumer) {
            super(converter, consumer);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            if (cell.isMissing()) {
                m_consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final int value = m_converter.convertIntoInt(cell);
                m_consumer.consumeIntCellValue(m_destination, value, params);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private final class LongMapper extends Mapper<CP, DataCellToLongConverter, LongCellValueConsumer<D, CP>> {

        private LongMapper(final DataCellToLongConverter<?> converter, final LongCellValueConsumer<D, CP> consumer) {
            super(converter, consumer);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            if (cell.isMissing()) {
                m_consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final long value = m_converter.convertIntoLong(cell);
                m_consumer.consumeLongCellValue(m_destination, value, params);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private final class BooleanMapper extends Mapper<CP, DataCellToBooleanConverter, BooleanCellValueConsumer<D, CP>> {

        private BooleanMapper(final DataCellToBooleanConverter converter,
            final BooleanCellValueConsumer<D, CP> consumer) {
            super(converter, consumer);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            if (cell.isMissing()) {
                m_consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final boolean value = m_converter.convertIntoBoolean(cell);
                m_consumer.consumeBooleanCellValue(m_destination, value, params);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private final class FloatMapper extends Mapper<CP, DataCellToFloatConverter, FloatCellValueConsumer<D, CP>> {

        private FloatMapper(final DataCellToFloatConverter converter, final FloatCellValueConsumer<D, CP> consumer) {
            super(converter, consumer);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            if (cell.isMissing()) {
                m_consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final float value = m_converter.convertIntoFloat(cell);
                m_consumer.consumeFloatCellValue(m_destination, value, params);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private final class ByteMapper extends Mapper<CP, DataCellToByteConverter, ByteCellValueConsumer<D, CP>> {

        private ByteMapper(final DataCellToByteConverter converter, final ByteCellValueConsumer<D, CP> consumer) {
            super(converter, consumer);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            if (cell.isMissing()) {
                m_consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final byte value = m_converter.convertIntoByte(cell);
                m_consumer.consumeByteCellValue(m_destination, value, params);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private final class ShortMapper extends Mapper<CP, DataCellToShortConverter, ShortCellValueConsumer<D, CP>> {

        private ShortMapper(final DataCellToShortConverter converter, final ShortCellValueConsumer<D, CP> consumer) {
            super(converter, consumer);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            if (cell.isMissing()) {
                m_consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final short value = m_converter.convertIntoShort(cell);
                m_consumer.consumeShortCellValue(m_destination, value, params);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private final class CharMapper extends Mapper<CP, DataCellToCharConverter, CharCellValueConsumer<D, CP>> {

        private CharMapper(final DataCellToCharConverter converter, final CharCellValueConsumer<D, CP> consumer) {
            super(converter, consumer);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            if (cell.isMissing()) {
                m_consumer.consumeMissingCellValue(m_destination, params);
            } else {
                @SuppressWarnings("unchecked")
                final char value = m_converter.convertIntoChar(cell);
                m_consumer.consumeCharCellValue(m_destination, value, params);
            }
        }
    }

    private final class ObjectMapper
        extends Mapper<CP, DataCellToJavaConverter<?, ?>, CellValueConsumer<D, Object, CP>> {

        private ObjectMapper(final DataCellToJavaConverter<?, ?> converter,
            final CellValueConsumer<D, Object, CP> consumer) {
            super(converter, consumer);
        }

        @Override
        protected void map(final DataCell cell, final CP params) throws Exception {
            final Object cellValue = cell.isMissing() ? null : m_converter.convertUnsafe(cell);
            m_consumer.consumeCellValue(m_destination, cellValue, params);
        }
    }

    private abstract static class Mapper<CP extends ConsumerParameters<?>, //
            P extends DataCellToJavaConverter<?, ?>, //
            C extends CellValueConsumer<?, ?, CP>> {

        protected final P m_converter;

        protected final C m_consumer;

        protected Mapper(final P converter, final C consumer) {
            m_converter = converter;
            m_consumer = consumer;
        }

        protected abstract void map(DataCell cell, final CP params) throws Exception;
    }
}
