/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * KNIME.com, Zurich, Switzerland
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
 *   08.12.2008 (ohl): created
 */
package org.knime.core.node.workflow;

/**
 *
 * @author ohl, University of Konstanz
 */
public class NodeExecutionJobReconnectException extends Exception {

    /**
     * @param message
     */
    public NodeExecutionJobReconnectException(final String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     */
    public NodeExecutionJobReconnectException(final String message,
            final Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
