/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   23.10.2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

/**
 * A single linked list with a double value.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
class LinkedDouble {

    /**
     * The double value of this list element.
     */
    double m_value;

    /**
     * The next double element in this list.
     */
    LinkedDouble m_next;

    /**
     * The previous double element in this list.
     */
    LinkedDouble m_previous;

    /**
     * Creates a new linked double list element.
     * 
     * @param value the value of this double list element
     */
    LinkedDouble(final double value) {

        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Value must be a valid number");
        }
        m_value = value;
    }

    /**
     * Removes this element from the double linked list.
     */
    void remove() {

        if (m_previous != null) {
            m_previous.m_next = m_next;
        }
        if (m_next != null) {
            m_next.m_previous = m_previous;
        }

    }

    /**
     * Returns a string representation from this element to the end of the list.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        LinkedDouble current = this;
        while (current != null) {
            sb.append(current.m_value).append(",");
            current = current.m_next;
        }
        return sb.toString();
    }
}
