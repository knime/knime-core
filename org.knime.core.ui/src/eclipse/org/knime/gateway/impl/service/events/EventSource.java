/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 25, 2021 (hornm): created
 */
package org.knime.gateway.impl.service.events;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * An event source produces events that are forwarded to an event consumer.
 *
 * The implementations call {@link #sendEvent(Object)} to emit events.
 *
 * An event source is directly associated with a certain event-type and is able to register event listener
 * ({@link #addEventListener(Object)} depending on the concrete event type instance.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz
 *
 * @param <T> the event type this event source is associated with
 * @param <E> the kind of event being emitted
 */
public abstract class EventSource<T, E> {

    private final BiConsumer<String, Object> m_eventConsumer;

    private final Object m_sendEventLock = new Object();

    /*
     * For testing purposes only.
     */
    private Runnable m_preEventCreationCallback = null;

    /**
     * @param eventConsumer the consumer for forward the emitted events to
     */
    protected EventSource(final BiConsumer<String, Object> eventConsumer) {
        m_eventConsumer = eventConsumer;
    }

    /**
     * Registers a listener with the event source for the specified type of event and optionally returns the very first
     * event to 'catch up' (instead of passing it to the associated event consumer).
     *
     * @param eventTypeEnt
     * @return the very first event or an empty optional if there isn't any (method must not wait for events to arrive -
     *         only returns if there is an event at event listener registration time)
     * @throws IllegalArgumentException if object describing the event type isn't valid
     */
    public abstract Optional<E> addEventListenerAndGetInitialEvent(T eventTypeEnt);

    /**
     * Registers a listener with the event source for the specified type of event.
     *
     * @param eventTypeEnt
     *
     * @throws IllegalArgumentException if object describing the event type isn't valid
     */
    public void addEventListener(final T eventTypeEnt) {
        // make sure the returned event is the first being send!
        synchronized (m_sendEventLock) {
            addEventListenerAndGetInitialEvent(eventTypeEnt).ifPresent(this::sendEvent);
        }
    }

    /**
     * Removes an event listener for a particular event type instance.
     *
     * @param eventTypeEnt
     */
    public abstract void removeEventListener(T eventTypeEnt);

    /**
     * Removes all event listeners registered with the event source. After this method, no events will be emitted
     * anymore.
     */
    public abstract void removeAllEventListeners();

    /**
     * Called by sub-classes to emit an event.
     *
     * @param event the event instance
     */
    protected final void sendEvent(final E event) {
        synchronized (m_sendEventLock) {
            m_eventConsumer.accept(getName(), event);
        }
    }

    /**
     * @return a name unique to the event source
     */
    protected abstract String getName();

    /**
     * For testing purposes only!
     *
     * @param callback
     */
    public final void setPreEventCreationCallback(final Runnable callback) {
        m_preEventCreationCallback = callback;
    }

    /**
     * For testing purposes only!
     */
    protected void preEventCreation() {
        if (m_preEventCreationCallback != null) {
            m_preEventCreationCallback.run();
        }
    }

}
