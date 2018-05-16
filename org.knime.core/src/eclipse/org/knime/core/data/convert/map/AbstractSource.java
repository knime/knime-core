package org.knime.core.data.convert.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.MappingFramework.CellValueProducer;
import org.knime.core.data.convert.map.MappingFramework.ProducerConsumerRegistry;
import org.knime.core.data.convert.map.MappingFramework.UnmappableTypeException;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionContext;

/**
 * Abstract implementation of {@link Source}.
 *
 * Extend this class instead of implementing {@link Source} directly.
 *
 * @author Jonathan Hale
 * @param <SourceType> Class which is extending this class
 * @param <Params> Implementation of {@link org.knime.core.data.convert.map.Source.ProducerParameters} which are used
 *            with the extending class
 */
public abstract class AbstractSource<SourceType extends Source<SourceType>, Params extends Source.ProducerParameters<SourceType>>
    implements Source<SourceType> {
    final ArrayList<JavaToDataCellConverter<?>> converters = new ArrayList<>();

    final ArrayList<CellValueProducer<SourceType, ?, Params>> producers = new ArrayList<>();

    final ArrayList<Params> parameters = new ArrayList<>();

    /**
     * Constructor
     *
     * Note to implementors: should initialize underlying data structures for given table specification. Per column
     * parameters should be initialized in {@link AbstractSource#createParameters(DataTableSpec)}.
     *
     * @param sourceType Class of the subclass
     * @param spec Table specification
     * @param context Execution context to create DataCells
     * @throws UnmappableTypeException In case no converter to consumer path was found for a given data type in the
     *             table spec.
     */
    public AbstractSource(final Class<SourceType> sourceType, final DataTableSpec spec, final ExecutionContext context)
        throws UnmappableTypeException {
        int i = 0;
        for (final DataColumnSpec c : spec) {
            final Optional<Class<?>> type =
                DataCellToJavaConverterRegistry.getInstance().getPreferredJavaTypeForCell(c.getType());

            if (!type.isPresent()) {
                throw new UnmappableTypeException(
                    "Could not find preferred java type for column at index " + i + " with type " + c.getType().getName(),
                    c.getType());
            }

            final Optional<?> maybeFactory = JavaToDataCellConverterRegistry.getInstance()
                .getConverterFactories(type.get(), c.getType()).stream().findFirst();

            if (!maybeFactory.isPresent()) {
                throw new UnmappableTypeException(
                    "Could not find converter for column at index " + i + " with type " + c.getType().getName(),
                    c.getType());
            }

            final JavaToDataCellConverterFactory<?> factory = (JavaToDataCellConverterFactory<?>)maybeFactory.get();

            final Class<?> javaType = factory.getSourceType();
            final CellValueProducer<SourceType, ?, Params> producer =
                ProducerConsumerRegistry.forSourceType(sourceType).get(javaType);

            if (producer == null) {
                throw new UnmappableTypeException("Could not find a producer for column at index " + i
                    + " with converter " + factory.getName() + " from java type " + factory.getSourceType(),
                    c.getType(), factory.getSourceType());
            }

            converters.add(factory.create(context));
            producers.add(producer);
            ++i;
        }
        parameters.addAll(createParameters(spec));
    }

    /**
     * @param spec Table spec for which to create the sink parameters.
     * @return {@link org.knime.core.data.convert.map.Source.ProducerParameters} per column, where each set of
     *         parameters corresponds to the column at the same index in the table.
     */
    protected abstract List<Params> createParameters(final DataTableSpec spec);

    /**
     * Read a single {@link DataCell} from this source.
     *
     * @param i Cell/column index in the current row.
     * @return The read value wrapped in a {@link DataCell}
     */
    public DataCell getCell(final int i) {
        return doGetCell(i);
    }

    /* "Hack" to make generics of consumeCellValue work */
    private <T> DataCell doGetCell(final int i) {
        final CellValueProducer<SourceType, T, Params> consumer =
            (CellValueProducer<SourceType, T, Params>)producers.get(i);

        final T value = consumer.produceCellValue((SourceType)this, parameters.get(i));
        try {
            return converters.get(i).convertUnsafe(value);
        } catch (Exception ex) {
            // Failed to convert a value, keep it at null.
            return new MissingCell(ex.getMessage());
        }
    }

    /**
     * Write a DataRow to this {@link Sink}.
     *
     * @return The data row which has been read
     */
    public DataRow getRow() {
        final DataCell[] cells =
            IntStream.range(0, getNumColumns()).mapToObj(i -> getCell(i)).toArray(n -> new DataCell[n]);
        return new DefaultRow(getRowKey(), cells);
    }

    /**
     * Write a DataTable to this {@link Sink}.
     *
     * @param input The data table.
     */
    public void addToContainer(final DataContainer input) {
        while (hasMoreRows()) {
            nextRow();
            input.addRowToTable(getRow());
        }
    }

    /**
     * Determine whether there are more rows to be read and a call to {@link #nextRow()} is valid.
     *
     * @return Whether there are more rows to be read.
     */
    protected abstract boolean hasMoreRows();

    /**
     * @return Number of columns in the current row.
     */
    protected abstract int getNumColumns();

    /**
     * @return A row key for the current row.
     */
    protected abstract RowKey getRowKey();

    /**
     * Called before a row will be read. Could be implemented to load the next row or just increment an index. Will only
     * ever be called as long as {@link #hasMoreRows()} returns true.
     */
    protected abstract void nextRow();
}
