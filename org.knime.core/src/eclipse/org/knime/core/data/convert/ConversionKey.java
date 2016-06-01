package org.knime.core.data.convert;

import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;

/**
 * Class which contains all necessary information to reference a {@link DataCellToJavaConverterFactory} or
 * {@link JavaToDataCellConverterFactory}. This class is for internal usage only and should not be used by other
 * plug-ins.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.2
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ConversionKey {

    private final int m_hashCode;

    private final Class<?> m_sourceType;

    private final Object m_destType;

    /**
     * Create from source and destination type.
     *
     * @param sourceType Source type the referenced factory should be able to handle
     * @param destType Destination type the referenced factory should be able to handle
     */
    public ConversionKey(final Class<?> sourceType, final Object destType) {
        m_sourceType = sourceType;
        m_destType = destType;

        // precompute hashCode, since general use cases involve at least one call to hashCode()
        final int prime = 31;
        m_hashCode = prime * (prime + sourceType.hashCode()) + destType.hashCode();
    }

    /**
     * Create from an existing factory.
     *
     * @param factory The existing factory which should be referenced by this key
     */
    public ConversionKey(final DataCellToJavaConverterFactory<?, ?> factory) {
        this(factory.getSourceType(), factory.getDestinationType());
    }

    /**
     * Create from an existing factory.
     *
     * @param factory The existing factory which should be referenced by this key
     */
    public ConversionKey(final JavaToDataCellConverterFactory<?> factory) {
        this(factory.getSourceType(), factory.getDestinationType());
    }

    @Override
    public int hashCode() {
        return m_hashCode;
    }

    /**
     * Get the source type of the factory this key refers to.
     *
     * @return the source type of the factory
     */
    public Class<?> getSourceType() {
        return m_sourceType;
    }

    /**
     * Get the destination type of the factory this key refers to.
     *
     * @return the destination type of the factory
     */
    public Object getDestType() {
        return m_destType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DataCellToJavaConverterFactory) {
            return this.equals(new ConversionKey((DataCellToJavaConverterFactory<?, ?>)obj));
        } else if (obj instanceof ConversionKey) {
            return m_hashCode == ((ConversionKey)obj).m_hashCode;
        }
        return false;
    }
}