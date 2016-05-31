package org.knime.core.data.convert.datacell;

import java.lang.reflect.Array;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.node.ExecutionContext;

/**
 * Default implementation of {@link JavaToDataCellConverterFactory}.
 *
 * @author Jonathan Hale
 *
 * @param <T> {@link JavaToDataCellConverter} subclass which can be created by this factory.
 * @param <F> Element factory type
 * @since 3.2
 */
public class ArrayToCollectionConverterFactory<T, F> implements JavaToDataCellConverterFactory<T> {

    private final JavaToDataCellConverterFactory<F> m_elementFactory;

    private class ToCollectionConverter<E> implements JavaToDataCellConverter<T> {

        final JavaToDataCellConverter<E> m_elementConverter;

        public ToCollectionConverter(final JavaToDataCellConverter<E> elementConverter) {
            m_elementConverter = elementConverter;
        }

        @Override
        public DataCell convert(final T source) throws Exception {
            final E[] array = (E[])source;
            final ArrayList<DataCell> cells = new ArrayList<>(array.length);

            for (final E element : array) {
                cells.add(m_elementConverter.convert(element));
            }

            return CollectionCellFactory.createListCell(cells);
        }
    }

    /**
     * @param elementFactory Factory to convert the components of the input array into components of the output array
     */
    public ArrayToCollectionConverterFactory(final JavaToDataCellConverterFactory<F> elementFactory) {
        m_elementFactory = elementFactory;
    }

    @Override
    public JavaToDataCellConverter<T> create(final ExecutionContext context) {
        return new ToCollectionConverter(m_elementFactory.create(context));
    }

    @Override
    public Class<T> getSourceType() {
        return (Class<T>)Array.newInstance(m_elementFactory.getSourceType(), 0).getClass();
    }

    @Override
    public DataType getDestinationType() {
        return ListCell.getCollectionType(m_elementFactory.getDestinationType());
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_elementFactory.getIdentifier() + ")";
    }
}