/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */

package org.knime.core.data.convert.datacell;

import java.lang.reflect.Array;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.node.ExecutionContext;

/**
 * {@link JavaToDataCellConverterFactory} for converting arrays of a type to a collection of a data cell type by
 * converting each element using a given converter.
 *
 * @author Jonathan Hale
 *
 * @param <T> {@link JavaToDataCellConverter} subclass which can be created by this factory.
 * @param <F> Element factory type
 * @since 3.2
 */
class ArrayToCollectionConverterFactory<T, F> implements JavaToDataCellConverterFactory<T> {

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
    ArrayToCollectionConverterFactory(final JavaToDataCellConverterFactory<F> elementFactory) {
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
