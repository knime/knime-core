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
 *   May 18, 2020 (marcel): created
 */
package org.knime.core.data.convert.map.experimental;

import org.knime.core.data.convert.map.BooleanCellValueConsumer;
import org.knime.core.data.convert.map.BooleanCellValueProducer;
import org.knime.core.data.convert.map.ByteCellValueConsumer;
import org.knime.core.data.convert.map.ByteCellValueProducer;
import org.knime.core.data.convert.map.CellValueConsumer;
import org.knime.core.data.convert.map.CellValueConsumerFactory;
import org.knime.core.data.convert.map.CellValueProducer;
import org.knime.core.data.convert.map.CellValueProducerFactory;
import org.knime.core.data.convert.map.CharCellValueConsumer;
import org.knime.core.data.convert.map.CharCellValueProducer;
import org.knime.core.data.convert.map.Destination;
import org.knime.core.data.convert.map.Destination.ConsumerParameters;
import org.knime.core.data.convert.map.DoubleCellValueConsumer;
import org.knime.core.data.convert.map.DoubleCellValueProducer;
import org.knime.core.data.convert.map.FloatCellValueConsumer;
import org.knime.core.data.convert.map.FloatCellValueProducer;
import org.knime.core.data.convert.map.IntCellValueConsumer;
import org.knime.core.data.convert.map.IntCellValueProducer;
import org.knime.core.data.convert.map.LongCellValueConsumer;
import org.knime.core.data.convert.map.LongCellValueProducer;
import org.knime.core.data.convert.map.ShortCellValueConsumer;
import org.knime.core.data.convert.map.ShortCellValueProducer;
import org.knime.core.data.convert.map.Source;
import org.knime.core.data.convert.map.Source.ProducerParameters;
import org.knime.core.data.convert.map.TypedCellValueConsumerFactory;
import org.knime.core.data.convert.map.TypedCellValueProducerFactory;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public final class Mappers {

    private Mappers() {}

    public static <T, //
            S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>> //
    Mapper<?, S, PP, ?, D, CP, ?> createMapper( //
        final CellValueProducerFactory<S, ?, T, PP> producerFactory,
        final CellValueConsumerFactory<D, T, ?, CP> consumerFactory) {
        Mapper<?, S, PP, ?, D, CP, ?> mapper = null;
        // Check if both value producer and consumer are applicable to the same primitive type.
        // This allows us to create a mapper that avoids autoboxing.
        if (canAvoidAutoboxing(producerFactory, consumerFactory)) {
            mapper = createPrimitiveMapper(producerFactory, consumerFactory);
        }
        if (mapper == null) {
            // Fall-through.
            final CellValueProducer<S, T, PP> producer = producerFactory.create();
            final CellValueConsumer<D, T, CP> consumer = consumerFactory.create();
            mapper = new ObjectMapper<>(producer, consumer);
        }
        return mapper;
    }

    private static <T, //
            S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>> //
    boolean canAvoidAutoboxing(final CellValueProducerFactory<S, ?, T, PP> producerFactory,
        final CellValueConsumerFactory<D, T, ?, CP> consumerFactory) {
        return producerFactory.getDestinationType().isPrimitive() //
            && producerFactory instanceof TypedCellValueProducerFactory //
            && consumerFactory.getSourceType().isPrimitive() //
            && consumerFactory instanceof TypedCellValueConsumerFactory;
    }

    @SuppressWarnings("unchecked") // Type safety is mostly ensured by instance checks.
    private static <T, //
            S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>> //
    Mapper<?, S, PP, ?, D, CP, ?> createPrimitiveMapper( //
        final CellValueProducerFactory<S, ?, T, PP> producerFactory,
        final CellValueConsumerFactory<D, T, ?, CP> consumerFactory) {
        final Class<?> producerType = ((TypedCellValueProducerFactory<?, ?, ?, ?, ?>)producerFactory).getProducerType();
        final Class<?> consumerType = ((TypedCellValueConsumerFactory<?, ?, ?, ?, ?>)consumerFactory).getConsumerType();
        // double:
        if (DoubleCellValueProducer.class.isAssignableFrom(producerType)
            && DoubleCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new DoubleMapper<>(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, DoubleCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, DoubleCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // int:
        else if (IntCellValueProducer.class.isAssignableFrom(producerType)
            && IntCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new IntMapper<>(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, IntCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, IntCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // long:
        else if (LongCellValueProducer.class.isAssignableFrom(producerType)
            && LongCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new LongMapper<>(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, LongCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, LongCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // boolean:
        else if (BooleanCellValueProducer.class.isAssignableFrom(producerType)
            && BooleanCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new BooleanMapper<>(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, BooleanCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, BooleanCellValueConsumer<D, CP>>)consumerFactory)
                    .create());
        }
        // float:
        else if (FloatCellValueProducer.class.isAssignableFrom(producerType)
            && FloatCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new FloatMapper<>(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, FloatCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, FloatCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // byte:
        else if (ByteCellValueProducer.class.isAssignableFrom(producerType)
            && ByteCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new ByteMapper<>(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, ByteCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, ByteCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // short:
        else if (ShortCellValueProducer.class.isAssignableFrom(producerType)
            && ShortCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new ShortMapper<>(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, ShortCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, ShortCellValueConsumer<D, CP>>)consumerFactory).create());
        }
        // char:
        else if (CharCellValueProducer.class.isAssignableFrom(producerType)
            && CharCellValueConsumer.class.isAssignableFrom(consumerType)) {
            return new CharMapper<>(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, CharCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedCellValueConsumerFactory<D, ?, ?, CP, CharCellValueConsumer<D, CP>>)consumerFactory).create());
        } else {
            // No primitive mapper matched: return null and let the caller create the default mapper.
            return null;
        }
    }

    // Mapper implementations:

    private static final class BooleanMapper<S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>>
        extends Mapper<Boolean, S, PP, BooleanCellValueProducer<S, PP>, D, CP, BooleanCellValueConsumer<D, CP>> {

        private BooleanMapper(final BooleanCellValueProducer<S, PP> producer,
            final BooleanCellValueConsumer<D, CP> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map(final S source, final PP producerParams, final D destination, final CP consumerParams)
            throws Exception {
            if (m_producer.producesMissingCellValue(source, producerParams)) {
                m_consumer.consumeMissingCellValue(destination, consumerParams);
            } else {
                final boolean value = m_producer.produceBooleanCellValue(source, producerParams);
                m_consumer.consumeBooleanCellValue(destination, value, consumerParams);
            }
        }
    }

    private static final class ByteMapper<S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>>
        extends Mapper<Byte, S, PP, ByteCellValueProducer<S, PP>, D, CP, ByteCellValueConsumer<D, CP>> {

        private ByteMapper(final ByteCellValueProducer<S, PP> producer, final ByteCellValueConsumer<D, CP> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map(final S source, final PP producerParams, final D destination, final CP consumerParams)
            throws Exception {
            if (m_producer.producesMissingCellValue(source, producerParams)) {
                m_consumer.consumeMissingCellValue(destination, consumerParams);
            } else {
                final byte value = m_producer.produceByteCellValue(source, producerParams);
                m_consumer.consumeByteCellValue(destination, value, consumerParams);
            }
        }
    }

    private static final class CharMapper<S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>>
        extends Mapper<Character, S, PP, CharCellValueProducer<S, PP>, D, CP, CharCellValueConsumer<D, CP>> {

        private CharMapper(final CharCellValueProducer<S, PP> producer, final CharCellValueConsumer<D, CP> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map(final S source, final PP producerParams, final D destination, final CP consumerParams)
            throws Exception {
            if (m_producer.producesMissingCellValue(source, producerParams)) {
                m_consumer.consumeMissingCellValue(destination, consumerParams);
            } else {
                final char value = m_producer.produceCharCellValue(source, producerParams);
                m_consumer.consumeCharCellValue(destination, value, consumerParams);
            }
        }
    }

    private static final class DoubleMapper<S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>>
        extends Mapper<Double, S, PP, DoubleCellValueProducer<S, PP>, D, CP, DoubleCellValueConsumer<D, CP>> {

        private DoubleMapper(final DoubleCellValueProducer<S, PP> producer,
            final DoubleCellValueConsumer<D, CP> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map(final S source, final PP producerParams, final D destination, final CP consumerParams)
            throws Exception {
            if (m_producer.producesMissingCellValue(source, producerParams)) {
                m_consumer.consumeMissingCellValue(destination, consumerParams);
            } else {
                final double value = m_producer.produceDoubleCellValue(source, producerParams);
                m_consumer.consumeDoubleCellValue(destination, value, consumerParams);
            }
        }
    }

    private static final class FloatMapper<S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>>
        extends Mapper<Float, S, PP, FloatCellValueProducer<S, PP>, D, CP, FloatCellValueConsumer<D, CP>> {

        private FloatMapper(final FloatCellValueProducer<S, PP> producer,
            final FloatCellValueConsumer<D, CP> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map(final S source, final PP producerParams, final D destination, final CP consumerParams)
            throws Exception {
            if (m_producer.producesMissingCellValue(source, producerParams)) {
                m_consumer.consumeMissingCellValue(destination, consumerParams);
            } else {
                final float value = m_producer.produceFloatCellValue(source, producerParams);
                m_consumer.consumeFloatCellValue(destination, value, consumerParams);
            }
        }
    }

    private static final class IntMapper<S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>>
        extends Mapper<Integer, S, PP, IntCellValueProducer<S, PP>, D, CP, IntCellValueConsumer<D, CP>> {

        private IntMapper(final IntCellValueProducer<S, PP> producer, final IntCellValueConsumer<D, CP> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map(final S source, final PP producerParams, final D destination, final CP consumerParams)
            throws Exception {
            if (m_producer.producesMissingCellValue(source, producerParams)) {
                m_consumer.consumeMissingCellValue(destination, consumerParams);
            } else {
                final int value = m_producer.produceIntCellValue(source, producerParams);
                m_consumer.consumeIntCellValue(destination, value, consumerParams);
            }
        }
    }

    private static final class LongMapper<S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>>
        extends Mapper<Long, S, PP, LongCellValueProducer<S, PP>, D, CP, LongCellValueConsumer<D, CP>> {

        private LongMapper(final LongCellValueProducer<S, PP> producer, final LongCellValueConsumer<D, CP> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map(final S source, final PP producerParams, final D destination, final CP consumerParams)
            throws Exception {
            if (m_producer.producesMissingCellValue(source, producerParams)) {
                m_consumer.consumeMissingCellValue(destination, consumerParams);
            } else {
                final long value = m_producer.produceLongCellValue(source, producerParams);
                m_consumer.consumeLongCellValue(destination, value, consumerParams);
            }
        }
    }

    private static final class ShortMapper<S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>>
        extends Mapper<Short, S, PP, ShortCellValueProducer<S, PP>, D, CP, ShortCellValueConsumer<D, CP>> {

        private ShortMapper(final ShortCellValueProducer<S, PP> producer,
            final ShortCellValueConsumer<D, CP> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map(final S source, final PP producerParams, final D destination, final CP consumerParams)
            throws Exception {
            if (m_producer.producesMissingCellValue(source, producerParams)) {
                m_consumer.consumeMissingCellValue(destination, consumerParams);
            } else {
                final short value = m_producer.produceShortCellValue(source, producerParams);
                m_consumer.consumeShortCellValue(destination, value, consumerParams);
            }
        }
    }

    private static final class ObjectMapper<T, //
            S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>> //
        extends Mapper<T, S, PP, CellValueProducer<S, T, PP>, D, CP, CellValueConsumer<D, T, CP>> {

        private ObjectMapper(final CellValueProducer<S, T, PP> producer, final CellValueConsumer<D, T, CP> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map(final S source, final PP producerParams, final D destination, final CP consumerParams)
            throws Exception {
            final T value = m_producer.produceCellValue(source, producerParams);
            m_consumer.consumeCellValue(destination, value, consumerParams);
        }
    }

    public abstract static class Mapper<T, //
            S extends Source<?>, //
            PP extends ProducerParameters<S>, //
            P extends CellValueProducer<S, T, PP>, //
            D extends Destination<?>, //
            CP extends ConsumerParameters<D>, //
            C extends CellValueConsumer<D, T, CP>> {

        protected final P m_producer;

        protected final C m_consumer;

        private Mapper(final P producer, final C consumer) {
            m_producer = producer;
            m_consumer = consumer;
        }

        public abstract void map(S source, PP producerParams, D destination, CP consumerParams) throws Exception;
    }
}
