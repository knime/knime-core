package org.knime.core.data.convert.map;

import org.knime.core.data.DataValue;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.map.MappingFramework.CellValueConsumer;
import org.knime.core.data.convert.map.MappingFramework.CellValueConsumerFactory;

/**
 * A selection of {@link DataCellToJavaConverter} to {@link CellValueConsumer} to write a certain {@link DataValue} to a
 * {@link Destination}.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 */
public class ConsumptionPath {
    /**
     * Converter factory
     */
    public final DataCellToJavaConverterFactory<?, ?> m_converterFactory;

    /**
     * Consumer factory
     */
    public final CellValueConsumerFactory<?, ?, ?> m_consumerFactory;

    /**
     * Constructor.
     *
     * @param factory Factory of the converter used to extract a Java value out a DataCell.
     * @param consumer CellValueConsumer which accepts the Java value extracted by the converter and writes it to some
     *            {@link Destination}.
     */
    public ConsumptionPath(final DataCellToJavaConverterFactory<?, ?> factory,
        final CellValueConsumerFactory<?, ?, ?> consumer) {
        this.m_converterFactory = factory;
        this.m_consumerFactory = consumer;
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
        ConsumptionPath other = (ConsumptionPath)obj;
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
