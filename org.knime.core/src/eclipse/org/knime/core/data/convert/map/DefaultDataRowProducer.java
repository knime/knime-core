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
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
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
import org.knime.core.data.filestore.FileStoreFactory;
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

    private final FileStoreFactory m_fileStoreFactory;

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
     * @see #DefaultDataRowProducer(Source, ProductionPath[], FileStoreFactory)
     */
    public DefaultDataRowProducer(final S source, final ProductionPath[] mapping, final ExecutionContext exec) {
        this(source, mapping, FileStoreFactory.createFileStoreFactory(exec));
    }

    /**
     * Creates a new data row producer for the given source and the given mapping.
     *
     * @param source The source from which to create data rows.
     * @param mapping Production paths that describe the mapping from source to {@link DataCell data cells}. The number
     *            and order of the passed paths must match the ones of the parameters later passed to
     *            {@link #produceDataRow(RowKey, ProducerParameters[])}.
     * @param fileStoreFactory {@link FileStoreFactory} which may be used for creating {@link CellFactory}s.
     * @since 4.0
     */
    public DefaultDataRowProducer(final S source, final ProductionPath[] mapping,
        final FileStoreFactory fileStoreFactory) {
        m_source = source;
        m_fileStoreFactory = fileStoreFactory;
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
        if (canAvoidAutoboxing(producerFactory, converterFactory)) {
            mapper = createPrimitiveMapper(producerFactory, converterFactory);
        }
        if (mapper == null) {
            // Fall-through.
            mapper = new ObjectMapper(((CellValueProducerFactory<S, ?, ?, PP>)producerFactory).create(),
                converterFactory.create(m_fileStoreFactory));
        }
        return mapper;
    }

    @SuppressWarnings("unchecked") // Type safety is mostly ensured by instance checks.
    private Mapper<PP, ?, ?> createPrimitiveMapper(final CellValueProducerFactory<?, ?, ?, ?> producerFactory,
        final JavaToDataCellConverterFactory<?> converterFactory) {
        final Class<?> producerType = ((TypedCellValueProducerFactory<?, ?, ?, ?, ?>)producerFactory).getProducerType();
        final Class<?> converterType = ((TypedJavaToDataCellConverterFactory<?, ?>)converterFactory).getConverterType();
        // double:
        if (DoubleCellValueProducer.class.isAssignableFrom(producerType)
            && DoubleToDataCellConverter.class.isAssignableFrom(converterType)) {
            return new DoubleMapper(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, DoubleCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedJavaToDataCellConverterFactory<?, DoubleToDataCellConverter>)converterFactory)
                    .create(m_fileStoreFactory));
        }
        // int:
        else if (IntCellValueProducer.class.isAssignableFrom(producerType)
            && IntToDataCellConverter.class.isAssignableFrom(converterType)) {
            return new IntMapper(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, IntCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedJavaToDataCellConverterFactory<?, IntToDataCellConverter>)converterFactory)
                    .create(m_fileStoreFactory));
        }
        // long:
        else if (LongCellValueProducer.class.isAssignableFrom(producerType)
            && LongToDataCellConverter.class.isAssignableFrom(converterType)) {
            return new LongMapper(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, LongCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedJavaToDataCellConverterFactory<?, LongToDataCellConverter>)converterFactory)
                    .create(m_fileStoreFactory));
        }
        // boolean:
        else if (BooleanCellValueProducer.class.isAssignableFrom(producerType)
            && BooleanToDataCellConverter.class.isAssignableFrom(converterType)) {
            return new BooleanMapper(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, BooleanCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedJavaToDataCellConverterFactory<?, BooleanToDataCellConverter>)converterFactory)
                    .create(m_fileStoreFactory));
        }
        // float:
        else if (FloatCellValueProducer.class.isAssignableFrom(producerType)
            && FloatToDataCellConverter.class.isAssignableFrom(converterType)) {
            return new FloatMapper(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, FloatCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedJavaToDataCellConverterFactory<?, FloatToDataCellConverter>)converterFactory)
                    .create(m_fileStoreFactory));
        }
        // byte:
        else if (ByteCellValueProducer.class.isAssignableFrom(producerType)
            && ByteToDataCellConverter.class.isAssignableFrom(converterType)) {
            return new ByteMapper(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, ByteCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedJavaToDataCellConverterFactory<?, ByteToDataCellConverter>)converterFactory)
                    .create(m_fileStoreFactory));
        }
        // short:
        else if (ShortCellValueProducer.class.isAssignableFrom(producerType)
            && ShortToDataCellConverter.class.isAssignableFrom(converterType)) {
            return new ShortMapper(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, ShortCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedJavaToDataCellConverterFactory<?, ShortToDataCellConverter>)converterFactory)
                    .create(m_fileStoreFactory));
        }
        // char:
        else if (CharCellValueProducer.class.isAssignableFrom(producerType)
            && CharToDataCellConverter.class.isAssignableFrom(converterType)) {
            return new CharMapper(
                ((TypedCellValueProducerFactory<S, ?, ?, PP, CharCellValueProducer<S, PP>>)producerFactory).create(),
                ((TypedJavaToDataCellConverterFactory<?, CharToDataCellConverter>)converterFactory)
                    .create(m_fileStoreFactory));
        } else {
            // no primitive mapper matched -> return null and let caller create the default mapper
            return null;
        }
    }

    private static boolean canAvoidAutoboxing(final CellValueProducerFactory<?, ?, ?, ?> producerFactory,
        final JavaToDataCellConverterFactory<?> converterFactory) {
        return producerFactory.getDestinationType().isPrimitive()
            && producerFactory instanceof TypedCellValueProducerFactory
            && converterFactory.getSourceType().isPrimitive()
            && converterFactory instanceof TypedJavaToDataCellConverterFactory;
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

    private final class DoubleMapper extends Mapper<PP, DoubleCellValueProducer<S, PP>, DoubleToDataCellConverter> {

        private DoubleMapper(final DoubleCellValueProducer<S, PP> producer, final DoubleToDataCellConverter converter) {
            super(producer, converter);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            if (m_producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return m_converter.convertDouble(m_producer.produceDoubleCellValue(m_source, params));
            }
        }
    }

    private final class IntMapper extends Mapper<PP, IntCellValueProducer<S, PP>, IntToDataCellConverter> {

        private IntMapper(final IntCellValueProducer<S, PP> producer, final IntToDataCellConverter converter) {
            super(producer, converter);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            if (m_producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return m_converter.convertInt(m_producer.produceIntCellValue(m_source, params));
            }
        }
    }

    private final class LongMapper extends Mapper<PP, LongCellValueProducer<S, PP>, LongToDataCellConverter> {

        private LongMapper(final LongCellValueProducer<S, PP> producer, final LongToDataCellConverter converter) {
            super(producer, converter);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            if (m_producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return m_converter.convertLong(m_producer.produceLongCellValue(m_source, params));
            }
        }
    }

    private final class BooleanMapper extends Mapper<PP, BooleanCellValueProducer<S, PP>, BooleanToDataCellConverter> {

        private BooleanMapper(final BooleanCellValueProducer<S, PP> producer,
            final BooleanToDataCellConverter converter) {
            super(producer, converter);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            if (m_producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return m_converter.convertBoolean(m_producer.produceBooleanCellValue(m_source, params));
            }
        }
    }

    private final class FloatMapper extends Mapper<PP, FloatCellValueProducer<S, PP>, FloatToDataCellConverter> {

        private FloatMapper(final FloatCellValueProducer<S, PP> producer, final FloatToDataCellConverter converter) {
            super(producer, converter);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            if (m_producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return m_converter.convertFloat(m_producer.produceFloatCellValue(m_source, params));
            }
        }
    }

    private final class ByteMapper extends Mapper<PP, ByteCellValueProducer<S, PP>, ByteToDataCellConverter> {

        private ByteMapper(final ByteCellValueProducer<S, PP> producer, final ByteToDataCellConverter converter) {
            super(producer, converter);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            if (m_producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return m_converter.convertByte(m_producer.produceByteCellValue(m_source, params));
            }
        }
    }

    private final class ShortMapper extends Mapper<PP, ShortCellValueProducer<S, PP>, ShortToDataCellConverter> {

        private ShortMapper(final ShortCellValueProducer<S, PP> producer, final ShortToDataCellConverter converter) {
            super(producer, converter);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            if (m_producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return m_converter.convertShort(m_producer.produceShortCellValue(m_source, params));
            }
        }
    }

    private final class CharMapper extends Mapper<PP, CharCellValueProducer<S, PP>, CharToDataCellConverter> {

        private CharMapper(final CharCellValueProducer<S, PP> producer, final CharToDataCellConverter converter) {
            super(producer, converter);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            if (m_producer.producesMissingCellValue(m_source, params)) {
                return new MissingCell(null);
            } else {
                return m_converter.convertChar(m_producer.produceCharCellValue(m_source, params));
            }
        }
    }

    private final class ObjectMapper extends Mapper<PP, CellValueProducer<S, ?, PP>, JavaToDataCellConverter<?>> {

        private ObjectMapper(final CellValueProducer<S, ?, PP> producer, final JavaToDataCellConverter<?> converter) {
            super(producer, converter);
        }

        @Override
        protected final DataCell map(final PP params) throws Exception {
            final Object value = m_producer.produceCellValue(m_source, params);
            if (value == null) {
                return DataType.getMissingCell();
            } else {
                return m_converter.convertUnsafe(value);
            }
        }
    }

    private abstract static class Mapper<PP extends ProducerParameters<?>, //
            P extends CellValueProducer<?, ?, PP>, //
            C extends JavaToDataCellConverter<?>> {

        protected final P m_producer;

        protected final C m_converter;

        protected Mapper(final P producer, final C converter) {
            m_producer = producer;
            m_converter = converter;
        }

        protected abstract DataCell map(PP params) throws Exception;
    }
}
