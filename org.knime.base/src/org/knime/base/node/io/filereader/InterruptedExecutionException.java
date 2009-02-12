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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.01.2008 (ohl): created
 */
package org.knime.base.node.io.filereader;

/**
 * Exception thrown by the FileAnalyzer, if the program (the node dialog)
 * interrupted the analysis.
 *
 * @author ohl, University of Konstanz
 */
public class InterruptedExecutionException extends Exception {

    /**
     * Creates a new exception with no detail message.
     */
    public InterruptedExecutionException() {
        super();
    }

    /**
     * Creates a new exception with a detail message.
     *
     * @param s a detail message
     */
    public InterruptedExecutionException(final String s) {
        super(s);
    }

}
