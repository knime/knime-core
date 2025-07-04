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
 *   Jul 2, 2025 (Paul Bärnreuther): created
 */
package org.knime.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.node.testing.DefaultNodeTestUtil.createNodeFactoryFromStage;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.DataServiceManager;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.PageResourceManager;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.view.NodeViewManager;
import org.knime.node.DefaultView.DefaultInitialDataService;
import org.knime.node.DefaultView.RequireInitialData;
import org.knime.node.DefaultView.RequireViewSettings;
import org.knime.node.testing.DefaultNodeTestUtil;
import org.knime.node.testing.TestWithWorkflowManager;

class DefaultViewTest extends TestWithWorkflowManager {

    private static final String INITIAL_ROW = "Initial row";

    private static final DataServiceManager<NodeWrapper> DATA_SERVICE_MANAGER =
        NodeViewManager.getInstance().getDataServiceManager();

    private static final PageResourceManager<NodeWrapper> PAGE_RESOURCE_MANAGER =
        NodeViewManager.getInstance().getPageResourceManager();

    static final DefaultNodeFactory createOneRow = createNodeFactoryFromStage(RequirePorts.class, s -> s.ports(p -> p//
        .addOutputTable("Output with one row", "")).model(m -> m//
            .withoutSettings().configure((i, o) -> {
                o.setOutSpec(new DataTableSpecCreator().createSpec());
            }).execute((i, o) -> {
                final var container =
                    i.getExecutionContext().createDataContainer(new DataTableSpecCreator().createSpec());
                container.addRowToTable(new DefaultRow(INITIAL_ROW, new DataCell[0]));
                container.close();
                final var outTable = container.getTable();
                o.setOutData(outTable);
            })));

    static final Function<RequireInitialData, DefaultInitialDataService> SHOW_FIRST_ROW_OF_INTERNAL_TABLE = d -> {
        return d.data((vi) -> {
            try (final var cursor = vi.getInternalTables()[0].cursor()) {
                if (cursor.canForward()) {
                    return cursor.forward().getRowKey().getString();
                } else {
                    return "No data";
                }
            }
        });
    };

    private NativeNodeContainer createAndAddNodeWithSingleInputAndInternalTableContainer(
        final Function<RequireViewSettings, DefaultView> view) {
        return addNode(DefaultNodeTestUtil.createNodeFactoryFromStage(RequirePorts.class, s -> s//
            .ports(p -> p//
                .addInputTable("Input", "") //
            ).model(m -> m//
                .withoutSettings().configure((i, o) -> {
                    // No configure
                }).execute((i, o) -> {
                    o.setInternalPortObjects(i.getInData(0));
                }))
            .addView(view)));
    }

    @Test
    void testViewWithInternalPortObjects() throws InvalidSettingsException {

        final var createNC = addNode(createOneRow);
        final var viewNC = createAndAddNodeWithSingleInputAndInternalTableContainer(//
            v -> v//
                .withoutSettings() //
                .description("A view")//
                .page(p -> p.fromString(() -> "foo").relativePath("bar.html")) //
                .initialData(SHOW_FIRST_ROW_OF_INTERNAL_TABLE));

        m_wfm.addConnection(createNC.getID(), 1, viewNC.getID(), 1);

        m_wfm.executeAllAndWaitUntilDone();

        final var initialData = DATA_SERVICE_MANAGER.callInitialDataService(NodeWrapper.of(viewNC));

        assertThat(initialData).contains(INITIAL_ROW);

    }

    @Test
    void testThrowsWhenAccessingInternalPortObjectsOnRearrangeColumnsModel() {
        final var createNC = addNode(createOneRow);
        final var rearrangeNC = addNode(DefaultNodeTestUtil.createNodeFactoryFromStage(RequirePorts.class, s -> s//
            .ports(p -> p//
                .addInputTable("Input", "").addOutputTable("Output", "") //
            ).model(m -> m//
                .withoutSettings().rearrangeColumns((i, o) -> {
                    o.setColumnRearranger(i.getColumnRearranger());
                }))
            .addView(v -> v//
                .withoutSettings() //
                .description("A view")//
                .page(p -> p.fromString(() -> "foo").relativePath("bar.html")) //
                .initialData(SHOW_FIRST_ROW_OF_INTERNAL_TABLE))));

        m_wfm.addConnection(createNC.getID(), 1, rearrangeNC.getID(), 1);

        m_wfm.executeAllAndWaitUntilDone();

        assertThat(DATA_SERVICE_MANAGER.callInitialDataService(NodeWrapper.of(rearrangeNC)))
            .contains("internalError", "rearrange", "internal port")
            .as("Should throw an error when trying to access internal port objects on a rearrange columns model");

    }

