/*
 * ------------------------------------------------------------------ *
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   23.03.2007 (berthold): created
 */
package org.knime.core.node.workflow;

/**
 * Runtime exception that is thrown when two or more branches of a workflow are
 * merged and they contain non-compatible {@link ScopeObjectStack}.
 * This can happen when the user connects a node contained in one loop of the
 * workflow to a node contained in a different loop.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
class IllegalContextStackObjectException extends RuntimeException {

    /** @see RuntimeException#RuntimeException(String) */
    public IllegalContextStackObjectException(final String message) {
        super(message);
    }

}
