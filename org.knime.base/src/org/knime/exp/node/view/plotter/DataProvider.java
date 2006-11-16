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
 *   31.08.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter;

import org.knime.base.node.util.DataArray;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public interface DataProvider {
    
    /** Default start row for DataArray creation. */
    public static final int START = 1;
    
    /** Default end row for DataArray creation. */
    public static final int END = 10000;
    
    /**
     * Provides the data that should be visualized.
     * @param index if the data of more than one data table should be 
     *  visualized.
     * @return the data as a data table.
     */
    public DataArray getDataArray(final int index);


}
