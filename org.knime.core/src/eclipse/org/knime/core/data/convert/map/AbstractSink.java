package org.knime.core.data.convert.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.MappingFramework.CellValueConsumer;
import org.knime.core.data.convert.map.MappingFramework.ProducerConsumerRegistry;
import org.knime.core.data.convert.map.MappingFramework.UnmappableTypeException;
import org.knime.core.data.convert.map.Sink.ConsumerParameters;
import org.knime.core.data.convert.map.Source.ProducerParameters;

/**
 * Abstract implementation of {@link Sink}.
 *
 * Extend this class instead of implementing {@link Sink} directly.
 *
 * @author Jonathan Hale
 * @param <SinkType> Class which is extending this class
 * @param <Params> Implementation of {@link ConsumerParameters} which are used with the extending class
 */
public abstract class AbstractSink<SinkType extends Sink<SinkType>, Params extends ConsumerParameters<SinkType>>
    implements Sink<SinkType> {
    final ArrayList<DataCellToJavaConverter<?, ?>> converters = new ArrayList<>();

    final ArrayList<CellValueConsumer<SinkType, ?, Params>> consumers = new ArrayList<>();

    final ArrayList<Params> parameters = new ArrayList<>();

    /**
     * Constructor
     *
     * Note to implementors: should initialize underlying data structures for given table specification. Per column
     * parameters should be initialized in {@link AbstractSink#createSinkParameters(DataTableSpec)}.
     *
     * @param sinkType Class of the subclass
     * @param spec Table specification
     * @throws UnmappableTypeException In case no converter to consumer path was found for a given data type in the
     *             table spec.
     */
    public AbstractSink(final Class<SinkType> sinkType, final DataTableSpec spec) throws UnmappableTypeException {
        int i = 0;
        for (final DataColumnSpec c : spec) {
            final Optional<DataCellToJavaConverterFactory<?, ?>> maybeFactory = DataCellToJavaConverterRegistry
                .getInstance().getFactoriesForSourceType(c.getType()).stream().findFirst();
            if (!maybeFactory.isPresent()) {
                throw new UnmappableTypeException(
                    "Could not find converter for column at index " + i + " with type " + c.getType().getName(),
                    c.getType());
            }

            final DataCellToJavaConverterFactory<?, ?> factory = maybeFactory.get();

            final Class<?> javaType = factory.getDestinationType();
            final CellValueConsumer<SinkType, ?, Params> consumer =
                ProducerConsumerRegistry.forSinkType(sinkType).get(javaType);

            if (consumer == null) {
                throw new UnmappableTypeException("Could not find a consumer for column at index " + i
                    + " with converter " + factory.getName() + " to java type " + factory.getDestinationType(),
                    c.getType(), factory.getDestinationType());
            }

            converters.add(factory.create());
            consumers.add(consumer);
            ++i;
        }
        parameters.addAll(createSinkParameters(spec));
    }

    /**
     * @param spec Table spec for which to create the sink parameters.
     * @return {@link ProducerParameters} per column, where each set of parameters corresponds to the column at the same
     *         index in the table.
     */
    protected abstract List<Params> createSinkParameters(final DataTableSpec spec);

    @Override
    public void setCellValue(final int i, final DataCell cell) {
        doSetCellValue(i, cell);
    }

    /* "Hack" to make generics of consumeCellValue work */
    private <T> void doSetCellValue(final int i, final DataCell cell) {
        T value = null;
        try {
            value = (T)converters.get(i).convertUnsafe(cell);
        } catch (Exception ex) {
            // Failed to convert a value, keep it at null.
        }
        final CellValueConsumer<SinkType, T, Params> consumer =
            (CellValueConsumer<SinkType, T, Params>)consumers.get(i);
        consumer.consumeCellValue((SinkType)this, value, parameters.get(i));
    }

    /**
     * Write a DataRow to this {@link Sink}.
     *
     * @param input The data row.
     */
    public void addRow(final DataRow input) {
        for (int i = 0; i < input.getNumCells(); ++i) {
            setCellValue(i, input.getCell(i));
        }
        finishRow();
    }

    /**
     * Write a DataTable to this {@link Sink}.
     *
     * @param input The data table.
     */
    public void addTable(final DataTable input) {
        for (final DataRow row : input) {
            addRow(row);
        }
    }
}
