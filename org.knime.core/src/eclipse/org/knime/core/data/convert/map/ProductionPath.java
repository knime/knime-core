package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;

/**
 * A selection of {@link DataCellToJavaConverterFactory} to {@link CellValueConsumerFactory} to read a value from a
 * {@link Source} into a {@link DataCell}.
 *
 * @author Jonathan Hale
 * @since 3.6
 */
public class ProductionPath {

    /**
     * Producer factory
     */
    public final CellValueProducerFactory<?, ?, ?, ?> m_producerFactory;

    /**
     * Converter factory
     */
    public final JavaToDataCellConverterFactory<?> m_converterFactory;

    /**
     * Constructor.
     *
     * @param producerFactory Factory to create the producer which reads a value from an external source
     * @param converterFactory Factory to create a converter used to wrap the read value from the producer into a data
     *            cell
     */
    public ProductionPath(final CellValueProducerFactory<?, ?, ?, ?> producerFactory,
        final JavaToDataCellConverterFactory<?> converterFactory) {
        this.m_producerFactory = producerFactory;
        this.m_converterFactory = converterFactory;
    }

    /**
     * @return The cell value producer factory used in this path.
     * @since 3.7
     */
    public CellValueProducerFactory<?, ?, ?, ?> getProducerFactory() {
        return m_producerFactory;
    }

    /**
     * @return The converter factory used in this path.
     * @since 3.7
     */
    public JavaToDataCellConverterFactory<?> getConverterFactory() {
        return m_converterFactory;
    }

    /**
     * @return the source type of this path
     */
    public Object getSourceType() {
        return m_producerFactory.getSourceType();
    }

    /**
     * @return the destination type of this path
     */
    public DataType getDestinationType() {
        return m_converterFactory.getDestinationType();
    }

    @Override
    public String toString() {
        return String.format("%s ---> %s --(\"%s\")-> %s", m_producerFactory.getSourceType(),
            m_converterFactory.getSourceType().getSimpleName(), m_converterFactory.getName(),
            m_converterFactory.getDestinationType().getName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_producerFactory == null) ? 0 : m_producerFactory.hashCode());
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
        final ProductionPath other = (ProductionPath)obj;
        if (m_producerFactory == null) {
            if (other.m_producerFactory != null) {
                return false;
            }
        } else if (!m_producerFactory.equals(other.m_producerFactory)) {
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
