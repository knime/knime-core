package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.map.MappingFramework.CellValueConsumer;

/**
 * Interface for the destination for extracting and writing {@link DataCell}/{@link DataValue} contents.
 *
 * May be implemented to represent an SQL Database, file, H2O frame and more.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 */
public interface Destination {

    /**
     * Parameters passed to a {@link CellValueConsumer}. Meant for very temporary parameters such as row specific
     * configuration.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <DestinationType> type of destination
     */
    public static interface ConsumerParameters<DestinationType extends Destination> {

    }
}
