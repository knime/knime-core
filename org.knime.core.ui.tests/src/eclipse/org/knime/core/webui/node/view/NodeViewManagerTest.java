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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.node.view.NodeViewNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;

import com.google.common.io.Files;

/**
 * Tests for {@link NodeViewManager}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
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
        NodeViewManager.getInstance().clearCachesAndFiles();
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
        NativeNodeContainer nc = createNodeWithNodeView(m_wfm, m -> createNodeView(page));

        assertThat("node expected to have a node view", NodeViewManager.hasNodeView(nc), is(true));
        var nodeView = NodeViewManager.getInstance().getNodeView(nc);
        assertThat(nodeView.getPage() == page, is(true));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> NodeViewManager.getInstance().getNodeView(nc).callTextInitialDataService());
        assertThat(ex.getMessage(), containsString("No text initial data service available"));
        assertThat(nodeView.getPage().isCompletelyStatic(), is(false));
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
            public Optional<InitialDataService> getInitialDataService() {
                return Optional.empty();
            }

            @Override
            public Optional<DataService> getDataService() {
                return Optional.empty();
            }

            @Override
            public Optional<ApplyDataService> getApplyDataService() {
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
        NodeViewManager.getInstance().getNodeView(nc);
        assertTrue(loadedNodeSettings.get().containsKey("view setting key"));
    }

    /**
     * Tests {@link NodeViewManager#getNodeViewPageUrl(NativeNodeContainer)}.
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
        String url = nodeViewManager.getNodeViewPageUrl(nnc).orElse("");
        String url2 = nodeViewManager.getNodeViewPageUrl(nnc2).orElse(null);
        String url3 = nodeViewManager.getNodeViewPageUrl(nnc3).orElse(null);
        String url4 = nodeViewManager.getNodeViewPageUrl(nnc3).orElse(null);
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
        m_wfm.executeAllAndWaitUntilDone();
        var dynamicPage2 = Page.builder(() -> "new page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        nnc = createNodeWithNodeView(m_wfm, m -> createNodeView(dynamicPage2));
        String url5 = nodeViewManager.getNodeViewPageUrl(nnc).orElse(null);
        pageContent = Files.readLines(new File(new URI(url5)), StandardCharsets.UTF_8).get(0);
        assertThat(pageContent, is("new page content"));

        runOnExecutor(() -> assertThat(nodeViewManager.getNodeViewPageUrl(nnc2).isEmpty(), is(true)));
    }

    /**
     * Tests {@link NodeViewManager#getNodeViewPagePath(NativeNodeContainer)}.
     */
    @Test
    public void testGetNodeViewPagePath() {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        var dynamicPage = Page.builder(() -> "page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        var nnc = createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage));
        var nnc2 = createNodeWithNodeView(m_wfm, m -> createNodeView(dynamicPage));

        var nodeViewManager = NodeViewManager.getInstance();
        assertThat(nodeViewManager.getNodeViewPagePath(nnc).isEmpty(), is(true));

        runOnExecutor(() -> { // NOSONAR
            String path = nodeViewManager.getNodeViewPagePath(nnc).orElse(null);
            assertThat(nodeViewManager.getPageMapSize(), is(1));
            String path2 = nodeViewManager.getNodeViewPagePath(nnc2).orElse(null);
            assertThat(nodeViewManager.getPageMapSize(), is(2));
            var resourcePrefix1 = nnc.getNode().getFactory().getClass().getName();
            var resourcePrefix2 = nnc2.getID().toString().replace(":", "_");
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
        assertThat(nodeViewManager.getNodeViewPageResource(resourcePrefix1 + "/page.html").isPresent(), is(true));
        assertThat(nodeViewManager.getNodeViewPageResource(resourcePrefix1 + "/resource.html").isPresent(), is(true));
        assertThat(nodeViewManager.getNodeViewPageResource(resourcePrefix2 + "/resource.html").isPresent(), is(true));
        assertThat(nodeViewManager.getNodeViewPageResource("/test").isEmpty(), is(true));
        assertThat(nodeViewManager.getNodeViewPageResource("test").isEmpty(), is(true));
        assertThat(nodeViewManager.getNodeViewPageResource("test/test").isEmpty(), is(true));
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
     * Makes sure that in case of a dynamic page the node view cache (but not! the page resource files) is cleaned up
     * after a node state change, node removal and closing the workflow.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testNodeCleanUpDynamicPage() throws URISyntaxException {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var nc = createNodeWithNodeView(m_wfm, m -> createNodeView(page));

        var nodeViewManager = NodeViewManager.getInstance();

        // node state change
        String url = nodeViewManager.getNodeViewPageUrl(nc).orElse(null);
        assertThat("node view file resource expected to be written", new File(new URI(url)).exists(), is(true));
        assertThat(nodeViewManager.getNodeViewMapSize(), is(1));
        m_wfm.executeAllAndWaitUntilDone();
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat("dynamic pages are expected to be remain if the node state changed",
                new File(new URI(url)).exists(), is(true));
            assertThat(nodeViewManager.getNodeViewMapSize(), is(1));
        });

        // remove node
        String url2 = nodeViewManager.getNodeViewPageUrl(nc).orElse(null);
        assertThat("node view file resource expected to be written", new File(new URI(url2)).exists(), is(true));
        assertThat(nodeViewManager.getNodeViewMapSize(), is(1));
        m_wfm.removeNode(nc.getID());
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat("dynamic pages are expected to be removed if the respective node has been removed",
                new File(new URI(url2)).exists(), is(false));
            assertThat(nodeViewManager.getNodeViewMapSize(), is(0));
        });

        // close workflow
        String url3 = nodeViewManager.getNodeViewPageUrl(nc).orElse(null);
        assertThat("node view file resource expected to be written", new File(new URI(url3)).exists(), is(true));
        assertThat(nodeViewManager.getNodeViewMapSize(), is(1));
        m_wfm.getParent().removeProject(m_wfm.getID());
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat("dynamic pages are expected to be removed after the workflow has been closed",
                new File(new URI(url3)).exists(), is(false));
            assertThat(nodeViewManager.getNodeViewMapSize(), is(0));
        });
    }


    /**
     * Makes sure that in case of a static page the node view cache and(!) the page resource files are cleaned up after
     * a node state change, node removal and closing the workflow.
     */
    @Test
    public void testNodeCleanUpStaticPage() {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").build();
        var nc = createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage));

        // node state change
        String url = NodeViewManager.getInstance().getNodeViewPageUrl(nc).orElse(null);
        m_wfm.executeAllAndWaitUntilDone();
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat("static pages are expected to remain if the node state changed",
                new File(new URI(url)).exists(), is(true)));

        // remove node
        String url2 = NodeViewManager.getInstance().getNodeViewPageUrl(nc).orElse(null);
        m_wfm.removeNode(nc.getID());
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(
            () -> assertThat("static pages are expected to be remain if the respective node has been removed",
                new File(new URI(url2)).exists(), is(true)));

        // close workflow
        String url3 = NodeViewManager.getInstance().getNodeViewPageUrl(nc).orElse(null);
        m_wfm.getParent().removeProject(m_wfm.getID());
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat("static pages are expected to be remain after the workflow has been closed",
                new File(new URI(url3)).exists(), is(true)));
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
