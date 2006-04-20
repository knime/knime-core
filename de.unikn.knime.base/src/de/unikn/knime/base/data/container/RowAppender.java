/* Created on Apr 20, 2006 10:03:10 AM by thor
 * -------------------------------------------------------------------
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
 *   Apr 20, 2006 (thor): created
 */
package de.unikn.knime.base.data.container;

import de.unikn.knime.core.data.DataRow;

/**
 * This is a very simple interface that allows adding DataRows to a table, container or anything else.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public interface RowAppender {
    /**
     * Appends a row to the end of a container. The row must comply with 
     * the settings in the <code>DataTableSpec</code> that has been set when 
     * the container or table has been construtced. 
     * @param row DataRow ro be added.
     * @throws NullPointerException If the argument is <code>null</code>.
     * @throws IllegalStateException If the structure of the row forbids to
     *         add it to the table or the row's key is already in the container.
     */
    public void addRowToTable(final DataRow row);
}
