package org.knime.core.data.convert.map;

import org.knime.core.data.DataValue;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;

/**
 * A pair of {@link DataCellToJavaConverterFactory} and {@link CellValueConsumerFactory} to write a certain
 * {@link DataValue} to a {@link Destination} as a certain external type.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.6
 */
public class ConsumptionPath {
    /**
     * Converter factory
     */
    public final DataCellToJavaConverterFactory<?, ?> m_converterFactory;

    /**
     * Consumer factory
     */
    public final CellValueConsumerFactory<?, ?, ?, ?> m_consumerFactory;

    /**
     * Constructor.
     *
     * @param converterFactory Factory for the converter to use for extracting the Java value out of a DataValue.
     * @param consumerFactory Factory for the CellValueConsumer which accepts the Java value extracted by the
     *            <code>converterFactory</code> and writes it to some {@link Destination}.
     */
    public ConsumptionPath(final DataCellToJavaConverterFactory<?, ?> converterFactory,
        final CellValueConsumerFactory<?, ?, ?, ?> consumerFactory) {
        this.m_converterFactory = converterFactory;
        this.m_consumerFactory = consumerFactory;
    }

    /**
     * @return The converter factory used in this path.
     * @since 3.7
     */
    public DataCellToJavaConverterFactory<?, ?> getConverterFactory() {
        return m_converterFactory;
    }

    /**
     * @return The consumer factory used in this path.
     * @since 3.7
     */
    public CellValueConsumerFactory<?, ?, ?, ?> getConsumerFactory() {
        return m_consumerFactory;
    }

    @Override
    public String toString() {
        return String.format("%s --(\"%s\")-> %s ---> %s", m_converterFactory.getSourceType().getSimpleName(),
            m_converterFactory.getName(), m_converterFactory.getDestinationType().getSimpleName(),
            m_consumerFactory.getDestinationType());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_consumerFactory == null) ? 0 : m_consumerFactory.hashCode());
        result = prime * result + ((m_converterFactory == null) ? 0 : m_converterFactory.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConsumptionPath other = (ConsumptionPath)obj;
        if (m_consumerFactory == null) {
            if (other.m_consumerFactory != null) {
                return false;
            }
        } else if (!m_consumerFactory.equals(other.m_consumerFactory)) {
            return false;
        }
        if (m_converterFactory == null) {
            if (other.m_converterFactory != null) {
                return false;
            }
        } else if (!m_converterFactory.equals(other.m_converterFactory)) {
            return false;
        }
        return true;
    }

}
