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
 *   Oct 24, 2018 (marcel): created
 */
package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.convert.datacell.BooleanToDataCellConverter;
import org.knime.core.data.convert.datacell.ByteToDataCellConverter;
import org.knime.core.data.convert.datacell.CharToDataCellConverter;
import org.knime.core.data.convert.datacell.DoubleToDataCellConverter;
import org.knime.core.data.convert.datacell.FloatToDataCellConverter;
import org.knime.core.data.convert.datacell.IntToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.LongToDataCellConverter;
import org.knime.core.data.convert.datacell.ShortToDataCellConverter;
import org.knime.core.data.convert.datacell.TypedJavaToDataCellConverterFactory;
import org.knime.core.data.convert.map.Source.ProducerParameters;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionContext;

/**
 * Default implementation of {@link DataRowProducer} that creates data rows out of a {@link Source} using a set of
 * {@link ProductionPath production paths}.
 * <P>
 * Internally, each production path is translated to an executable mapper. There are mapper implementations for all Java
 * primitive types (to avoid autoboxing) as well as a common one for all object types.
 *
 * @param <S> Type of the {@link Source} from which to create data rows.
 * @param <PP> Subtype of {@link Source.ProducerParameters} that can be used to configure the producers per call to
 *            {@link #produceDataRow(RowKey, ProducerParameters[])}.
 * @since 3.7
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultDataRowProducer<S extends Source<?>, PP extends ProducerParameters<S>>
    implements DataRowProducer<PP> {

    private final S m_source;

    private final ExecutionContext m_executionContext;

    private final Mapper<PP, ?, ?>[] m_mappers;

    private final DataCell[] m_tempCells;

    /**
     * Creates a new data row producer for the given source and the given mapping.
     *
     * @param source The source from which to create data rows.
     * @param mapping Production paths that describe the mapping from source to {@link DataCell data cells}. The number
     *            and order of the passed paths must match the ones of the parameters later passed to
     *            {@link #produceDataRow(RowKey, ProducerParameters[])}.
     * @param exec Execution context. Potentially required to create converters. May be {@code null} if it is known that
     *            none of the converter factories in {@code mapping} require an execution context.
     */
    public DefaultDataRowProducer(final S source, final ProductionPath[] mapping, final ExecutionContext exec) {
        m_source = source;
        m_executionContext = exec;
        m_tempCells = new DataCell[mapping.length];
        @SuppressWarnings("unchecked")
        final Mapper<PP, ?, ?>[] mappers = new Mapper[mapping.length];
        for (int i = 0; i < mapping.length; i++) {
            final ProductionPath path = mapping[i];
            final CellValueProducerFactory<?, ?, ?, ?> producerFactory = path.m_producerFactory;
            final JavaToDataCellConverterFactory<?> converterFactory = path.m_converterFactory;
            mappers[i] = createMapper(producerFactory, converterFactory);
        }
        m_mappers = mappers;
    }

    @SuppressWarnings("unchecked") // Type safety is mostly ensured by instance checks.
    private Mapper<PP, ?, ?> createMapper(final CellValueProducerFactory<?, ?, ?, ?> producerFactory,
        final JavaToDataCellConverterFactory<?> converterFactory) {
        Mapper<PP, ?, ?> mapper = null;
        // Check if both value producer and converter are applicable to the same primitive type.
        // This allows us to create a mapper that avoids autoboxing.
        if (producerFactory.getDestinationType().isPrimitive()
            && producerFactory instanceof TypedCellValueProducerFactory
            && converterFactory.getSourceType().isPrimitive()
            && converterFactory instanceof TypedJavaToDataCellConverterFactory) {
            final Class<?> producerType =
                ((TypedCellValueProducerFactory<?, ?, ?, ?, ?>)producerFactory).getProducerType();
            final Class<?> converterType =
                ((TypedJavaToDataCellConverterFactory<?, ?>)converterFactory).getConverterType();
            // double:
            if (DoubleCellValueProducer.class.isAssignableFrom(producerType)
                && DoubleToDataCellConverter.class.isAssignableFrom(converterType)) {
                mapper = new DoubleMapper(
                    (TypedCellValueProducerFactory<S, ?, ?, PP, DoubleCellValueProducer<S, PP>>)producerFactory,
                    (TypedJavaToDataCellConverterFactory<?, DoubleToDataCellConverter>)converterFactory);
            }
            // int:
            else if (IntCellValueProducer.class.isAssignableFrom(producerType)
                && IntToDataCellConverter.class.isAssignableFrom(converterType)) {
                mapper = new IntMapper(
                    (TypedCellValueProducerFactory<S, ?, ?, PP, IntCellValueProducer<S, PP>>)producerFactory,
                    (TypedJavaToDataCellConverterFactory<?, IntToDataCellConverter>)converterFactory);
            }
            // long:
            else if (LongCellValueProducer.class.isAssignableFrom(producerType)
                && LongToDataCellConverter.class.isAssignableFrom(converterType)) {
                mapper = new LongMapper(
                    (TypedCellValueProducerFactory<S, ?, ?, PP, LongCellValueProducer<S, PP>>)producerFactory,
                    (TypedJavaToDataCellConverterFactory<?, LongToDataCellConverter>)converterFactory);
            }
            // boolean:
            else if (BooleanCellValueProducer.class.isAssignableFrom(producerType)
                && BooleanToDataCellConverter.class.isAssignableFrom(converterType)) {
                mapper = new BooleanMapper(
                    (TypedCellValueProducerFactory<S, ?, ?, PP, BooleanCellValueProducer<S, PP>>)producerFactory,
                    (TypedJavaToDataCellConverterFactory<?, BooleanToDataCellConverter>)converterFactory);
            }
            // float:
            else if (FloatCellValueProducer.class.isAssignableFrom(producerType)
                && FloatToDataCellConverter.class.isAssignableFrom(converterType)) {
                mapper = new FloatMapper(
                    (TypedCellValueProducerFactory<S, ?, ?, PP, FloatCellValueProducer<S, PP>>)producerFactory,
                    (TypedJavaToDataCellConverterFactory<?, FloatToDataCellConverter>)converterFactory);
            }
            // byte:
            else if (ByteCellValueProducer.class.isAssignableFrom(producerType)
                && ByteToDataCellConverter.class.isAssignableFrom(converterType)) {
                mapper = new ByteMapper(
                    (TypedCellValueProducerFactory<S, ?, ?, PP, ByteCellValueProducer<S, PP>>)producerFactory,
                    (TypedJavaToDataCellConverterFactory<?, ByteToDataCellConverter>)converterFactory);
            }
            // short:
            else if (ShortCellValueProducer.class.isAssignableFrom(producerType)
                && ShortToDataCellConverter.class.isAssignableFrom(converterType)) {
                mapper = new ShortMapper(
                    (TypedCellValueProducerFactory<S, ?, ?, PP, ShortCellValueProducer<S, PP>>)producerFactory,
                    (TypedJavaToDataCellConverterFactory<?, ShortToDataCellConverter>)converterFactory);
            }
            // char:
            else if (CharCellValueProducer.class.isAssignableFrom(producerType)
                && CharToDataCellConverter.class.isAssignableFrom(converterType)) {
                mapper = new CharMapper(
                    (TypedCellValueProducerFactory<S, ?, ?, PP, CharCellValueProducer<S, PP>>)producerFactory,
                    (TypedJavaToDataCellConverterFactory<?, CharToDataCellConverter>)converterFactory);
            }
        }
        if (mapper == null) {
            // Fall-through.
            mapper = new ObjectMapper((CellValueProducerFactory<S, ?, ?, PP>)producerFactory, converterFactory);
        }
        return mapper;
    }

    @Override
    public final DataRow produceDataRow(final RowKey rowKey, final PP[] params) throws Exception {
        for (int i = 0; i < m_mappers.length; i++) {
            m_tempCells[i] = m_mappers[i].map(params[i]);
        }
        // Constructor copies temporary cell array.
        return new DefaultRow(rowKey, m_tempCells);
    }

    // Mapper implementations:

    private final class DoubleMapper extends Mapper<PP, //
            TypedCellValueProducerFactory<S, ?, ?, PP, DoubleCellValueProducer<S, PP>>, //
            TypedJavaToDataCellConverterFactory<?, DoubleToDataCellConverter>> {

        private DoubleMapper(
            final TypedCellValueProducerFactory<S, ?, ?, PP, DoubleCellValueProducer<S, PP>> producerFactory,
            final TypedJavaToDataCellConverterFactory<?, DoubleToDataCellConverter> converterFactory) {
            super(producerFactory, converterFactory);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final DoubleCellValueProducer<S, PP> producer = m_producerFactory.create();
            final DoubleToDataCellConverter converter = m_converterFactory.create(m_executionContext);
            if (producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return converter.convertDouble(producer.produceDoubleCellValue(m_source, params));
            }
        }
    }

    private final class IntMapper extends Mapper<PP, //
            TypedCellValueProducerFactory<S, ?, ?, PP, IntCellValueProducer<S, PP>>, //
            TypedJavaToDataCellConverterFactory<?, IntToDataCellConverter>> {

        private IntMapper(final TypedCellValueProducerFactory<S, ?, ?, PP, IntCellValueProducer<S, PP>> producerFactory,
            final TypedJavaToDataCellConverterFactory<?, IntToDataCellConverter> converterFactory) {
            super(producerFactory, converterFactory);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final IntCellValueProducer<S, PP> producer = m_producerFactory.create();
            final IntToDataCellConverter converter = m_converterFactory.create(m_executionContext);
            if (producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return converter.convertInt(producer.produceIntCellValue(m_source, params));
            }
        }
    }

    private final class LongMapper extends Mapper<PP, //
            TypedCellValueProducerFactory<S, ?, ?, PP, LongCellValueProducer<S, PP>>, //
            TypedJavaToDataCellConverterFactory<?, LongToDataCellConverter>> {

        private LongMapper(
            final TypedCellValueProducerFactory<S, ?, ?, PP, LongCellValueProducer<S, PP>> producerFactory,
            final TypedJavaToDataCellConverterFactory<?, LongToDataCellConverter> converterFactory) {
            super(producerFactory, converterFactory);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final LongCellValueProducer<S, PP> producer = m_producerFactory.create();
            final LongToDataCellConverter converter = m_converterFactory.create(m_executionContext);
            if (producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return converter.convertLong(producer.produceLongCellValue(m_source, params));
            }
        }
    }

    private final class BooleanMapper extends Mapper<PP, //
            TypedCellValueProducerFactory<S, ?, ?, PP, BooleanCellValueProducer<S, PP>>, //
            TypedJavaToDataCellConverterFactory<?, BooleanToDataCellConverter>> {

        private BooleanMapper(
            final TypedCellValueProducerFactory<S, ?, ?, PP, BooleanCellValueProducer<S, PP>> producerFactory,
            final TypedJavaToDataCellConverterFactory<?, BooleanToDataCellConverter> converterFactory) {
            super(producerFactory, converterFactory);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final BooleanCellValueProducer<S, PP> producer = m_producerFactory.create();
            final BooleanToDataCellConverter converter = m_converterFactory.create(m_executionContext);
            if (producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return converter.convertBoolean(producer.produceBooleanCellValue(m_source, params));
            }
        }
    }

    private final class FloatMapper extends Mapper<PP, //
            TypedCellValueProducerFactory<S, ?, ?, PP, FloatCellValueProducer<S, PP>>, //
            TypedJavaToDataCellConverterFactory<?, FloatToDataCellConverter>> {

        private FloatMapper(
            final TypedCellValueProducerFactory<S, ?, ?, PP, FloatCellValueProducer<S, PP>> producerFactory,
            final TypedJavaToDataCellConverterFactory<?, FloatToDataCellConverter> converterFactory) {
            super(producerFactory, converterFactory);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final FloatCellValueProducer<S, PP> producer = m_producerFactory.create();
            final FloatToDataCellConverter converter = m_converterFactory.create(m_executionContext);
            if (producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return converter.convertFloat(producer.produceFloatCellValue(m_source, params));
            }
        }
    }

    private final class ByteMapper extends Mapper<PP, //
            TypedCellValueProducerFactory<S, ?, ?, PP, ByteCellValueProducer<S, PP>>, //
            TypedJavaToDataCellConverterFactory<?, ByteToDataCellConverter>> {

        private ByteMapper(
            final TypedCellValueProducerFactory<S, ?, ?, PP, ByteCellValueProducer<S, PP>> producerFactory,
            final TypedJavaToDataCellConverterFactory<?, ByteToDataCellConverter> converterFactory) {
            super(producerFactory, converterFactory);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final ByteCellValueProducer<S, PP> producer = m_producerFactory.create();
            final ByteToDataCellConverter converter = m_converterFactory.create(m_executionContext);
            if (producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return converter.convertByte(producer.produceByteCellValue(m_source, params));
            }
        }
    }

    private final class ShortMapper extends Mapper<PP, //
            TypedCellValueProducerFactory<S, ?, ?, PP, ShortCellValueProducer<S, PP>>, //
            TypedJavaToDataCellConverterFactory<?, ShortToDataCellConverter>> {

        private ShortMapper(
            final TypedCellValueProducerFactory<S, ?, ?, PP, ShortCellValueProducer<S, PP>> producerFactory,
            final TypedJavaToDataCellConverterFactory<?, ShortToDataCellConverter> converterFactory) {
            super(producerFactory, converterFactory);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final ShortCellValueProducer<S, PP> producer = m_producerFactory.create();
            final ShortToDataCellConverter converter = m_converterFactory.create(m_executionContext);
            if (producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return converter.convertShort(producer.produceShortCellValue(m_source, params));
            }
        }
    }

    private final class CharMapper extends Mapper<PP, //
            TypedCellValueProducerFactory<S, ?, ?, PP, CharCellValueProducer<S, PP>>, //
            TypedJavaToDataCellConverterFactory<?, CharToDataCellConverter>> {

        private CharMapper(
            final TypedCellValueProducerFactory<S, ?, ?, PP, CharCellValueProducer<S, PP>> producerFactory,
            final TypedJavaToDataCellConverterFactory<?, CharToDataCellConverter> converterFactory) {
            super(producerFactory, converterFactory);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final CharCellValueProducer<S, PP> producer = m_producerFactory.create();
            final CharToDataCellConverter converter = m_converterFactory.create(m_executionContext);
            if (producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return converter.convertChar(producer.produceCharCellValue(m_source, params));
            }
        }
    }

    private final class ObjectMapper extends Mapper<PP, //
            CellValueProducerFactory<S, ?, ?, PP>, //
            JavaToDataCellConverterFactory<?>> {

        private ObjectMapper(final CellValueProducerFactory<S, ?, ?, PP> producerFactory,
            final JavaToDataCellConverterFactory<?> converterFactory) {
            super(producerFactory, converterFactory);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final CellValueProducer<S, ?, PP> producer = m_producerFactory.create();
            final JavaToDataCellConverter<?> converter = m_converterFactory.create(m_executionContext);
            final Object value = producer.produceCellValue(m_source, params);
            if (value == null) {
                return new MissingCell(null);
            } else {
                return converter.convertUnsafe(value);
            }
        }
    }

    private abstract static class Mapper<PP extends ProducerParameters<?>, //
            P extends CellValueProducerFactory<?, ?, ?, ?>, //
            C extends JavaToDataCellConverterFactory<?>> {

        protected final P m_producerFactory;

        protected final C m_converterFactory;

        private Mapper(final P producerFactory, final C converterFactory) {
            m_producerFactory = producerFactory;
            m_converterFactory = converterFactory;
        }

        protected abstract DataCell map(PP params) throws Exception;
    }
}
