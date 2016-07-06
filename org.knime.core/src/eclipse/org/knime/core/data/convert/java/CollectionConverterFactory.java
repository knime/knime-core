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

package org.knime.core.data.convert.java;

import java.lang.reflect.Array;
import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.convert.util.ClassUtil;

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
class CollectionConverterFactory<D, SE extends DataValue, DE>
    implements DataCellToJavaConverterFactory<CollectionDataValue, D> {

    private final Class<D> m_destType;

    private final DataCellToJavaConverterFactory<SE, DE> m_elementConverterFactory;

    /**
     * Constructor.
     *
     * @param elementConverter Implementation (possibly a Lambda expression) of the {@link DataCellToJavaConverter}
     *            defining the single instance returned by {@link #create()}.
     */
    CollectionConverterFactory(final DataCellToJavaConverterFactory<SE, DE> elementConverterFactory) {

        m_destType = ClassUtil.getArrayType(elementConverterFactory.getDestinationType());
        m_elementConverterFactory = elementConverterFactory;
    }

    @Override
    public DataCellToJavaConverter<CollectionDataValue, D> create() {
        return (final CollectionDataValue source) -> {
            final DataCellToJavaConverter<SE, DE> elementConverter = m_elementConverterFactory.create();
            final CollectionDataValue val = source;
            final Object outputArray = Array.newInstance(m_destType.getComponentType(), val.size());

            final Iterator<DataCell> itor = val.iterator();

            int i = 0;
            while (itor.hasNext()) {
                Array.set(outputArray, i, elementConverter.convert((SE)itor.next()));
                i++;
            }

            return (D)outputArray;
        };
    }

    @Override
    public Class<CollectionDataValue> getSourceType() {
        return CollectionDataValue.class;
    }

    @Override
    public Class<D> getDestinationType() {
        return m_destType;
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_elementConverterFactory.getIdentifier() + ")";
    }
}
