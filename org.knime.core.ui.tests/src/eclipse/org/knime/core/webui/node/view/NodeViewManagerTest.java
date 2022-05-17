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
import static org.junit.Assert.assertTrue;
import static org.knime.core.webui.node.view.NodeViewTest.createNodeView;
import static org.knime.core.webui.page.PageTest.BUNDLE_ID;
import static org.knime.testing.util.WorkflowManagerUtil.createAndAddNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.data.text.TextReExecuteDataService;
import org.knime.core.webui.node.view.selection.SelectionTranslationService;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.node.view.NodeViewNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests for {@link NodeViewManager}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class NodeViewManagerTest {

    private static final String JAVA_AWT_HEADLESS = "java.awt.headless";

    private WorkflowManager m_wfm;

    /**
     * Clears the caches and files of the {@link NodeViewManager}.
     */
    @Before
    @After
    public void clearNodeViewManagerCachesAndFiles() {
        NodeViewManager.getInstance().clearCaches();
    }

    @SuppressWarnings("javadoc")
    @Before
    public void createEmptyWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    @SuppressWarnings("javadoc")
    @After
    public void disposeWorkflow() {
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
    }

    /**
     * Tests multiple {@link NodeViewManager}-methods using a simple node view.
     */
    @Test
    public void testSimpleNodeViewNode() {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var hasView = new AtomicBoolean(true);
        NativeNodeContainer nc = createNodeWithNodeView(m_wfm, m -> createNodeView(page), hasView::get);

        assertThat("node expected to have a node view", NodeViewManager.hasNodeView(nc), is(true));
        var nodeView = NodeViewManager.getInstance().getNodeView(nc);
        assertThat(nodeView.getPage() == page, is(true));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> NodeViewManager.getInstance().callTextInitialDataService(nc));
        assertThat(ex.getMessage(), containsString("No text initial data service available"));
        assertThat(nodeView.getPage().isCompletelyStatic(), is(false));

        hasView.set(false);
        assertThat("node not expected to have a node view", NodeViewManager.hasNodeView(nc), is(false));
    }

    /**
     * Makes sure that view settings are loaded from the node into the node view when created for the first time.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testLoadViewSettingsOnViewCreation() throws InvalidSettingsException {
        var page = Page.builder(() -> "test page content", "index.html").build();
        AtomicReference<NodeSettingsRO> loadedNodeSettings = new AtomicReference<>();
        NativeNodeContainer nc = createNodeWithNodeView(m_wfm, m -> new NodeView() { // NOSONAR

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
            public Page getPage() {
                return page;
            }

            @Override
            public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                //
            }

            @Override
            public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
                loadedNodeSettings.set(settings);
            }

        });

        // prepare view settings
        var settings = new NodeSettings("node_settings");
        m_wfm.saveNodeSettings(nc.getID(), settings);
        var viewSettings = new NodeSettings("view");
        viewSettings.addString("view setting key", "view setting value");
        settings.addNodeSettings(viewSettings);
        settings.addNodeSettings(new NodeSettings("model"));
        m_wfm.loadNodeSettings(nc.getID(), settings);

        // test
        NodeViewManager.getInstance().updateNodeViewSettings(nc);
        assertTrue(loadedNodeSettings.get().containsKey("view setting key"));
    }

    /**
     * Tests {@link NodeViewManager#getPageUrl(NativeNodeContainer)}.
     *
     * @throws URISyntaxException
     * @throws IOException
     */
    @Test
    public void testGetNodeViewPageUrl() throws URISyntaxException, IOException {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        var dynamicPage = Page.builder(() -> "page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        NativeNodeContainer nnc = createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage));
        NativeNodeContainer nnc2 = createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage));
        NativeNodeContainer nnc3 = createNodeWithNodeView(m_wfm, m -> createNodeView(dynamicPage));
        var nodeViewManager = NodeViewManager.getInstance();
        String url = nodeViewManager.getPageUrl(nnc).orElse("");
        String url2 = nodeViewManager.getPageUrl(nnc2).orElse(null);
        String url3 = nodeViewManager.getPageUrl(nnc3).orElse(null);
        String url4 = nodeViewManager.getPageUrl(nnc3).orElse(null);
        assertThat("url of static pages not expected to change", url, is(url2));
        assertThat("url of dynamic pages expected to change between node instances", url, is(not(url3)));
        assertThat("url of dynamic pages not expected for same node instance (without node state change)", url3,
            is(url4));

        runOnExecutor(() -> assertThat(nodeViewManager.getPageUrl(nnc2).isEmpty(), is(true)));
    }

    /**
     * Tests {@link NodeViewManager#getPagePath(NativeNodeContainer)}.
     */
    @Test
    public void testGetNodeViewPagePath() {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        var dynamicPage = Page.builder(() -> "page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        var nnc = createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage));
        var nnc2 = createNodeWithNodeView(m_wfm, m -> createNodeView(dynamicPage));

        var nodeViewManager = NodeViewManager.getInstance();
        assertThat(nodeViewManager.getPagePath(nnc).isEmpty(), is(true));

        runOnExecutor(() -> { // NOSONAR
            String path = nodeViewManager.getPagePath(nnc).orElse(null);
            assertThat(nodeViewManager.getPageMapSize(), is(1));
            String path2 = nodeViewManager.getPagePath(nnc2).orElse(null);
            assertThat(nodeViewManager.getPageMapSize(), is(2));
            var resourcePrefix1 = "view_" + nnc.getNode().getFactory().getClass().getName();
            var resourcePrefix2 = "view_" + nnc2.getID().toString().replace(":", "_");
            assertThat(path, is(resourcePrefix1 + "/page.html"));
            assertThat(path2, is(resourcePrefix2 + "/page.html"));

            testGetNodeViewPageResource(resourcePrefix1, resourcePrefix2);
        });

        m_wfm.removeNode(nnc.getID());
        // make sure that the pages are removed from the cache after the node has been deleted)
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(nodeViewManager.getPageMapSize(), is(1)));
    }

    private static void testGetNodeViewPageResource(final String resourcePrefix1, final String resourcePrefix2) {
        var nodeViewManager = NodeViewManager.getInstance();
        assertThat(nodeViewManager.getPageResource(resourcePrefix1 + "/page.html").isPresent(), is(true));
        assertThat(nodeViewManager.getPageResource(resourcePrefix1 + "/resource.html").isPresent(), is(true));
        assertThat(nodeViewManager.getPageResource(resourcePrefix2 + "/resource.html").isPresent(), is(true));
        assertThat(nodeViewManager.getPageResource("/test").isEmpty(), is(true));
        assertThat(nodeViewManager.getPageResource("test").isEmpty(), is(true));
        assertThat(nodeViewManager.getPageResource("test/test").isEmpty(), is(true));
    }

    /**
     * Tests {@link NodeViewManager#hasNodeView(org.knime.core.node.workflow.NodeContainer)} and
     * {@link NodeViewManager#getNodeView(org.knime.core.node.workflow.NodeContainer)} for a node without a node view.
     */
    @Test
    public void testNodeWithoutNodeView() {
        NativeNodeContainer nc = createAndAddNode(m_wfm, new VirtualSubNodeInputNodeFactory(null, new PortType[0]));
        assertThat("node not expected to have a node view", NodeViewManager.hasNodeView(nc), is(false));
        assertThrows(IllegalArgumentException.class, () -> NodeViewManager.getInstance().getNodeView(nc));
    }

    /**
     * Makes sure that the page-cache is cleaned up after node removal or closing the workflow for dynamic pages.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testNodeCleanUpDynamicPage() throws URISyntaxException {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var nc = createNodeWithNodeView(m_wfm, m -> createNodeView(page));
        var nodeViewManager = NodeViewManager.getInstance();

        // remove node
        nodeViewManager.getPageUrl(nc);
        assertThat(nodeViewManager.getNodeViewMapSize(), is(1));
        assertThat(nodeViewManager.getPageMapSize(), is(1));
        m_wfm.removeNode(nc.getID());
        untilAsserted(() -> {
            assertThat(nodeViewManager.getNodeViewMapSize(), is(0));
            assertThat(nodeViewManager.getPageMapSize(), is(0));
        });

        // close workflow
        nodeViewManager.getPageUrl(nc);
        assertThat(nodeViewManager.getNodeViewMapSize(), is(1));
        assertThat(nodeViewManager.getPageMapSize(), is(1));
        m_wfm.getParent().removeProject(m_wfm.getID());
        untilAsserted(() -> {
            assertThat(nodeViewManager.getNodeViewMapSize(), is(0));
            assertThat(nodeViewManager.getPageMapSize(), is(0));
        });
    }

    /**
     * Makes sure that the page-cache is cleaned up after node removal or closing the workflow for static pages.
     */
    @Test
    public void testNodeCleanUpStaticPage() {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").build();
        var nc = createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage));
        var nodeViewManager = NodeViewManager.getInstance();

        // remove node
        nodeViewManager.getPageUrl(nc);
        assertThat(nodeViewManager.getPageMapSize(), is(1));
        m_wfm.removeNode(nc.getID());
        untilAsserted(() -> assertThat(nodeViewManager.getPageMapSize(), is(0)));

        // close workflow
        nodeViewManager.getPageUrl(nc);
        assertThat(nodeViewManager.getPageMapSize(), is(1));
        m_wfm.getParent().removeProject(m_wfm.getID());
        untilAsserted(() -> assertThat(nodeViewManager.getPageMapSize(), is(0)));
    }

    private static void untilAsserted(final ThrowingRunnable assertion) {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(assertion);
    }

    /**
     * Tests {@link NodeViewManager#callTextInitialDataService(NodeContainer)},
     * {@link NodeViewManager#callTextDataService(NodeContainer, String)} and
     * {@link NodeViewManager#callTextApplyDataService(NodeContainer, String)}
     */
    @Test
    public void testCallDataServices() {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var nodeView = createNodeView(page, new TextInitialDataService() {

            @Override
            public String getInitialData() {
                return "init service";
            }
        }, new TextDataService() {

            @Override
            public String handleRequest(final String request) {
                return "general data service";
            }
        }, new TextReExecuteDataService() {

            @Override
            public Optional<String> validateData(final String data) throws IOException {
                throw new UnsupportedOperationException("should not be called in this test");
            }

            @Override
            public void applyData(final String data) throws IOException {
                throw new UnsupportedOperationException("should not be called in this test");
            }

            @Override
            public void reExecute(final String data) throws IOException {
                throw new IOException("re-execute data service");

            }
        });
        NativeNodeContainer nc = NodeViewManagerTest.createNodeWithNodeView(m_wfm, m -> nodeView);

        var nodeViewManager = NodeViewManager.getInstance();
        assertThat(nodeViewManager.callTextInitialDataService(nc), is("init service"));
        assertThat(nodeViewManager.callTextDataService(nc, ""), is("general data service"));
        String message =
            assertThrows(IOException.class, () -> nodeViewManager.callTextApplyDataService(nc, "ERROR,test"))
                .getMessage();
        assertThat(message, is("re-execute data service"));
    }

    /**
     * Tests {@link NodeViewManager#callSelectionTranslationService(NodeContainer, Set)} and
     * {@link NodeViewManager#callSelectionTranslationService(NodeContainer, List)}.
     */
    @Test
    public void testCallSelectionTranslationService() {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var nodeView = createNodeView(page, null, null, null, new SelectionTranslationService() {
            @Override
            public Set<RowKey> toRowKeys(final List<String> selection) throws IOException {
                throw new IOException(selection.toString());
            }

            @Override
            public List<String> fromRowKeys(final Set<RowKey> rowKeys) throws IOException {
                throw new IOException(rowKeys.toString());
            }
        });
        var nc = NodeViewManagerTest.createNodeWithNodeView(m_wfm, m -> nodeView);

        var message = assertThrows(IOException.class,
            () -> NodeViewManager.getInstance().callSelectionTranslationService(nc, Collections.singletonList("foo")))
                .getMessage();
        assertThat(message, is("[foo]"));

        message = assertThrows(IOException.class,
            () -> NodeViewManager.getInstance().callSelectionTranslationService(nc, Set.of(new RowKey("bar"))))
                .getMessage();
        assertThat(message, is("[bar]"));

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

    private static NativeNodeContainer createNodeWithNodeView(final WorkflowManager wfm,
        final Function<NodeViewNodeModel, NodeView> nodeViewCreator, final BooleanSupplier hasView) {
        return createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator, hasView));
    }

    /**
     * Simulates to run stuff as if it was run on the executor (which usually means to run the AP headless).
     *
     * @param r
     */
    public static void runOnExecutor(final Runnable r) {
        System.setProperty(JAVA_AWT_HEADLESS, "true");
        try {
            r.run();
        } finally {
            System.clearProperty(JAVA_AWT_HEADLESS);
        }
    }

}
