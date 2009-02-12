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
 *   31.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter;

import org.knime.base.node.util.DataArray;

/**
 * The plotters rely on a <code>DataProvider</code> to get the data to 
 * visualize. It provides the data as a 
 * {@link org.knime.base.node.util.DataArray} with the 
 * {@link #getDataArray(int)}, where the index can be used, if a NodeModel has 
 * two inports and both data should be visualized. Then the index provides 
 * means to determine which {@link org.knime.base.node.util.DataArray} should 
 * be returned. 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public interface DataProvider {
    
    /** Default start row for DataArray creation. */
    public static final int START = 1;
    
    /** Default end row for DataArray creation. */
    public static final int END = 2500;
    
    /**
     * Provides the data that should be visualized. The index can be used, if a 
     * NodeModel has two inports and both data should be visualized. Then the 
     * index provides means to determine which 
     * {@link org.knime.base.node.util.DataArray} should be returned.
     * 
     * @param index if the data of more than one data table should be 
     *  visualized.
     * @return the data as a data array.
     */
    public DataArray getDataArray(final int index);


}
