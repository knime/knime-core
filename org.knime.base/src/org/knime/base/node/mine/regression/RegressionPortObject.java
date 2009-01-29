/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Jun 6, 2008 (wiswedel): created
 */
package org.knime.base.node.mine.regression;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;

/**
 * Port object representing simple regression models.
 * 
 * <p>The accompanying spec is of type {@link DataTableSpec}, whereby the last
 * column is not an actual variable but the target column (kept in order to 
 * guess a good response column name), i.e. this column does not need to be 
 * present in the test data. The remaining columns reflect the names and types 
 * of the variables. All regressor variables and the response column are 
 * supposed to be double compatible (meaning that the type of the columns in the
 * spec is ignored for that matter). 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface RegressionPortObject extends PortObject {
    
    /** Convenience access member for port type. */
    public static final PortType TYPE = 
        new PortType(RegressionPortObject.class);
    
    /** Spec to this regression model. Refer to the {@link RegressionPortObject
     * interface description} for details on the structure.
     * {@inheritDoc} */
    @Override
    public DataTableSpec getSpec();
    
    /** Predict a row, returning the regression value. The row is pre-processed 
     * such that it contains only the relevant variables (also in the 
     * order reflected by {@link #getSpec()}), whereby there is no value for the
     * last (response) column, i.e. 
     * <code>row.getNumCells() == getSpec().getNumColumns() - 1</code>.
     * @param row to predict
     * @return calculated value according to regression model. The return class
     * is supposed to be {@link org.knime.core.data.def.DoubleCell} (unless
     * missing). 
     */
    public DataCell predict(final DataRow row);

}
