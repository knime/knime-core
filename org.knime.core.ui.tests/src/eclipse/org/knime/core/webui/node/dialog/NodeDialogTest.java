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
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.node.dialog.NodeDialog.LegacyFlowVariableNodeDialog;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.dialog.NodeDialogNodeFactory;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests for {@link NodeDialog}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeDialogTest {

    /**
     * Tests that model- and view-settings a being applied correctly and most importantly that the node is being reset
     * in case of changed model settings but not in case of changed view settings.
     *
     * @throws Exception
     */
    @Test
    public void testApplyChangedSettings() throws Exception {
        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(() -> createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                createTextSettingsDataService(), null)));

        var modelSettings = new NodeSettings("model");
        var viewSettings = new NodeSettings("view");
        modelSettings.addInt("model_key1", 1);
        viewSettings.addInt("view_key1", 1);

        var nodeDialogManager = NodeDialogManager.getInstance();
        nodeDialogManager.callTextApplyDataService(nc, settingsToString(modelSettings, viewSettings));
        wfm.executeAllAndWaitUntilDone();
        assertThat(nc.getNodeContainerState().isExecuted(), is(true));
        wfm.save(wfm.getContext().getCurrentLocation(), new ExecutionMonitor(), false);
        assertThat(wfm.isDirty(), is(false));

        // change view settings and apply -> node is not being reset
        viewSettings.addInt("view_key2", 2);
        nodeDialogManager.callTextApplyDataService(nc, settingsToString(modelSettings, viewSettings));
        assertThat(nc.getNodeContainerState().isExecuted(), is(true));
        var newSettings = new NodeSettings("node_settings");
        wfm.saveNodeSettings(nc.getID(), newSettings);
        assertThat(newSettings.getNodeSettings(SettingsType.VIEW.getConfigKey()), is(viewSettings));
        assertThat(nc.isDirty(), is(true));

        // change model settings and apply -> node is expected to be reset
        modelSettings.addInt("model_key2", 2);
        nodeDialogManager.callTextApplyDataService(nc, settingsToString(modelSettings, viewSettings));
        assertThat(nc.getNodeContainerState().isExecuted(), is(false));
        wfm.saveNodeSettings(nc.getID(), newSettings);
        assertThat(newSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()), is(modelSettings));

        WorkflowManagerUtil.disposeWorkflow(wfm);
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
        var nc = WorkflowManagerUtil.createAndAddNode(wfm,
            new NodeDialogNodeFactory(() -> createNodeDialog(Page.builder(() -> "test", "test.html").build(),
                createTextSettingsDataService(), null)));

        openLegacyFlowVariableDialogAndCheckViewSettings(nc, "a default view setting value");

        var newViewSettings = new NodeSettings("new_view_settings");
        newViewSettings.addString("new view setting", "new view setting value");
        NodeDialogManager.getInstance().callTextApplyDataService(nc,
            settingsToString(newViewSettings, newViewSettings));
        openLegacyFlowVariableDialogAndCheckViewSettings(nc, "new view setting value");
    }

    private static void openLegacyFlowVariableDialogAndCheckViewSettings(final NativeNodeContainer nc,
        final String viewSettingValue) throws NotConfigurableException {
        LegacyFlowVariableNodeDialog legacyNodeDialog = initLegacyFlowVariableDialog(nc);

        var tabbedPane = getChild(legacyNodeDialog.getPanel(), 1);
        var flowVariablesTab = getChild(getChild(getChild(tabbedPane, 0), 0), 0);

        var modelSettingsJTree =
            (ConfigEditJTree)getChild(getChild(getChild(getChild(flowVariablesTab, 0), 0), 0), 0);
        var modelRootNode = modelSettingsJTree.getModel().getRoot();
        var firstModelConfigNode = (ConfigEditTreeNode)modelRootNode.getChildAt(0);
        assertThat(firstModelConfigNode.getConfigEntry().toStringValue(), is("default model setting value"));

        var viewSettingsJTree =
                (ConfigEditJTree)getChild(getChild(getChild(getChild(flowVariablesTab, 0), 0), 0), 1);
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

    private static TextNodeSettingsService createTextSettingsDataService() {
        return new TextNodeSettingsService() {

            @Override
            public String fromNodeSettings(final Map<SettingsType, NodeSettingsRO> settings,
                final PortObjectSpec[] specs) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void toNodeSettings(final String textSettings, final Map<SettingsType, NodeSettingsWO> settings) {
                stringToSettings(textSettings, settings.get(SettingsType.MODEL), settings.get(SettingsType.VIEW));
            }

            @Override
            public void getDefaultNodeSettings(final Map<SettingsType, NodeSettingsWO> settings,
                final PortObjectSpec[] specs) {
                settings.get(SettingsType.VIEW).addString("a default view setting", "a default view setting value");
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

    static NodeDialog createNodeDialog(final Page page, final TextNodeSettingsService settingsDataService,
        final DataService dataService) {
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
            public Page getPage() {
                return page;
            }

        };
    }

}
