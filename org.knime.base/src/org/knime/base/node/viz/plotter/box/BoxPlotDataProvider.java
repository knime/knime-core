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
 *   29.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.box;

import java.util.Map;
import java.util.Set;

import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public interface BoxPlotDataProvider extends DataProvider {
    
    /**
     * 
     * @return a map of the column name and a double array containing
     * the minimum, the lower quartile, the median, the upper quatile and
     * the maximum value for that column.
     */
    public Map<DataColumnSpec, double[]>getStatistics();
    
    /**
     * Mild outliers are values > q1 - 3 * iqr and < q1 - 1.5 * iqr and
     * < q3 + 3 * iqr and > q3 + 1.5 * iqr.
     * @return a list of mild outliers for each column.
     */
    public Map<String, Map<Double, Set<RowKey>>> getMildOutliers();
    
    /**
     * Extreme outliers are values < q1 - 3 * iqr and > q3 + 3 * iqr.
     * @return a list of extreme outliers for each column.
     */
    public Map<String, Map<Double, Set<RowKey>>> getExtremeOutliers();
    

}
