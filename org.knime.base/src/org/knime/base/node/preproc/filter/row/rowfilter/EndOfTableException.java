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
 *   04.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

/**
 * Used by {@link RowFilter}s to tell that no more rows will fulfill their
 * criteria and the
 * {@link org.knime.base.node.preproc.filter.row.RowFilterTable} iterator can
 * flag the end of table.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class EndOfTableException extends Exception {
    /**
     * Creates a new Exception with no message.
     */
    public EndOfTableException() {
        super();
    }

    /**
     * Creates a new exception object with a message.
     * 
     * @param msg the message to store in the exception object.
     */
    public EndOfTableException(final String msg) {
        super(msg);
    }
}
