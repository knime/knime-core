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
 *   12.02.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.view.interaction;

import java.util.Set;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public interface SelectionManager {

    /**
     * Selects the given objects.
     * 
     * @param objects The objects to select.
     */
    public void select(final Set<Selectable> objects);

    /**
     * Selects the given object.
     * 
     * @param object The object to select.
     */
    public void select(final Selectable object);    
    
    /**
     * Unselects the given objects.
     * 
     * @param objects The objects to unselect.
     */
    public void unSelect(final Set<Selectable> objects);
    
    /**
     * Unselects the given object.
     * 
     * @param object The object to unselect.
     */
    public void unSelect(final Selectable object);      
    
    /**
     * Clears the current selection.
     */
    public void clearSelection();
    
    /**
     * @return The currently selected objects.
     */
    public Set<Selectable> getSelected();
    
    /**
     * Returns true if the given object is selected.
     * 
     * @param object The object to check if it is selected.
     * @return True if given object is selected, false otherwise.
     */
    public boolean isSelected(final Selectable object);
}
