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
 *   5 Nov 2024 (jasper): created
 */
package org.knime.core.node.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * Allows extensions to register resources with a workflow, which get disposed when the workflow itself is disposed.
 *
 * <p>
 * Typical use cases are workflow-wide caches or resources that are shared between multiple nodes in a workflow. The
 * nodes itself should unregister the resource when they are disposed to prevent memory leaks but this global resource
 * cleanup is a safety net in case a node forgets to do so.
 *
 * <p>
 * Registering and retrieving a resource needs to be done while a thread is annotated with a {@link NodeContext},
 * which is guaranteed by the framework for node execution and other typical workflow operations (e.g. in dialog code).
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.4
 */
public final class WorkflowResourceCache {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowResourceCache.class);

    private final Map<Class<? extends WorkflowResource>, WorkflowResource> m_classToInstanceMap;

    private boolean m_isDisposed;

    WorkflowResourceCache() {
        m_classToInstanceMap = new HashMap<>();
        m_isDisposed = false;
    }

    // For testing and debugging
    Map<Class<? extends WorkflowResource>, WorkflowResource> getInternalMap() {
        return m_classToInstanceMap;
    }

    private static WorkflowResourceCache getCacheFromCurrentWorkflow() throws IllegalStateException {
        final var cache = NodeContext.getContextOptional() //
                .map(NodeContext::getWorkflowManager) //
                .map(WorkflowManager::getWorkflowResourceCache) //
                .orElseThrow(() -> new IllegalStateException("No context available."));
        CheckUtils.checkState(!cache.m_isDisposed, "Cache is disposed.");
        return cache;
    }

    /**
     * Gets a new workflow resource associated with the current thread's workflow. If no such resource exists, an empty
     * {@link Optional} is returned. If the current thread is not associated with a workflow (via NodeContext), an
     * runtime exception is thrown.
     *
     * @param <T> Type of resource (extension dependent)
     * @param clazz The class of <code>T</code>
     * @return The resource instance or an empty {@link Optional}.
     * @throws IllegalStateException If the current thread is not associated with a workflow or the cache/workflow is
     *             already disposed
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T extends WorkflowResource> Optional<T> get(final Class<T> clazz) {
        CheckUtils.checkArgumentNotNull(clazz, "Class must not be null.");
        final WorkflowResourceCache cache = getCacheFromCurrentWorkflow();
        return Optional.ofNullable((T)cache.m_classToInstanceMap.get(clazz));
    }

    /**
     * Registers a new workflow resource with the workflow associated with the current thread. If no such resource
     * exists, a new one is created using the supplier and registered with the workflow. If the current thread is not
     * associated with a workflow (via NodeContext), an runtime exception is thrown.
     *
     * @param <T> Type of resource (extension dependent)
     * @param clazz The class of <code>T</code>
     * @param supplier The supplier providing a new resource instance, must not return <code>null</code> on invocation
     * @return The resource instance
     * @throws IllegalStateException If the current thread is not associated with a workflow or the cache/workflow is
     *             already disposed
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T extends WorkflowResource> T computeIfAbsent(final Class<T> clazz,
        final Supplier<T> supplier) {
        CheckUtils.checkArgumentNotNull(clazz, "Class must not be null.");
        CheckUtils.checkArgumentNotNull(supplier, "Supplier must not be null.");
        final WorkflowResourceCache cache = getCacheFromCurrentWorkflow();
        return (T)cache.m_classToInstanceMap.computeIfAbsent(clazz, k -> {
            final T value = CheckUtils.checkArgumentNotNull(supplier.get(), "Supplier returned null");
            LOGGER.debugWithFormat("Added new cache entry for class '%s'", clazz);
            return value;
        });
    }

    /**
     * Returns the resource instance for the given class if it is already present in the cache.
     *
     * @since 5.10
     * @param <T>  type of resource
     * @param clazz the class used as key for the resource, must not be {@code null}
     * @return an {@link Optional} containing the cached instance if present; otherwise {@link Optional#empty()}
     */
    public synchronized <T extends WorkflowResource> Optional<T> getFromCache(final Class<T> clazz) {
        CheckUtils.checkArgumentNotNull(clazz, "Class must not be null.");
        return Optional.ofNullable((T) m_classToInstanceMap.get(clazz));
    }

    /**
     * Registers a new workflow resource with this workflow. The resource is associated with the given class.
     * An existing value is returned or {@code null} if the class was previously not associated with a resource.
     *
     * @since 5.10
     * @param <T> Type of resource (extension dependent)
     * @param clazz The class of {@code T}
     * @param value The resource instance to register
     * @return The previous value associated with the class or {@code null} if no such value existed
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends WorkflowResource> T put(final Class<T> clazz, final T value) {
        CheckUtils.checkArgumentNotNull(clazz, "Class must not be null.");
        CheckUtils.checkArgumentNotNull(value, "Value must not be null.");
        CheckUtils.checkState(!m_isDisposed, "Cache is disposed.");
        LOGGER.debugWithFormat("Added new cache entry for class \"%s\"", clazz);
        return (T)m_classToInstanceMap.put(clazz, value);
    }

    /**
     * Clear the cache. Additionally call the {@link WorkflowResource#dispose()} method on all cache entries that
     * implement the {@link WorkflowResource} interface.
     */
    synchronized void dispose() {
        for (final var value : m_classToInstanceMap.values()) {
            value.dispose();
        }
        m_classToInstanceMap.clear();
        m_isDisposed = true;
    }

    @Override
    public String toString() {
        return "WorkflowCache { m_cache=" + m_classToInstanceMap.toString() + " }";
    }

    /**
     * A resource that is associated with a workflow via {@link WorkflowResourceCache#computeIfAbsent(Class, Supplier)}
     * and whose {@link WorkflowResource#dispose()} method is called when the cache is cleared.
     */
    public interface WorkflowResource {

        /**
         * Called when the cache is cleared. For cache implementations, this implementation could also verify that the
         * cache is already empty when the method is called (otherwise it would indicate a memory leak).
         */
        void dispose();
    }
}
