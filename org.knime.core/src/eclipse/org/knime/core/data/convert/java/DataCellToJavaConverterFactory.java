package org.knime.core.data.convert.java;

/**
 * Interface for all factory classes which create {@link DataCellToJavaConverter DataCellToJavaConverters}.
 *
 * You may extend this class and register the derived class under the extension point
 * "org.knime.core.DataCellToJavaConverter". It is recommended that you extend
 * {@link SimpleDataCellToJavaConverterFactory} if possible.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 *
 * @param <S> type which the created {@link DataCellToJavaConverter}s can convert
 * @param <D> type which the created {@link DataCellToJavaConverter}s convert to
 * @since 3.2
 *
 * @see DataCellToJavaConverter
 * @see DataCellToJavaConverterRegistry
 */
public interface DataCellToJavaConverterFactory<S, D> {

    /**
     * @return a {@link DataCellToJavaConverter} which converts an instance of the type returned by
     *         {@link #getSourceType()} into an instance of the type returned by {@link #getDestinationType()}
     */
    public DataCellToJavaConverter<S, D> create();

    /**
     * @return type which the created {@link DataCellToJavaConverter}s can convert
     */
    public Class<S> getSourceType();

    /**
     * @return type which the created {@link DataCellToJavaConverter}s convert to
     */
    public Class<D> getDestinationType();

    /**
     * @return the name of this converter factory or <code>""</code> if this is the default for the source and
     *         destination types
     */
    default String getName() {
        return "";
    }

    /**
     * @return a ideally unique identifier for this factory
     */
    public String getIdentifier();
}