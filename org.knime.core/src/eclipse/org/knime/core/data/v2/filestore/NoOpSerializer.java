package org.knime.core.data.v2.filestore;

import java.io.IOException;
import java.util.function.Supplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;

/**
 * A simple {@link DataCellSerializer} that serializes no data.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @param <T> the type of the {@link DataCell}
 * @since 5.1
 */
public abstract class NoOpSerializer<T extends DataCell> implements DataCellSerializer<T> {

    private final Supplier<T> m_deserializer;

    /**
     * @param deserializer a supplier for new instances of the {@link DataCell} used by
     *            {@link #deserialize(DataCellDataInput)}
     */
    protected NoOpSerializer(final Supplier<T> deserializer) {
        m_deserializer = deserializer;
    }

    @Override
    public void serialize(final T cell, final DataCellDataOutput output) throws IOException {
        // No Op
    }

    @Override
    public T deserialize(final DataCellDataInput input) throws IOException {
        return m_deserializer.get();
    }
}
