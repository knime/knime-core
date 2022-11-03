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

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.core.webui.node.dialog.NodeDialogTest.createNodeDialog;
import static org.knime.core.webui.page.PageTest.BUNDLE_ID;
import static org.knime.testing.util.WorkflowManagerUtil.createAndAddNode;

import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.util.DefaultConfigurationLayoutCreator;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.SubnodeContainerConfigurationStringProvider;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.view.NodeViewManager;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.dialog.NodeDialogNodeFactory;
import org.knime.testing.node.dialog.NodeDialogNodeModel;
import org.knime.testing.node.dialog.NodeDialogNodeView;
import org.knime.testing.util.WorkflowManagerUtil;
import org.osgi.framework.FrameworkUtil;

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
    @BeforeEach
    @AfterEach
    public void clearNodeDialogManagerCachesAndFiles() {
        NodeDialogManager.getInstance().clearCaches();
    }

    @SuppressWarnings("javadoc")
    @BeforeEach
    public void createEmptyWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    @SuppressWarnings("javadoc")
    @AfterEach
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

        assertThat(NodeDialogManager.hasNodeDialog(nc)).as("node expected to have a node dialog").isTrue();
        var nodeDialog = NodeDialogManager.getInstance().getNodeDialog(nc);
        assertThat(nodeDialog.getPage() == page).isTrue();

        assertThat(NodeDialogManager.getInstance().callTextInitialDataService(NodeWrapper.of(nc))).isEqualTo("test settings");
        assertThat(nodeDialog.getPage().isCompletelyStatic()).isFalse();

        hasDialog.set(false);
        assertThat(NodeDialogManager.hasNodeDialog(nc)).as("node not expected to have a node dialog").isFalse();
    }

    /**
     * Tests a {@link SubNodeContainer} dialog
     *
     * @throws IOException
     */
    @Test
    public void testSubNodeContainerDialog() throws IOException {
        final var uiModeProperty = "org.knime.component.ui.mode";
        var componentUiMode = System.setProperty(uiModeProperty, "js");
        var bundleContext = FrameworkUtil.getBundle(this.getClass()) .getBundleContext();
        var serviceRegistration = bundleContext.registerService(DefaultConfigurationLayoutCreator.class.getName(),
            new DefaultConfigurationLayoutCreator() { // NOSONAR

                @Override
                public String createDefaultConfigurationLayout(final Map<NodeIDSuffix, DialogNode> configurationNodes)
                    throws IOException {
                    return null;
                }

                @Override
                public void addUnreferencedDialogNodes(
                    final SubnodeContainerConfigurationStringProvider configurationStringProvider,
                    final Map<NodeIDSuffix, DialogNode> allNodes) {
                    //
                }

                @Override
                public void updateConfigurationLayout(
                    final SubnodeContainerConfigurationStringProvider configurationStringProvider) {
                    //
                }

                @Override
                public List<Integer> getConfigurationOrder(
                    final SubnodeContainerConfigurationStringProvider configurationStringProvider,
                    final Map<NodeID, DialogNode> nodes, final WorkflowManager wfm) {
                    return Collections.singletonList(0);
                }

            }, new Hashtable<>());

        try {
            // build workflow
            var wfm = WorkflowManagerUtil.createEmptyWorkflow();
            var nnc = WorkflowManagerUtil.createAndAddNode(wfm, new TestConfigurationNodeFactory());

            var componentId =
                wfm.collapseIntoMetaNode(new NodeID[]{nnc.getID()}, new WorkflowAnnotationID[0], "TestComponent")
                    .getCollapsedMetanodeID();
            wfm.convertMetaNodeToSubNode(componentId);

            var component = wfm.getNodeContainer(componentId);

            assertThat(NodeDialogManager.hasNodeDialog(component)).as("node expected to have a node dialog").isTrue();
            var nodeDialog = NodeDialogManager.getInstance().getNodeDialog(component);
            assertThat(nodeDialog.getPage().getRelativePath()).isEqualTo("NodeDialog.umd.min.js");

            var pageId = NodeDialogManager.getInstance().getPageId(NodeWrapper.of(component), nodeDialog.getPage());
            assertThat(pageId).isEqualTo("defaultdialog");

            // The jsonforms dialog cannot be built from our test node, because it is no valid/known DialogNodeRepresentation,
            // So we just check for the error here.
            var result = NodeDialogManager.getInstance().callTextInitialDataService(NodeWrapper.of(component));
            assertThat(result).contains(
                "Could not read dialog node org.knime.core.webui.node.dialog.TestConfigurationNodeFactory$TestConfigNodeModel");
        } finally {
            if (componentUiMode != null) {
                System.setProperty(uiModeProperty, componentUiMode);
            } else {
                System.clearProperty(uiModeProperty);
            }
            serviceRegistration.unregister();
        }
    }

    /**
     * Tests {@link NodeDialogManager#getPagePath(NodeWrapper)}.
     */
    @Test
    public void testGetNodeDialogPageUrl() {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        var dynamicPage = Page.builder(() -> "page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        var nnc = NodeWrapper.of(createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(staticPage)));
        var nnc2 = NodeWrapper.of(createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(staticPage)));
        var nnc3 = NodeWrapper.of(createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(dynamicPage)));
        var nodeDialogManager = NodeDialogManager.getInstance();
        String path = nodeDialogManager.getPagePath(nnc);
        String path2 = nodeDialogManager.getPagePath(nnc2);
        String path3 = nodeDialogManager.getPagePath(nnc3);
        String path4 = nodeDialogManager.getPagePath(nnc3);
        assertThat(path).as("url of static pages not expected to change").isEqualTo(path2);
        assertThat(path).as("url of dynamic pages expected to change between node instances").isNotEqualTo(path3);
        assertThat(path3).as("url of dynamic pages not expected for same node instance (without node state change)")
            .isEqualTo(path4);
    }

    /**
     * Tests {@link NodeDialogManager#hasNodeDialog(org.knime.core.node.workflow.NodeContainer)} and
     * {@link NodeDialogManager#getNodeDialog(org.knime.core.node.workflow.NodeContainer)} for a node without a node
     * view.
     */
    @Test
    public void testNodeWithoutNodeDialog() {
        NativeNodeContainer nc = createAndAddNode(m_wfm, new VirtualSubNodeInputNodeFactory(null, new PortType[0]));
        assertThat(NodeDialogManager.hasNodeDialog(nc)).as("node not expected to have a node dialog").isFalse();
        Assertions.assertThatThrownBy(() -> NodeDialogManager.getInstance().getNodeDialog(nc))
            .isInstanceOf(IllegalArgumentException.class);
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
                assertThat(settings.size()).isEqualTo(2);
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

        var nc = NodeDialogManagerTest.createNodeWithNodeDialog(m_wfm, nodeDialogSupplier);
        var nncWrapper = NodeWrapper.of(nc);

        var nodeDialogManager = NodeDialogManager.getInstance();
        assertThat(nodeDialogManager.callTextInitialDataService(nncWrapper)).isEqualTo("the node settings");
        assertThat(nodeDialogManager.callTextDataService(nncWrapper, "")).isEqualTo("general data service");
        // apply data, i.e. settings
        nodeDialogManager.callTextApplyDataService(nncWrapper, "key,node settings value");

        // check node model settings
        var modelSettings = ((NodeDialogNodeModel)nc.getNode().getNodeModel()).getLoadNodeSettings();
        assertThat(modelSettings.getString("key")).isEqualTo("node settings value");
        assertThat(nc.getNodeSettings().getNodeSettings("model").getString("key")).isEqualTo("node settings value");

        // check view settings
        var viewSettings = getNodeViewSettings(nc);
        assertThat(viewSettings).isNull(); // no view settings available without updating the node view
        NodeViewManager.getInstance().updateNodeViewSettings(nc);
        viewSettings = getNodeViewSettings(nc);
        assertThat(viewSettings.getString("key")).isEqualTo("node settings value");
        assertThat(nc.getNodeSettings().getNodeSettings("view").getString("key")).isEqualTo("node settings value");

        // check error on apply settings
        Assertions.assertThatThrownBy(() -> nodeDialogManager.callTextApplyDataService(nncWrapper, "ERROR,invalid"))
            .isInstanceOf(IOException.class).hasMessage("Invalid node settings: validation expected to fail");
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
