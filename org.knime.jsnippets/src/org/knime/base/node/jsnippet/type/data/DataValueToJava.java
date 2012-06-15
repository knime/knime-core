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
 *   16.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.type.data;

import java.util.Arrays;
import java.util.Comparator;

import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.core.data.DataCell;


/**
 * Converter to create a java object from a data cell.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("rawtypes")
public abstract class DataValueToJava {
    private Class[] m_canJavaTypes;
    private Class m_preferredJavaType;

    /**
     * The java types this converter can create.
     *
     * @param canJavaTypes the java types that can be created
     */
    public DataValueToJava(final Class... canJavaTypes) {
        super();
        m_preferredJavaType = canJavaTypes[0];
        Arrays.sort(canJavaTypes, new Comparator<Class>() {

            @Override
            public int compare(final Class o1, final Class o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        m_canJavaTypes = canJavaTypes;
    }


    /**
     * The java types this converter can create.
     *
     * @return the java types that can be created
     */
    public Class[] canProvideJavaTypes() {
        return m_canJavaTypes;
    }


    /**
     * The preferred java type among those that can be created
     * (see method canProvideJavaTypes).
     * @return the preferred java type
     */
    public Class getPreferredJavaType() {
        return m_preferredJavaType;
    }


    /**
     * Returns true when the given java type can be created.
     * @param javaType the java type to test for
     * @return true when the given java type can be created
     */
    public boolean canProvideJavaType(final Class javaType) {
        return Arrays.binarySearch(m_canJavaTypes, javaType,
                new Comparator<Class>() {
            @Override
            public int compare(final Class o1, final Class o2) {
                return o1.getName().compareTo(o2.getName());
            }
        }) >= 0;
    }

    /**
     * Test if a java object of the the given class can be created from the
     * given data cell.
     * @param cell the data cell
     * @param c the class
     * @return true when cell and class are compatible
     * @throws TypeException in case of incompatibility
     */
    public abstract boolean isCompatibleTo(final DataCell cell, final Class c)
        throws TypeException;

    /**
     * Get the value of the given data cell as a java object of the given class.
     * @param cell the data cell
     * @param c the class
     * @return an object of the given class
     * @throws TypeException in case of incompatibility
     */
    public abstract Object getValue(final DataCell cell, final Class c)
        throws TypeException;

}

