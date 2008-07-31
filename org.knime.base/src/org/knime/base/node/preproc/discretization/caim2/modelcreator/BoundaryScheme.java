/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   Nov 6, 2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

/**
 * A boundary scheme holds the boundaries as linked doubles and the number of
 * boundaries (number of linked doubles).
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class BoundaryScheme {

    private LinkedDouble m_head;

    private int m_numBoundaries;

    /**
     * Creates a boundary scheme from the linked double list and the number of
     * doubles.
     * 
     * @param head the head of the linked double list
     * @param numBoundaries the num of linked doubles (boundaries)
     */
    public BoundaryScheme(final LinkedDouble head, final int numBoundaries) {
        m_head = head;
        m_numBoundaries = numBoundaries;
    }

    /**
     * The head of the linked double list.
     * 
     * @return the linked double list (the boundaries)
     */
    public LinkedDouble getHead() {
        return m_head;
    }

    /**
     * The number of boundaries.
     * 
     * @return the number of boundaries
     */
    public int getNumBoundaries() {
        return m_numBoundaries;
    }
}
