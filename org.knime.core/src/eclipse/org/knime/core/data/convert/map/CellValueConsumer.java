package org.knime.core.data.convert.map;

import org.knime.core.data.convert.map.Destination.ConsumerParameters;

/**
 * A cell value consumer accepts a Java value and writes it to a {@link Destination} as a certain external type.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <D> Type of {@link Destination} this consumer writes to
 * @param <T> Type of Java value the consumer accepts
 * @param <CP> Subtype of {@link ConsumerParameters} that can be used to configure this consumer
 * @since 3.6
 */
@FunctionalInterface
public interface CellValueConsumer<D extends Destination<?>, T, CP extends Destination.ConsumerParameters<D>> {

    /**
     * Writes the <code>value</code> to <code>destination</code> using given <code>destinationParams</code>.
     *
     * @param destination The {@link Destination}.
     * @param value The value to write.
     * @param consumerParams The parameters further specifying how to write to the destination, e.g. to which SQL
     *            column or table to write. Specific to the type of {@link Destination} and {@link CellValueConsumer}
     *            that is being used.
     * @throws MappingException If an exception occurs while consuming the cell value
     */
    public void consumeCellValue(final D destination, final T value, final CP consumerParams)
        throws MappingException;
}