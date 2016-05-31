package org.knime.core.data.convert.datacell;

import java.io.InputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.node.ExecutionContext;

/**
 * Default implementation of {@link JavaToDataCellConverterFactory}. Supports conversions from byte[], InputStream and
 * Byte to {@link BinaryObjectDataCell} .
 *
 * @author Jonathan Hale
 *
 * @param <T> {@link JavaToDataCellConverter} subclass which can be created by this factory.
 * @since 3.2
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class JavaToBinaryObjectCellConverterFactory<T> implements JavaToDataCellConverterFactory<T> {

    private final Class<T> m_sourceType;

    /**
     * Constructor
     *
     * @param sourceType class of <T> which can be converted to {@link BinaryObjectDataCell} by created
     *            {@link DataCellToJavaConverter Converters}
     */
    protected JavaToBinaryObjectCellConverterFactory(final Class<T> sourceType) {
        m_sourceType = sourceType;

        if (!sourceType.equals(byte[].class) && !sourceType.equals(InputStream.class)
            && !sourceType.equals(Byte.class)) {
            // source type can only be byte[] or InputStream
            throw new IllegalArgumentException("Invalid source type.");
        }
    }

    @SuppressWarnings("unchecked")
    // m_sourceType has to be Class<T>, therefore we know what T is at Runtime
    @Override
    public JavaToDataCellConverter<T> create(final ExecutionContext context) {
        if (m_sourceType.isAssignableFrom(InputStream.class)) {
            return (JavaToDataCellConverter<T>)new JavaToDataCellConverter<InputStream>() {

                private final BinaryObjectCellFactory m_factory = new BinaryObjectCellFactory(context);

                @Override
                public DataCell convert(final InputStream source) throws Exception {
                    return m_factory.create(source);
                }
            };
        } else if (m_sourceType.isAssignableFrom(byte[].class)) {
            return (JavaToDataCellConverter<T>)new JavaToDataCellConverter<byte[]>() {

                private final BinaryObjectCellFactory m_factory = new BinaryObjectCellFactory(context);

                @Override
                public DataCell convert(final byte[] source) throws Exception {
                    return m_factory.create(source);
                }
            };
        } else if (m_sourceType.isAssignableFrom(Byte.class)) {
            return (JavaToDataCellConverter<T>)new JavaToDataCellConverter<Byte>() {

                private final BinaryObjectCellFactory m_factory = new BinaryObjectCellFactory(context);

                @Override
                public DataCell convert(final Byte source) throws Exception {
                    return m_factory.create(new byte[]{source});
                }
            };
        }

        throw new IllegalArgumentException("Invalid source type.");
    }

    @Override
    public Class<T> getSourceType() {
        return m_sourceType;
    }

    @Override
    public DataType getDestinationType() {
        return BinaryObjectDataCell.TYPE;
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_sourceType.getName() + ")";
    }
}