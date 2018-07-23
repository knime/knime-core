package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;

/**
 * Interface for the destination for extracting and writing {@link DataCell}/{@link DataValue} contents.
 *
 * May be implemented to represent an SQL Database, file, H2O frame and more.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <ET> Type of external types (used for generic parameter deduction in
 *            {@link MappingFramework#forDestinationType(Class)} for example.
 * @since 3.6
 */
public interface Destination<ET> {

    /**
     * Parameters passed to a {@link CellValueConsumer}. Meant for very temporary parameters such as row specific
     * configuration.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <D> type of destination
     */
    public static interface ConsumerParameters<D extends Destination<?>> {

    }
}
