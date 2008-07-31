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
 *   21.12.2005 (ohl): created
 */
package org.knime.base.node.io.filetokenizer;

/**
 * The exception the Filetokenizer throws if something goes wrong. 
 * 
 * This is a runtime exception for now. 
 * 
 * @author ohl, University of Konstanz
 */
public class FileTokenizerException extends RuntimeException {
    
    /**
     * Always provide a good user message why things go wrong.
     * 
     * @param msg the message to store in the exception.
     */
    FileTokenizerException(final String msg) {
        super(msg);
    }
}
