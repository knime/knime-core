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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Container;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.ConfigEditJTree;
import org.knime.core.node.config.ConfigEditTreeModel.ConfigEditTreeNode;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.config.base.JSONConfig.WriterConfig;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.dialog.NodeDialog.LegacyFlowVariableNodeDialog;
import org.knime.core.webui.node.view.NodeViewManager;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.dialog.NodeDialogNodeFactory;
import org.knime.testing.node.dialog.NodeDialogNodeModel;
import org.knime.testing.node.dialog.NodeDialogNodeView;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests for {@link NodeDialog}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeDialogTest {

    /**
     * Tests that even for an unconnected node with input ports, where the flow object stack is null, settings are
     * loaded correctly (see UIEXT-394)
     *
     * @throws Exception
     */
    @Test
    public void testInitialSettingsForUnconnectedNode() throws Exception {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(() -> createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                createTextSettingsDataService(), null), 1));
        var nncWrapper = NodeWrapper.of(nnc);

        var modelSettings = new NodeSettings("model");
        var viewSettings = new NodeSettings("view");
        modelSettings.addString("model_key1", "model_setting_value");
        viewSettings.addString("view_key1", "view_setting_value");

        var nodeDialogManager = NodeDialogManager.getInstance();
        nodeDialogManager.callTextApplyDataService(nncWrapper, settingsToString(modelSettings, viewSettings));
        var nodeSettings = new NodeSettings("node_settings");
        wfm.saveNodeSettings(nnc.getID(), nodeSettings);

        // apply node settings that are controlled by a flow variable -> the flow variable must not end up in the settings
        wfm.loadNodeSettings(nnc.getID(), nodeSettings);
        var initialSettings = nodeDialogManager.callTextInitialDataService(nncWrapper);
        assertThat(initialSettings,
            containsString("\"view_key1\":{\"type\":\"string\",\"value\":\"view_setting_value\"}"));
        assertThat(initialSettings,
            containsString("\"model_key1\":{\"type\":\"string\",\"value\":\"model_setting_value\"}"));
    }

    /**
     * Tests that model- and view-settings a being applied correctly and most importantly that the node is being reset
     * in case of changed model settings but not in case of changed view settings.
     *
     * @throws Exception
     */
    @Test
    public void testApplyChangedSettings() throws Exception {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(() -> createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                createTextSettingsDataService(), null)));
        var nncWrapper = NodeWrapper.of(nnc);

        var modelSettings = new NodeSettings("model");
        var viewSettings = new NodeSettings("view");
        modelSettings.addInt("model_key1", 1);
        viewSettings.addInt("view_key1", 1);

        var nodeDialogManager = NodeDialogManager.getInstance();
        nodeDialogManager.callTextApplyDataService(nncWrapper, settingsToString(modelSettings, viewSettings));
        wfm.executeAllAndWaitUntilDone();
        assertThat(nnc.getNodeContainerState().isExecuted(), is(true));
        wfm.save(wfm.getContext().getCurrentLocation(), new ExecutionMonitor(), false);
        assertThat(wfm.isDirty(), is(false));

        // change view settings and apply -> node is not being reset
        viewSettings.addInt("view_key2", 2);
        nodeDialogManager.callTextApplyDataService(nncWrapper, settingsToString(modelSettings, viewSettings));
        assertThat(nnc.getNodeContainerState().isExecuted(), is(true));
        var newSettings = new NodeSettings("node_settings");
        wfm.saveNodeSettings(nnc.getID(), newSettings);
        assertThat(newSettings.getNodeSettings(SettingsType.VIEW.getConfigKey()), is(viewSettings));
        assertThat(nnc.isDirty(), is(true));

        // change model settings and apply -> node is expected to be reset
        modelSettings.addInt("model_key2", 2);
        nodeDialogManager.callTextApplyDataService(nncWrapper, settingsToString(modelSettings, viewSettings));
        assertThat(nnc.getNodeContainerState().isExecuted(), is(false));
        wfm.saveNodeSettings(nnc.getID(), newSettings);
        assertThat(newSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()), is(modelSettings));

        // change view settings and expose as flow variable -> node is expected to reset
        var variablesTree =
            newSettings.addNodeSettings("view_variables").addNodeSettings("tree").addNodeSettings("view_key2");
        variablesTree.addString("used_variable", null);
        variablesTree.addString("exposed_variable", "foo");
        wfm.loadNodeSettings(nnc.getID(), newSettings);
        wfm.executeAllAndWaitUntilDone();
        assertThat(nnc.getNodeContainerState().isExecuted(), is(true));
        viewSettings.addInt("view_key2", 3);
        nodeDialogManager.callTextApplyDataService(nncWrapper, settingsToString(modelSettings, viewSettings));
        assertThat(nnc.getNodeContainerState().isExecuted(), is(false));
        wfm.saveNodeSettings(nnc.getID(), newSettings);
        assertThat(newSettings.getNodeSettings(SettingsType.VIEW.getConfigKey()), is(viewSettings));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    /**
     * Tests that settings that are controlled by flow variables are properly returned in the node dialog's initial data
     * (i.e. with the flow variable value) and properly applied (i.e. ignored when being written into the node
     * settings).
     *
     * @throws IOException
     * @throws InvalidSettingsException
     */
    @Test
    public void testGetAndApplySettingsControlledByFlowVariables() throws IOException, InvalidSettingsException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(() -> createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                createTextSettingsDataService(), null)));
        var nncWrapper = NodeWrapper.of(nnc);

        var modelSettings = new NodeSettings("model");
        var viewSettings = new NodeSettings("view");
        modelSettings.addString("model_key1", "model_setting_value");
        viewSettings.addString("view_key1", "view_setting_value");

        var nodeDialogManager = NodeDialogManager.getInstance();
        nodeDialogManager.callTextApplyDataService(nncWrapper, settingsToString(modelSettings, viewSettings));
        var nodeSettings = new NodeSettings("node_settings");
        wfm.saveNodeSettings(nnc.getID(), nodeSettings);

        // apply node settings that are controlled by a flow variable -> the flow variable must not end up in the settings
        wfm.loadNodeSettings(nnc.getID(), nodeSettings);
        var initialSettings = nodeDialogManager.callTextInitialDataService(nncWrapper);
        assertThat(initialSettings,
            containsString("\"view_key1\":{\"type\":\"string\",\"value\":\"view_setting_value\"}"));
        assertThat(initialSettings,
            containsString("\"model_key1\":{\"type\":\"string\",\"value\":\"model_setting_value\"}"));

        var viewVariables = nodeSettings.addNodeSettings("view_variables");
        viewVariables.addString("version", "V_2019_09_13");
        var viewVariable = viewVariables.addNodeSettings("tree").addNodeSettings("view_key1");
        viewVariable.addString("used_variable", "view_variable");
        viewVariable.addString("exposed_variable", null);
        var modelVariables = nodeSettings.addNodeSettings("variables");
        modelVariables.addString("version", "V_2019_09_13");
        var modelVariable = modelVariables.addNodeSettings("tree").addNodeSettings("model_key1");
        modelVariable.addString("used_variable", "model_variable");
        modelVariable.addString("exposed_variable", null);
        wfm.loadNodeSettings(nnc.getID(), nodeSettings);
        nnc.getFlowObjectStack().push(new FlowVariable("view_variable", "view_variable_value"));
        nnc.getFlowObjectStack().push(new FlowVariable("model_variable", "model_variable_value"));

        // make sure that the flow variable value is part of the initial data
        initialSettings = nodeDialogManager.callTextInitialDataService(nncWrapper);
        assertThat(initialSettings,
            containsString("\"view_key1\":{\"type\":\"string\",\"value\":\"view_variable_value\"}"));
        assertThat(initialSettings,
            containsString("\"model_key1\":{\"type\":\"string\",\"value\":\"model_variable_value\"}"));

        // make sure that any applied settings that are controlled by a flow variable, are ignored
        // (i.e. aren't 'persisted' with the node settings)
        viewSettings.addString("view_key1", "new_value_to_be_ignored_on_apply");
        modelSettings.addString("model_key1", "new_value_to_be_ignored_on_apply");
        nodeDialogManager.callTextApplyDataService(nncWrapper, settingsToString(modelSettings, viewSettings));
        wfm.saveNodeSettings(nnc.getID(), nodeSettings);
        assertThat(nodeSettings.getNodeSettings("view").getString("view_key1"), is("view_setting_value"));
        assertThat(nodeSettings.getNodeSettings("model").getString("model_key1"), is("model_setting_value"));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    /**
     * Test overwriting and exposing settings with flow variables via the {@link TextVariableSettingsService}.
     *
     * @throws IOException
     * @throws InvalidSettingsException
     */
    @Test
    public void testSettingWithFlowVariables() throws IOException, InvalidSettingsException {
        TextVariableSettingsService varService = (textSettings, settings) -> { // NOSONAR: The lambda is easy to understand
            try {
                // First level settings
                settings.get(SettingsType.MODEL).addUsedVariable("model_key1", "model_variable");
                settings.get(SettingsType.MODEL).addExposedVariable("model_key2", "exp_model_variable");
                settings.get(SettingsType.VIEW).addUsedVariable("view_key1", "view_variable");
                settings.get(SettingsType.VIEW).addExposedVariable("view_key2", "exp_view_variable");

                // Nested settings
                settings.get(SettingsType.MODEL) //
                    .getChild("settings_group") //
                    .addUsedVariable("child_key1", "child1_variable");
                settings.get(SettingsType.MODEL) //
                    .getChild("settings_group") //
                    .addExposedVariable("child_key2", "exp_child2_variable");
                settings.get(SettingsType.VIEW) //
                    .getChild("deep_settings_group") //
                    .getChild("inner_settings_group") //
                    .addUsedVariable("child_key3", "child3_variable");
                settings.get(SettingsType.VIEW) //
                    .getChild("deep_settings_group") //
                    .getChild("inner_settings_group") //
                    .addExposedVariable("child_key4", "exp_child4_variable");
            } catch (final InvalidSettingsException ex) {
                throw new IllegalStateException(ex);
            }
        };

        var nodeDialogManager = NodeDialogManager.getInstance();
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(() -> createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                createTextSettingsDataService(), varService, null)));
        var nncWrapper = NodeWrapper.of(nnc);

        var modelSettings = new NodeSettings("model");
        var viewSettings = new NodeSettings("view");
        modelSettings.addString("model_key1", "model_setting_value1");
        modelSettings.addString("model_key2", "model_setting_value2");
        viewSettings.addString("view_key1", "view_setting_value1");
        viewSettings.addString("view_key2", "view_setting_value2");

        // Nested settings
        // 1 level deep
        var settingsGroup = modelSettings.addNodeSettings("settings_group");
        settingsGroup.addString("child_key1", "child_setting_value1");
        settingsGroup.addString("child_key2", "child_setting_value2");

        // 2 levels deep
        var deepsettingsGroup = viewSettings.addNodeSettings("deep_settings_group");
        var innerSettingsGroup = deepsettingsGroup.addNodeSettings("inner_settings_group");
        innerSettingsGroup.addString("child_key3", "child_setting_value3");
        innerSettingsGroup.addString("child_key4", "child_setting_value4");

        // Set the flow variable
        wfm.addWorkflowVariables(true, //
            new FlowVariable("model_variable", "model_variable_value"), //
            new FlowVariable("view_variable", "view_variable_value"), //
            new FlowVariable("child1_variable", "child1_variable_value"), //
            new FlowVariable("child3_variable", "child3_variable_value") //
        );

        // Apply the settings using the TextApplyDataService
        nodeDialogManager.callTextApplyDataService(nncWrapper, settingsToString(modelSettings, viewSettings));

        // Assert that the model settings get overwritten
        var loadedModelSettings = getNodeModelSettings(nnc);
        assertEquals("model_variable_value", loadedModelSettings.getString("model_key1"));
        assertEquals("child1_variable_value",
            loadedModelSettings.getNodeSettings("settings_group").getString("child_key1"));

        // Assert that the view settings get overwritten
        NodeViewManager.getInstance().updateNodeViewSettings(nnc);
        var loadedViewSettings = getNodeViewSettings(nnc);
        assertEquals("view_variable_value", loadedViewSettings.getString("view_key1"));
        assertEquals("child3_variable_value", loadedViewSettings.getNodeSettings("deep_settings_group")
            .getNodeSettings("inner_settings_group").getString("child_key3"));

        // Assert that the variables get exposed
        var outgoingFlowVars = nnc.getOutgoingFlowObjectStack().getAllAvailableFlowVariables();
        assertEquals("model_setting_value2", outgoingFlowVars.get("exp_model_variable").getStringValue());
        assertEquals("view_setting_value2", outgoingFlowVars.get("exp_view_variable").getStringValue());
        assertEquals("child_setting_value2", outgoingFlowVars.get("exp_child2_variable").getStringValue());
        assertEquals("child_setting_value4", outgoingFlowVars.get("exp_child4_variable").getStringValue());
    }

    private static NodeSettingsRO getNodeModelSettings(final NativeNodeContainer nnc) {
        return ((NodeDialogNodeModel)nnc.getNode().getNodeModel()).getLoadNodeSettings();
    }

    private static NodeSettingsRO getNodeViewSettings(final NodeContainer nc) {
        return ((NodeDialogNodeView)NodeViewManager.getInstance().getNodeView(nc)).getLoadNodeSettings();
    }

    /**
     * Test that overwriting or exposing variables for settings that do not exist fails.
     *
     * @throws IOException
     */
    @Test
    public void testFailingSettingWithFlowVariables() throws IOException {
        TextVariableSettingsService varService = (textSettings, settings) -> { // NOSONAR: The lambda is easy to understand
            try {
                settings.get(SettingsType.MODEL).addUsedVariable("key1", "var1");
            } catch (final InvalidSettingsException ex) { // NOSONAR
                throw new Key1Exception();
            }
            VariableSettingsWO childSettings;
            try {
                childSettings = settings.get(SettingsType.MODEL).getChild("child_settings");
            } catch (final InvalidSettingsException ex) { // NOSONAR
                throw new ChildKeyException();
            }
            try {
                childSettings.addExposedVariable("key2", "exp_var2");
            } catch (final InvalidSettingsException ex) { // NOSONAR
                throw new Key2Exception();
            }
        };

        var nodeDialogManager = NodeDialogManager.getInstance();
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(() -> createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                createTextSettingsDataService(), varService, null)));
        var nncWrapper = NodeWrapper.of(nnc);

        var modelSettings = new NodeSettings("model");
        var viewSettings = new NodeSettings("view");

        // Setting with key "key1" not available
        assertThrows(Key1Exception.class, //
            () -> nodeDialogManager.callTextApplyDataService(nncWrapper,
                settingsToString(modelSettings, viewSettings)));

        modelSettings.addString("key1", "val1");
        wfm.addWorkflowVariables(true, new FlowVariable("var1", "var_value1"));

        // Child settings not available
        assertThrows(ChildKeyException.class, //
            () -> nodeDialogManager.callTextApplyDataService(nncWrapper,
                settingsToString(modelSettings, viewSettings)));

        modelSettings.addNodeSettings("child_settings");

        // Settings with key "key2" not available
        assertThrows(Key2Exception.class, //
            () -> nodeDialogManager.callTextApplyDataService(nncWrapper,
                settingsToString(modelSettings, viewSettings)));
    }

    @SuppressWarnings("serial")
    private static final class Key1Exception extends RuntimeException {
    }

    @SuppressWarnings("serial")
    private static final class ChildKeyException extends RuntimeException {
    }

    @SuppressWarnings("serial")
    private static final class Key2Exception extends RuntimeException {
    }

    /**
     * Tests to create the legacy flow variable node dialog ({@link NodeDialog#createLegacyFlowVariableNodeDialog()})
     * and makes sure the default view settings and applied view settings are available in the flow variable tree
     * (jtree).
     *
     * @throws IOException
     * @throws NotConfigurableException
     */
    @Test
    public void testCreateLegacyFlowVariableNodeDialog() throws IOException, NotConfigurableException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(() -> createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                createTextSettingsDataService(), null)));

        openLegacyFlowVariableDialogAndCheckViewSettings(nnc, "a default view setting value");

        var newViewSettings = new NodeSettings("new_view_settings");
        newViewSettings.addString("new view setting", "new view setting value");
        NodeDialogManager.getInstance().callTextApplyDataService(NodeWrapper.of(nnc),
            settingsToString(newViewSettings, newViewSettings));
        openLegacyFlowVariableDialogAndCheckViewSettings(nnc, "new view setting value");
    }

    private static void openLegacyFlowVariableDialogAndCheckViewSettings(final NativeNodeContainer nc,
        final String viewSettingValue) throws NotConfigurableException {
        LegacyFlowVariableNodeDialog legacyNodeDialog = initLegacyFlowVariableDialog(nc);

        var tabbedPane = getChild(legacyNodeDialog.getPanel(), 1);
        var flowVariablesTab = getChild(getChild(getChild(tabbedPane, 0), 0), 0);

        var modelSettingsJTree = (ConfigEditJTree)getChild(getChild(getChild(getChild(flowVariablesTab, 0), 0), 0), 0);
        var modelRootNode = modelSettingsJTree.getModel().getRoot();
        var firstModelConfigNode = (ConfigEditTreeNode)modelRootNode.getChildAt(0);
        assertThat(firstModelConfigNode.getConfigEntry().toStringValue(), is("default model setting value"));

        var viewSettingsJTree = (ConfigEditJTree)getChild(getChild(getChild(getChild(flowVariablesTab, 0), 0), 0), 1);
        var viewRootNode = viewSettingsJTree.getModel().getRoot();
        var firstViewConfigNode = (ConfigEditTreeNode)viewRootNode.getChildAt(0);
        assertThat(firstViewConfigNode.getConfigEntry().toStringValue(), is(viewSettingValue));
    }

    private static LegacyFlowVariableNodeDialog initLegacyFlowVariableDialog(final NativeNodeContainer nc)
        throws NotConfigurableException {
        NodeContext.pushContext(nc);
        LegacyFlowVariableNodeDialog legacyNodeDialog;
        try {
            legacyNodeDialog = (LegacyFlowVariableNodeDialog)NodeDialogManager.getInstance().getNodeDialog(nc)
                .createLegacyFlowVariableNodeDialog();
            var nodeSettings = new NodeSettings("node_settings");
            var modelSettings = nodeSettings.addNodeSettings("model");
            modelSettings.addString("default model setting", "default model setting value");
            nodeSettings.addNodeSettings("internal_node_subsettings");
            legacyNodeDialog.initDialogForTesting(nodeSettings, new PortObjectSpec[]{});
        } finally {
            NodeContext.removeLastContext();
        }
        return legacyNodeDialog;
    }

    /**
     * Makes sure that model settings are properly saved again after the legacy node dialog has been closed. The model
     * settings are essentially just 'taken over' - i.e. not modified from within the legacy node dialog.
     *
     * @throws IOException
     * @throws InvalidSettingsException
     * @throws NotConfigurableException
     */
    @Test
    public void testLegacyFlowVariableDialogModelSettingsOnClose()
        throws IOException, InvalidSettingsException, NotConfigurableException {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(() -> createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                createTextSettingsDataService(), null)));

        LegacyFlowVariableNodeDialog legacyNodeDialog = initLegacyFlowVariableDialog(nc);

        var settings = new NodeSettings("test");
        legacyNodeDialog.finishEditingAndSaveSettingsTo(settings);
        assertThat(settings.getNodeSettings("model").getString("default model setting"),
            is("default model setting value"));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    private static Container getChild(final Container cont, final int index) {
        return (Container)cont.getComponent(index);
    }

    /**
     * Helper to create a {@link TextNodeSettingsService}-instance for testing.
     *
     * @return a new instance
     */
    public static TextNodeSettingsService createTextSettingsDataService() {
        return new TextNodeSettingsService() {

            @Override
            public String fromNodeSettings(final Map<SettingsType, NodeSettingsRO> settings,
                final PortObjectSpec[] specs) {
                return settingsToString(settings.get(SettingsType.MODEL), settings.get(SettingsType.VIEW));
            }

            @Override
            public void toNodeSettings(final String textSettings, final Map<SettingsType, NodeSettingsWO> settings) {
                stringToSettings(textSettings, settings.get(SettingsType.MODEL), settings.get(SettingsType.VIEW));
            }

            @Override
            public void getDefaultNodeSettings(final Map<SettingsType, NodeSettingsWO> settings,
                final PortObjectSpec[] specs) {
                if (settings.containsKey(SettingsType.VIEW)) {
                    settings.get(SettingsType.VIEW).addString("a default view setting", "a default view setting value");
                }
                if (settings.containsKey(SettingsType.MODEL)) {
                    settings.get(SettingsType.MODEL).addString("a default model setting",
                        "a default model setting value");
                }
            }
        };
    }

    private static final String SEP = "###################";

    private static String settingsToString(final NodeSettingsRO modelSettings, final NodeSettingsRO viewSettings) {
        return JSONConfig.toJSONString(modelSettings, WriterConfig.DEFAULT) + SEP
            + JSONConfig.toJSONString(viewSettings, WriterConfig.DEFAULT);
    }

    private static void stringToSettings(final String s, final NodeSettingsWO modelSettings,
        final NodeSettingsWO viewSettings) {
        var splitString = s.split(SEP); // NOSONAR
        try {
            JSONConfig.readJSON(modelSettings, new StringReader(splitString[0]));
            JSONConfig.readJSON(viewSettings, new StringReader(splitString[1]));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Helper to create a {@link NodeDialog}.
     *
     * @param page the page to create the node dialog with
     *
     * @return a new dialog instance
     */
    public static NodeDialog createNodeDialog(final Page page) {
        var settingsMapper = new TextNodeSettingsService() {

            @Override
            public void toNodeSettings(final String textSettings, final Map<SettingsType, NodeSettingsWO> settings) {
                //
            }

            @Override
            public String fromNodeSettings(final Map<SettingsType, NodeSettingsRO> settings,
                final PortObjectSpec[] specs) {
                return "test settings";
            }

            @Override
            public void getDefaultNodeSettings(final Map<SettingsType, NodeSettingsWO> settings,
                final PortObjectSpec[] specs) {
                //
            }

        };
        return createNodeDialog(page, settingsMapper, null);
    }

    /**
     * Helper to create {@link NodeDialog}.
     *
     * @param page
     * @param settingsDataService
     * @param dataService
     * @return a new dialog instance
     */
    public static NodeDialog createNodeDialog(final Page page, final TextNodeSettingsService settingsDataService,
        final DataService dataService) {
        return createNodeDialog(page, settingsDataService, null, dataService);
    }

    /**
     * Helper to create {@link NodeDialog}.
     *
     * @param page
     * @param settingsDataService
     * @param dataService
     * @param variableSettingsService
     * @return a new dialog instance
     */
    public static NodeDialog createNodeDialog(final Page page, final TextNodeSettingsService settingsDataService,
        final TextVariableSettingsService variableSettingsService, final DataService dataService) {
        return new NodeDialog(SettingsType.MODEL, SettingsType.VIEW) {

            @Override
            public Optional<DataService> createDataService() {
                return Optional.ofNullable(dataService);
            }

            @Override
            protected TextNodeSettingsService getNodeSettingsService() {
                return settingsDataService;
            }

            @Override
            protected Optional<TextVariableSettingsService> getVariableSettingsService() {
                return Optional.ofNullable(variableSettingsService);
            }

            @Override
            public Page getPage() {
                return page;
            }

        };
    }
}
