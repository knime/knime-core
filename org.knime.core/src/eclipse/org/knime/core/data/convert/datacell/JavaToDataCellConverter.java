package org.knime.core.data.convert.datacell;

import org.knime.core.data.DataCell;

/**
 * Interface for converters converting Java objects into a {@link DataCell}.
 *
 * @author Jonathan Hale
 *
 * @param <S> source type of this converter
 * @since 3.2
 */
@FunctionalInterface
public interface JavaToDataCellConverter<S> {

    /**
     * Convert the given <code>source</code> object into a {@link DataCell}.
     *
     * @param source object to convert
     * @return a {@link DataCell}.
     * @throws Exception When something went wrong during conversion
     */
    public DataCell convert(S source) throws Exception;
}