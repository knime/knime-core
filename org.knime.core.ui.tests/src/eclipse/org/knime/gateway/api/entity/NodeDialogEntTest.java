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
 *   Mar 23, 2022 (hornm): created
 */
package org.knime.gateway.api.entity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogManagerTest;
import org.knime.core.webui.node.dialog.NodeDialogTest;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.dialog.NodeDialogNodeFactory;
import org.knime.testing.util.WorkflowManagerUtil;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Tests {@link NodeDialogEnt}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeDialogEntTest {

    /**
     * Makes sure that {@link NodeDialogEnt#getFlowVariableSettings()} are correctly created.
     *
     * @throws IOException
     * @throws InvalidSettingsException
     */
    @Test
    public void testFlowVariableSettingsEnt() throws IOException, InvalidSettingsException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();

        Supplier<NodeDialog> nodeDialogCreator =
            () -> NodeDialogTest.createNodeDialog(Page.builder(() -> "page content", "page.html").build());
        var nnc = NodeDialogManagerTest.createNodeWithNodeDialog(wfm, nodeDialogCreator);

        initNodeSettings(nnc);
        nnc.getFlowObjectStack().push(new FlowVariable("flow variable 2", "test"));

        var flowVariableSettingsEnt = new NodeDialogEnt(nnc).getFlowVariableSettings();

        var mapper = JsonMapper.builder().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true).build();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(Include.NON_NULL);
        var expectedJson = "{\n"
            + "  \"modelVariables\" : {\n"
            + "    \"model setting\" : {\n"
            + "      \"controllingFlowVariableAvailable\" : false,\n"
            + "      \"controllingFlowVariableName\" : \"flow variable 1\",\n"
            + "      \"leaf\" : true\n"
            + "    }\n"
            + "  },\n"
            + "  \"viewVariables\" : {\n"
            + "    \"view setting\" : {\n"
            + "      \"controllingFlowVariableAvailable\" : true,\n"
            + "      \"controllingFlowVariableName\" : \"flow variable 2\",\n"
            + "      \"leaf\" : true\n"
            + "    },\n"
            + "    \"nested\" : {\n"
            + "      \"nested view settings 2\" : {\n"
            + "        \"exposedFlowVariableName\" : \"exposed var name\",\n"
            + "        \"leaf\" : true\n"
            + "      },\n"
            + "      \"nested view settings\" : {\n"
            + "        \"controllingFlowVariableAvailable\" : false,\n"
            + "        \"controllingFlowVariableName\" : \"flow variable 3\",\n"
            + "        \"exposedFlowVariableName\" : \"exposed var name\",\n"
            + "        \"leaf\" : true\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";
        ObjectWriter writer =
            mapper.writer(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter().withLinefeed("\n")));
        var json = writer.writeValueAsString(flowVariableSettingsEnt);
        assertThat(json, is(expectedJson));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    /**
     * Tests that {@link NodeDialogEnt}-instances can be created without problems even if the input ports of a node are
     * not connected.
     *
     * @throws IOException
     */
    @Test
    public void testOpenDialogWithoutConnectedInput() throws IOException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(
                () -> NodeDialogTest.createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                    NodeDialogTest.createTextSettingsDataService(), null),
                1));

        var nodeDialogEnt = new NodeDialogEnt(nc);
        assertThat(nodeDialogEnt.getFlowVariableSettings().getViewVariables().isEmpty(), is(true));
        assertThat(nodeDialogEnt.getInitialData(), containsString("a default model setting"));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    private static void initNodeSettings(final NativeNodeContainer nnc) throws InvalidSettingsException {
        var parent = nnc.getParent();
        var nodeSettings = new NodeSettings("node_settings");
        parent.saveNodeSettings(nnc.getID(), nodeSettings);

        initModelSettings(nodeSettings);
        initModelVariableSettings(nodeSettings);
        initViewSettings(nodeSettings);
        initViewVariableSettings(nodeSettings);

        parent.loadNodeSettings(nnc.getID(), nodeSettings);
        parent.executeAllAndWaitUntilDone();
    }

    private static void initModelSettings(final NodeSettings ns) {
        var modelSettings = ns.addNodeSettings("model");
        modelSettings.addString("model setting", "model setting value");
    }

    private static void initModelVariableSettings(final NodeSettings ns) {
        var modelVariables = ns.addNodeSettings("variables");
        modelVariables.addString("version", "V_2019_09_13");
        var variableTree = modelVariables.addNodeSettings("tree");
        var variableTreeNode = variableTree.addNodeSettings("model setting");
        variableTreeNode.addString("used_variable", "flow variable 1");
        variableTreeNode.addString("exposed_variable", null);
    }

    private static void initViewSettings(final NodeSettings ns) {
        var viewSettings = ns.addNodeSettings("view");
        viewSettings.addString("view setting", "view setting value");

        var nested = viewSettings.addNodeSettings("nested");
        nested.addString("nested view setting", "nested view setting value");
    }

    private static void initViewVariableSettings(final NodeSettings ns) {
        var viewVariables = ns.addNodeSettings("view_variables");
        viewVariables.addString("version", "V_2019_09_13");
        var variableTree = viewVariables.addNodeSettings("tree");
        var variableTreeNode1 = variableTree.addNodeSettings("view setting");
        variableTreeNode1.addString("used_variable", "flow variable 2");
        variableTreeNode1.addString("exposed_variable", null);

        var nested = variableTree.addNodeSettings("nested");
        var variableTreeNode2 = nested.addNodeSettings("nested view settings");
        variableTreeNode2.addString("used_variable", "flow variable 3");
        variableTreeNode2.addString("exposed_variable", "exposed var name");

        var variableTreeNode3 = nested.addNodeSettings("nested view settings 2");
        variableTreeNode3.addString("exposed_variable", "exposed var name");
    }

}
