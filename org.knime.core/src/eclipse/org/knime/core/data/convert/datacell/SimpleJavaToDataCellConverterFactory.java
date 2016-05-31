package org.knime.core.data.convert.datacell;

import org.knime.core.data.DataType;
import org.knime.core.node.ExecutionContext;

/**
 * Default implementation of {@link JavaToDataCellConverterFactory}.
 *
 * @author Jonathan Hale
 *
 * @param <T> {@link JavaToDataCellConverter} subclass which can be created by this factory.
 * @since 3.2
 */
public class SimpleJavaToDataCellConverterFactory<T> implements JavaToDataCellConverterFactory<T> {

    private final Class<T> m_sourceType;

    private final DataType m_dataType;

    private final JavaToDataCellConverter<T> m_converter;

    private final String m_name;

    /**
     * @param sourceType source type to be converted
     * @param dataType destination type to be converted into
     * @param converter lambda or converter used to convert an object of sourceType to dataType
     */
    public SimpleJavaToDataCellConverterFactory(final Class<T> sourceType, final DataType dataType,
        final JavaToDataCellConverter<T> converter) {
        this(sourceType, dataType, converter, "");
    }

    /**
     * @param sourceType source type to be converted
     * @param dataType destination type to be converted into
     * @param converter lambda or converter used to convert an object of sourceType to dataType
     * @param name name of this factory
     */
    public SimpleJavaToDataCellConverterFactory(final Class<T> sourceType, final DataType dataType,
        final JavaToDataCellConverter<T> converter, final String name) {
        m_sourceType = sourceType;
        m_dataType = dataType;
        m_converter = converter;
        m_name = name;
    }

    @Override
    public JavaToDataCellConverter<T> create(final ExecutionContext context) {
        return m_converter;
    }

    @Override
    public Class<T> getSourceType() {
        return m_sourceType;
    }

    @Override
    public DataType getDestinationType() {
        return m_dataType;
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_sourceType.getSimpleName() + "," + m_dataType.toString() + "," + m_name
            + ")";
    }

}