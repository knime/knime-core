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

