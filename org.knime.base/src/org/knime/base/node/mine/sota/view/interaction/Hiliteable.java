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
 *   Dec 8, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.view.interaction;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public interface Hiliteable {
    /**
     * Returns <code>true</code> if object is hilited, <code>false</code> if
     * not.
     * 
     * @return <code>true</code> if object is hilited, <code>false</code> if
     *         not
     */
    public boolean isHilited();

    /**
     * Sets the given hilit flag.
     * 
     * @param hilit hilitflag to set
     */
    public void setHilited(boolean hilit);
}
