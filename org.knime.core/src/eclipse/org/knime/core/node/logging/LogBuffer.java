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
import java.util.function.Consumer;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.apache.log4j.Level;

/**
 * Simple fixed-size buffer of log messages. This class is <b>not thread-safe</b>.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class LogBuffer {

    private final CircularFiFoBuffer m_logBuffer;

    LogBuffer(final int bufferSize) {
        m_logBuffer = new CircularFiFoBuffer(bufferSize);
    }

    /**
     * Buffered log message for storage until the logging system is initialized and log messages can be written
     * to their intended location based on user configuration.
     *
     * @param instant instant the log message was buffered
     * @param name name of the logger (e.g. the class name)
     * @param level log level for the message
     * @param message {@link KNIMELogMessage} or {@link Object} to log
     * @param cause the nullable cause for the message
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    record BufferedLogMessage(Instant instant, String name, Level level, Object message,
        Throwable cause) { }

    /**
     * Ring buffer with fixed capacity that evicts the least recently inserted entry.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    private static final class CircularFiFoBuffer {

        private long m_evicted;
        private final BoundedFifoBuffer m_delegate;

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
         * Adds the given entry to the buffer, indicating if another entry was evicted to make room for the
         * given one.
         *
         * @param entry entry to buffer
         * @return {@code true} if another entry was evicted in order to add the given entry,
         *     {@code false} otherwise
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
         * @return number of evicted entries
         */
        long getNumberOfEvictedEntries() {
            return m_evicted;
        }

        /**
         * Checks whether the buffer is empty or not.
         * @return {@code true} if the buffer is empty, {@code false} otherwise
         */
        boolean isEmpty() {
            return m_delegate.isEmpty();
        }

        /**
         * Returns an iterator over the entries in the buffer, draining it in the process.
         * <p>
         * <b>Note:</b> this method is <i>not thread-safe</i>.
         *     Therefore, you have to synchronize over the parent object.
         *
         * @return draining iterator over contained entries in insertion order (from least recently inserted to
         *     most recently inserted)
         */
        Iterator<BufferedLogMessage> drainingIterator() {
            return new Iterator<BufferedLogMessage>() {
                @Override
                public boolean hasNext() {
                    return !m_delegate.isEmpty();
                }

                @Override
                public BufferedLogMessage next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return (BufferedLogMessage)m_delegate.remove();
                }
            };
        }

        private int size() {
            return m_delegate.size();
        }
    }

    /**
     * Drain the buffer to the given consumer. If the buffer evicted any messages, a warning message will be logged.
     *
     * @param consumer consumer for buffered log messages
     */
    void drainTo(final Consumer<BufferedLogMessage> consumer) {
        if (!m_logBuffer.isEmpty()) {
            final var logger = KNIMELogger.getLogger(KNIMELogger.class);
            final var current = m_logBuffer.size();
            final var evicted = m_logBuffer.getNumberOfEvictedEntries();
            final var total = current + evicted;
            final var countMessages = total > 1 ? "%d messages were".formatted(total) : "1 message was";
            logger.log(Level.DEBUG, () -> "%s logged before logging was initialized; see below..."
                .formatted(countMessages), null, false);
            if (evicted > 0) {
                logger.log(m_logBuffer.m_levelForEvictionMessage,
                    () -> "[*** Log incomplete: log buffer did wrap around -- "
                            + "%d messages were evicted from buffer in total ***]".formatted(evicted), null, false);
            }
            m_logBuffer.drainingIterator().forEachRemaining(consumer);
            logger.log(Level.DEBUG, "End of buffered log messages", null, false);
        }
    }

    /**
     * Buffer the given message at the given level under the given logger name.
     *
     * @param level level to log at
     * @param name logger name to log under
     * @param msg message to log
     * @param cause {@code null}-able cause for the message
     */
    void log(final Level level, final String name, final Object msg, final Throwable cause) {
        m_logBuffer.add(new BufferedLogMessage(Instant.now(), Objects.requireNonNull(name),
            Objects.requireNonNull(level), Objects.requireNonNullElse(msg, ""), cause));
    }

}
