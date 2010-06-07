/*  
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A manager for hilite events between one source (from) {@link HiLiteHandler} 
 * and a number of target handlers (to). This class provides one source hilite 
 * handler instantiated within the constructor. The target hilite handlers can 
 * be added individually.
 * <p>
 * This class hosts two listeners one which is registered with the source
 * handler and one which is registered with all target handlers. These listeners
 * are called when something changes either on the source (from) or target side 
 * (to), and then invoke the corresponding handlers on the other side to hilite,
 * unhilite, and clear hilite.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class HiLiteManager {
    
    /** Target handler used for hiliting on the aggregation side. */
    private final Set<HiLiteHandler> m_targetHandlers;
    
    /** Source handlers used for hiliting for single items. */
    private final HiLiteHandler m_sourceHandler;
    
    private final Object m_eventSource = this;
    
    /** 
     * Listener on the source handler used to forward events  
     * to all registered target handlers.
     */
    private final HiLiteListener m_sourceListener = new HiLiteListener() {
        /**
         * {@inheritDoc}
         */
        public void hiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            for (HiLiteHandler h : m_targetHandlers) {
                h.fireHiLiteEvent(new KeyEvent(m_eventSource, event.keys()));
            }
        }

        /**
         * {@inheritDoc}
         */
        public void unHiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            for (HiLiteHandler h : m_targetHandlers) {
                h.fireUnHiLiteEvent(
                        new KeyEvent(m_eventSource, event.keys()));
            }
        }

        /**
         * {@inheritDoc}
         */
        public void unHiLiteAll(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            for (HiLiteHandler h : m_targetHandlers) {
                h.fireClearHiLiteEvent(
                        new KeyEvent(m_eventSource, event.keys()));
            }
        }
    };
        
    /** 
     * Listener to all target handlers that send clear hilite
     * events to the source handler.
     */
    private final HiLiteListener m_targetListener = new HiLiteListener() {
        /**
         * {@inheritDoc}
         */
        public void hiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            m_sourceHandler.fireHiLiteEvent(
                    new KeyEvent(m_eventSource, event.keys()));
        }
        /**
         * {@inheritDoc}
         */
        public void unHiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            m_sourceHandler.fireUnHiLiteEvent(
                                new KeyEvent(m_eventSource, event.keys()));
 
        }
        /**
         * {@inheritDoc}
         */
        public void unHiLiteAll(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            m_sourceHandler.fireClearHiLiteEvent(
                    new KeyEvent(m_eventSource, event.keys()));
        }
    };
    
    /**
     * Creates a new manager.
     */
    public HiLiteManager() {
        m_sourceHandler = new HiLiteHandler();
        m_targetHandlers = new LinkedHashSet<HiLiteHandler>();
    }
    
    /**
     * Removes the given target <code>HiLiteHandler</code> from the list of 
     * registered hilite handlers and removes the private target listener from 
     * if the list of hilit keys is empty.
     * 
     * @param targetHandler the target hilite handler to remove
     */
    public void removeToHiLiteHandler(final HiLiteHandler targetHandler) {
        if (targetHandler != null) {
            m_targetHandlers.remove(targetHandler);
            targetHandler.removeHiLiteListener(m_targetListener);
            if (m_targetHandlers.isEmpty()) {
                m_sourceHandler.removeHiLiteListener(m_sourceListener);
            }
        }
    }    

    /**
     * Adds a new target <code>HiLiteHandler</code> to the list of registered 
     * hilite handlers and adds the private target listener if the list of hilit
     * keys is empty.
     * 
     * @param targetHandler the target hilite handler to add
     */
    public void addToHiLiteHandler(final HiLiteHandler targetHandler) {
        if (targetHandler != null) {
            if (m_targetHandlers.isEmpty()) {
                m_sourceHandler.addHiLiteListener(m_sourceListener);
            }
            m_targetHandlers.add(targetHandler);
            targetHandler.addHiLiteListener(m_targetListener);
        }
    }
    
    /**
     * An unmodifiable set of target hilite handlers.
     * 
     * @return the set of target hilite handlers
     */
    public Set<HiLiteHandler> getToHiLiteHandlers() {
        return Collections.unmodifiableSet(m_targetHandlers);
    }
    
    /**
     * Removes all target hilite handlers from this translator. To be
     * used from the node that instantiates this instance when a new 
     * connection is made. 
     */
    public void removeAllToHiliteHandlers() {
        for (HiLiteHandler hh : m_targetHandlers) {
            hh.removeHiLiteListener(m_targetListener);
        }
        m_targetHandlers.clear();
        m_sourceHandler.removeHiLiteListener(m_sourceListener);
    }

    /**
     * The source hilite handler.
     * 
     * @return source hilite handler
     */
    public HiLiteHandler getFromHiLiteHandler() {
        return m_sourceHandler;
    }

}
