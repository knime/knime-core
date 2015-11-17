/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   17.11.2015 (thor): created
 */
package org.knime.core.data.container;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.util.Pair;

/**
 * This class is only used for testing. It tracks creation and deletion of {@link Buffer}s. By default tracking is
 * disabled. To enable it the system property <tt>org.knime.core.data.enable_buffer_tracker</tt> must be set to
 * <tt>true</tt>.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 * @since 3.1
 */
public final class BufferTracker {
    private static final BufferTracker INSTANCE = new BufferTracker();

    private static final boolean IS_ENABLED = Boolean.getBoolean("org.knime.core.data.enable_buffer_tracker");

    private final Map<Buffer, Pair<NodeContainer, StackTraceElement[]>> m_buffers = new ConcurrentHashMap<>();

    private BufferTracker() {}

    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance
     */
    public static BufferTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Call this method whenever a buffer is created.
     *
     * @param buffer the buffer
     */
    public void bufferCreated(final Buffer buffer) {
        if (IS_ENABLED) {
            m_buffers.put(buffer,
                new Pair<>(NodeContext.getContext().getNodeContainer(), Thread.currentThread().getStackTrace()));
        }
    }

    /**
     * Call this method when the buffer is cleared.
     *
     * @param buffer the buffer
     */
    public void bufferCleared(final Buffer buffer) {
        if (IS_ENABLED) {
            m_buffers.remove(buffer);
        }
    }

    /**
     * Returns a collection of all currently open buffers. The collection contains pairs, with the first item
     * being the node container to which the buffer belongs. The second item is the stack trace when the buffer was
     * created.
     *
     * @return a collection of open buffers
     */
    public Collection<Pair<NodeContainer, StackTraceElement[]>> getOpenBuffers() {
        return m_buffers.values();
    }

    /**
     * Clears the tracker.
     */
    public void clear() {
        m_buffers.clear();
    }
}
