/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   22.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.parcoord;

import org.knime.base.util.coordinate.NumericCoordinate;

/**
 * Represents a {@link org.knime.base.node.viz.plotter.parcoord.ParallelAxis} 
 * with numeric values and provides access to the minimum and maximum value of 
 * the domain.
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
