/* -------------------------------------------------------------------
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
 *   15.08.2006 (Fabian Dill): created
 */
package org.knime.base.data.bitvector;

import org.knime.base.data.replace.ReplacedCellFactory;


/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class BitVectorCellFactory extends ReplacedCellFactory {
    
    /**
     * 
     * @return the number of set bits.
     */
    public abstract int getNumberOfSetBits();
    
    /**
     * 
     * @return the number of not set bits.
     */
    public abstract int getNumberOfNotSetBits();

}
