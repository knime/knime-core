package org.knime.core.data.convert.map;


/**
 * A cell value consumer accepts a Java value and writes it to a destination as a certain external type.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <D> Type of destination this consumer writes to
 * @param <T> Type of Java value the consumer accepts
 * @since 3.6
 * @see CellValueProducer
 */
@FunctionalInterface
public interface CellValueConsumer<D, T> {

    /**
     * Writes the given value to the given destination using the given parameters.
     *
     * @param destination The destination.
     * @param value The value to write.
     * @throws MappingException If an exception occurs while consuming the cell value
     */
    public void consumeCellValue(final D destination, final T value) throws MappingException;
}