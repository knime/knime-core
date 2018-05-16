package org.knime.core.data.convert.map;

import org.knime.core.data.DataValue;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.map.MappingFramework.CellValueConsumer;

/**
 * A selection of {@link DataCellToJavaConverter} to {@link CellValueConsumer} to write a certain {@link DataValue} to a
 * {@link Sink}.
 *
 * @author Jonathan Hale
 */
public class ConsumptionPath {
    final DataCellToJavaConverterFactory<?, ?> m_factory;

    final CellValueConsumer<?, ?, ?> m_consumer;

    /**
     * Constructor.
     *
     * @param factory Factory of the converter used to extract a Java value out a DataCell.
     * @param consumer CellValueConsumer which accepts the Java value extracted by the converter and writes it to some
     *            {@link Sink}.
     */
    public ConsumptionPath(final DataCellToJavaConverterFactory<?, ?> factory,
        final CellValueConsumer<?, ?, ?> consumer) {
        this.m_factory = factory;
        this.m_consumer = consumer;
    }

    @Override
    public String toString() {
        return String.format("%s --(\"%s\")-> %s ---> %s Consumer", m_factory.getSourceType().getSimpleName(),
            m_factory.getName(), m_factory.getDestinationType().getSimpleName(), m_consumer.getClass().getSimpleName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_consumer == null) ? 0 : m_consumer.hashCode());
        result = prime * result + ((m_factory == null) ? 0 : m_factory.hashCode());
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
        if (m_consumer == null) {
            if (other.m_consumer != null) {
                return false;
            }
        } else if (!m_consumer.equals(other.m_consumer)) {
            return false;
        }
        if (m_factory == null) {
            if (other.m_factory != null) {
                return false;
            }
        } else if (!m_factory.equals(other.m_factory)) {
            return false;
        }
        return true;
    }
}
