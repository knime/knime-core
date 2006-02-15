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
 *   01.02.2006 (mb): created
 */
package de.unikn.knime.core.data.property;

import de.unikn.knime.core.data.DataCell;

/**
 * Interface for a Handler computing sizes of objects based on - usually
 * user controlled - function on the value of a data cell. Derived methods
 * will allow to e.g. use DoubleCells or NominalCells to specifiy sizes.
 * 
 * @author M. Berthold, University of Konstanz
 */
public abstract class SizeHandler implements PropertyHandler {
    
    /** The default size in case no SizeHandler exists. */
    public static final double DEFAULT_SIZE = 0.0;
    
    /**
     * Return size (in [0,1], that is percent, as specified by the content
     * of the given <code>DataCell</code>.
     * 
     * @param dc value to be used to compute size
     * @return percentage value to base actual size on. -1 if value is illegal.
     */
    public abstract double getSize(final DataCell dc);
    
}
