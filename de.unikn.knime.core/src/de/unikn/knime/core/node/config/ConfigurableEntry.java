/* 
 * -------------------------------------------------------------------
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
 *   17.01.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node.config;

import java.io.Serializable;

/**
 * Interface implemented by all Config entries.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
interface ConfigurableEntry extends Serializable {

    /**
     * @return A String representation of the entry value.
     */
    String toStringValue();

    /**
     * Checks the identity of two entry values. This method must only be called
     * on equal objects.
     * 
     * @param ce the entry to compare the values with.
     * @return true if the entry value is identical with the one in the passed
     *         argument.
     */
    boolean isIdentical(final ConfigurableEntry ce);

}
