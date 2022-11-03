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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.knime.core.webui.node.view.NodeViewManagerTest.runOnExecutor;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.config.base.JSONConfig.WriterConfig;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
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
     * Tests the creation of {@link NodeViewEnt} instances.
     *
     * @throws IOException
     * @throws InvalidSettingsException
     */
    @Test
    public void testNodeViewEnt() throws IOException, InvalidSettingsException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();

        NativeNodeContainer nncWithoutNodeView =
            WorkflowManagerUtil.createAndAddNode(wfm, new VirtualSubNodeInputNodeFactory(null, new PortType[0]));
        Assertions.assertThatThrownBy(() -> NodeViewEnt.create(nncWithoutNodeView))
            .isInstanceOf(IllegalArgumentException.class);

        Function<NodeViewNodeModel, NodeView> nodeViewCreator = m -> new TestNodeView();
        NativeNodeContainer nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));

        // test entity  when node is _not_ executed
        var ent = NodeViewEnt.create(nnc, null);
        assertThat(ent.getInitialData()).isNull();
        assertThat(ent.getNodeInfo().getNodeState()).isEqualTo("configured");
        assertThat(ent.getNodeInfo().isCanExecute()).isTrue();

        initViewSettingsAndExecute(nnc);
        ent = NodeViewEnt.create(nnc, null);
        checkViewSettings(ent, "view setting value");

        overwriteViewSettingWithFlowVariable(nnc);
        ent = NodeViewEnt.create(nnc, null);
        checkViewSettings(ent, "flow variable value");
        checkOutgoingFlowVariable(nnc, "exposed_flow_variable", "exposed view settings value");

        nnc.setNodeMessage(NodeMessage.newWarning("node message"));
        nnc.getNodeAnnotation().getData().setText("node annotation");
        ent = NodeViewEnt.create(nnc, null);
        assertThat(ent.getProjectId()).startsWith("workflow");
        assertThat(ent.getWorkflowId()).isEqualTo("root");
        assertThat(ent.getNodeId()).isEqualTo("root:2");
        assertThat(ent.getInitialData()).startsWith("dummy initial data");
        assertThat(ent.getInitialSelection()).isNull();
        var resourceInfo = ent.getResourceInfo();
        assertThat(resourceInfo.getPath()).endsWith("index.html");
        assertThat(resourceInfo.getBaseUrl()).isEqualTo("http://org.knime.core.ui.view/");
        assertThat(resourceInfo.getType()).isEqualTo(Resource.ContentType.HTML.toString());
        assertThat(resourceInfo.getId()).isEqualTo(PageUtil.getPageId(nnc, false, PageType.VIEW));
        var nodeInfo = ent.getNodeInfo();
        assertThat(nodeInfo.getNodeName()).isEqualTo("NodeView");
        assertThat(nodeInfo.getNodeAnnotation()).isEqualTo("node annotation");
        assertThat(nodeInfo.getNodeState()).isEqualTo("executed");
        assertThat(nodeInfo.getNodeWarnMessage()).isEqualTo("node message");
        assertThat(nodeInfo.getNodeErrorMessage()).isNull();
        assertThat(nodeInfo.isCanExecute()).isNull();

        // a node view as a 'component' without initial data
        nodeViewCreator = m -> {
            Page p = Page.builder(PageTest.BUNDLE_ID, "files", "component.umd.min.js").build();
            return NodeViewTest.createNodeView(p);
        };
        nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));
        wfm.executeAllAndWaitUntilDone();
        ent = NodeViewEnt.create(nnc, null);
        resourceInfo = ent.getResourceInfo();
        assertThat(ent.getInitialData()).isNull();
        assertThat(resourceInfo.getType()).isEqualTo(Resource.ContentType.VUE_COMPONENT_LIB.toString());
        assertThat(resourceInfo.getPath()).endsWith("component.umd.min.js");
        assertThat(resourceInfo.getBaseUrl()).isEqualTo("http://org.knime.core.ui.view/");

        // test to create a node view entity while running headless (e.g. on the executor)
        NativeNodeContainer nnc2 = nnc;
        runOnExecutor(() -> {
            var ent2 = NodeViewEnt.create(nnc2, null);
            assertThat(ent2.getResourceInfo().getPath()).endsWith("component.umd.min.js");
            assertThat(ent2.getResourceInfo().getBaseUrl()).isNull();
        });

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    /**
     * Extra tests for the {@link NodeViewEnt}'s {@link NodeInfoEnt#isCanExecute()} property.
     * @throws IOException
     */
    @Test
    public void testCanExecuteNodeViewEnt() throws IOException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();

        // node view node with one unconnected input
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(1, 1));
        var ent = NodeViewEnt.create(nnc, null);
        assertThat(ent.getNodeInfo().getNodeState()).isEqualTo("idle");
        assertThat(ent.getNodeInfo().isCanExecute()).isFalse();

        // test node view with available input spec
        var nnc2 = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(0, 1));
        wfm.addConnection(nnc2.getID(), 1, nnc.getID(), 1);
        ent = NodeViewEnt.create(nnc, null);
        assertThat(ent.getNodeInfo().getNodeState()).isEqualTo("configured");
        assertThat(ent.getNodeInfo().isCanExecute()).isTrue();

        // test node view with available input spec but failing configure-call (i.e. node is idle but input spec available)
        var nnc3 = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(1, 0) {

            @Override
            public NodeViewNodeModel createNodeModel() {
                return new NodeViewNodeModel(1, 0) {
                    @Override
                    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
                        throw new InvalidSettingsException("problem");
                    }
                };
            }
        });
        wfm.addConnection(nnc2.getID(), 1, nnc3.getID(), 1);
        ent = NodeViewEnt.create(nnc3, null);
        assertThat(ent.getNodeInfo().getNodeState()).isEqualTo("idle");
        assertThat(ent.getNodeInfo().isCanExecute()).isTrue();
        assertThat(ent.getNodeInfo().getNodeWarnMessage()).isEqualTo("problem");

        WorkflowManagerUtil.disposeWorkflow(wfm);


    }

    private static void initViewSettingsAndExecute(final NativeNodeContainer nnc) throws InvalidSettingsException {
        var nodeSettings = new NodeSettings("node_settings");
        nodeSettings.addNodeSettings("model");
        nodeSettings.addNodeSettings("internal_node_subsettings");

        // some dummy view settings
        var viewSettings = nodeSettings.addNodeSettings("view");
        viewSettings.addString("view setting key", "view setting value");
        viewSettings.addString("view setting key 2", "view setting value 2");
        viewSettings.addString("exposed view setting key", "exposed view settings value");

        var parent = nnc.getParent();
        parent.loadNodeSettings(nnc.getID(), nodeSettings);
        parent.executeAllAndWaitUntilDone();
    }

    private static void overwriteViewSettingWithFlowVariable(final NativeNodeContainer nnc)
        throws InvalidSettingsException {

        var parent = nnc.getParent();
        var nodeSettings = new NodeSettings("node_settings");
        parent.saveNodeSettings(nnc.getID(), nodeSettings);
        var viewVariables = nodeSettings.addNodeSettings("view_variables");
        viewVariables.addString("version", "V_2019_09_13");
        var variableTree = viewVariables.addNodeSettings("tree");

        var variableTreeNode = variableTree.addNodeSettings("view setting key");
        variableTreeNode.addString("used_variable", "flow variable");
        variableTreeNode.addString("exposed_variable", null);

        var exposedVariableTreeNode = variableTree.addNodeSettings("exposed view setting key");
        exposedVariableTreeNode.addString("used_variable", null);
        exposedVariableTreeNode.addString("exposed_variable", "exposed_flow_variable");

        parent.loadNodeSettings(nnc.getID(), nodeSettings);
        parent.executeAllAndWaitUntilDone();

        nnc.getFlowObjectStack().push(new FlowVariable("flow variable", "flow variable value"));
    }

    private static void checkViewSettings(final NodeViewEnt ent, final String expectedSettingValue)
        throws IOException, InvalidSettingsException {
        var settingsWithOverwrittenFlowVariable = new NodeSettings("");
        JSONConfig.readJSON(settingsWithOverwrittenFlowVariable,
            new StringReader(ent.getInitialData().replace("dummy initial data", "")));
        assertThat(settingsWithOverwrittenFlowVariable.getString("view setting key")).isEqualTo(expectedSettingValue);
    }

    private static void checkOutgoingFlowVariable(final NativeNodeContainer nnc, final String key, final String value) {
        var outgoingFlowVariables = nnc.getOutgoingFlowObjectStack().getAllAvailableFlowVariables();
        assertThat(outgoingFlowVariables).containsKey(key);
        assertThat(outgoingFlowVariables.get(key).getValueAsString()).isEqualTo(value);
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
        var initialSelection = selectionEventSource.addEventListenerAndGetInitialEventFor(nnc)
            .map(SelectionEvent::getSelection).orElse(Collections.emptyList());
        var nodeViewEnt = NodeViewEnt.create(nnc, () -> initialSelection);

        assertThat(nodeViewEnt.getInitialSelection()).isEqualTo(List.of("k1", "k2"));

        hiLiteHandler.fireHiLiteEvent(new RowKey("k3"));
        await().pollDelay(ONE_HUNDRED_MILLISECONDS).timeout(FIVE_SECONDS)
            .untilAsserted(() -> verify(consumerMock, times(1)).accept(eq("SelectionEvent"),
                argThat(se -> se.getSelection().equals(List.of("k3")) && se.getMode() == SelectionEventMode.ADD)));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }


    private class TestNodeView implements NodeView {

        private NodeSettingsRO m_settings;

        @Override
        public Optional<InitialDataService> createInitialDataService() {
            return Optional.of(new TextInitialDataService() {

                @Override
                public String getInitialData() {
                    return "dummy initial data\n" + JSONConfig.toJSONString(m_settings, WriterConfig.DEFAULT);
                }
            });
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
            return Page.builder(() -> "blub", "index.html").build();
        }

        @Override
        public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            //
        }

        @Override
        public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
            m_settings = settings;
        }

    }

}
