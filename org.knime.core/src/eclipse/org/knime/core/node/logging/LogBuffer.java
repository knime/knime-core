/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   9 Jan 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.logging;

import java.time.Instant;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.apache.log4j.Level;

/**
 * Simple fixed-size circular buffer for log messages.
 *
 * <p><strong>This class is not thread-safe.</strong> Callers must ensure that concurrent {@link #add} calls and calls
 * to {@link #drain} do not overlap.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class LogBuffer {

    private final CircularFiFoBuffer m_logBuffer;

    /**
     * Buffered log message for storage until the logging system is initialized and log messages can be written to their
     * intended location based on user configuration.
     *
     * @param instant instant the log message was buffered
     * @param name name of the logger (e.g. the class name)
     * @param level log level for the message
     * @param message {@link KNIMELogMessage} or {@link Object} to log
     * @param cause the nullable cause for the message
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    record BufferedLogMessage(Instant instant, String name, Level level, Object message, Throwable cause) {
    }

    /**
     * The result of draining the buffer. The caller exclusively owns the entries after {@link LogBuffer#drain()}
     * returns.
     *
     * @param messages iterator over the buffered entries in insertion order
     * @param evictedEntries number of entries evicted over the lifetime of the buffer
     * @param total total number of entries that passed through the buffer (buffered + evicted)
     * @param evictionMessageLevel most severe level among evicted entries, at least {@link Level#WARN}
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    record DrainResult(Iterator<BufferedLogMessage> messages, long evictedEntries, long total,
        Level evictionMessageLevel) {
    }

    /**
     * Ring buffer with fixed capacity that evicts the least recently inserted entry.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    private static final class CircularFiFoBuffer {

        private long m_evicted;

        private BoundedFifoBuffer m_delegate;

        /**
         * Level used for log message informing the user that some log messages were evicted.
         */
        private Level m_levelForEvictionMessage = Level.WARN;

        /**
         * Constructs a new buffer with the given capacity.
         *
         * @param capacity capacity of the buffer
         */
        CircularFiFoBuffer(final int capacity) {
            m_delegate = new BoundedFifoBuffer(capacity);
        }

        /**
         * Adds the given entry to the buffer, indicating if another entry was evicted to make room for the given one.
         *
         * @param entry entry to buffer
         * @return {@code true} if another entry was evicted in order to add the given entry, {@code false} otherwise
         */
        boolean add(final BufferedLogMessage entry) {
            var didEvict = false;
            if (m_delegate.isFull()) {
                didEvict = true;
                m_evicted++;

                // we increase the level for the message informing the user about evictions to the maximum of the
                // most severe evicted message and `Level.WARN`
                final var evictedLevel = ((BufferedLogMessage)m_delegate.remove()).level();
                if (!m_levelForEvictionMessage.isGreaterOrEqual(evictedLevel)) {
                    m_levelForEvictionMessage = evictedLevel;
                }
            }
            m_delegate.add(entry);
            return didEvict;
        }

        /**
         * Returns the number of evicted entries during the lifetime of this buffer.
         *
         * @return number of evicted entries
         */
        long getNumberOfEvictedEntries() {
            return m_evicted;
        }

        /**
         * Checks whether the buffer is empty or not.
         *
         * @return {@code true} if the buffer is empty, {@code false} otherwise
         */
        boolean isEmpty() {
            return m_delegate.isEmpty();
        }

        /**
         * Transfers ownership of all currently buffered entries to a detached iterator.
         * <p>
         * After this call, the delegate is set to {@code null}: the buffer must not be used further.
         *
         * @return iterator over the detached buffered entries in insertion order
         */
        private Iterator<BufferedLogMessage> transferEntries() {
            final var detachedEntries = m_delegate;
            m_delegate = null;
            return new Iterator<BufferedLogMessage>() {
                @SuppressWarnings("unchecked")
                private final Iterator<BufferedLogMessage> m_iterator = detachedEntries.iterator();

                @Override
                public boolean hasNext() {
                    return m_iterator.hasNext();
                }

                @Override
                public BufferedLogMessage next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return m_iterator.next();
                }
            };
        }

        private int size() {
            return m_delegate.size();
        }
    }

    LogBuffer(final int bufferSize) {
        m_logBuffer = new CircularFiFoBuffer(bufferSize);
    }

    /**
     * Adds a log message to the buffer.
     *
     * @param level level to log at
     * @param name logger name to log under
     * @param msg message to log
     * @param cause {@code null}-able cause for the message
     */
    void add(final Level level, final String name, final Object msg, final Throwable cause) {
        m_logBuffer.add(new BufferedLogMessage(Instant.now(), Objects.requireNonNull(name),
            Objects.requireNonNull(level), Objects.requireNonNullElse(msg, ""), cause));
    }

    /**
     * Checks whether the buffer is currently empty.
     *
     * @return {@code true} if the buffer is empty, {@code false} otherwise
     */
    boolean isEmpty() {
        return m_logBuffer.isEmpty();
    }

    /**
     * Transfers ownership of all buffered entries to the caller.
     * <p>
     * <strong>After this call, this buffer must not be used for further {@link #add} calls.</strong> The caller is
     * responsible for ensuring no concurrent {@link #add} calls are in progress when this method is invoked.
     *
     * @return drain result holding the buffered entries, or {@code null} if the buffer is empty
     */
    DrainResult drain() {
        if (m_logBuffer.isEmpty()) {
            return null;
        }
        final var evictedEntries = m_logBuffer.getNumberOfEvictedEntries();
        final var total = m_logBuffer.size() + evictedEntries;
        return new DrainResult(m_logBuffer.transferEntries(), evictedEntries, total,
            m_logBuffer.m_levelForEvictionMessage);
    }

}
