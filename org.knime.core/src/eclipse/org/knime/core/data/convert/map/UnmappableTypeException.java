package org.knime.core.data.convert.map;

import org.knime.core.data.DataType;

/**
 * Exception thrown when either a {@link CellValueProducer} or converter was missing for a type which needed to be
 * converted.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.6
 */
public class UnmappableTypeException extends Exception {

    /* Generated serial version UID */
    private static final long serialVersionUID = 6498668986346262079L;

    private final DataType m_type;

    private final Class<?> m_javaType;

    /**
     * Constructor
     *
     * @param message Error message
     * @param type Type that could not be mapped.
     */
    public UnmappableTypeException(final String message, final DataType type) {
        this(message, type, null);
    }

    /**
     * Constructor
     *
     * @param message Error message
     * @param javaType Java type that could not be mapped
     */
    public UnmappableTypeException(final String message, final Class<?> javaType) {
        this(message, null, javaType);
    }

    /**
     * Constructor
     *
     * @param message Error message
     * @param type Type that could not be mapped.
     * @param javaType Java type that could not be mapped
     */
    public UnmappableTypeException(final String message, final DataType type, final Class<?> javaType) {
        super(message);

        m_type = type;
        m_javaType = javaType;
    }

    /**
     * @return The data type that was not mappable. May be <code>null</code>.
     */
    public DataType getDataType() {
        return m_type;
    }

    /**
     * @return The java type that was not mappable. May be <code>null</code>.
     */
    public Class<?> getJavaType() {
        return m_javaType;
    }
}
