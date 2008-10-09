/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 * 
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.Set;

import org.knime.core.data.RowKey;


/**
 * Interface for all hilite handlers supporting set/reset of hilite status
 * and un/registering of listeners.
 * 
 * All methods are public, allowing for objects being manager and listener at
 * the same time (i.e. they can change the status and query the current status).
 * 
 * {@link org.knime.core.node.property.hilite.HiLiteListener} can register
 * and will receive {@link org.knime.core.node.property.hilite.KeyEvent}
 * objects informing them about a change of status of certain row IDs.
 * 
 * <br />
 * Due to performance issues (as all views for example will query the status
 * from this object) all hilite handlers should be able to answer calls to get
 * status methods quickly.
 * 
 * @see DefaultHiLiteHandler
 * @see HiLiteListener
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class HiLiteHandler {
    
    /**
     * Constant for the Menu entry 'HiLite'.
     */
    public static final String HILITE = "HiLite";
    
    /**
     * Constant for the menu entry 'HiLite Selected'.
     */
    public static final String HILITE_SELECTED = "HiLite Selected";
    
    /**
     * Constant for the menu entry 'UnHiLite Selected'.
     */
    public static final String UNHILITE_SELECTED = "UnHiLite Selected";
    
    /**
     * Constant for the menu entry 'Clear HiLite'.
     */
    public static final String CLEAR_HILITE = "Clear HiLite";
    
    /**
     * Adds a new <code>HiLiteListener</code> to the list of registered
     * listener objects that will then in turn receive (un)hilite events.
     * 
     * @param listener the hilite listener to add
     */
    public abstract void addHiLiteListener(final HiLiteListener listener);

    /**
     * Removes the <code>HiLiteListener</code> from the list.
     * 
     * @param listener the hilite listener to remove from the list
     */
    public abstract void removeHiLiteListener(final HiLiteListener listener);

    /**
     * Removes all hilite listeners from this handler.
     */
    public abstract void removeAllHiLiteListeners();

    /**
     * Checks if the given row IDs have been hilit.
     * 
     * @param ids the row IDs to check the hilite status of
     * @return <code>true</code> if all IDs are hilit, <code>false</code>
     * otherwise
     */
    public abstract boolean isHiLit(final RowKey... ids);

    /**
     * Returns an unmodifiable set of hilit keys.
     * 
     * @return a set of hilit keys.
     */
    public abstract Set<RowKey> getHiLitKeys();

    /**
     * Hilites the given items and fires an event to all registered listeners.
     * 
     * @param ids an array of row IDs to hilite
     */
    public abstract void fireHiLiteEvent(final RowKey... ids);

    /**
     * Hilites the given keys and fires an event to all registered listeners.
     * 
     * @param ids the set of row keys to hilite
     */
    public abstract void fireHiLiteEvent(final Set<RowKey> ids);

    /**
     * Unhilites the given items and fires the event to all registered
     * listeners.
     * 
     * @param ids an array of row IDs to reset hilite status
     */
    public abstract void fireUnHiLiteEvent(final RowKey... ids);

    /**
     * Unhilites the given keys and fires an event to all registered listeners.
     * 
     * @param ids the set of row IDs to unhilite
     */
    public abstract void fireUnHiLiteEvent(final Set<RowKey> ids);

    /**
     * Unhilites all hilit items and fires an event.
     */
    public abstract void fireClearHiLiteEvent();
    
    /**
     * Internal fire hilite method used to fire specific hilite events.
     * @param event the event to fire
     */
    protected abstract void fireHiLiteEventInternal(final KeyEvent event);
    
    /**
     * Internal fire hilite method used to fire specific unhilite events.
     * @param event the event to fire
     */
    protected abstract void fireUnHiLiteEventInternal(final KeyEvent event);
    
    /**
     * Internal fire hilite method used to fire specific clear hilite events.
     * @param event the event to fire
     */
    protected abstract void fireClearHiLiteEventInternal(final KeyEvent event);
}
