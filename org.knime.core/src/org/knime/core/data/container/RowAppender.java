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
 *   Apr 20, 2006 (thor): created
 */
package org.knime.core.data.container;

import org.knime.core.data.DataRow;

/**
 * This is a very simple interface that allows adding <code>DataRow</code>s to 
 * a <code>DataTable</code>, <code>DataContainer</code>, or anything else.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public interface RowAppender {
    /**
     * Appends a row to the end of a container. The row must comply with 
     * the settings in the <code>DataTableSpec</code> that has been set when 
     * the container or table has been constructed. 
     * @param row <code>DataRow</code> to be added
     * @throws NullPointerException if the argument is <code>null</code>
     * @throws IllegalStateException If the state forbids to add rows.
     * @throws IllegalArgumentException if the structure of the row forbids to
     *         add it to the table 
     * @throws org.knime.core.util.DuplicateKeyException 
     *         If the row's key has already been added.
     */
    public void addRowToTable(final DataRow row);
}
