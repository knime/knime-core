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
 *   06.02.2006 (gabriel): created
 */
package de.unikn.knime.core.node.property;

import java.util.LinkedHashMap;

import de.unikn.knime.core.data.DataCell;

/**
 * ColorHandler maps nominal or other DataCell values to ColorAttr objects.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorHandlerNominal extends ColorHandler {
    
    /** Maps DataCell values to ColorAttr. */
    private final LinkedHashMap<DataCell, ColorAttr> m_map;
    
    /**
     * Creates new ColorHandler based on a mapping.
     * @param map Mapping form DataCell values to ColorAttr objects.
     * @throws NullPointerException If the map is null.
     */
    public ColorHandlerNominal(final LinkedHashMap<DataCell, ColorAttr> map) {
        if (map == null)  {
            throw new NullPointerException();
        }
        m_map = map;
    }

    /**
     * Returns a ColorAttr for the given DataCell value, or ColorAttr.DEFAULT
     * if not set.
     * @param dc A DataCell value to get color for.
     * @return A ColorAttr for a DataCell value.
     */
    @Override
    public ColorAttr getColorAttr(final DataCell dc) {
        Object o = m_map.get(dc);
        if (o == null) {
            return ColorAttr.DEFAULT;
        }
        return (ColorAttr) o;
    }

}
