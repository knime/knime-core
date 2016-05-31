package org.knime.core.data.convert.java;

import java.lang.reflect.Array;
import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.CollectionDataValue;

/**
 * A {@link DataCellToJavaConverterFactory} which creates converters for converting {@link CollectionDataValue} subtypes
 * to Array subtypes.
 *
 * Warning: Incorrect use of this class results in undefined behavior. Handle with care.
 *
 * @author Jonathan Hale
 *
 * @param <S> An {@link CollectionDataValue} subtype so that the element type of S is SE.
 * @param <D> An array class so that <code> D == SE[] </code>
 * @param <SE> Type of elements of <S>
 * @param <DE> Type of elements of <DE>
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class CollectionConverterFactory<S, D, SE, DE> implements DataCellToJavaConverterFactory<S, D> {

    private final Class<S> m_sourceType;

    private final Class<D> m_destType;

    private final DataCellToJavaConverter<SE, DE> m_elementConverter;

    /**
     * Constructor
     *
     * @param sourceType Type the created {@link DataCellToJavaConverter}s convert from
     * @param destType Type the created {@link DataCellToJavaConverter}s convert to
     * @param elementConverter Implementation (possibly a Lambda expression) of the {@link DataCellToJavaConverter}
     *            defining the single instance returned by {@link #create()}.
     */
    public CollectionConverterFactory(final Class<S> sourceType, final Class<D> destType,
        final DataCellToJavaConverter<SE, DE> elementConverter) {
        assert CollectionDataValue.class.isAssignableFrom(sourceType);
        assert destType.isArray();

        m_sourceType = sourceType;
        m_destType = destType;
        m_elementConverter = elementConverter;
    }

    @Override
    public DataCellToJavaConverter<S, D> create() {
        return (final S source) -> {
            final CollectionDataValue val = (CollectionDataValue)source;
            final Object outputArray = Array.newInstance(m_destType.getComponentType(), val.size());

            final Iterator<DataCell> itor = val.iterator();

            int i = 0;
            while (itor.hasNext()) {
                Array.set(outputArray, i, m_elementConverter.convert((SE)itor.next()));
                i++;
            }

            return (D)outputArray;
        };
    }

    @Override
    public Class<S> getSourceType() {
        return m_sourceType;
    }

    @Override
    public Class<D> getDestinationType() {
        return m_destType;
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_sourceType + "," + m_destType + ")";
    }
}