    @Test
    void testDataService() {
        final var createNC = addNode(createOneRow);
        final var viewNC = createAndAddNodeWithSingleInputAndInternalTableContainer(//
            v -> v//
                .withoutSettings() //
                .description("A view")//
                .page(p -> p.fromString(() -> "foo").relativePath("bar.html")) //
                .dataService(p -> p.service(MyDataService::new)));

        m_wfm.addConnection(createNC.getID(), 1, viewNC.getID(), 1);

        m_wfm.executeAllAndWaitUntilDone();

        final var dataResult = DATA_SERVICE_MANAGER.callRpcDataService(NodeWrapper.of(viewNC),
            RpcDataService.jsonRpcRequest("getFirstRowKey"));

        assertThat(dataResult).contains(INITIAL_ROW);

    }

    @Test
    void testPageCreation() {
        final var pagePath = "path.html";
        final var createNC = addNode(createOneRow);
        final var viewNC = createAndAddNodeWithSingleInputAndInternalTableContainer(//
            v -> v//
                .withoutSettings() //
                .description("A view")//
                .page(p -> p.fromString(() -> "pageContent").relativePath(pagePath)) //
        );

        m_wfm.addConnection(createNC.getID(), 1, viewNC.getID(), 1);

        m_wfm.executeAllAndWaitUntilDone();

        final var createdPagePath = PAGE_RESOURCE_MANAGER.getPagePath(NodeWrapper.of(viewNC));

        assertThat(createdPagePath).endsWith(pagePath);

    }

    // VIEW SETTINGS

    static class TestSettings implements DefaultNodeSettings {
        String m_testString = "default";

        static final String INVALID_VALUE = "invalid";

        @Override
        public void validate() throws InvalidSettingsException {
            if (m_testString == INVALID_VALUE) {
                throw new InvalidSettingsException("Test string is invalid");
            }
        }

    }

    @Test
    void testViewSettings() throws InvalidSettingsException {
        final var createNC = addNode(createOneRow);

        final var settingsClass = TestSettings.class;
        final var viewNC = createAndAddNodeWithSingleInputAndInternalTableContainer(//
            v -> v//
                .settingsClass(settingsClass) //
                .description("A view")//
                .page(p -> p.fromString(() -> "foo").relativePath("bar.html")) //
                .initialData(p -> p.data(vi -> {
                    final var settings = vi.getSettings();
                    assertThat(settings).isInstanceOf(settingsClass);
                    return null;
                })));

        m_wfm.addConnection(createNC.getID(), 1, viewNC.getID(), 1);

        m_wfm.executeAllAndWaitUntilDone();

        NodeViewManager.getInstance().updateNodeViewSettings(viewNC);

        DATA_SERVICE_MANAGER.callInitialDataService(NodeWrapper.of(viewNC));
    }

    @Test
    void testLoadViewSettings() throws InvalidSettingsException {
        final var createNC = addNode(createOneRow);

        final var loadedValue = "loadedValue";

        final var settingsClass = TestSettings.class;
        final var viewNC = createAndAddNodeWithSingleInputAndInternalTableContainer(//
            v -> v//
                .settingsClass(settingsClass) //
                .description("A view")//
                .page(p -> p.fromString(() -> "foo").relativePath("bar.html")) //
                .initialData(p -> p.data(vi -> {
                    final var settings = vi.getSettings();
                    assertThat(settings).isInstanceOf(settingsClass);
                    assertThat(((TestSettings)settings).m_testString).isEqualTo(loadedValue);
                    return null;
                })));

        m_wfm.addConnection(createNC.getID(), 1, viewNC.getID(), 1);

        final var settings = viewNC.getNodeSettings();
        setViewSettingsWithValue(settings, loadedValue);
        m_wfm.loadNodeSettings(viewNC.getID(), settings);

        m_wfm.executeAllAndWaitUntilDone();

        NodeViewManager.getInstance().updateNodeViewSettings(viewNC);

        DATA_SERVICE_MANAGER.callInitialDataService(NodeWrapper.of(viewNC));

    }

    private static void setViewSettingsWithValue(final NodeSettings nodeSettings, final String value) {
        final var settings = new TestSettings();
        settings.m_testString = value;
        DefaultNodeSettings.saveSettings(TestSettings.class, settings, nodeSettings.addNodeSettings("view"));
    }

    @Test
    void testValidateSettings() throws InvalidSettingsException {
        final var createNC = addNode(createOneRow);

        final var settingsClass = TestSettings.class;
        final var viewNC = createAndAddNodeWithSingleInputAndInternalTableContainer(//
            v -> v//
                .settingsClass(settingsClass) //
                .description("A view")//
                .page(p -> p.fromString(() -> "foo").relativePath("bar.html")));

        m_wfm.addConnection(createNC.getID(), 1, viewNC.getID(), 1);

        final var nodeSettings = new NodeSettings("viewSettings");
        final var settings = new TestSettings();
        settings.m_testString = TestSettings.INVALID_VALUE;
        DefaultNodeSettings.saveSettings(settingsClass, settings, nodeSettings);

        final var nodeViewManager = NodeViewManager.getInstance();
        assertThrows(InvalidSettingsException.class, () -> {
            nodeViewManager.validateSettings(viewNC, nodeSettings);
        }, "Should throw an exception when trying to update the view settings with invalid settings");

    }

}
