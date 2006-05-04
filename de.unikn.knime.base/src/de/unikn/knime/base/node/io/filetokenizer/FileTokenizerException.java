/* -------------------------------------------------------------------
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
 *   21.12.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.filetokenizer;

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
