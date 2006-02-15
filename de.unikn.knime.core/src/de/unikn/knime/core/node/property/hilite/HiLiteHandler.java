/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node.property.hilite;

import java.awt.Color;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;

/** 
 * Interface for all highlight handlers supporting set/reset of highlight
 * status and un/registering of listeners.
 * 
 * All methods are public, allowing for objects being manager and listener at 
 * the same time (i.e. they can change the status and query the current status).
 * 
 * <code>HiLiteListener</code> can register and will receive 
 * <code>KeyEvent</code> objects informing them about a change of status of
 * certain row IDs.
 *
 * <p>Due to performance issues (as all views for example will query the status
 * from this object) all hilit handlers should be able to answer calls to
 * get status methods quickly.
 * 
 * @see DefaultHiLiteHandler
 * @see HiLiteListener
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface HiLiteHandler {
    
    /** 
     * The <code>Color</code> which has to be used for diplaying hilit items.
     */
    public static final Color COLOR_HILITE = Color.CYAN;
    
    /** 
     * Adds a new <code>HiLiteListener</code> to the list of registered 
     * listener objects that will then in turn recieve (un)hilite events. 
     * @param listener The hilite listener to add.
     */
    void addHiLiteListener(final HiLiteListener listener);

    /** 
     * Removes the <code>HiLiteListener</code> from the list.
     * @param listener The hilite listener to be removed from the list.
     */
    void removeHiLiteListener(final HiLiteListener listener);

    /** 
     * Checks if the given row ID has been hilit.
     * @param  rowID The row ID to check the hilite status of.
     * @return <code>true</code> if highlighted.
     */
    boolean isHiLit(final DataCell rowID);
    
    /** 
     * Hilites the given item and fires the event to all registered listeners.
     * @param  rowID The row key to hilite.
     */
    void hiLite(final DataCell rowID);

    /** 
     * Hilites the given keys and fires the event to all registered listeners.
     * @param  ids The set of row keys to hilite.
     */
    void hiLite(final Set<DataCell> ids);

    /** 
     * Unhilites the given item and fires the event to all registered listeners.
     * @param  rowID The row ID to reset hilite status.
     */
    void unHiLite(final DataCell rowID);

    /** 
     * Unhilites the given keys and fires the event to all registered listeners.
     * @param  ids The set of row IDs to unhilite.
     */
    void unHiLite(final Set<DataCell> ids);
    
    /** 
     * Unhilites all hilit items and fires a the event.
     */
    void resetHiLite();

}   // HiLiteHandler
