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
 *   18.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

/**
 * An exception thrown by a row filter to indicate that the current and all
 * following rows from now on are to be included into the result table.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class IncludeFromNowOn extends Exception {
    /**
     * Creates a new Exception with no message.
     */
    public IncludeFromNowOn() {
        super();
    }

    /**
     * Creates a new exception object with a message.
     * 
     * @param msg the message to store in the exception object
     */
    public IncludeFromNowOn(final String msg) {
        super(msg);
    }

}
