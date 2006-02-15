/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.core.node.property.hilite;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import de.unikn.knime.core.data.DataCell;

/**
 * A translator for hilite events between two <code>HiLiteHandler</code>, both
 * can be set independently as well as the mapping which is defined between
 * <code>DataCell</code> keys and <code>DataCellSet</code> objects.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class HiLiteTranslator implements HiLiteListener {
    
    /** Handlers where to fire events to. Contains the patterns. */
    private final Vector<HiLiteHandler> m_toHandlers;
    /** Handler where events had fired from. Contains the patterns. */
    private final HiLiteHandler m_fromHandler;
    /** Containing cluster to pattern mapping. */
    private HiLiteMapper m_mapper;
    
    /**
     * Default constructor with no hilite handler and no initial mapping to
     * translate.
     * @param fromHandler The handler to translate events from.
     */
    public HiLiteTranslator(final HiLiteHandler fromHandler) {
        if (fromHandler == null) {
            throw new IllegalArgumentException("Handler must not be null.");
        }
        m_fromHandler = fromHandler;
        m_toHandlers = new Vector<HiLiteHandler>();
        m_mapper = null;
    }
    
    /**
     * Default constructor with no hilite handler and no initial mapping to
     * translate.
     * @param fromHandler The handler to translate events from.
     * @param mapper Contains the cluster to pattern mapping.
     */
    public HiLiteTranslator(
            final HiLiteHandler fromHandler, final HiLiteMapper mapper) {
        this(fromHandler);
        m_mapper = mapper;
    }
    
    /**
     * Sets a new hilite mapper which can be null in case no hilite translation
     * is available.
     * @param mapper The new hilite mapper.
     */
    public void setMapper(final HiLiteMapper mapper) {
        m_fromHandler.resetHiLite();
        m_mapper = mapper;
    }
    
    

    /**
     * Adds a to <code>HiLiteHandler</code>.
     * @param toHandler The new to hilite handler to add.
     */
    public void addToHiLiteHandler(final HiLiteHandler toHandler) {
        if (toHandler != null) {
            m_toHandlers.add(toHandler);
        }
    }
    
    /**
     * @return the current to hilit handlers.
     */
    public Enumeration getToHiLiteHandlers() {
        return m_toHandlers.elements();
    }

    /**
     * @return The hilite handler events are translated from. 
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
                for (int i = 0; i < m_toHandlers.size(); i++) {
                    m_toHandlers.get(i).hiLite(fireSet);
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
                for (int i = 0; i < m_toHandlers.size(); i++) {
                    m_toHandlers.get(i).unHiLite(fireSet);
                }
            }
        }
    }

    /**
     * @see HiLiteListener#resetHiLite()
     */
    public void resetHiLite() {
        for (int i = 0; i < m_toHandlers.size(); i++) {
            m_toHandlers.get(i).resetHiLite();
        }
    }
    
   
}
