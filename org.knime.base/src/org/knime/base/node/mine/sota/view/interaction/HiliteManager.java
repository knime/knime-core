/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   13.02.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.view.interaction;

import java.util.Set;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public interface HiliteManager {
    /**
     * Hilites the given objects.
     * 
     * @param objects The objects to select.
     */
    public void hilite(final Set<Hiliteable> objects);

    /**
     * Hilites the given object.
     * 
     * @param object The object to select.
     */
    public void hilite(final Hiliteable object);    
    
    /**
     * Unhilites the given objects.
     * 
     * @param objects The objects to unselect.
     */
    public void unHilite(final Set<Hiliteable> objects);
    
    /**
     * Unhilites the given object.
     * 
     * @param object The object to unselect.
     */
    public void unHilite(final Hiliteable object);      
    
    /**
     * Clears the current hilited objects.
     */
    public void clearHilite();
    
    /**
     * @return The currently hilited objects.
     */
    public Set<Hiliteable> getHilited();
    
    /**
     * Returns true if the given object is hilited.
     * 
     * @param object The object to check if it is hilited.
     * @return True if given object is hilited, false otherwise.
     */
    public boolean isHilited(final Hiliteable object);
}
