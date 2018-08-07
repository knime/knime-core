package org.knime.core.data.convert.map;

import org.knime.core.data.DataValue;

/**
 * Interface for external data destinations to which to write KNIME {@link DataValue data values} using
 * {@link CellValueConsumer cell value consumers}.
 *
 * May be implemented to represent an SQL Database, file, H2O frame or any other external data source.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <ET> Type of external types (used for generic parameter deduction in
 *            {@link MappingFramework#forDestinationType(Class)} for example.
 * @since 3.6
 */
public interface Destination<ET> {

    /**
     * Parameters further specifying how to write to a destination, e.g. to which SQL column or table to write to.
     * Specific to the type of {@link Destination} and {@link CellValueConsumer} that are being used.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <D> type of destination
     */
    public static interface ConsumerParameters<D extends Destination<?>> {

    }
}
