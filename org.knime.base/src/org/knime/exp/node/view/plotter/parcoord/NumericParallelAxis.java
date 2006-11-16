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
 *   22.09.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.parcoord;

import org.knime.base.util.coordinate.NumericCoordinate;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class NumericParallelAxis extends ParallelAxis {
    
    /**
     * 
     * @return thre minimum value of the domain.
     */
    public double getMinValue() {
        return ((NumericCoordinate)getCoordinate()).getMinDomainValue();
    }
    
    /**
     * 
     * @return the maximum value of the domain.
     */
    public double getMaxValue() {
        return ((NumericCoordinate)getCoordinate()).getMaxDomainValue();
    }

}
