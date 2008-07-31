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
 *   Jun 27, 2005 (tg): created
 */
package org.knime.base.node.viz.parcoord.visibility;

import java.util.Set;

import org.knime.core.data.RowKey;

/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public interface VisibilityHandler {
    
    /** 
     * Adds the <code>VisibilityListener</code> from the list.
     * @param listener The selection listener to be removed from the list.
     */
    public void addVisibilityListener(final VisibilityListener listener);
    /** 
     * Removes the <code>VisibilityListener</code> from the list.
     * @param listener The selection listener to be removed from the list.
     */
    void removeVisibilityListener(final VisibilityListener listener);

    /** 
     * Checks if the given row <code>key</code> has been selected.
     * @param  key The row to check the hilite status of.
     * @return <code>true</code> if selected.
     */
    boolean isSelected(final RowKey key);

    
    /** 
     * Selects the given item and fires the event to all registered listeners.
     * @param  key The row key to select.
     */
    void select(final RowKey key);

    /** 
     * Selects the given keys and fires the event to all registered listeners.
     * @param  keys The set of row keys to select.
     */
    void select(final Set<RowKey> keys);

    /** 
     * Unselects the given item and fires the event to all registered listeners.
     * @param  key The row key to reset select status.
     */
    void unselect(final RowKey key);

    /** 
     * Uslects the given keys and fires the event to all registered listeners.
     * @param  keys The set of row keys to unselect.
     */
    void unselect(final Set<RowKey> keys);
    
    /** 
     * Unselects all selected items and fires the event.
     */
    void resetSelection();
    

    
    /** 
     * Checks if the given row <code>key</code> has been selected.
     * @param  key The row to check the hilite status of.
     * @return <code>true</code> if selected.
     */
    boolean isVisible(final RowKey key);
    
    /** 
     * Selects the given item and fires the event to all registered listeners.
     * @param  key The row key to select.
     */
    void makeVisible(final RowKey key);

    /** 
     * Selects the given keys and fires the event to all registered listeners.
     * @param  keys The set of row keys to select.
     */
    void makeVisible(final Set<RowKey> keys);

    /** 
     * Unselects the given item and fires the event to all registered listeners.
     * @param  key The row key to reset select status.
     */
    void makeInvisible(final RowKey key);

    /** 
     * Uslects the given keys and fires the event to all registered listeners.
     * @param  keys The set of row keys to unselect.
     */
    void makeInvisible(final Set<RowKey> keys);
    
    /** 
     * Makes all selected items visible and fires the event.
     */
    void resetVisibility();
}
