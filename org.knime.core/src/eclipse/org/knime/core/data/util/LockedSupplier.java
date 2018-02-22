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
 *   29 Jan 2018 (Moritz): created
 */
package org.knime.core.data.util;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Supplier of an object that uses a lock to protect concurrent access to the object.
 *
 * This class uses a {@link ReentrantLock} for managing thread-safe access of the stored object.<br/>
 * It is <b>important</b> to unlock the object as soon as it isn't used anymore to provide high a throughput and avoid
 * possible deadlocks. The best practice is to use an autocloseable try-block as seen underneath. Another possibility
 * would be to follow {@link #get()} by a try-catch-finally block, where {@link #close()} is called in the finally
 * block. This guarantees that the lock will be released even when an (un-)checked Exception is thrown. <br />
 * Furthermore, holding a reference to the stored object after releasing the lock is highly discouraged as thread-safety
 * is <b>not</b> guaranteed. <br/>
 * <br/>
 * Examples 1 (recommended):
 *
 * <pre>
 * try (T object = lockedSupplier.get()) {
 *     doStuff();
 *     // usage of the object is thread-safe within this block.
 * }
 * </pre>
 *
 * <br/>
 * Example 2:
 *
 * <pre>
 * try {
 *     T object = lockedSupplier.get();
 *     // usage of object is thread-safe
 *     doStuff();
 * } catch (Exception e) {
 *     exceptionHandling();
 * } finally {
 *     lockedSupplier.close();
 *     // No further usage of object after this the previous line!
 * }
 * // usage of the object is not thread-safe anymore!
 * </pre>
 *
 * <br/>
 *
 * Example 3 (possible but highly discouraged!):
 *
 * <pre>
 * T object = lockedSupplier.getObject();
 * // possible code in between is discouraged as an (un-)checked Exception
 * // may yield that the object will never be released.
 * try {
 *     doStuff();
 * } catch (Exception e) {
 *     exceptionHandling();
 * } finally {
 *     // No further usage of object after the following line!
 *     lockedSupplier.close();
 * }
 * // usage of the object is not thread-safe anymore!
 * </pre>
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 * @param <T> The type that shall be stored.
 * @since 3.6
 */
public final class LockedSupplier<T> implements AutoCloseable, Supplier<T> {
    private final T m_object;

    private final ReentrantLock m_lock;

    /**
     * Creates a new supplier and immediately locks the passed lock.
     *
     * @param object object to be wrapped.
     * @param lock {@link ReentrantLock} that is used to protect the passed object
     */
    public LockedSupplier(final T object, final ReentrantLock lock) {
        m_object = object;
        m_lock = lock;
        m_lock.lock();
    }

    @Override
    public T get() {
        return m_object;
    }

    /**
     * Unlocks the lock to the contained object. {@link #get()} must not be called afterwards any more.
     */
    @Override
    public void close() {
        m_lock.unlock();
    }

    /**
     * Calls {@link Function#apply(Object)} on the passed {@link Function} using the stored object. This model locks
     * releases the lock after applying the function automatically. Therefore you can only call it once on this supplier.
     *
     * @param function {@link Function} that shall be applied to the object.
     * @return Result of the function.
     */
    public <V> V compute(final Function<T, V> function) {
        try {
            return function.apply(get());
        } finally {
            close();
        }
    }
}
