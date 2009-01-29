/*
 * -------------------------------------------------------------------
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
 *    12.03.2007 (Tobias Koetter): created
 */

package org.knime.core.node.util;

import org.knime.core.data.DataColumnSpec;


/**
 * This interface is used in the {@link ColumnSelectionPanel
 * #update(org.knime.core.data.DataTableSpec, String)} and 
 * {@link ColumnSelectionComboxBox
 * #update(org.knime.core.data.DataTableSpec, String, boolean)} method 
 * to filter all given columns.
 * @author Tobias Koetter, University of Konstanz
 */
public interface ColumnFilter {

    /**
     * Checks if the column with the given {@link DataColumnSpec} should 
     * be included or not.
     * 
     * @param colSpec the column specification of the column to check
     * @return <code>true</code> if the column should be included or 
     * <code>false</code> if the column is filtered.
     */
    public boolean includeColumn(final DataColumnSpec colSpec);
    
    /**
     * @return the message to display if all columns are filtered
     */
    public String allFilteredMsg();
}
