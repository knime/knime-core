/* ------------------------------------------------------------------
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
 *   Mar 6, 2009 (wiswedel): created
 */
package org.knime.core.data.container;

/**
 * Exception that may be thrown by a {@link DataContainer} if the data is
 * invalid. This can be, e.g. null values, incompatible types, duplicate keys,
 * ill-sized rows.
 * 
 * <p>This exception has typically a cause being set, which indicates the true
 * origin of the problem. This class wraps these errors because the data is 
 * possibly written asynchronously, i.e. the error may occur later than it is
 * actually caused.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DataContainerException extends RuntimeException {

    /** Creates new exception without specific cause.
     * @param message The message of the exception.
     */
    DataContainerException(final String message) {
        super(message);
    }

    /** Creates new exception with a given cause.
     * @param message The message of the exception.
     * @param cause The cause of the problem.
     */
    DataContainerException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
