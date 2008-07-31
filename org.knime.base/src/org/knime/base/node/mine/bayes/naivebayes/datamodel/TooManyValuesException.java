/*
 * -------------------------------------------------------------------
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
 *    29.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.mine.bayes.naivebayes.datamodel;


/**
 * Exception if the maximum number of different values is exceeded.
 * @author Tobias Koetter, University of Konstanz
 */
public class TooManyValuesException extends Exception {

    private static final long serialVersionUID = -2908267177526816082L;

    /**Constructor for class TooManyValuesException.
     *
     */
    public TooManyValuesException() {
        //nothing to do
    }

    /**Constructor for class TooManyValuesException.
     * @param message the message
     */
    public TooManyValuesException(final String message) {
        super(message);
    }

    /**Constructor for class TooManyValuesException.
     * @param cause teh cause
     */
    public TooManyValuesException(final Throwable cause) {
        super(cause);
    }

    /**Constructor for class TooManyValuesException.
     * @param message the message
     * @param cause the cause
     */
    public TooManyValuesException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
