/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   03.02.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.type;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.base.node.jsnippet.type.data.DataValueToJava;
import org.knime.core.data.DataCell;

/**
 * A {@link DataValueToJava} converter that can utilize several converters for
 * doing his job.
 *
 * @author Heiko Hofer
 */
public class MultiValueToJava extends DataValueToJava {
    private DataValueToJava[] m_converters;

    /**
     * Create a {@link DataValueToJava} converter that uses one of the given
     * converters.
     *
     * @param converters the converters that do the work.
     */
    public MultiValueToJava(final DataValueToJava... converters) {
        super(getJavaTypes(converters));
        m_converters = converters;
    }

    @SuppressWarnings("rawtypes")
    private static Class[] getJavaTypes(final DataValueToJava[] conveters) {
        Set<Class> classes = new LinkedHashSet<Class>();
        for (DataValueToJava conv : conveters) {
            classes.add(conv.getPreferredJavaType());
            classes.addAll(Arrays.asList(conv.canProvideJavaTypes()));
        }
        return classes.toArray(new Class[classes.size()]);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean isCompatibleTo(final DataCell cell, final Class c)
            throws TypeException {
        for (DataValueToJava dvtj : m_converters) {
            if (dvtj.isCompatibleTo(cell, c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object getValue(final DataCell cell, final Class c)
            throws TypeException {
        if (isCompatibleTo(cell, c)) {
            for (DataValueToJava dvtj : m_converters) {
                if (dvtj.isCompatibleTo(cell, c)) {
                    return dvtj.getValue(cell, c);
                }
            }
        }
        throw new TypeException("The data cell of type "
                    + cell.getType()
                    + " cannot provide a value of type "
                    + c.getSimpleName());
    }


}
