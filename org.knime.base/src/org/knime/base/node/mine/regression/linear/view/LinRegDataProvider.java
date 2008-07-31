/* 
 * -------------------------------------------------------------------
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
 *   Mar 30, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.view;

import org.knime.base.node.mine.regression.linear.LinearRegressionPortObject;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.DataProvider;

/**
 * An interface that both the learner node model and the predictor node model
 * implement. It describes the access methods for the 2D line viewer.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface LinRegDataProvider extends DataProvider {
    /**
     * Get the parameters for the regression line.
     * 
     * @return the parameters, may be <code>null</code>
     */
    public LinearRegressionPortObject getParams();

    /**
     * Get the row container for the rows to paint in the view.
     * 
     * @return the rows to paint
     */
    public DataArray getRowContainer();
    
    
    /**
     * 
     * @return those columns which were used to calculate the model
     */
    public String[] getIncludedColumns();
    
}
