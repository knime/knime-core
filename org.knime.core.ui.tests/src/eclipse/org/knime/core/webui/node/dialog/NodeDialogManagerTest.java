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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.knime.core.webui.node.dialog.NodeDialogTest.createNodeDialog;
import static org.knime.core.webui.page.PageTest.BUNDLE_ID;
import static org.knime.testing.util.WorkflowManagerUtil.createAndAddNode;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.node.view.NodeViewManager;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.dialog.NodeDialogNodeFactory;
import org.knime.testing.node.dialog.NodeDialogNodeModel;
import org.knime.testing.node.dialog.NodeDialogNodeView;
import org.knime.testing.util.WorkflowManagerUtil;

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
        NodeDialogManager.getInstance().clearCaches();
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
        var page = Page.builder(() -> "test page content", "index.html").build();
        var hasDialog = new AtomicBoolean(true);
        NativeNodeContainer nc = createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(page), hasDialog::get);

        assertThat("node expected to have a node dialog", NodeDialogManager.hasNodeDialog(nc), is(true));
        var nodeDialog = NodeDialogManager.getInstance().getNodeDialog(nc);
        assertThat(nodeDialog.getPage() == page, is(true));

        assertThat(NodeDialogManager.getInstance().callTextInitialDataService(nc), is("test settings"));
        assertThat(nodeDialog.getPage().isCompletelyStatic(), is(false));

        hasDialog.set(false);
        assertThat("node not expected to have a node dialog", NodeDialogManager.hasNodeDialog(nc), is(false));
    }

    /**
     * Tests {@link NodeDialogManager#getPageUrl(NativeNodeContainer)}.
     */
    @Test
    public void testGetNodeDialogPageUrl() {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        var dynamicPage = Page.builder(() -> "page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        NativeNodeContainer nnc = createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(staticPage));
        NativeNodeContainer nnc2 = createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(staticPage));
        NativeNodeContainer nnc3 = createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(dynamicPage));
        var nodeDialogManager = NodeDialogManager.getInstance();
        String url = nodeDialogManager.getPageUrl(nnc).orElse(null);
        String url2 = nodeDialogManager.getPageUrl(nnc2).orElse(null);
        String url3 = nodeDialogManager.getPageUrl(nnc3).orElse(null);
        String url4 = nodeDialogManager.getPageUrl(nnc3).orElse(null);
        assertThat("url of static pages not expected to change", url, is(url2));
        assertThat("url of dynamic pages expected to change between node instances", url, is(not(url3)));
        assertThat("url of dynamic pages not expected for same node instance (without node state change)", url3,
            is(url4));
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
     * Tests {@link NodeDialogManager#callTextInitialDataService(NodeContainer)},
     * {@link NodeDialogManager#callTextDataService(NodeContainer, String)} and
     * {@link NodeDialogManager#callTextApplyDataService(NodeContainer, String)}
     *
     * @throws IOException
     * @throws InvalidSettingsException
     */
    @Test
    public void testCallDataServices() throws IOException, InvalidSettingsException {
        var page = Page.builder(() -> "test page content", "index.html").build();
        Supplier<NodeDialog> nodeDialogSupplier = () -> createNodeDialog(page, new TextNodeSettingsService() { // NOSONAR

            @Override
            public void toNodeSettings(final String s, final Map<SettingsType, NodeSettingsWO> settings) {
                var split = s.split(",");
                settings.get(SettingsType.MODEL).addString(split[0], split[1]);
                settings.get(SettingsType.VIEW).addString(split[0], split[1]);
            }

            @Override
            public String fromNodeSettings(final Map<SettingsType, NodeSettingsRO> settings,
                final PortObjectSpec[] specs) {
                assertThat(settings.size(), is(2));
                return "the node settings";
            }

            @Override
            public void getDefaultNodeSettings(final Map<SettingsType, NodeSettingsWO> settings,
                final PortObjectSpec[] specs) {
                //
            }

        }, new TextDataService() {

            @Override
            public String handleRequest(final String request) {
                return "general data service";
            }
        });

        NativeNodeContainer nc = NodeDialogManagerTest.createNodeWithNodeDialog(m_wfm, nodeDialogSupplier);

        var nodeDialogManager = NodeDialogManager.getInstance();
        assertThat(nodeDialogManager.callTextInitialDataService(nc), is("the node settings"));
        assertThat(nodeDialogManager.callTextDataService(nc, ""), is("general data service"));
        // apply data, i.e. settings
        nodeDialogManager.callTextApplyDataService(nc, "key,node settings value");

        // check node model settings
        var modelSettings = ((NodeDialogNodeModel)nc.getNode().getNodeModel()).getLoadNodeSettings();
        assertThat(modelSettings.getString("key"), is("node settings value"));
        assertThat(nc.getNodeSettings().getNodeSettings("model").getString("key"), is("node settings value"));

        // check view settings
        var viewSettings = getNodeViewSettings(nc);
        assertThat(viewSettings, is(nullValue())); // no view settings available without updating the node view
        NodeViewManager.getInstance().updateNodeViewSettings(nc);
        viewSettings = getNodeViewSettings(nc);
        assertThat(viewSettings.getString("key"), is("node settings value"));
        assertThat(nc.getNodeSettings().getNodeSettings("view").getString("key"), is("node settings value"));

        // check error on apply settings
        String message =
            assertThrows(IOException.class, () -> nodeDialogManager.callTextApplyDataService(nc, "ERROR,invalid"))
                .getMessage();
        assertThat(message, is("Invalid node settings"));
    }

    private static NodeSettingsRO getNodeViewSettings(final NodeContainer nc) {
        return ((NodeDialogNodeView)NodeViewManager.getInstance().getNodeView(nc)).getLoadNodeSettings();
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

    private static NativeNodeContainer createNodeWithNodeDialog(final WorkflowManager wfm,
        final Supplier<NodeDialog> nodeDialogCreator, final BooleanSupplier hasDialog) {
        return createAndAddNode(wfm, new NodeDialogNodeFactory(nodeDialogCreator, hasDialog));
    }

}
