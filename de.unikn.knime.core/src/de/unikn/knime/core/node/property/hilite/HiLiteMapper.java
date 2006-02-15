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
 */
package de.unikn.knime.core.node.property.hilite;

import java.util.Set;

import de.unikn.knime.core.data.DataCell;

/**
 * This mapper has to be implemented by all class than are interested in 
 * mapping hilite events between <code>DataCell</code> events. 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface HiLiteMapper {

    /**
     * Returns a set of <code>DataCell</code> elements which are associated
     * be the specified <b>key</b> or <code>null</code> if no mapping
     * is available.
     * @param key The key to get the mapping for.
     * @return A set of mapped <code>DataCell</code> elements.
     */
    Set<DataCell> getKeys(final DataCell key);
    
}
