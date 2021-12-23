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
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.dialog.NodeDialogNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests for {@link NodeDialog}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeDialogTest {

    private WorkflowManager m_wfm;

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
     * Tests {@link NodeDialog#callTextInitialDataService()}, {@link NodeDialog#callTextDataService(String)} and
     * {@link NodeDialog#callTextAppyDataService(String)}
     *
     * @throws IOException
     * @throws InvalidSettingsException
     */
    @Test
    public void testCallDataServices() throws IOException, InvalidSettingsException {
        var page = Page.builderFromString(() -> "test page content", "index.html").build();
        Supplier<NodeDialog> nodeDialogSupplier = () -> createNodeDialog(page, new TextSettingsMapper() { // NOSONAR

            @Override
            public void toSettings(final String s, final Map<SettingsType, NodeSettingsWO> settings) {
                var split = s.split(",");
                settings.get(SettingsType.MODEL).addString(split[0], split[1]);
                // TODO test VIEW settings type, too
            }

            @Override
            public String fromSettings(final Map<SettingsType, NodeSettingsRO> settings, final PortObjectSpec[] specs) {
                return "the node settings";
            }
        }, new TextDataService() {

            @Override
            public String handleRequest(final String request) {
                return "general data service";
            }
        });

        NativeNodeContainer nc = NodeDialogManagerTest.createNodeWithNodeDialog(m_wfm, nodeDialogSupplier);

        var newNodeDialog = NodeDialogManager.getInstance().getNodeDialog(nc);
        assertThat(newNodeDialog.callTextInitialDataService(), is("the node settings"));
        assertThat(newNodeDialog.callTextDataService(""), is("general data service"));
        newNodeDialog.callTextAppyDataService("key,node settings value");
        var loadNodeSettings = ((NodeDialogNodeModel)nc.getNode().getNodeModel()).getLoadNodeSettings();
        assertThat(loadNodeSettings.getString("key"), is("node settings value"));
        String message =
            assertThrows(IOException.class, () -> newNodeDialog.callTextAppyDataService("ERROR,invalid")).getMessage();
        assertThat(message, is("Invalid node settings"));
    }

    /**
     * Helper to create a {@link NodeDialog}.
     *
     * @param page the page to create the node dialg with
     *
     * @return a new dialog instance
     */
    public static NodeDialog createNodeDialog(final Page page) {
        var settingsMapper = new TextSettingsMapper() {

            @Override
            public void toSettings(final String textSettings, final Map<SettingsType, NodeSettingsWO> settings) {
                //
            }

            @Override
            public String fromSettings(final Map<SettingsType, NodeSettingsRO> settings, final PortObjectSpec[] specs) {
                return "test settings";
            }

        };
        return createNodeDialog(page, settingsMapper, null);
    }

    private static NodeDialog createNodeDialog(final Page page, final TextSettingsMapper settingsMapper, final DataService dataService) {
        return new NodeDialog(settingsMapper, SettingsType.MODEL) {

            @Override
            public Optional<DataService> getDataService() {
                return Optional.ofNullable(dataService);
            }

            @Override
            public Page getPage() {
                return page;
            }

        };
    }

}
