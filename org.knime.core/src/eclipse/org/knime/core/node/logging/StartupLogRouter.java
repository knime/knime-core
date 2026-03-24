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
 *   24 Mar 2026 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.logging;

import java.io.PrintStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.knime.core.node.NodeLoggerPatternLayout;
import org.knime.core.node.logging.LogBuffer.BufferedLogMessage;

/**
 * Routes pre-initialization log messages: buffers until the logging system is initialized (normal path), or emits to a
 * configurable output stream when the JVM shuts down before initialization completes (failsafe path).
 *
 * <h2>Ownership</h2>
 * <p>
 * This class exclusively owns the {@link LogBuffer} it was constructed with. No other code may access the buffer
 * directly. Ownership of buffered entries is transferred to the consumer passed to {@link #drainToLogger}, or to this
 * class during {@link #activateFailsafeAndDrain()}.
 *
 * <h2>Routing state</h2>
 * <p>
 * Starts in {@link Target#BUFFER} state. Transitions to {@link Target#FAILSAFE} at most once, via
 * {@link #activateFailsafeAndDrain()}.
 * <p>
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class StartupLogRouter {

    /**
     * Environment variable controlling where the logging failsafe output is written. Values: "stderr"
     * (case-insensitive) for {@code stderr}, anything else (including if not set) for {@code stdout}.
     */
    private static final String ENV_LOGGING_FAILSAFE_TARGET = "KNIME_CORE_LOGGING_FAILSAFE_TARGET";

    /**
     * Environment variable controlling the minimum level written by the logging failsafe output. Values: any valid
     * Log4j 1 level name (case-insensitive), e.g. "INFO". Defaults to "DEBUG" if not set or invalid.
     */
    private static final String ENV_LOGGING_FAILSAFE_MIN_LEVEL = "KNIME_CORE_LOGGING_FAILSAFE_MIN_LEVEL";

    private enum Target {
            /** Log messages are buffered in {@link #m_buffer}. */
            BUFFER,
            /** Log messages are emitted directly to {@link #m_failsafeOut}. */
            FAILSAFE
    }

    // The log buffer is exclusively owned by this instance. All access must be preceded by acquiring this instance's
    // monitor, except after m_isDrained is set. At that point the buffer's contents are owned by the drainer.
    private final LogBuffer m_buffer;

    // Immutable after construction
    private final PrintStream m_failsafeOut;

    private final Level m_failsafeMinLevel;

    private final Layout m_failsafeLayout;

    private final AtomicBoolean m_isShutdownHookInstalled = new AtomicBoolean();

    /* Both {@code m_target} and {@code m_isDrained} are guarded by this instance's monitor and must only be read or
     * written while holding it.
     */
    // The routing target specifies where to route incoming log messages to. It transitions BUFFER→FAILSAFE at most
    // once (in activateFailsafeAndDrain). Is guarded by this instance's monitor.
    private Target m_target = Target.BUFFER;

    // The flag is {@code true} once the buffer's contents have been transferred out (to logger or to failsafe output).
    // After this is set, no further LogBuffer#add calls are made. Is guarded by this instance's monitor.
    private boolean m_isDrained;

    @SuppressWarnings("resource") // no ownership over System streams
    StartupLogRouter(final int bufferSize) {
        this(bufferSize, initFailsafeTarget(), initFailsafeMinLevel(), initFailsafeLayout());
    }

    StartupLogRouter(final int bufferSize, final PrintStream failsafeOut, final Level failsafeMinLevel,
        final Layout failsafeLayout) {
        m_buffer = new LogBuffer(bufferSize);
        m_failsafeOut = failsafeOut;
        m_failsafeMinLevel = failsafeMinLevel;
        m_failsafeLayout = failsafeLayout;
    }

    /**
     * Routes a pre-initialization log message.
     * <p>
     * In {@link Target#BUFFER} state, the message is added to the buffer (unless already drained). In
     * {@link Target#FAILSAFE} state, the message is emitted directly to the failsafe output if it meets the minimum
     * level.
     *
     * @param isInitialized supplier for whether the logging system is already initialized
     * @param level level to log at
     * @param name logger name
     * @param message message to log
     * @param cause {@code null}-able cause
     * @return {@code true} if the message was handled; {@code false} if the caller should forward it to the logging
     *         framework
     */
    boolean route(final BooleanSupplier isInitialized, final Level level, final String name, final Object message,
        final Throwable cause) {
        // double-checked locking to avoid lock contention on happy path after initialization
        if (isInitialized.getAsBoolean()) {
            return false;
        }
        installShutdownHookIfNeeded();
        synchronized (this) {
            if (isInitialized.getAsBoolean()) {
                return false;
            }
            if (m_target == Target.BUFFER) {
                if (m_isDrained) {
                    return false;
                }
                m_buffer.add(level, name, message, cause);
                return true;
            }
            // Target.FAILSAFE: release the lock before doing I/O
        }
        if (level.isGreaterOrEqual(m_failsafeMinLevel)) {
            emitToFailsafe(name, level, message, cause, Instant.now());
        }
        return true;
    }

    /**
     * Routes a pre-initialization log message with a lazy message supplier.
     * <p>
     * The supplier is evaluated inside the synchronized block in {@link Target#BUFFER} state, or outside it (only if
     * the level meets the minimum threshold) in {@link Target#FAILSAFE} state.
     *
     * @param isInitialized supplier for whether the logging system is already initialized
     * @param level level to log at
     * @param name logger name
     * @param messageSupplier supplier for the message
     * @param cause {@code null}-able cause
     * @return {@code true} if the message was handled; {@code false} if the caller should forward it to the logging
     *         framework
     */
    boolean route(final BooleanSupplier isInitialized, final Level level, final String name,
        final Supplier<Object> messageSupplier, final Throwable cause) {
        // double-checked locking to avoid lock contention on happy path after initialization
        if (isInitialized.getAsBoolean()) {
            return false;
        }
        installShutdownHookIfNeeded();
        synchronized (this) {
            if (isInitialized.getAsBoolean()) {
                return false;
            }
            if (m_target == Target.BUFFER) {
                if (m_isDrained) {
                    return false;
                }
                m_buffer.add(level, name, messageSupplier.get(), cause);
                return true;
            }
            // Target.FAILSAFE: release the lock before evaluating the supplier or doing I/O
        }
        if (level.isGreaterOrEqual(m_failsafeMinLevel)) {
            emitToFailsafe(name, level, messageSupplier.get(), cause, Instant.now());
        }
        return true;
    }

    /**
     * Drains the buffer to the given consumer. Must only be called after the logging system is initialized.
     * <p>
     * Ownership of the buffered entries is transferred to the consumer. This method is idempotent: subsequent calls
     * after the first have no effect.
     * <p>
     * <em>Lock ordering:</em> callers may hold an outer lock (e.g. {@code KNIMELogger.class}) when invoking this
     * method. This method acquires {@code StartupLogRouter.this} internally, then releases it before any further calls.
     * No code inside this class ever acquires an outer lock while holding {@code StartupLogRouter.this}, so the
     * ordering is always outer → {@code StartupLogRouter.this}, preventing deadlocks.
     *
     * @param consumer consumer for each buffered log message
     */
    void drainToLogger(final Consumer<BufferedLogMessage> consumer) {
        final LogBuffer.DrainResult drainResult;
        synchronized (this) {
            if (m_isDrained) {
                return;
            }
            m_isDrained = true;
            drainResult = m_buffer.drain();
        }
        if (drainResult == null) {
            return;
        }
        final var omitCtx = true;
        final var logger = KNIMELogger.getLogger(KNIMELogger.class);
        final var count = formatMessageCount(drainResult.total());
        logger.log(Level.DEBUG, () -> "%s logged before logging was initialized; see below...".formatted(count),
            omitCtx, null);
        if (drainResult.evictedEntries() > 0) {
            logger.log(drainResult.evictionMessageLevel(), () -> formatEvictionNotice(drainResult.evictedEntries()),
                omitCtx, null);
        }
        drainResult.messages().forEachRemaining(consumer);
        logger.log(Level.DEBUG, "End of buffered log messages", omitCtx, null);
    }

    /**
     * Transitions to {@link Target#FAILSAFE} state and drains any buffered messages to the failsafe output.
     * <p>
     * After this call, any new uninitialized log messages are emitted directly to the failsafe output (subject to the
     * configured minimum level). This method is idempotent: if already in failsafe state, it returns immediately.
     * <p>
     * If the buffer was already drained to the logger (normal path), the transition to failsafe still occurs so that
     * any subsequent messages are routed correctly, but there is nothing to emit.
     */
    void activateFailsafeAndDrain() {
        final LogBuffer.DrainResult drainResult;
        synchronized (this) {
            if (m_target == Target.FAILSAFE) {
                return;
            }
            m_target = Target.FAILSAFE;
            if (m_isDrained) {
                // Buffer was already drained to the logger; nothing to emit to failsafe
                return;
            }
            m_isDrained = true;
            drainResult = m_buffer.drain();
        }
        if (drainResult == null) {
            return;
        }

        final var shouldEmitEvictionNotice =
            drainResult.evictedEntries() > 0 && drainResult.evictionMessageLevel().isGreaterOrEqual(m_failsafeMinLevel);

        final var count = formatMessageCount(drainResult.total());
        final var reason = "JVM is shutting down before KNIME logging could finish initialization";
        final Runnable notice =
            () -> m_failsafeOut.printf("--- KNIME startup failsafe logging dump: %s. %s buffered. Minimum level: %s.%n",
                reason, count, m_failsafeMinLevel);

        var didEmitAnything = false;
        if (shouldEmitEvictionNotice) {
            notice.run();
            emitToFailsafe(KNIMELogger.class.getName(), drainResult.evictionMessageLevel(),
                formatEvictionNotice(drainResult.evictedEntries()), null, Instant.now());
            didEmitAnything = true;
        }

        final var messages = drainResult.messages();
        while (messages.hasNext()) {
            final var message = messages.next();
            if (!message.level().isGreaterOrEqual(m_failsafeMinLevel)) {
                continue;
            }
            if (!didEmitAnything) {
                notice.run();
                didEmitAnything = true;
            }
            emitToFailsafe(message.name(), message.level(), message.message(), message.cause(), message.instant());
        }

        if (didEmitAnything) {
            m_failsafeOut.println("--- End of KNIME startup failsafe logging dump.");
            m_failsafeOut.flush();
        }
    }

    private void installShutdownHookIfNeeded() {
        if (!m_isShutdownHookInstalled.get() && m_isShutdownHookInstalled.compareAndSet(false, true)) {
            try {
                Runtime.getRuntime()
                    .addShutdownHook(new Thread(this::activateFailsafeAndDrain, "KNIME startup log dump"));
            } catch (IllegalStateException ex) { // NOSONAR: failsafe path cannot rely on regular logging here
                emitToFailsafe(KNIMELogger.class.getName(), Level.WARN,
                    "Unable to register KNIME startup log shutdown hook because JVM shutdown is already in progress."
                        + " Buffered startup log messages may follow now.",
                    ex, Instant.now());
                activateFailsafeAndDrain();
            } catch (SecurityException ex) { // NOSONAR: failsafe path cannot rely on regular logging here
                m_isShutdownHookInstalled.compareAndSet(true, false);
                emitToFailsafe(KNIMELogger.class.getName(), Level.ERROR,
                    "Unable to register KNIME startup log shutdown hook due to security manager restrictions.", ex,
                    Instant.now());
            }
        }
    }

    private static String formatMessageCount(final long total) {
        return total > 1 ? "%d messages were".formatted(total) : "1 message was";
    }

    private static String formatEvictionNotice(final long evictedEntries) {
        return "[*** Log incomplete: log buffer did wrap around -- "
            + "%d messages were evicted from buffer in total ***]".formatted(evictedEntries);
    }

    private void emitToFailsafe(final String name, final Level level, final Object message, final Throwable cause,
        final Instant instant) {
        final var event = new LoggingEvent(KNIMELogger.class.getName(), Logger.getLogger(name), instant.toEpochMilli(),
            level, message, cause);
        m_failsafeOut.print(m_failsafeLayout.format(event));
        if (cause != null && m_failsafeLayout.ignoresThrowable()) {
            cause.printStackTrace(m_failsafeOut);
        }
        m_failsafeOut.flush();
    }

    private static PrintStream initFailsafeTarget() {
        final var configuredTarget = System.getenv(ENV_LOGGING_FAILSAFE_TARGET);
        // do not involve another logging framework, that's the whole point of this failsafe
        return "stderr".equalsIgnoreCase(configuredTarget) ? System.err : System.out; // NOSONAR see comment above
    }

    private static Level initFailsafeMinLevel() {
        return Level.toLevel(System.getenv(ENV_LOGGING_FAILSAFE_MIN_LEVEL), Level.DEBUG);
    }

    private static Layout initFailsafeLayout() {
        final var layout = new NodeLoggerPatternLayout();
        layout.setConversionPattern("%d{ISO8601} : %-5p : %t : %c{1} : %m%n");
        layout.activateOptions();
        return layout;
    }

}
