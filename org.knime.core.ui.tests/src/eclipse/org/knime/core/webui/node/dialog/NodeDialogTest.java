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

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.config.base.JSONConfig.WriterConfig;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.data.DataService;
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
     * @throws IOException
     * @throws InvalidSettingsException
     */
    @Test
    public void testApplyChangedSettings() throws IOException, InvalidSettingsException {
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

        // change view settings and apply -> node is not being reset
        viewSettings.addInt("view_key2", 2);
        nodeDialogManager.callTextApplyDataService(nc, settingsToString(modelSettings, viewSettings));
        assertThat(nc.getNodeContainerState().isExecuted(), is(true));
        var newSettings = new NodeSettings("node_settings");
        wfm.saveNodeSettings(nc.getID(), newSettings);
        assertThat(newSettings.getNodeSettings(SettingsType.VIEW.getConfigKey()), is(viewSettings));

        // change model settings and apply -> node is expected to be reset
        modelSettings.addInt("model_key2", 2);
        nodeDialogManager.callTextApplyDataService(nc, settingsToString(modelSettings, viewSettings));
        assertThat(nc.getNodeContainerState().isExecuted(), is(false));
        wfm.saveNodeSettings(nc.getID(), newSettings);
        assertThat(newSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()), is(modelSettings));

        WorkflowManagerUtil.disposeWorkflow(wfm);
    }

    private static TextSettingsDataService createTextSettingsDataService() {
        return new TextSettingsDataService() {

            @Override
            public String getInitialData(final Map<SettingsType, NodeSettingsRO> settings,
                final PortObjectSpec[] specs) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void applyData(final String textSettings, final Map<SettingsType, NodeSettingsWO> settings) {
                stringToSettings(textSettings, settings.get(SettingsType.MODEL), settings.get(SettingsType.VIEW));
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
     * @param page the page to create the node dialg with
     *
     * @return a new dialog instance
     */
    public static NodeDialog createNodeDialog(final Page page) {
        var settingsMapper = new TextSettingsDataService() {

            @Override
            public void applyData(final String textSettings, final Map<SettingsType, NodeSettingsWO> settings) {
                //
            }

            @Override
            public String getInitialData(final Map<SettingsType, NodeSettingsRO> settings,
                final PortObjectSpec[] specs) {
                return "test settings";
            }

        };
        return createNodeDialog(page, settingsMapper, null);
    }

    static NodeDialog createNodeDialog(final Page page, final TextSettingsDataService settingsDataService,
        final DataService dataService) {
        return new NodeDialog(SettingsType.MODEL, SettingsType.VIEW) {

            @Override
            public Optional<DataService> createDataService() {
                return Optional.ofNullable(dataService);
            }

            @Override
            protected TextSettingsDataService getSettingsDataService() {
                return settingsDataService;
            }

            @Override
            public Page getPage() {
                return page;
            }

        };
    }

}
