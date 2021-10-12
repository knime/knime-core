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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.knime.core.webui.node.view.NodeViewManagerTest.runOnExecutor;

import java.io.IOException;
import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.PageTest;
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
        Assert.assertThrows(IllegalArgumentException.class, () -> new NodeViewEnt(nncWithoutNodeView));

        Function<NodeViewNodeModel, NodeView> nodeViewCreator = m -> {
            Page p = Page.builderFromString(() -> "blub", "index.html").build();
            return NodeView.builder(p).initialDataService(new TextInitialDataService() {

                @Override
                public String getInitialData() {
                    return "dummy initial data";
                }
            }).build();
        };
        NativeNodeContainer nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));
        nnc.setNodeMessage(NodeMessage.newWarning("node message"));
        nnc.getNodeAnnotation().getData().setText("node annotation");

        var ent = new NodeViewEnt(nnc);
        assertThat(ent.getProjectId(), startsWith("workflow"));
        assertThat(ent.getWorkflowId(), is("root"));
        assertThat(ent.getNodeId(), is("root:2"));
        assertThat(ent.getUrl(), endsWith("index.html"));
        assertThat(ent.getPath(), is(nullValue()));
        assertThat(ent.isWebComponent(), is(false));
        assertThat(ent.getInitialData(), is("dummy initial data"));
        NodeInfoEnt info = ent.getNodeInfo();
        assertThat(info.getNodeName(), is("NodeView"));
        assertThat(info.getNodeAnnotation(), is("node annotation"));
        assertThat(info.getNodeState(), is("configured"));
        assertThat(info.getNodeWarnMessage(), is("node message"));
        assertThat(info.getNodeErrorMessage(), is(nullValue()));

        // a node view as a 'component' without initial data
        nodeViewCreator = m -> {
            Page p = Page.builder(PageTest.BUNDLE_ID, "files", "component.js").build();
            return NodeView.create(p);
        };
        nnc = WorkflowManagerUtil.createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));
        ent = new NodeViewEnt(nnc);
        assertThat(ent.isWebComponent(), is(true));
        assertThat(ent.getUrl(), Matchers.endsWith("component.js"));
        assertThat(ent.getPath(), is(nullValue()));
        assertThat(ent.getInitialData(), is(nullValue()));

        // test to create a node view entity while running headless (e.g. on the executor)
        NativeNodeContainer nnc2 = nnc;
        runOnExecutor(() -> {
            var ent2 = new NodeViewEnt(nnc2);
            assertThat(ent2.getPath(), endsWith("component.js"));
            assertThat(ent2.getUrl(), is(nullValue()));
        });

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

}
