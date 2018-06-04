package org.knime.core.data.convert.map;

import org.knime.core.data.convert.map.Destination.ConsumerParameters;

/**
 * A cell value consumer receives a Java value and writes it to a {@link Destination} using a certain external type.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <DestinationType> Type of {@link Destination} this consumer writes to
 * @param <T> Type of Java value the consumer accepts
 * @param <CP> Subtype of {@link ConsumerParameters} that can be used to configure this consumer
 */
@FunctionalInterface
public interface CellValueConsumer<DestinationType extends Destination, T, CP extends Destination.ConsumerParameters<DestinationType>> {

    /**
     * Writes the <code>value</code> to <code>destination</code> using given <code>destinationParams</code>.
     *
     * @param destination The {@link Destination}.
     * @param value The value to write.
     * @param destinationParams The parameters further specifying how to write to the destination, e.g. to which SQL
     *            column or table to write. Specific to the type of {@link Destination} and
     *            {@link CellValueConsumer} that is being used.
     */
    public void consumeCellValue(final DestinationType destination, final T value, final CP destinationParams);
}