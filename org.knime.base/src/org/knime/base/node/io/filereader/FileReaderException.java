/*
 * ------------------------------------------------------------------
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
 *   11.01.2006 (ohl): created
 */
package org.knime.base.node.io.filereader;

import org.knime.core.data.DataRow;

/**
 * The exception the {@link java.io.FileReader} (more specificaly the
 * {@link org.knime.base.node.io.filereader.FileRowIterator}) throws if
 * something goes wrong.
 * 
 * This is a runtime exception for now.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class FileReaderException extends RuntimeException {

    private final DataRow m_row;

    private final int m_lineNumber;

    private String m_detailsMsg;

    /**
     * Always provide a good user message why things go wrong.
     * 
     * @param msg the message to store in the exception
     */
    FileReaderException(final String msg) {
        super(msg);
        m_row = null;
        m_lineNumber = -1;
        m_detailsMsg = null;
    }

    /**
     * Constructor for an exception that stores the last (partial) row where
     * things went wrong.
     * 
     * @param msg the message what went wrong
     * @param faultyRow the row as far as it got read
     * @param lineNumber the lineNumber the error occured
     */
    FileReaderException(final String msg, final DataRow faultyRow,
            final int lineNumber) {
        super(msg);
        m_row = faultyRow;
        m_lineNumber = lineNumber;
    }

    /**
     * @return the row that was (possibly partially!) read before things went
     *         wrong. Could be <code>null</code>, if not set.
     */
    DataRow getErrorRow() {
        return m_row;
    }

    /**
     * @return the line number where the error occurred in the file. Could be -1
     *         if not set.
     */
    int getErrorLineNumber() {
        return m_lineNumber;
    }

    /**
     * Sets an additional message.
     * 
     * @param msg the additional message
     */
    void setDetailsMessage(final String msg) {
        m_detailsMsg = msg;
    }

    /**
     * @return the previously set message, or null.
     */
    String getDetailedMessage() {
        return m_detailsMsg;
    }
    

}

