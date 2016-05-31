package org.knime.core.data.convert.java;

/**
 * Default implementation of {@link DataCellToJavaConverterFactory}.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <S> Source type which can be converted by converters created by this factory
 * @param <D> Destination type which can be converted by converters created by this factory
 * @since 3.2
 */
public class SimpleDataCellToJavaConverterFactory<S, D> implements DataCellToJavaConverterFactory<S, D> {

    private final Class<S> m_sourceType;

    private final Class<D> m_destType;

    private final DataCellToJavaConverter<S, D> m_converter;

    private final String m_name;

    /**
     * @param sourceType Type the created {@link DataCellToJavaConverter}s convert from
     * @param destType Type the created {@link DataCellToJavaConverter}s convert to
     * @param converter Implementation (possibly a Lambda expression) of the {@link DataCellToJavaConverter} defining
     *            the single instance returned by {@link #create()}.
     */
    public SimpleDataCellToJavaConverterFactory(final Class<S> sourceType, final Class<D> destType,
        final DataCellToJavaConverter<S, D> converter) {
        this(sourceType, destType, converter, "");
    }

    /**
     * @param sourceType Type the created {@link DataCellToJavaConverter}s convert from
     * @param destType Type the created {@link DataCellToJavaConverter}s convert to
     * @param converter Implementation (possibly a Lambda expression) of the {@link DataCellToJavaConverter} defining
     *            the single instance returned by {@link #create()}.
     * @param name Name for this factory
     */
    public SimpleDataCellToJavaConverterFactory(final Class<S> sourceType, final Class<D> destType,
        final DataCellToJavaConverter<S, D> converter, final String name) {
        m_sourceType = sourceType;
        m_destType = destType;
        m_converter = converter;
        m_name = name;
    }

    @Override
    public DataCellToJavaConverter<S, D> create() {
        return m_converter;
    }

    @Override
    public Class<S> getSourceType() {
        return m_sourceType;
    }

    @Override
    public Class<D> getDestinationType() {
        return m_destType;
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_sourceType.getSimpleName() + "," + m_destType.toString() + "," + m_name
            + ")";
    }
}