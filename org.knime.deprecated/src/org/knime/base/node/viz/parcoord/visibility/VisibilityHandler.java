/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
