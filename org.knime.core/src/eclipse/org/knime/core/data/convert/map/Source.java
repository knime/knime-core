package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;

/**
 * Interface for external data sources from which to read KNIME {@link DataCell data cells} using
 * {@link CellValueProducer cell value producers}.
 *
 * May be implemented to represent an SQL Database, file, H2O frame or any other external data source.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <ET> Type of external types (used for generic parameter deduction in
 *            {@link MappingFramework#forSourceType(Class)} for example.
 * @since 3.6
 * @see Destination
 * @see CellValueProducer
 */
public interface Source<ET> {

    /**
     * Parameters further specifying how to read from a {@link Source}, e.g. to which SQL column or table to read from.
     * Specific to the type of {@link Source} and {@link CellValueProducer} that are being used.
     *
     * @author Jonathan Hale, KNIME, Konstanz, Germany
     * @param <S> Type of {@link Source} these parameters work with
     */
    public static interface ProducerParameters<S extends Source<?>> {
    }
}
