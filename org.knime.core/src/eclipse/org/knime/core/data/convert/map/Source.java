package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.convert.map.MappingFramework.CellValueProducer;

/**
 * Interface for data sources from which to create KNIME {@link DataCell data cells} using {@link CellValueProducer cell
 * value producers}.
 *
 * May be implemented to represent an SQL Database, file, H2O frame and more.
 *
 * @author Jonathan Hale
 */
public interface Source {
    /**
     * Parameters passed to a {@link CellValueProducer}, further specializing it to a specific input column.
     *
     * @author Jonathan Hale
     * @param <S> Type of {@link Source} these parameters work with
     */
    public static interface ProducerParameters<S extends Source> {
    }
}
