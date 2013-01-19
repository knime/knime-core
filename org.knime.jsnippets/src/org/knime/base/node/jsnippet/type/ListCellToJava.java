/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   16.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.type;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.base.node.jsnippet.type.data.DataValueToJava;
import org.knime.core.data.DataCell;
import org.knime.core.data.collection.CollectionDataValue;


/**
 * A type converter to create a java array from a list cell.
 *
 * @author Heiko Hofer
 */
public class ListCellToJava extends DataValueToJava {
    private DataValueToJava m_elementToJava;

    /**
     * A type conversion for collection cells of the given type conversion.
     *
     * @param typeConverter the type conversion
     */
    public ListCellToJava(final DataValueToJava typeConverter) {
        super(createArrayTypes(typeConverter));
        m_elementToJava = typeConverter;
    }

    /** Create array classes from the given classes. */
    @SuppressWarnings("rawtypes")
    private static Class[] createArrayTypes(
            final DataValueToJava typeConversion) {
        Class[] javaTypes = typeConversion.canProvideJavaTypes();
        Class preferred = typeConversion.getPreferredJavaType();

        Class[] result = new Class[javaTypes.length];
        result[0] = Array.newInstance(preferred, 0).getClass();
        int c = 1;
        for (int i = 0; i < result.length; i++) {
            if (!preferred.equals(javaTypes[i])) {
                result[c] = Array.newInstance(javaTypes[i], 0).getClass();
                c++;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean isCompatibleTo(final DataCell cell, final Class c)
            throws TypeException {
        return canProvideJavaType(c)
            && cell.getType().isCollectionType();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"rawtypes", "unchecked" })
    @Override
    public Object getValueUnchecked(final DataCell cell, final Class c) {
        List values = new ArrayList();
        for (DataCell element : ((CollectionDataValue)cell)) {
            values.add(m_elementToJava.getValue(element,
                    c.getComponentType()));
        }
        return values.toArray();
    }

}
