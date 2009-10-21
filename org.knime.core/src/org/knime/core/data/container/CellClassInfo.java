/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.data.container;

import java.util.concurrent.ConcurrentHashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.util.ConvenienceMethods;

/** Pair of cell class and possibly a DataType that represents the 
 * collection element type (only if cell class is a collection). 
 * @author Bernd Wiswedel, University of Konstanz 
 */ 
final class CellClassInfo {
    
    /** Map of Class objects (DataCell classes) and the CellClassInfo. Used
     * to reduce the overhead of creating new CellClassInfo objects. There
     * will be a total of as many different CellClassInfo objects as there
     * are DataCell classes used in KNIME (significant less than 100), though
     * we would create for each DataCell object that is added to a Buffer 
     * a new CellClassInfo ... unless we use this map here. We use
     * a map of Object to CellClassInfo as the key may be a 
     * Class<? extends DataValue> (for non-collection cells) or a 
     * CellClassInfo (for collection cells). The latter is used to retrieve
     * the singleton for an equal CellClassInfo. */
    private static final ConcurrentHashMap<Object, CellClassInfo> MAP
        = new ConcurrentHashMap<Object, CellClassInfo>();
    
    /** Access method to retrieve the CellClassInfo for a given DataCell. Used
     * to avoid overhead of creating a new object for each DataCell being added
     * to a buffer.
     * @param cell The argument cell
     * @return The representing cell class info.
     */
    static CellClassInfo get(final DataCell cell) {
        return get(cell.getClass(), (cell instanceof CollectionDataValue
                ? ((CollectionDataValue)cell).getElementType() : null));
    }
    
    /** Same as {@link #get(DataCell)}, only with resolved arguments. 
     * @param cellClass Class of DataCell
     * @param collectionElementType DataType of collection elements if the cell
     *        class is an instance of {@link CollectionDataValue}.
     * @return The representing CellClassInfo
     */
    static CellClassInfo get(final Class<? extends DataCell> cellClass,
            final DataType collectionElementType) {
        CellClassInfo result = MAP.get(cellClass);
        if (result != null) {
            return result;
        }
        result = new CellClassInfo(cellClass, collectionElementType);
        if (result.getCollectionElementType() != null) {
            CellClassInfo original = MAP.get(result);
            if (original != null) {
                return original;
            } else {
                MAP.put(result, result);
                return result;
            }
        } else {
            MAP.put(cellClass, result);
            return result;
        }
    }
    
    private final DataType m_collectionElementType;
    private final Class<? extends DataCell> m_cellClass;

    private CellClassInfo(final Class<? extends DataCell> cellClass, 
            final DataType collectionElementType) {
        if (cellClass == null) {
            throw new NullPointerException("Cell class must not be null");
        }
        if (CollectionDataValue.class.isAssignableFrom(cellClass)) {
            if (collectionElementType == null) {
                throw new IllegalArgumentException("No collection element "
                        + "type provided for class " + cellClass);
            }
        } else {
            if (collectionElementType != null) {
                throw new IllegalArgumentException("Non-collection cells "
                        + "must not have collection element type: " 
                        + cellClass);
            }
        }
        m_cellClass = cellClass;
        m_collectionElementType = collectionElementType;
    }
    
    /** @return the collectionElementType or null if this represent not
     * a collection cell. */
    DataType getCollectionElementType() {
        return m_collectionElementType;
    }

    /** @return the cellClass */
    Class<? extends DataCell> getCellClass() {
        return m_cellClass;
    }

    /** Get the DataType of the underlying cell.
     * @return The associated DataType.
     */
    DataType getDataType() {
        return DataType.getType(m_cellClass, m_collectionElementType);
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = m_cellClass.hashCode();
        if (m_collectionElementType != null) {
            hash ^= m_collectionElementType.hashCode();
        }
        return hash;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CellClassInfo)) {
            return false;
        }
        CellClassInfo c = (CellClassInfo)obj;
        return c.m_cellClass.equals(m_cellClass) 
            && ConvenienceMethods.areEqual(
                    c.m_collectionElementType, m_collectionElementType);
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(m_cellClass.getSimpleName());
        if (m_collectionElementType != null) {
            b.append(" (Collection of ");
            b.append(m_collectionElementType).append(")");
        }
        return b.toString();
    }
    
}