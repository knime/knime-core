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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 */
package org.knime.core.data.util;


import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * Factory to get <code>DataCell</code> representation for various 
 * (java-)objects. 
 * 
 * <p>This class is used within the <code>DefaultTable</code> to wrap
 * java objects in <code>DataCell</code>. If you implement your own 
 * <code>DataCell</code> and use the <code>DefaultTable</code>, you
 * probably  want to override this class.
 * <p>This default implementation serves to get <code>DataCell</code> instances
 * for basic java objects like <code>String</code>, <code>Integer</code>, 
 * <code>Double</code> and their generic types like <code>int</code>, 
 * <code>byte</code>, <code>double</code> etc. 
 * 
 * <p>To implement additional functionality, you usually override the proper
 * <code>createDataCell</code> method like this:
 * <pre>
 * ObjectToDataCellConverter converter = new ObjectToDataCellConverter() {
 *     public DataCell createDataCell(final Object o) {
 *         if (o instanceof FooObject) {
 *             return new FooDataCell((FooObject)o);
 *         }
 *         if (o instanceof FooBarObject) {
 *             return new FooBarDataCell((FooBarObject)o);
 *         }
 *         return super.createDataCell(o);
 *     }
 * };
 * </pre>
 * It is also up to the user to implement further handling in the factory method
 * for generic data types.
 * @see org.knime.core.data.def.DefaultTable#DefaultTable(
 * Object[][], String[], String[], ObjectToDataCellConverter)
 * @author Bernd Wiswedel, University of Konstanz
 * @deprecated This class is obsolete as the class 
 * {@link org.knime.core.data.def.DefaultTable} is deprecated.  DataCell 
 * objects should be created using their respective constructor rather than 
 * using an ObjectToDataCellConverter.
 */
public class ObjectToDataCellConverter {
    
    /** 
     * Singleton to be used for default handling. This convenience object may
     * be used when default handling is sufficient. 
     */
    public static final ObjectToDataCellConverter INSTANCE = 
        new ObjectToDataCellConverter();
    
    /** 
     * Factory method to get <code>DataCell</code>s from basic types. This 
     * implementation creates <code>DataCell</code>s depending on the class 
     * type of <code>o</code> as follows:
     * <table>
     * <tr>
     *   <th>Class or value of <code>o</code></th>
     *   <th>Return class</th>
     * </tr>
     * <tr>
     *   <td><code>null</code></td> 
     *   <td><code>StringCell.INSTANCE</code></td>
     * </tr>
     * <tr>
     *   <td><code>String</code></td> <td><code>StringCell</code></td>
     * </tr>
     * <tr>
     *   <td><code>Integer</code></td> <td><code>IntCell</code></td>
     * </tr>
     * <tr>
     *   <td><code>Byte</code></td> <td><code>IntCell</code></td>
     * </tr>
     * <tr>
     *   <td><code>Double</code></td> <td><code>DoubleCell</code></td>
     * </tr>
     * <tr>
     *   <td><code>Float</code></td> <td><code>DoubleCell</code></td>
     * </tr>
     * </table>
     *   
     * @param o The object to be converted into a <code>DataCell</code> or 
     *          <code>null</code> to indicate a missing value.
     * @return a new <code>DataCell</code> representing <code>o</code>.
     * @throws IllegalArgumentException if <code>o</code> is not an instance
     *         of the classes mentioned above. Derivates may override this
     *         behavior.
     */
    public DataCell createDataCell(final Object o) {
        if (o == null) {
            return DataType.getMissingCell();
        }
        if (o instanceof String) {
            return new StringCell((String)o);
        }
        if (o instanceof Integer) {
            return new IntCell(((Integer)o).intValue());
        }
        if (o instanceof Byte) {
            return new IntCell(((Byte)o).intValue());
        }
        if (o instanceof Double) {
            return new DoubleCell(((Double)o).doubleValue());
        }
        if (o instanceof Float) {
            return new DoubleCell(((Float)o).doubleValue());
        }
        throw new IllegalArgumentException("Cannot create DataCell from "
                + "objects of type \"" + o.getClass().getName() + "\".");
    } // createDataCell(Object)
    
    /** 
     * Creates new <code>DoubleCell</code> for a double. 
     * @param d Double to be wrapped in a <code>DataCell</code>
     * @return <code>new DoubleCell(d);</code>
     * @see DoubleCell
     */
    public DataCell createDataCell(final double d) {
        return new DoubleCell(d);
    }

    /** 
     * Creates new <code>DoubleCell</code> for a float. 
     * @param f Float to be wrapped in a <code>DataCell</code>
     * @return <code>new DoubleCell((double)f);</code>
     * @see DoubleCell
     */
    public DataCell createDataCell(final float f) {
        return createDataCell((double)f);
    }

    /** 
     * Creates new <code>IntCell</code> for an int. 
     * @param i Int to be wrapped in a <code>DataCell</code>
     * @return <code>new IntCell(i);</code>
     * @see IntCell
     */
    public DataCell createDataCell(final int i) {
        return new IntCell(i); 
    }

    /** 
     * Creates new <code>IntCell</code> for a byte. 
     * @param b Byte to be wrapped in a <code>DataCell</code>
     * @return <code>new IntCell((int)b);</code>
     * @see IntCell
     */
    public DataCell createDataCell(final byte b) {
        return new IntCell(b);
    }

    /** 
     * Creates new <code>IntCell</code> for a boolean having value
     * 1 if <code>b==true</code> or 0 if <code>b==false</code>. 
     * @param b Boolean to be wrapped in a <code>DataCell</code>
     * @return A new <code>IntCell</code> having either value 1 or 0 
     *         depending on <code>b</code>
     * @see IntCell
     */
    public DataCell createDataCell(final boolean b) {
        final int i = b ? 1 : 0;
        return createDataCell(i);
    }

}
