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
 *   Nov 25, 2021 (hornm): created
 */
package org.knime.gateway.impl.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.selection.SelectionTranslationService;
import org.knime.core.webui.page.Page;
import org.knime.gateway.impl.service.events.SelectionEventSource.SelectionEventMode;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.node.view.NodeViewNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests {@link SelectionEventSource}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public class SelectionEventSourceTest {

    private static final String WORKFLOW_NAME = "workflow";

    private static final List<String> ROWKEYS_1 = List.of("Row01");

    private static final List<String> ROWKEYS_2 = List.of("Row02");

    private static final List<String> ROWKEYS_1_2 = List.of("Row01", "Row02");

    private WorkflowManager m_wfm;

    private NativeNodeContainer m_nnc;

    private HiLiteHandler m_hlh;

    @BeforeEach
    public void setup() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
        m_nnc = WorkflowManagerUtil.createAndAddNode(m_wfm, new NodeViewNodeFactory());
        m_hlh = m_nnc.getNodeModel().getInHiLiteHandler(0);
    }

    @AfterEach
    public void tearDown() {
        m_wfm.getParent().removeProject(m_wfm.getID());
    }

    @Test
    public void testConsumeHiLiteEvent() {

        @SuppressWarnings("unchecked")
        final BiConsumer<String, SelectionEvent> consumerMock = mock(BiConsumer.class);

        registerSelectionEventSource(consumerMock, m_nnc);

        m_nnc.getNodeModel().getInHiLiteHandler(0).fireHiLiteEvent(stringListToRowKeySet(ROWKEYS_1_2));

        await().pollDelay(ONE_HUNDRED_MILLISECONDS).timeout(FIVE_SECONDS)
            .untilAsserted(() -> verify(consumerMock, times(1)).accept(eq("SelectionEvent"),
                argThat(se -> verifySelectionEvent(se, "root", "root:1"))));

        assertThat(m_hlh.getHiLitKeys()).isEqualTo(stringListToRowKeySet(ROWKEYS_1_2));
        m_hlh.fireClearHiLiteEvent();
    }

    @Test
    public void testConsumeUnHiLiteEvent() {

        m_hlh.fireHiLiteEvent(stringListToRowKeySet(ROWKEYS_1_2));

        @SuppressWarnings("unchecked")
        final BiConsumer<String, SelectionEvent> consumerMock = mock(BiConsumer.class);

        registerSelectionEventSource(consumerMock, m_nnc);

        m_nnc.getNodeModel().getInHiLiteHandler(0).fireUnHiLiteEvent(stringListToRowKeySet(ROWKEYS_1));

        await().pollDelay(ONE_HUNDRED_MILLISECONDS).timeout(FIVE_SECONDS)
            .untilAsserted(() -> verify(consumerMock, times(1)).accept(eq("SelectionEvent"),
                argThat(se -> se.getSelection().equals(ROWKEYS_1) && se.getMode() == SelectionEventMode.REMOVE)));

        assertThat(m_hlh.getHiLitKeys()).isEqualTo(stringListToRowKeySet(ROWKEYS_2));
        m_hlh.fireClearHiLiteEvent();
    }

    @Test
    public void testConsumeReplaceHiLiteEvent() {

        m_hlh.fireHiLiteEvent(stringListToRowKeySet(ROWKEYS_1));

        @SuppressWarnings("unchecked")
        final BiConsumer<String, SelectionEvent> consumerMock = mock(BiConsumer.class);

        registerSelectionEventSource(consumerMock, m_nnc);

        m_nnc.getNodeModel().getInHiLiteHandler(0).fireReplaceHiLiteEvent(stringListToRowKeySet(ROWKEYS_2));

        await().pollDelay(ONE_HUNDRED_MILLISECONDS).timeout(FIVE_SECONDS)
            .untilAsserted(() -> verify(consumerMock, times(1)).accept(eq("SelectionEvent"),
                argThat(se -> se.getSelection().equals(ROWKEYS_2) && se.getMode() == SelectionEventMode.REPLACE)));

        assertThat(m_hlh.getHiLitKeys()).isEqualTo(stringListToRowKeySet(ROWKEYS_2));
        m_hlh.fireClearHiLiteEvent();
    }

    /**
     * Tests the {@code async}-parameter of the
     * {@link SelectionEventSource#processSelectionEvent(NativeNodeContainer, SelectionEventMode, boolean, List)}-method.
     */
    @Test
    public void testProcessSelectionEventAsync() {
        var hiLiteHandler = m_nnc.getNodeModel().getInHiLiteHandler(0);
        var hiLiteListener = new TestHiLiteListener();
        hiLiteHandler.addHiLiteListener(hiLiteListener);

        // async call
        var rowKeys = stringListToRowKeySet(List.of("1"));
        SelectionEventSource.processSelectionEvent(m_nnc, SelectionEventMode.ADD, true, rowKeys);
        SelectionEventSource.processSelectionEvent(m_nnc, SelectionEventMode.REMOVE, true, rowKeys);
        SelectionEventSource.processSelectionEvent(m_nnc, SelectionEventMode.REPLACE, true, rowKeys);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> assertThat(hiLiteListener.m_callerThreadName).isNotNull());
        assertThat(hiLiteListener.m_callerThreadName).isNotEqualTo("INVALID");
        assertThat(hiLiteListener.m_callerThreadName).isNotEqualTo(Thread.currentThread().getName());
        hiLiteListener.m_callerThreadName = null;

        // sync call
        rowKeys = stringListToRowKeySet(List.of("2"));
        SelectionEventSource.processSelectionEvent(m_nnc, SelectionEventMode.ADD, false, rowKeys);
        SelectionEventSource.processSelectionEvent(m_nnc, SelectionEventMode.REMOVE, false, rowKeys);
        SelectionEventSource.processSelectionEvent(m_nnc, SelectionEventMode.REPLACE, false, rowKeys);
        assertThat(hiLiteListener.m_callerThreadName).isEqualTo(Thread.currentThread().getName());
    }

    /**
     * Makes sure that an event source registered on a node without a view doesn't emit selection events.
     */
    @Test
    public void testNoSelectionEventForNodeWithoutAView() {
        NativeNodeContainer nnc = WorkflowManagerUtil.createAndAddNode(m_wfm,
            new VirtualSubNodeInputNodeFactory(null, new PortType[]{BufferedDataTable.TYPE}));

        @SuppressWarnings("unchecked")
        final BiConsumer<String, SelectionEvent> consumerMock = mock(BiConsumer.class);
        registerSelectionEventSource(consumerMock, nnc);

        nnc.getNodeModel().getInHiLiteHandler(0).fireUnHiLiteEvent(new KeyEvent(stringListToRowKeySet(ROWKEYS_1)), false);

        verify(consumerMock, times(0)).accept(eq("SelectionEvent"), any());
    }

    @Test
    public void testSelectionEventWithError() {
        Function<NodeViewNodeModel, NodeView> viewCreator = m -> { // NOSONAR
            return new NodeView() { // NOSONAR

                @Override
                public Optional<SelectionTranslationService> createSelectionTranslationService() {
                    return Optional.of(new SelectionTranslationService() {

                        @Override
                        public Set<RowKey> toRowKeys(final List<String> selection) throws IOException {
                            return Collections.emptySet();
                        }

                        @Override
                        public List<String> fromRowKeys(final Set<RowKey> rowKeys) throws IOException {
                            throw new IOException("foo");
                        }
                    });
                }

                @Override
                public Optional<InitialDataService> createInitialDataService() {
                    return Optional.empty();
                }

                @Override
                public Optional<DataService> createDataService() {
                    return Optional.empty();
                }

                @Override
                public Optional<ApplyDataService> createApplyDataService() {
                    return Optional.empty();
                }

                @Override
                public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                    //
                }

                @Override
                public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
                    //
                }

                @Override
                public Page getPage() {
                    return Page.builder(() -> "foo", "bar").build();
                }

            };
        };
        var nnc = WorkflowManagerUtil.createAndAddNode(m_wfm, new NodeViewNodeFactory(viewCreator));

        @SuppressWarnings("unchecked")
        final BiConsumer<String, SelectionEvent> consumerMock = mock(BiConsumer.class);
        registerSelectionEventSource(consumerMock, nnc);

        nnc.getNodeModel().getInHiLiteHandler(0).fireHiLiteEvent(new KeyEvent(stringListToRowKeySet(ROWKEYS_1)), false);

        verify(consumerMock, times(1)).accept(eq("SelectionEvent"), argThat(se -> se.getSelection() == null
            && se.getMode() == SelectionEventMode.ADD && se.getError().equals("foo")));
    }

    private static class TestHiLiteListener implements HiLiteListener {

        String m_callerThreadName = null;

        @Override
        public void hiLite(final KeyEvent event) {
            updateThreadName();
        }

        @Override
        public void unHiLite(final KeyEvent event) {
            updateThreadName();
        }

        @Override
        public void unHiLiteAll(final KeyEvent event) {
            updateThreadName();
        }

        private void updateThreadName() {
            var threadName = Thread.currentThread().getName();
            if (m_callerThreadName == null) {
                m_callerThreadName = threadName;
            } else if (!m_callerThreadName.equals(threadName)) {
                m_callerThreadName = "INVALID";
            }
        }

    }

    private static boolean verifySelectionEvent(final SelectionEvent se, final String workflowId, final String nodeId) {
        return se.getSelection().equals(ROWKEYS_1_2) && se.getMode() == SelectionEventMode.ADD
            && se.getNodeId().equals(nodeId) && se.getWorkflowId().equals(workflowId)
            && se.getProjectId().startsWith(WORKFLOW_NAME);
    }

    private static Set<RowKey> stringListToRowKeySet(final List<String> keys) {
        return keys.stream().map(RowKey::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }


    public static SelectionEventSource
        createSelectionEventSource(final BiConsumer<String, SelectionEvent> selectionEventConsumer) {
        return new SelectionEventSource((s, o) -> selectionEventConsumer.accept(s, (SelectionEvent)o));
    }

    private static void registerSelectionEventSource(final BiConsumer<String, SelectionEvent> selectionEventConsumer,
        final NativeNodeContainer node) {
        createSelectionEventSource(selectionEventConsumer).addEventListenerFor(node);
    }

}
