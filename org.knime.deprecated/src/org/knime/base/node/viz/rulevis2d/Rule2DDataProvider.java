/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   23.05.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.rulevis2d;

import org.knime.base.node.util.DataArray;

/**
 * An interface to provide the necessary data for the Rule2D visualization.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public interface Rule2DDataProvider {
    
    /**
     * Returns the rules as FuzzyIntervals.
     * @return - the rules.
     */
    public DataArray getRules();
    
    /**
     * Returns the data points.
     * @return - the data points.
     */
    public DataArray getDataPoints();

}
