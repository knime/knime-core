package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;

/**
 * A selection of {@link CellValueProducer} to {@link JavaToDataCellConverter} to write a certain value from
 * {@link Source} to a {@link DataCell}.
 *
 * @author Jonathan Hale
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
     * @param producerFactory Factory to create the producer which gets a value from an external source
     * @param f Factory to create a converter used to wrap the value from the producer into a data cell
     */
    public ProductionPath(final CellValueProducerFactory<?, ?, ?, ?> producerFactory,
        final JavaToDataCellConverterFactory<?> f) {
        this.m_producerFactory = producerFactory;
        this.m_converterFactory = f;
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
        ProductionPath other = (ProductionPath)obj;
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
