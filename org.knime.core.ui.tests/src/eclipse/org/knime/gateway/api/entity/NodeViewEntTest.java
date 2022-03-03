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
 *   Oct 4, 2021 (hornm): created
 */
package org.knime.gateway.api.entity;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.knime.core.webui.node.view.NodeViewManagerTest.runOnExecutor;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.RowKey;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.NodeViewTest;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.PageTest;
import org.knime.core.webui.page.PageUtil;
import org.knime.core.webui.page.PageUtil.PageType;
import org.knime.core.webui.page.Resource;
import org.knime.gateway.impl.service.events.SelectionEvent;
import org.knime.gateway.impl.service.events.SelectionEventSource;
import org.knime.gateway.impl.service.events.SelectionEventSource.SelectionEventMode;
import org.knime.gateway.impl.service.events.SelectionEventSourceTest;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.node.view.NodeViewNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests {@link NodeViewEnt}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeViewEntTest {

    /**
     * Tests {@link NodeViewEnt#NodeViewEnt(NativeNodeContainer)}.
     *
     * @throws IOException
     */
    @Test
    public void testNodeViewEnt() throws IOException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();

        NativeNodeContainer nncWithoutNodeView =
            WorkflowManagerUtil.createAndAddNode(wfm, new VirtualSubNodeInputNodeFactory(null, new PortType[0]));
        Assert.assertThrows(IllegalArgumentException.class, () -> NodeViewEnt.create(nncWithoutNodeView));

        Function<NodeViewNodeModel, NodeView> nodeViewCreator = m -> {
            Page p = Page.builder(() -> "blub", "index.html").build();
            return NodeViewTest.createNodeView(p, new TextInitialDataService() {

                @Override
                public String getInitialData() {
                    return "dummy initial data";
                }
            }, null, null);
        };
        NativeNodeContainer nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));
        wfm.executeAllAndWaitUntilDone();
        nnc.setNodeMessage(NodeMessage.newWarning("node message"));
        nnc.getNodeAnnotation().getData().setText("node annotation");

        var ent = NodeViewEnt.create(nnc, null);
        assertThat(ent.getProjectId(), startsWith("workflow"));
        assertThat(ent.getWorkflowId(), is("root"));
        assertThat(ent.getNodeId(), is("root:2"));
        assertThat(ent.getInitialData(), is("dummy initial data"));
        assertThat(ent.getInitialSelection(), is(nullValue()));
        var resourceInfo = ent.getResourceInfo();
        assertThat(resourceInfo.getUrl(), endsWith("index.html"));
        assertThat(resourceInfo.getPath(), is(nullValue()));
        assertThat(resourceInfo.getType(), is(Resource.ContentType.HTML.toString()));
        assertThat(resourceInfo.getId(), is(PageUtil.getPageId(nnc, false, PageType.VIEW)));
        var nodeInfo = ent.getNodeInfo();
        assertThat(nodeInfo.getNodeName(), is("NodeView"));
        assertThat(nodeInfo.getNodeAnnotation(), is("node annotation"));
        assertThat(nodeInfo.getNodeState(), is("executed"));
        assertThat(nodeInfo.getNodeWarnMessage(), is("node message"));
        assertThat(nodeInfo.getNodeErrorMessage(), is(nullValue()));

        // a node view as a 'component' without initial data
        nodeViewCreator = m -> {
            Page p = Page.builder(PageTest.BUNDLE_ID, "files", "component.umd.min.js").build();
            return NodeViewTest.createNodeView(p);
        };
        nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));
        wfm.executeAllAndWaitUntilDone();
        ent = NodeViewEnt.create(nnc, null);
        resourceInfo = ent.getResourceInfo();
        assertThat(ent.getInitialData(), is(nullValue()));
        assertThat(resourceInfo.getType(), is(Resource.ContentType.VUE_COMPONENT_LIB.toString()));
        assertThat(resourceInfo.getUrl(), endsWith("component.umd.min.js"));
        assertThat(resourceInfo.getPath(), is(nullValue()));

        // test to create a node view entity while running headless (e.g. on the executor)
        NativeNodeContainer nnc2 = nnc;
        runOnExecutor(() -> {
            var ent2 = NodeViewEnt.create(nnc2, null);
            assertThat(ent2.getResourceInfo().getPath(), endsWith("component.umd.min.js"));
            assertThat(ent2.getResourceInfo().getUrl(), is(nullValue()));
        });

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    /**
     * Tests the {@link SelectionEventSource} in conjunction with {@link NodeViewEnt}.
     *
     * @throws IOException
     */
    @Test
    public void testNodeViewEntWithSelectionEventSource() throws IOException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();

        Function<NodeViewNodeModel, NodeView> nodeViewCreator =
            m -> NodeViewTest.createNodeView(Page.builder(() -> "blub", "index.html").build());
        NativeNodeContainer nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));
        wfm.executeAllAndWaitUntilDone();

        var hiLiteHandler = nnc.getNodeModel().getInHiLiteHandler(0);
        hiLiteHandler.fireHiLiteEvent(new RowKey("k1"), new RowKey("k2"));

        @SuppressWarnings("unchecked")
        final BiConsumer<String, SelectionEvent> consumerMock = mock(BiConsumer.class);
        var selectionEventSource = SelectionEventSourceTest.createSelectionEventSource(consumerMock);
        var initialSelection = selectionEventSource.addEventListenerAndGetInitialEventFor(nnc).map(SelectionEvent::getKeys)
            .orElse(Collections.emptyList());
        var nodeViewEnt = NodeViewEnt.create(nnc, () -> initialSelection);

        assertThat(nodeViewEnt.getInitialSelection(), is(List.of("k1", "k2")));

        hiLiteHandler.fireHiLiteEvent(new RowKey("k3"));
        await().pollDelay(ONE_HUNDRED_MILLISECONDS).timeout(FIVE_SECONDS)
            .untilAsserted(() -> verify(consumerMock, times(1)).accept(eq("SelectionEvent"),
                argThat(se -> se.getKeys().equals(List.of("k3")) && se.getMode() == SelectionEventMode.ADD)));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }


}
