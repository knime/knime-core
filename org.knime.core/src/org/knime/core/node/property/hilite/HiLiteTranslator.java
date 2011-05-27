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
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.RowKey;

/**
 * A translator for hilite events between one source (from)
 * {@link HiLiteHandler} and a number of target handlers (to). The source hilite
 * handler is passed through the constructor of this class. The target hilite
 * handlers can be set independently, as well as the mapping which is defined
 * between {@link RowKey} row keys and {@link RowKey} sets.
 * <p>
 * This class hosts two listeners one which is registered with the source
 * handler and one which is registered with all target handlers. These listeners
 * are called when something changes either on the source or target side, and
 * then invoke the corresponding handlers on the other side to hilite, unhilite,
 * and clear mapped keys.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class HiLiteTranslator {

    /** Target handler used for hiliting on the aggregation side. */
    private final Set<HiLiteHandler> m_targetHandlers;

    /** Source handlers used for hiliting for single items. */
    private final HiLiteHandler m_sourceHandler;

    /** Contains the mapping between aggregation and single items. */
    private HiLiteMapper m_mapper;

    /** Event source used to indicate hilite events fired by this translator. */
    private final Object m_eventSource = this;

    /**
     * Listener on the source handler used to forward events
     * to all registered target handlers.
     */
    private final HiLiteListener m_sourceListener = new HiLiteListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void hiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            if (m_mapper != null && m_targetHandlers.size() > 0) {
                Set<RowKey> fireSet = new LinkedHashSet<RowKey>();
                for (RowKey key : event.keys()) {
                    Set<RowKey> s = m_mapper.getKeys(key);
                    if (s != null && !s.isEmpty()) {
                        fireSet.addAll(s);
                    }
                }
                if (!fireSet.isEmpty()) {
                    for (HiLiteHandler h : m_targetHandlers) {
                        h.fireHiLiteEvent(new KeyEvent(m_eventSource, fireSet));
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void unHiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            if (m_mapper != null && m_targetHandlers.size() > 0) {
                Set<RowKey> fireSet = new LinkedHashSet<RowKey>();
                for (RowKey key : event.keys()) {
                    Set<RowKey> s = m_mapper.getKeys(key);
                    if (s != null && !s.isEmpty()) {
                        fireSet.addAll(s);
                    }
                }
                if (!fireSet.isEmpty()) {
                    for (HiLiteHandler h : m_targetHandlers) {
                        h.fireUnHiLiteEvent(
                            new KeyEvent(m_eventSource, fireSet));
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void unHiLiteAll(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            for (HiLiteHandler h : m_targetHandlers) {
                h.fireClearHiLiteEvent(new KeyEvent(m_eventSource));
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
        @Override
        public void hiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            if (m_mapper != null) {
                // add all hilite keys from the event and all hilite keys
                // from the target hilite handlers
                final Set<RowKey> all = new LinkedHashSet<RowKey>(
                        event.keys());
                for (HiLiteHandler hdl : m_targetHandlers) {
                    all.addAll(hdl.getHiLitKeys());
                }
                // check overlap with all mappings
                for (RowKey key : m_mapper.keySet()) {
                    final Set<RowKey> keys = m_mapper.getKeys(key);
                    // if all mapped keys are hilite then fire event
                    if (all.containsAll(keys)) {
                        m_sourceHandler.fireHiLiteEvent(
                            new KeyEvent(m_eventSource, key));
                    }
                }
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void unHiLite(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            if (m_mapper != null) {
                // check all mappings
                for (RowKey key : m_mapper.keySet()) {
                    final Set<RowKey> keys = m_mapper.getKeys(key);
                    // if at least one item is unhilite then fire event
                    for (RowKey hilite : event.keys()) {
                        if (keys.contains(hilite)) {
                            m_sourceHandler.fireUnHiLiteEvent(
                                new KeyEvent(m_eventSource, key));
                            break;
                        }
                    }
                }
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void unHiLiteAll(final KeyEvent event) {
            if (event.getSource() == m_eventSource) {
                return;
            }
            m_sourceHandler.fireClearHiLiteEvent(new KeyEvent(m_eventSource));
        }
    };

    /**
     * Creates a translator with an empty mapping and a default hilite
     * handler.
     */
    public HiLiteTranslator() {
        this((HiLiteMapper) null);
    }

    /**
     * Creates a new translator.
     * @param handler a given source <code>HiLiteHandler</code>
     */
    public HiLiteTranslator(final HiLiteHandler handler) {
        this(handler, null);
    }

    /**
     * Creates a new translator.
     * @param mapper mapping from aggregation to single patterns
     */
    public HiLiteTranslator(final HiLiteMapper mapper) {
        this(new HiLiteHandler(), mapper);
    }

    /**
     * Creates a new translator.
     * @param handler a given source <code>HiLiteHandler</code>
     * @param mapper mapping from aggregation to single patterns
     */
    public HiLiteTranslator(final HiLiteHandler handler,
            final HiLiteMapper mapper) {
        if (handler == null) {
            throw new IllegalArgumentException(
                    "Source HiLiteHandler must not be null.");
        }
        m_sourceHandler = handler;
        m_targetHandlers = new LinkedHashSet<HiLiteHandler>();
        m_mapper = mapper;
    }

    /**
     * Sets a new hilite mapper which can be <code>null</code> in case no
     * hilite translation is available.
     * @param mapper the new hilite mapper
     */
    public void setMapper(final HiLiteMapper mapper) {
        m_mapper = mapper;
    }

    /**
     * @return mapper which contains the mapping, can be null
     */
    public HiLiteMapper getMapper() {
        return m_mapper;
    }

    /**
     * Removes the given target <code>HiLiteHandler</code> from the list of
     * registered hilite handlers and removes the private target listener from
     * if the list of hilite keys is empty.
     *
     * @param targetHandler the target hilite handler to remove
     */
    public void removeToHiLiteHandler(final HiLiteHandler targetHandler) {
        if (targetHandler != null) {
            m_sourceListener.unHiLite(new KeyEvent(targetHandler,
                    m_sourceHandler.getHiLitKeys()));
            m_targetHandlers.remove(targetHandler);
            targetHandler.removeHiLiteListener(m_targetListener);
            if (m_targetHandlers.isEmpty()) {
                m_sourceHandler.removeHiLiteListener(m_sourceListener);
            }
        }
    }

    /**
     * Adds a new target <code>HiLiteHandler</code> to the list of registered
     * hilite handlers and adds the private target listener if the list of
     * hilite keys is empty.
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
            m_targetListener.hiLite(new KeyEvent(targetHandler,
                    targetHandler.getHiLitKeys()));
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
        Set<HiLiteHandler> copy = new HashSet<HiLiteHandler>(m_targetHandlers);
        for (HiLiteHandler hh : copy) {
            removeToHiLiteHandler(hh);
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
