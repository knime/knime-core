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
 *   3 Apr 2020 ("Marc Bux, KNIME GmbH, Berlin, Germany"): created
 */
package org.knime.core.data.container;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * An interface for types that hold resources which should be released when the buffer is cleared (or earlier).
 *
 * @author "Marc Bux, KNIME GmbH, Berlin, Germany"
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface BufferResource {

    /**
     * Release the resource held by the type implementing this interface.
     */
    void releaseResource();

    /**
     * A class that holds {@link BufferResource BufferResources} and associates them with their owning
     * {@link CloseableRowIterator CloseableRowIterators}. The former are strongly referenced, since we do not want them
     * to be garbage-collected before their resources are released. The latter are weakly referenced, since we want to
     * allow unreferenced (and unclosed) iterators to be garbage-collected. If such is the case, the resource is
     * considered stale and we release their resources.
     */
    static final class BufferResourceRegistry {

        private final BiMap<BufferResource, Reference<CloseableRowIterator>> m_openResources = HashBiMap.create();

        private final ReferenceQueue<CloseableRowIterator> m_refQueue = new ReferenceQueue<>();

        /**
         * Releases and unregisters stale {@link BufferResource BufferResources} and determines the amount of still
         * registered resources.
         *
         * @return the number of registered resources
         */
        synchronized int size() {
            removeStaleEntriesAndReleaseResources();
            return m_openResources.size();
        }

        /**
         * Releases and unregisters stale {@link BufferResource BufferResources} and registers a new resource.
         *
         * @param resource the to-be-registered resource
         * @param owner the iterator that owns the to-be-registered resource
         */
        synchronized void register(final BufferResource resource, final CloseableRowIterator owner) {
            removeStaleEntriesAndReleaseResources();
            m_openResources.put(resource, new WeakReference<>(owner, m_refQueue));
        }

        /**
         * Unregisters a given {@link BufferResource BufferResource}.
         *
         * @param resource the to-be-unregistered resource
         */
        synchronized void unregister(final BufferResource resource) {
            m_openResources.remove(resource);
        }

        /**
         * Releases and unregisters all {@link BufferResource BufferResources}.
         */
        synchronized void releaseResourcesAndClear() {
            m_openResources.keySet().stream().forEach(BufferResource::releaseResource);
            m_openResources.clear();
        }

        private void removeStaleEntriesAndReleaseResources() {
            Reference<? extends CloseableRowIterator> ref;
            while ((ref = m_refQueue.poll()) != null) {
                final BufferResource resource = m_openResources.inverse().remove(ref);
                // resource could be null if entry was already unregistered
                if (resource != null) {
                    resource.releaseResource();
                }
            }
        }
    }
}
