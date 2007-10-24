/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   18.03.2005 (mb): created
 *   09.01.2006 (mb): clean up for code review
 */
package org.knime.core.node.workflow;


/**
 * Interface for an object that's held within a NodeContainer and allows loading
 * and saving of information. Usually such objects will hold information about
 * the Node's position in some layout.
 * 
 * @author M. Berthold, University of Konstanz
 */
public interface NodeExtraInfo extends ExtraInfo {

    /**
     * Checks if all information for this extra info is set properly.
     * 
     * @return true if infos are set properly
     */
    public boolean isFilledProperly();

    /**
     * Changes the position according to the given moving distance.
     * 
     * @param moveDist the distance to change position
     */
    public void changePosition(final int[] moveDist);

    /**
     * Creates and returns a copy of this object.
     * @return a copy of this object
     * @throws  CloneNotSupportedException  if the object's class does not
     *               support the <code>Cloneable</code> interface. Subclasses
     *               that override the <code>clone</code> method can also
     *               throw this exception to indicate that an instance cannot
     *               be cloned.
     */
    public Object clone() throws CloneNotSupportedException;
}
