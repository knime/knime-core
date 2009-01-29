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
 *   Sep 1, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.Arrays;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.ExecutionMonitor;

/**
 * Default implementation of a {@link CellFactory}, which creates more than
 * a single new column. 
 * @see SingleCellFactory
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractCellFactory implements CellFactory {
    
    private final DataColumnSpec[] m_colSpecs;
    
    /** Creates instance, which will produce content for the columns as
     * specified by the array argument.
     * @param colSpecs The specs of the columns being created.
     */
    public AbstractCellFactory(final DataColumnSpec... colSpecs) {
        if (colSpecs == null || Arrays.asList(colSpecs).contains(null)) {
            throw new NullPointerException("Argument must not be null or " 
                    + "contain null elements");
        }
        m_colSpecs = colSpecs;
    }

    /** {@inheritDoc} */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return m_colSpecs;
    }

    /** {@inheritDoc} */
    @Override
    public void setProgress(final int curRowNr, final int rowCount, 
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double)rowCount, "Processed row " 
                + curRowNr + " (\"" + lastKey + "\")");
    }

}
