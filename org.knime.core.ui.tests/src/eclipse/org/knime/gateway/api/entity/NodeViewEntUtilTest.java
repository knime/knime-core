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
 *   Feb 10, 2022 (hornm): created
 */
package org.knime.gateway.api.entity;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.awaitility.core.ThrowingRunnable;
import org.junit.Test;
import org.knime.core.data.RowKey;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.gateway.impl.service.events.NodeViewStateEvent;
import org.knime.gateway.impl.service.events.SelectionEvent;
import org.knime.gateway.impl.service.events.SelectionEventSource.SelectionEventMode;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.util.WorkflowManagerUtil;
import org.mockito.Mockito;

/**
 * Tests {@link NodeViewEntUtil}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeViewEntUtilTest {

    /**
     * Tests
     * {@link NodeViewEntUtil#createNodeViewEntAndEventSources(org.knime.core.node.workflow.NativeNodeContainer, BiConsumer, boolean)}.
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateNodeViewEntAndSetUpEventSources() throws IOException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(0, 0));
        var hlh = nnc.getNodeModel().getInHiLiteHandler(0);
        wfm.executeAllAndWaitUntilDone();

        BiConsumer<String, Object> eventConsumer = Mockito.mock(BiConsumer.class);

        /* assert that the selection event source is properly set up */
        var eventSource =
            NodeViewEntUtil.createNodeViewEntAndEventSources(nnc, eventConsumer, false).getSecond()[0];
        fireHiLiteEvent(hlh, "test");
        verify(eventConsumer).accept(eq("Selection"), argThat(se -> verifySelectionEvent((SelectionEvent)se, "test")));

        /* assert that all the listeners are removed from the selection event source on node state change */
        wfm.resetAndConfigureAll();
        fireHiLiteEvent(hlh, "test2");
        verify(eventConsumer, never()).accept(eq("Selection"),
            argThat(se -> verifySelectionEvent((SelectionEvent)se, "test2")));

        eventSource.removeAllEventListeners();

        /* test the selection event source in combination with the node view state event source */
        // test selection events
        wfm.executeAllAndWaitUntilDone();
        NodeViewEntUtil.createNodeViewEntAndEventSources(nnc, eventConsumer, true);
        fireHiLiteEvent(hlh, "test3");
        verify(eventConsumer).accept(eq("Selection"), argThat(se -> verifySelectionEvent((SelectionEvent)se, "test3")));
        // test node view state event: configured
        wfm.resetAndConfigureAll();
        awaitUntilAsserted(() -> verify(eventConsumer).accept(eq("NodeViewState"),
            argThat(se -> verifyNodeViewStateEvent((NodeViewStateEvent)se, "configured", null))));
        // make sure no selection events are fired if node is not executed
        fireHiLiteEvent(hlh, "test4");
        verify(eventConsumer, never()).accept(eq("Selection"),
            argThat(se -> verifySelectionEvent((SelectionEvent)se, "test4")));
        // test node view state event: executed
        wfm.executeAllAndWaitUntilDone();
        awaitUntilAsserted(() -> verify(eventConsumer).accept(eq("NodeViewState"),
            argThat(se -> verifyNodeViewStateEvent((NodeViewStateEvent)se, "executed", "the initial data"))));
        // make sure that selection events are issued again
        fireHiLiteEvent(hlh, "test5");
        verify(eventConsumer).accept(eq("Selection"), argThat(se -> verifySelectionEvent((SelectionEvent)se, "test5")));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    private static boolean verifySelectionEvent(final SelectionEvent se, final String rowKey) {
        return se.getKeys().equals(List.of(rowKey)) && se.getMode() == SelectionEventMode.ADD;
    }

    private static boolean verifyNodeViewStateEvent(final NodeViewStateEvent e, final String state,
        final String initialData) {
        return e.getNodeView().getNodeInfo().getNodeState().equals(state)
            && Objects.equals(e.getNodeView().getInitialData(), initialData);
    }

    private static void fireHiLiteEvent(final HiLiteHandler hlh, final String rowKey) {
        hlh.fireHiLiteEvent(new KeyEvent(hlh, new RowKey(rowKey)), false);
    }

    private static void awaitUntilAsserted(final ThrowingRunnable runnable) {
        await().pollDelay(ONE_HUNDRED_MILLISECONDS).timeout(FIVE_SECONDS).untilAsserted(runnable);
    }

}
