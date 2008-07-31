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
 *    19.10.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.util;


/**
 * This {@link Exception} is thrown more section exists than defined in the
 * {@link PieColumnFilter#MAX_NO_OF_SECTIONS} variable.
 * @author Tobias Koetter, University of Konstanz
 */
public class TooManySectionsException extends Exception {
    private static final long serialVersionUID = 2137456313063174430L;

    /**Constructor for class TooManySectionsException.
     * @param string the error message to display
     */
    public TooManySectionsException(final String string) {
        super(string);
    }

}
