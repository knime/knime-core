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
 *   Sep 13, 2021 (hornm): created
 */
package org.knime.core.webui.node.view;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThrows;
import static org.knime.core.webui.page.PageTest.BUNDLE_ID;
import static org.knime.testing.util.WorkflowManagerUtil.createAndAddNode;
import static org.knime.testing.util.WorkflowManagerUtil.createEmptyWorkflow;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.awaitility.Awaitility;
import org.junit.Test;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.data.text.TextReExecuteDataService;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.node.view.NodeViewNodeModel;

import com.google.common.io.Files;

/**
 * Tests for {@link NodeViewManager}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeViewManagerTest {

    /**
     * Tests multiple {@link NodeViewManager}-methods using a simple node view.
     *
     * @throws IOException
     */
    @Test
    public void testSimpleNodeViewNode() throws IOException {
        WorkflowManager wfm = createEmptyWorkflow();

        Page page = Page.builderFromString(() -> "test page content", "index.html").build();
        NativeNodeContainer nc = createNodeWithNodeView(wfm, m -> NodeView.create(page));

        assertThat("node expected to have a node view", NodeViewManager.hasNodeView(nc), is(true));
        NodeView nodeView = NodeViewManager.getInstance().getNodeView(nc);
        assertThat(nodeView.getPage() == page, is(true));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> NodeViewManager.getInstance().callTextInitialDataService(nc));
        assertThat(ex.getMessage(), containsString("does not provide a 'initial data service'"));
        assertThat(nodeView.getPage().isCompletelyStatic(), is(false));

        wfm.getParent().removeProject(wfm.getID());
    }

    /**
     * Tests {@link NodeViewManager#callTextInitialDataService(org.knime.core.node.workflow.NodeContainer)},
     * {@link NodeViewManager#callTextDataService(org.knime.core.node.workflow.NodeContainer, String)} and
     * {@link NodeViewManager#callTextReExecuteDataService(org.knime.core.node.workflow.NodeContainer, String)}
     *
     * @throws IOException
     */
    @Test
    public void testCallDataServices() throws IOException {

        WorkflowManager wfm = createEmptyWorkflow();

        Page page = Page.builderFromString(() -> "test page content", "index.html").build();
        NodeView nodeView = NodeView.builder(page).initialDataService(new TextInitialDataService() {

            @Override
            public String getInitialData() {
                return "init service";
            }
        }).dataService(new TextDataService() {

            @Override
            public String handleRequest(final String request) {
                return "general data service";
            }
        }).reExecuteDataService(new TextReExecuteDataService() {

            @Override
            public void applyData(final String data) throws IOException {
                throw new UnsupportedOperationException("should not be called in this test");
            }

            @Override
            public void reExecute(final String data) throws IOException {
                throw new IOException("re-execute data service");

            }
        }).build();
        NativeNodeContainer nc = createNodeWithNodeView(wfm, m -> nodeView);

        NodeViewManager nodeViewManager = NodeViewManager.getInstance();
        assertThat(nodeViewManager.callTextInitialDataService(nc), is("init service"));
        assertThat(nodeViewManager.callTextDataService(nc, ""), is("general data service"));
        String message =
            assertThrows(IOException.class, () -> nodeViewManager.callTextReExecuteDataService(nc, "")).getMessage();
        assertThat(message, is("re-execute data service"));

        wfm.getParent().removeProject(wfm.getID());
    }

    /**
     * Tests {@link NodeViewManager#writeNodeViewResourcesToDiscAndGetFileUrl(NativeNodeContainer)}.
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testWriteNodeViewResourceToDiscAndGetFileUrl() throws IOException, URISyntaxException {
        Page staticPage = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        Page dynamicPage = Page.builderFromString(() -> "page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        WorkflowManager wfm = createEmptyWorkflow();
        NativeNodeContainer nnc = createNodeWithNodeView(wfm, m -> NodeView.create(staticPage));
        NativeNodeContainer nnc2 = createNodeWithNodeView(wfm, m -> NodeView.create(staticPage));
        NativeNodeContainer nnc3 = createNodeWithNodeView(wfm, m -> NodeView.create(dynamicPage));
        NodeViewManager nodeViewManager = NodeViewManager.getInstance();
        String url = nodeViewManager.writeNodeViewResourcesToDiscAndGetFileUrl(nnc);
        String url2 = nodeViewManager.writeNodeViewResourcesToDiscAndGetFileUrl(nnc2);
        String url3 = nodeViewManager.writeNodeViewResourcesToDiscAndGetFileUrl(nnc3);
        String url4 = nodeViewManager.writeNodeViewResourcesToDiscAndGetFileUrl(nnc3);
        assertThat("file url of static pages not expected to change", url, is(url2));
        assertThat("file url of dynamic pages expected to change between node instances", url, is(not(url3)));
        assertThat("file url of dynamic pages not expected for same node instance (without node state change)", url3,
            is(url4));
        assertThat("resource files are expected to be written, too",
            new File(new URI(url.replace("page.html", "resource.html"))).exists(), is(true));
        assertThat(new File(new URI(url)).exists(), is(true));
        assertThat(new File(new URI(url3)).exists(), is(true));
        String pageContent = Files.readLines(new File(new URI(url3)), StandardCharsets.UTF_8).get(0);
        assertThat(pageContent, is("page content"));

        // impose node state changes
        wfm.executeAllAndWaitUntilDone();
        Page dynamicPage2 = Page.builderFromString(() -> "new page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        nnc = createNodeWithNodeView(wfm, m -> NodeView.create(dynamicPage2));
        String url5 = nodeViewManager.writeNodeViewResourcesToDiscAndGetFileUrl(nnc);
        pageContent = Files.readLines(new File(new URI(url5)), StandardCharsets.UTF_8).get(0);
        assertThat(pageContent, is("new page content"));

        wfm.getParent().removeProject(wfm.getID());
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(
            () -> assertThat("dynamic pages are expected to be removed after the workflow has been closed",
                new File(new URI(url5)).exists(), is(false)));
    }

    /**
     * Tests {@link NodeViewManager#hasNodeView(org.knime.core.node.workflow.NodeContainer)} and
     * {@link NodeViewManager#getNodeView(org.knime.core.node.workflow.NodeContainer)} for a node without a node view.
     *
     * @throws IOException
     */
    @Test
    public void testNodeWithoutNodeView() throws IOException {
        WorkflowManager wfm = createEmptyWorkflow();
        NativeNodeContainer nc = createAndAddNode(wfm, new VirtualSubNodeInputNodeFactory(null, new PortType[0]));
        assertThat("node not expected to have a node view", NodeViewManager.hasNodeView(nc), is(false));
        assertThrows(IllegalArgumentException.class, () -> NodeViewManager.getInstance().getNodeView(nc));

        wfm.getParent().removeProject(wfm.getID());
    }

    /**
     * Helper to create a node with a {@link NodeView}.
     *
     * @param wfm the workflow to create the node in
     * @param nodeViewCreator function to create the node view instance
     * @return the newly created node container
     */
    public static NativeNodeContainer createNodeWithNodeView(final WorkflowManager wfm,
        final Function<NodeViewNodeModel, NodeView> nodeViewCreator) {
        return createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));
    }

}
