/* 
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
 *   06.02.2006 (tg): created
 */
package de.unikn.knime.core.data.property;

import de.unikn.knime.core.data.DataCell;

/**
 * Interface for a Handler generating colors based on - usually
 * user controlled - function on the value of a data cell. Derived methods
 * will allow to e.g. use DoubleCells or NominalCells to specifiy colors.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class ColorHandler implements PropertyHandler {
    
    /**
     * Return a <code>ColorAttr</code> as specified by the content
     * of the given <code>DataCell</code>. If no <code>ColorAttr</code>
     * is assigned to <code>dc</code>, this method shall return a default
     * color, never <code>null</code>.
     * 
     * @param dc Value to be used to generate color.
     * @return A <code>ColorAttr</code> object to be used.
     * @see ColorAttr#DEFAULT
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public abstract ColorAttr getColorAttr(final DataCell dc);
    
}
