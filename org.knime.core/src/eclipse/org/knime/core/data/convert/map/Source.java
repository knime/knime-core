package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;

/**
 * Interface for data sources from which to create KNIME {@link DataCell data cells} using {@link CellValueProducer cell
 * value producers}.
 *
 * May be implemented to represent an SQL Database, file, H2O frame and more.
 *
 * @author Jonathan Hale
 * @param <ExternalType> Type of external types (used for generic parameter deduction in
 *            {@link MappingFramework#forSourceType(Class)} for example.
 * @since 3.6
 */
public interface Source<ExternalType> {
    /**
     * Parameters passed to a {@link CellValueProducer}, further specializing it to a specific input column.
     *
     * @author Jonathan Hale
     * @param <S> Type of {@link Source} these parameters work with
     */
    public static interface ProducerParameters<S extends Source<?>> {
    }
}
