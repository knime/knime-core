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
 *   Oct 3, 2024 (wiswedel): created
 */
package org.knime.core.data.util.memory;

import java.lang.ref.Cleaner;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.KNIMETimer;
import org.knime.core.util.ThreadUtils;

/**
 * A memory leak monitor that tracks the number of created instances of a certain type. Objects of this class are to be
 * used as static singletons, to which newly created objects are added during the object construction. The counter is
 * decremented when the object is garbage collected (using a {@link Cleaner}).
 *
 * <p>
 * Instance statistics are regularly printed to the log facilities and also accessible via {@link #stream()} for
 * external monitoring.
 *
 * <p>
 * A counter should not be used for many short-living objects (like <code>DataCell</code>) as each object is registered
 * with a {@link Cleaner} which comes at some cost.
 *
 * <p>
 * Usage is as follows:
 *
 * <pre>
 * public class MyClass {
 *    private static final InstanceCounter<MyClass> COUNTER = InstanceCounter.register("MyClass");
 *    public MyClass() {
 *       COUNTER.track(this); // NOSONAR (partially constructed instance)
 *       ...
 *    }
 * }
 * </pre>
 *
 * @param <T> Types of objects registered at this counter.
 *
 * @author Leo Woerteler, KNIME GmbH, Konstanz, Germany
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.4
 */
public final class InstanceCounter<T> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(InstanceCounter.class);

    private static final List<InstanceCounter<?>> COUNTER_LIST = new CopyOnWriteArrayList<>();

    private static final AtomicBoolean MODIFIED_SINCE_LAST_LOG = new AtomicBoolean(false);

    /**
     * Creates and register a new instance counter. This method is called during class initialization
     * and the returned counter is stored in a static field.
     *
     * @param cl The class for which the counter is created.
     *
     * @param <T> Type of objects to be registered
     * @return A new instance counter.
     */
    public static <T> InstanceCounter<T> register(final Class<T> cl) {
        return register(cl, null);
    }

    /**
     * Similar to {@link #register(Class)} but with an additional detail to distinguish different instances of the same
     * class. The detail is appended to the class name, e.g. "MyClass (detail)"
     *
     * @param cl The class for which the counter is created.
     * @param detail A detail, possibly null, that can be used for further distinction.
     *
     * @param <T> Type of objects to be registered
     * @return A new instance counter.
     */
    public static <T> InstanceCounter<T> register(final Class<T> cl, final String detail) {
        final String name = cl.getName() + (StringUtils.isNotEmpty(detail) ? (" (" + detail + ")") : "");
        final var instanceCounter = new InstanceCounter<T>(name);
        COUNTER_LIST.add(instanceCounter); // NOSONAR - hot spot if called often - false alarm (only class definitions)
        return instanceCounter;
    }

    static {
        final long logInterval = TimeUnit.HOURS.toMillis(1);
        KNIMETimer.getInstance().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                log();
            }
        }, logInterval, logInterval);
    }

    private final String m_name;

    private final AtomicLong m_counter;

    InstanceCounter(final String name) {
        m_name = CheckUtils.checkArgumentNotNull(name);
        m_counter = new AtomicLong(0);
    }

    /**
     * Increments the instance count by one and registers the argument with a {@link Cleaner} instance that decrements
     * the counter when the object is gc'ed.
     * @param obj The non-null object to be tracked.
     */
    public void track(final T obj) {
        ThreadUtils.cleaner().register(obj, this::decrement); // includes argument null check
        MODIFIED_SINCE_LAST_LOG.set(true);
        m_counter.incrementAndGet();
    }

    /**
     * @return an estimate of the number of currently alive instances.
     */
    public long get() {
        return m_counter.get();
    }

    /** Decrement the instance count. */
    void decrement() {
        MODIFIED_SINCE_LAST_LOG.set(true);
        m_counter.decrementAndGet();
    }

    @Override
    public String toString() {
        return String.format("%s: %d", m_name, get());
    }

    /**
     * Stream of all instance counters, each entry is a pair of class name and instance count.
     * @return A stream of all known instance counters
     */
    public static Stream<Map.Entry<String, Long>> stream() {
        return COUNTER_LIST.stream()
            .map(counter -> new AbstractMap.SimpleEntry<>(counter.m_name, counter.m_counter.get()));
    }

    static void log() {
        if (LOGGER.isDebugEnabled() && MODIFIED_SINCE_LAST_LOG.getAndSet(false)) {
            LOGGER.debugWithFormat("Alive instance counts for %d types follows:", COUNTER_LIST.size());
            stream().forEach(e -> LOGGER.debugWithFormat("%s: %d", e.getKey(), e.getValue()));
        }
    }

}
