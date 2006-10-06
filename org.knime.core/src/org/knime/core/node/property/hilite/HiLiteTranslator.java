/*  
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.DataCell;


/**
 * A translator for hilite events between two {@link HiLiteHandler}s, 
 * the source hilite handler has to be set during creating of this object, 
 * whereby the target hilite handlers can be set independently, as well as the 
 * mapping which is defined between {@link DataCell} keys and {@link DataCell}
 * sets.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class HiLiteTranslator implements HiLiteListener {
    /** Handlers where to fire events to. Contains the patterns. */
    private final Set<HiLiteHandler> m_toHandlers;
    
    /** Handler where events have been fired from. */
    private final HiLiteHandler m_fromHandler;
    
    /** Containing cluster to pattern mapping. */
    private HiLiteMapper m_mapper;
    
    
    /** Listener that sends events back in the opposite direction. */
    private final HiLiteListener m_sendBackListener;
    
    /**
     * Default constructor with no hilite handler and no initial mapping to
     * translate. This instance will add itself as listener to the
     * <code>fromHandler</code>.
     * 
     * @param fromHandler the handler to translate events from
     */
    public HiLiteTranslator(final HiLiteHandler fromHandler) {
        if (fromHandler == null) {
            throw new IllegalArgumentException("Handler must not be null.");
        }
        m_fromHandler = fromHandler;
        m_fromHandler.addHiLiteListener(this);
        m_toHandlers = new LinkedHashSet<HiLiteHandler>();
        m_mapper = null;
        
        m_sendBackListener = new HiLiteListener() {
            public void hiLite(final KeyEvent event) { /* do nothing */ }
            public void unHiLite(final KeyEvent event) { /* do nothing */ }
            public void unHiLiteAll() {
                // to avoid that the event travels forth and back
                // do a check here if there is still something to unhilite
                if (m_fromHandler.getHiLitKeys().size() > 0) {
                    m_fromHandler.fireClearHiLiteEvent();
                }
            }
        };
    }
    
    /**
     * Default constructor with no hilite handler and no initial mapping to
     * translate. This instance will add itself as listener to the
     * <code>fromHandler</code>.
     * 
     * @param fromHandler The handler to translate events from.
     * @param mapper Contains the cluster to pattern mapping.
     */
    public HiLiteTranslator(
            final HiLiteHandler fromHandler, final HiLiteMapper mapper) {
        this(fromHandler);
        m_mapper = mapper;
    }
    
    /**
     * Sets a new hilite mapper which can be <code>null</code> in case no
     * hilite translation is available.
     * 
     * @param mapper the new hilite mapper
     */
    public void setMapper(final HiLiteMapper mapper) {
        m_fromHandler.fireClearHiLiteEvent();
        m_mapper = mapper;
    }
    
    /**
     * @return The mapper which contains the mapping, can be null.
     */
    public HiLiteMapper getMapper() {
        return m_mapper;
    }
        
    /**
     * Removes a <code>HiLiteHandler</code> from "to"-set.
     * 
     * @param toHandler the hilite handler to remove
     */
    public void removeToHiLiteHandler(final HiLiteHandler toHandler) {
        if (toHandler != null) {
            m_toHandlers.remove(toHandler);
            toHandler.removeHiLiteListener(m_sendBackListener);
        }
    }    

    /**
     * Adds a <code>HiLiteHandler</code>.
     * 
     * @param toHandler the new to-hilite handler to add
     */
    public void addToHiLiteHandler(final HiLiteHandler toHandler) {
        if (toHandler != null) {
            m_toHandlers.add(toHandler);
            toHandler.addHiLiteListener(m_sendBackListener);
        }
    }
    
    /**
     * An unmodifiable set of target hilite handlers.
     * 
     * @return a set of target hilite handlers
     */
    public Set<HiLiteHandler> getToHiLiteHandlers() {
        return Collections.unmodifiableSet(m_toHandlers);
    }
    
    /**
     * Removes all receiving hilite handlers from this translator. To be
     * used from the node that instantiates this instance when a new 
     * connection is made. 
     */
    public void removeAllToHiliteHandlers() {
        for (HiLiteHandler hh : m_toHandlers) {
            hh.removeHiLiteListener(m_sendBackListener);
        }
        m_toHandlers.clear();
    }

    /**
     * The hilite handler events are translated from.
     * 
     * @return the hilite handler events are translated from. 
     */
    public HiLiteHandler getFromHiLiteHandler() {
        return m_fromHandler;
    }

    /**
     * @see HiLiteListener#hiLite(KeyEvent)
     */
    public void hiLite(final KeyEvent event) {
        if (m_mapper != null && m_toHandlers.size() > 0) {
            HashSet<DataCell> fireSet = new HashSet<DataCell>();
            for (DataCell key : event.keys()) {
                if (key != null) {
                    Set<DataCell> s = m_mapper.getKeys(key);
                    if (s != null && !s.isEmpty()) {
                        fireSet.addAll(s);
                    }
                }     
            }
            if (!fireSet.isEmpty()) {
                for (HiLiteHandler h : m_toHandlers) {
                    h.fireHiLiteEvent(fireSet);
                }
            }
        }
    }

    /**
     * @see HiLiteListener#unHiLite(KeyEvent)
     */
    public void unHiLite(final KeyEvent event) {
        if (m_mapper != null && m_toHandlers.size() > 0) {
            HashSet<DataCell> fireSet = new HashSet<DataCell>();
            for (DataCell key : event.keys()) {
                if (key != null) {
                    Set<DataCell> s = m_mapper.getKeys(key);
                    if (s != null && !s.isEmpty()) {
                        fireSet.addAll(s);
                    }
                }     
            }
            if (!fireSet.isEmpty()) {
                for (HiLiteHandler h : m_toHandlers) {
                    h.fireUnHiLiteEvent(fireSet);
                }
            }
        }
    }

    /**
     * @see HiLiteListener#unHiLiteAll()
     */
    public void unHiLiteAll() {
        for (HiLiteHandler h : m_toHandlers) {
            h.fireClearHiLiteEvent();
        }
    }
}
