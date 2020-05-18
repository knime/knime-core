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

import org.knime.core.data.convert.map.MappingException;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public final class Mappers {

    private Mappers() {}

    public static <T> Mapper createMapper(final CellValueProducerNoSource<T> producer,
        final CellValueConsumerNoDestination<T> consumer) {
        Mapper mapper = null;
        // Check if both value producer and consumer are applicable to the same primitive type.
        // This allows us to create a mapper that avoids autoboxing.
        if (canAvoidAutoboxing(producer, consumer)) {
            mapper = createPrimitiveMapper(producer, consumer);
        }
        if (mapper == null) {
            // Fall-through.
            mapper = new ObjectMapper<>(producer, consumer);
        }
        return mapper;
    }

    private static <T> boolean canAvoidAutoboxing(final CellValueProducerNoSource<T> producer,
        final CellValueConsumerNoDestination<T> consumer) {
        return producer instanceof PrimitiveCellValueProducerNoSource //
            && consumer instanceof PrimitiveCellValueConsumerNoDestination;
    }

    private static <T> Mapper createPrimitiveMapper(final CellValueProducerNoSource<T> producer,
        final CellValueConsumerNoDestination<T> consumer) {
        // double:
        if (producer instanceof DoubleCellValueProducerNoSource
            && consumer instanceof DoubleCellValueConsumerNoDestination) {
            return new DoubleMapper((DoubleCellValueProducerNoSource)producer,
                (DoubleCellValueConsumerNoDestination)consumer);
        }
        // int:
        else if (producer instanceof IntCellValueProducerNoSource
            && consumer instanceof IntCellValueConsumerNoDestination) {
            return new IntMapper((IntCellValueProducerNoSource)producer, (IntCellValueConsumerNoDestination)consumer);
        }
        // long:
        else if (producer instanceof LongCellValueProducerNoSource
            && consumer instanceof LongCellValueConsumerNoDestination) {
            return new LongMapper((LongCellValueProducerNoSource)producer,
                (LongCellValueConsumerNoDestination)consumer);
        }
        // boolean:
        else if (producer instanceof BooleanCellValueProducerNoSource
            && consumer instanceof BooleanCellValueConsumerNoDestination) {
            return new BooleanMapper((BooleanCellValueProducerNoSource)producer,
                (BooleanCellValueConsumerNoDestination)consumer);
        }
        // float:
        else if (producer instanceof FloatCellValueProducerNoSource
            && consumer instanceof FloatCellValueConsumerNoDestination) {
            return new FloatMapper((FloatCellValueProducerNoSource)producer,
                (FloatCellValueConsumerNoDestination)consumer);
        }
        // byte:
        else if (producer instanceof ByteCellValueProducerNoSource
            && consumer instanceof ByteCellValueConsumerNoDestination) {
            return new ByteMapper((ByteCellValueProducerNoSource)producer,
                (ByteCellValueConsumerNoDestination)consumer);
        }
        // short:
        else if (producer instanceof ShortCellValueProducerNoSource
            && consumer instanceof ShortCellValueConsumerNoDestination) {
            return new ShortMapper((ShortCellValueProducerNoSource)producer,
                (ShortCellValueConsumerNoDestination)consumer);
        }
        // char:
        else if (producer instanceof CharCellValueProducerNoSource
            && consumer instanceof CharCellValueConsumerNoDestination) {
            return new CharMapper((CharCellValueProducerNoSource)producer,
                (CharCellValueConsumerNoDestination)consumer);
        }
        // No primitive mapper matched: return null and let the caller create the default mapper.
        return null;
    }

    // Mapper implementations:

    private static final class BooleanMapper
        extends AbstractMapper<BooleanCellValueProducerNoSource, BooleanCellValueConsumerNoDestination> {

        private BooleanMapper(final BooleanCellValueProducerNoSource producer,
            final BooleanCellValueConsumerNoDestination consumer) {
            super(producer, consumer);
        }

        @Override
        public void map() throws MappingException {
            if (m_producer.producesMissingCellValue()) {
                m_consumer.consumeMissingCellValue();
            } else {
                m_consumer.consumeBooleanCellValue(m_producer.produceBooleanCellValue());
            }
        }
    }

    private static final class ByteMapper
        extends AbstractMapper<ByteCellValueProducerNoSource, ByteCellValueConsumerNoDestination> {

        private ByteMapper(final ByteCellValueProducerNoSource producer,
            final ByteCellValueConsumerNoDestination consumer) {
            super(producer, consumer);
        }

        @Override
        public void map() throws MappingException {
            if (m_producer.producesMissingCellValue()) {
                m_consumer.consumeMissingCellValue();
            } else {
                m_consumer.consumeByteCellValue(m_producer.produceByteCellValue());
            }
        }
    }

    private static final class CharMapper
        extends AbstractMapper<CharCellValueProducerNoSource, CharCellValueConsumerNoDestination> {

        private CharMapper(final CharCellValueProducerNoSource producer,
            final CharCellValueConsumerNoDestination consumer) {
            super(producer, consumer);
        }

        @Override
        public void map() throws MappingException {
            if (m_producer.producesMissingCellValue()) {
                m_consumer.consumeMissingCellValue();
            } else {
                m_consumer.consumeCharCellValue(m_producer.produceCharCellValue());
            }
        }
    }

    private static final class DoubleMapper
        extends AbstractMapper<DoubleCellValueProducerNoSource, DoubleCellValueConsumerNoDestination> {

        private DoubleMapper(final DoubleCellValueProducerNoSource producer,
            final DoubleCellValueConsumerNoDestination consumer) {
            super(producer, consumer);
        }

        @Override
        public void map() throws MappingException {
            if (m_producer.producesMissingCellValue()) {
                m_consumer.consumeMissingCellValue();
            } else {
                m_consumer.consumeDoubleCellValue(m_producer.produceDoubleCellValue());
            }
        }
    }

    private static final class FloatMapper
        extends AbstractMapper<FloatCellValueProducerNoSource, FloatCellValueConsumerNoDestination> {

        private FloatMapper(final FloatCellValueProducerNoSource producer,
            final FloatCellValueConsumerNoDestination consumer) {
            super(producer, consumer);
        }

        @Override
        public void map() throws MappingException {
            if (m_producer.producesMissingCellValue()) {
                m_consumer.consumeMissingCellValue();
            } else {
                m_consumer.consumeFloatCellValue(m_producer.produceFloatCellValue());
            }
        }
    }

    private static final class IntMapper
        extends AbstractMapper<IntCellValueProducerNoSource, IntCellValueConsumerNoDestination> {

        private IntMapper(final IntCellValueProducerNoSource producer,
            final IntCellValueConsumerNoDestination consumer) {
            super(producer, consumer);
        }

        @Override
        public void map() throws MappingException {
            if (m_producer.producesMissingCellValue()) {
                m_consumer.consumeMissingCellValue();
            } else {
                m_consumer.consumeIntCellValue(m_producer.produceIntCellValue());
            }
        }
    }

    private static final class LongMapper
        extends AbstractMapper<LongCellValueProducerNoSource, LongCellValueConsumerNoDestination> {

        private LongMapper(final LongCellValueProducerNoSource producer,
            final LongCellValueConsumerNoDestination consumer) {
            super(producer, consumer);
        }

        @Override
        public void map() throws MappingException {
            if (m_producer.producesMissingCellValue()) {
                m_consumer.consumeMissingCellValue();
            } else {
                m_consumer.consumeLongCellValue(m_producer.produceLongCellValue());
            }
        }
    }

    private static final class ShortMapper
        extends AbstractMapper<ShortCellValueProducerNoSource, ShortCellValueConsumerNoDestination> {

        private ShortMapper(final ShortCellValueProducerNoSource producer,
            final ShortCellValueConsumerNoDestination consumer) {
            super(producer, consumer);
        }

        @Override
        public void map() throws MappingException {
            if (m_producer.producesMissingCellValue()) {
                m_consumer.consumeMissingCellValue();
            } else {
                m_consumer.consumeShortCellValue(m_producer.produceShortCellValue());
            }
        }
    }

    private static final class ObjectMapper<T>
        extends AbstractMapper<CellValueProducerNoSource<T>, CellValueConsumerNoDestination<T>> {

        private ObjectMapper(final CellValueProducerNoSource<T> producer,
            final CellValueConsumerNoDestination<T> consumer) {
            super(producer, consumer);
        }

        @Override
        public void map() throws MappingException {
            m_consumer.consumeCellValue(m_producer.produceCellValue());
        }
    }

    private abstract static class AbstractMapper<P, C> implements Mapper {

        protected final P m_producer;

        protected final C m_consumer;

        private AbstractMapper(final P producer, final C consumer) {
            m_producer = producer;
            m_consumer = consumer;
        }
    }

    public interface Mapper {

        void map() throws MappingException;
    }
}
