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
 */
package org.knime.core.node.workflow;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages loop state change events. An event listener is registered on a target status and is notified when the loop
 * enters this target status.
 *
 * Note:
 *  <ul>
 *      <li>
 *          When implementing a new listener, take care that the listeners are notified at the adequate points in core
 *          routines.
 *      </li>
 *      <li>
 *          In the future, the target event type (i.e. the key type of the map) may be generalised to include other events or state
 *          changes involving loops beyond the granularity of {@link org.knime.core.node.workflow.NativeNodeContainer.LoopStatus}.
 *          For instance, see {@code org.knime.gateway.api.webui.util.EntityBuilderUtil.LoopState}.
 *      </li>
 *  </ul>
 * @author Benjamin Moser, KNIME GmbH, Konstanz
 * @since 4.6
 * @noextend
 * @noreference
 */
public class LoopStatusChangeHandler {

    private final Map<NativeNodeContainer.LoopStatus, Set<LoopStatusChangeListener>> m_loopStatusListeners = new ConcurrentHashMap<>();

    /**
     * Add a listener that will be notified when the loop enters the {@link org.knime.core.node.workflow.NativeNodeContainer.LoopStatus.PAUSED}
     * state. This applies directly after the "Pause Loop Execution" action is triggered.
     * @param l The listener to add
     */
    public void addLoopPausedListener(final LoopStatusChangeListener l) {
        addListener(NativeNodeContainer.LoopStatus.PAUSED, l);
    }

    /**
     * @see #addLoopPausedListener(LoopStatusChangeListener)
     * @param l The listener to remove
     */
    public void removeLoopPausedListener(final LoopStatusChangeListener l) {
        removeListener(NativeNodeContainer.LoopStatus.PAUSED, l);
    }

    /**
     * Notify all listeners subscribed to the target status.
     * @param newStatus The new loop status and target status of all listeners to notify.
     */
    public void notifyLoopStatusChangeListener(final NativeNodeContainer.LoopStatus newStatus) {
        m_loopStatusListeners.getOrDefault(newStatus, Collections.emptySet()).forEach(
                LoopStatusChangeListener::stateChanged
        );
    }

    private void addListener(final NativeNodeContainer.LoopStatus target, final LoopStatusChangeListener l) {
        m_loopStatusListeners.computeIfAbsent(target, k -> new CopyOnWriteArraySet<>()).add(l);
    }

    private void removeListener(final NativeNodeContainer.LoopStatus target, final LoopStatusChangeListener l) {
        m_loopStatusListeners.getOrDefault(target, Collections.emptySet()).remove(l);
    }

}
