/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
