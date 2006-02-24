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

import java.util.Set;

import de.unikn.knime.core.data.DataCell;

/**
 * Interface for all highlight handlers supporting set/reset of highlight status
 * and un/registering of listeners.
 * 
 * All methods are public, allowing for objects being manager and listener at
 * the same time (i.e. they can change the status and query the current status).
 * 
 * <code>HiLiteListener</code> can register and will receive
 * <code>KeyEvent</code> objects informing them about a change of status of
 * certain row IDs.
 * 
 * <p>
 * Due to performance issues (as all views for example will query the status
 * from this object) all hilit handlers should be able to answer calls to get
 * status methods quickly.
 * 
 * @see DefaultHiLiteHandler
 * @see HiLiteListener
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface HiLiteHandler {

    /**
     * Adds a new <code>HiLiteListener</code> to the list of registered
     * listener objects that will then in turn recieve (un)hilite events.
     * 
     * @param listener The hilite listener to add.
     */
    void addHiLiteListener(final HiLiteListener listener);

    /**
     * Removes the <code>HiLiteListener</code> from the list.
     * 
     * @param listener The hilite listener to be removed from the list.
     */
    void removeHiLiteListener(final HiLiteListener listener);

    /**
     * Removes all hilite listeners from this handler.
     */
    void removeAllHiLiteListeners();

    /**
     * Checks if the given row IDs hava been hilit.
     * 
     * @param ids The row IDs to check the hilite status of.
     * @return <code>true</code> if all IDs are hilit.
     */
    boolean isHiLit(final DataCell... ids);

    /**
     * @return An unmodifiable set of hilit keys.
     */
    Set<DataCell> getHiLitKeys();

    /**
     * Hilites the given itemsand fires the event to all registered listeners.
     * 
     * @param ids An array of row IDs to hilite.
     */
    void hiLite(final DataCell... ids);

    /**
     * Hilites the given keys and fires the event to all registered listeners.
     * 
     * @param ids The set of row keys to hilite.
     */
    void hiLite(final Set<DataCell> ids);

    /**
     * Unhilites the given items and fires the event to all registered
     * listeners.
     * 
     * @param ids An array of row IDs to reset hilite status.
     */
    void unHiLite(final DataCell... ids);

    /**
     * Unhilites the given keys and fires the event to all registered listeners.
     * 
     * @param ids The set of row IDs to unhilite.
     */
    void unHiLite(final Set<DataCell> ids);

    /**
     * Unhilites all hilit items and fires a the event.
     */
    void resetHiLite();

} // HiLiteHandler
