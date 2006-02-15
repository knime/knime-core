/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   21.03.2005 (gabriel): created
 */
package de.unikn.knime.core.node.property.hilite;

import java.util.Map;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;

/**
 * A default mapper for hilite translation which holds a map from 
 * <code>DataCell</code> to a <code>DataCellSet</code> as value.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DefaultHiLiteMapper implements HiLiteMapper {
    
    /*
     * Keep the mapping.
     */
    private final Map<DataCell, Set<DataCell>> m_map;
    
    /**
     * Creates a new default hilite mapper.
     * @param map Keeps the <code>DataCell</code> to <code>DataCellSet</code> 
     *        mapping.
     */
    public DefaultHiLiteMapper(final Map<DataCell, Set<DataCell>> map) {
        if (map == null) {
            throw new IllegalArgumentException("Map must not be null.");
        }
        m_map = map;
    }

    /**
     * @see HiLiteMapper#getKeys(de.unikn.knime.core.data.DataCell)
     */
    public Set<DataCell> getKeys(final DataCell key) {
        return m_map.get(key);
    }

}
