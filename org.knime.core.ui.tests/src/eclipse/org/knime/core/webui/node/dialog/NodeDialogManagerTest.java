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
 *  NodeDialog, and NodeDialog) and that only interoperate with KNIME through
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
 *   Oct 17, 2021 (hornm): created
 */
package org.knime.core.webui.node.dialog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThrows;
import static org.knime.core.webui.page.PageTest.BUNDLE_ID;
import static org.knime.testing.util.WorkflowManagerUtil.createAndAddNode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.dialog.NodeDialogNodeFactory;
import org.knime.testing.util.WorkflowManagerUtil;

import com.google.common.io.Files;

/**
 * Tests for {@link NodeDialogManager}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeDialogManagerTest {

    private WorkflowManager m_wfm;

    /**
     * Clears the caches and files of the {@link NodeDialogManager}.
     */
    @Before
    @After
    public void clearNodeDialogManagerCachesAndFiles() {
        NodeDialogManager.getInstance().clearCachesAndFiles();
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
     * Tests multiple {@link NodeDialogManager}-methods using a simple node dialog.
     */
    @Test
    public void testSimpleNodeDialogNode() {
        var page = Page.builderFromString(() -> "test page content", "index.html").build();
        NativeNodeContainer nc = createNodeWithNodeDialog(m_wfm, () -> NodeDialog.builder(page).build());

        assertThat("node expected to have a node dialog", NodeDialogManager.hasNodeDialog(nc), is(true));
        var nodeDialog = NodeDialogManager.getInstance().getNodeDialog(nc);
        assertThat(nodeDialog.getPage() == page, is(true));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> NodeDialogManager.getInstance().getNodeDialog(nc).callTextInitialDataService());
        assertThat(ex.getMessage(), containsString("No text initial data service available"));
        assertThat(nodeDialog.getPage().isCompletelyStatic(), is(false));
    }

    /**
     * Tests {@link NodeDialogManager#getNodeDialogPageUrl(NativeNodeContainer)}.
     *
     * @throws URISyntaxException
     * @throws IOException
     */
    @Test
    public void testGetNodeDialogPageUrl() throws URISyntaxException, IOException {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        var dynamicPage = Page.builderFromString(() -> "page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        NativeNodeContainer nnc = createNodeWithNodeDialog(m_wfm, () -> NodeDialog.builder(staticPage).build());
        NativeNodeContainer nnc2 = createNodeWithNodeDialog(m_wfm, () -> NodeDialog.builder(staticPage).build());
        NativeNodeContainer nnc3 = createNodeWithNodeDialog(m_wfm, () -> NodeDialog.builder(dynamicPage).build());
        var nodeDialogManager = NodeDialogManager.getInstance();
        String url = nodeDialogManager.getNodeDialogPageUrl(nnc);
        String url2 = nodeDialogManager.getNodeDialogPageUrl(nnc2);
        String url3 = nodeDialogManager.getNodeDialogPageUrl(nnc3);
        String url4 = nodeDialogManager.getNodeDialogPageUrl(nnc3);
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
        var dynamicPage2 = Page.builderFromString(() -> "new page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        nnc = createNodeWithNodeDialog(m_wfm, () -> NodeDialog.builder(dynamicPage2).build());
        String url5 = nodeDialogManager.getNodeDialogPageUrl(nnc);
        pageContent = Files.readLines(new File(new URI(url5)), StandardCharsets.UTF_8).get(0);
        assertThat(pageContent, is("new page content"));
    }

    /**
     * Tests {@link NodeDialogManager#hasNodeDialog(org.knime.core.node.workflow.NodeContainer)} and
     * {@link NodeDialogManager#getNodeDialog(org.knime.core.node.workflow.NodeContainer)} for a node without a node
     * view.
     */
    @Test
    public void testNodeWithoutNodeDialog() {
        NativeNodeContainer nc = createAndAddNode(m_wfm, new VirtualSubNodeInputNodeFactory(null, new PortType[0]));
        assertThat("node not expected to have a node dialog", NodeDialogManager.hasNodeDialog(nc), is(false));
        assertThrows(IllegalArgumentException.class, () -> NodeDialogManager.getInstance().getNodeDialog(nc));
    }

    /**
     * Helper to create a node with a {@link NodeDialog}.
     *
     * @param wfm the workflow to create the node in
     * @param nodeDialogCreator function to create the node dialog instance
     * @return the newly created node container
     */
    public static NativeNodeContainer createNodeWithNodeDialog(final WorkflowManager wfm,
        final Supplier<NodeDialog> nodeDialogCreator) {
        return createAndAddNode(wfm, new NodeDialogNodeFactory(nodeDialogCreator));
    }

}
