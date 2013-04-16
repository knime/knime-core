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
 *   16.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.type.data;

import java.util.Arrays;
import java.util.Comparator;

import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;


/**
 * A converter to create a data cell from a java object.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("rawtypes")
public abstract class JavaToDataCell {
    private Class[] m_canJavaTypes;
    private Class m_preferredJavaType;

    /**
     * Creates a new converter that can create a data cell from data object
     * of the given list of classes.
     *
     * @param canJavaTypes the compatible classes
     */
    public JavaToDataCell(final Class... canJavaTypes) {
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
     * Get a list of classes from which this converter can create data cells
     * from.
     *
     * @return the compatible classes
     */
    public Class[] canJavaTypes() {
        return m_canJavaTypes;
    }


    /**
     * The preferred class among the compatible classes (see the method
     * can JavaTypes).
     * @return the preferred class
     */
    public Class getPreferredJavaType() {
        return m_preferredJavaType;
    }


    /**
     * Test whether a data cell from the given object can be created.
     * @param o the object to test
     * @return true when a data cell from the given class can be created
     */
    public boolean canProcess(final Object o) {
        for (int i = 0; i < m_canJavaTypes.length; i++) {
            // if o is instance of a java type.
            if (m_canJavaTypes[i].isInstance(o)) {
                return true;
            }
        }
        // no compatible type
        return false;
    }

    /**
     * Creates a DataCell from the given object without checking type
     * compatibility.
     * @param value the object
     * @return the data cell
     * @throws Exception when creation of data cell fails
     */
    protected abstract DataCell createDataCellUnchecked(Object value)
        throws Exception;


    /**
     * Creates a DataCell from the given object.
     * @param value the object
     * @return the data cell
     * @throws TypeException when this converter does not support the class
     * of the given object
     * @throws Exception when creation of data cell fails
     */
    public DataCell createDataCell(final Object value)
        throws TypeException, Exception {
        if (value == null) {
            return DataType.getMissingCell();
        }
        if (canProcess(value)) {
            return createDataCellUnchecked(value);
        } else {
            throw new TypeException("The data cell"
                    + " cannot be created from an java object of type "
                    + value.getClass().getSimpleName());
        }
    }
}

