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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.node.testing.DefaultNodeTestUtil.createNodeFactoryFromStage;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.node.DefaultModel.BiConsumerWithInvalidSettingsException;
import org.knime.node.DefaultModel.ConfigureInput;
import org.knime.node.DefaultModel.ConfigureOutput;
import org.knime.node.DefaultModel.ExecuteConsumer;
import org.knime.node.DefaultModel.RequireModelSettings;
import org.knime.node.DefaultNode.RequireModel;
import org.knime.node.testing.TestWithWorkflowManager;

class DefaultModelTest extends TestWithWorkflowManager {

    static final BiConsumerWithInvalidSettingsException<ConfigureInput, ConfigureOutput> NOOP_CONFIGURE = (i, o) -> {
    };

    static final ExecuteConsumer NOOP_EXECUTE = (i, o) -> {
    };

    NodeContainer createAndAddNodeWithModel(final Function<RequireModelSettings, DefaultModel> model) {
        final var node = createNodeFactoryFromStage(RequireModel.class, s -> s.model(model));
        return addNode(node);
    }

    static final DefaultNodeFactory createOneRow = createNodeFactoryFromStage(RequirePorts.class, s -> s.ports(p -> p//
        .addOutputTable("Output with one row", "")).model(m -> m//
            .withoutSettings().configure((i, o) -> {
                o.setOutSpec(new DataTableSpecCreator().createSpec());
            }).execute((i, o) -> {
                final var container =
                    i.getExecutionContext().createDataContainer(new DataTableSpecCreator().createSpec());
                container.addRowToTable(new DefaultRow("Initial row", new DataCell[0]));
                container.close();
                final var outTable = container.getTable();
                o.setOutData(outTable);
            })));

    static final DefaultNodeFactory addOneRow = createNodeFactoryFromStage(RequirePorts.class, s -> s.ports(p -> p//
        .addInputTable("Input", "").addOutputTable("Output with one row more", "")).model(m -> m//
            .withoutSettings().configure((i, o) -> {
                o.setOutSpec(i.getInSpecs());
            }).execute((i, o) -> {
                final var inTable = i.<BufferedDataTable> getInData(0);
                final var container = i.getExecutionContext().createDataContainer(inTable.getSpec());
                try (final var inCursor = inTable.cursor()) {
                    while (inCursor.canForward()) {
                        container
                            .addRowToTable(new DefaultRow(inCursor.forward().getRowKey().getString(), new DataCell[0]));
                    }
                }
                container.addRowToTable(new DefaultRow("Added row", new DataCell[0]));
                container.close();
                final var outTable = container.getTable();
                o.setOutData(outTable);
            })));

    @Test
    void testSetExecuteOutput() {

        final var createNC = addNode(createOneRow);
        final var addNC = addNode(addOneRow);

        m_wfm.addConnection(createNC.getID(), 1, addNC.getID(), 1);

        m_wfm.executeAllAndWaitUntilDone();

        final var output = (BufferedDataTable)addNC.getOutPort(1).getPortObject();

        assertThat(output.size()).isEqualTo(2);

    }

    @Test
    void testMultipleInputPorts() {
        final var createNC = addNode(createOneRow);

        final var appendNC = addNode(createNodeFactoryFromStage(RequirePorts.class, s -> s.ports(p -> p//
            .addInputTable("Input", "") //
            .addInputTable("Additional input", "") //
            .addOutputTable("Output with two rows", "") //
            .addOutputTable("An inactive branch", "") //
        ).model(m -> m//
            .withoutSettings().configure((i, o) -> {
                o.setOutSpec(i.getInSpec(0));
                o.setOutSpec(1, InactiveBranchPortObjectSpec.INSTANCE);
            }).execute((i, o) -> {
                final var inTable1 = i.<BufferedDataTable> getInData(0);
                final var inTable2 = i.<BufferedDataTable> getInData(1);
                final var container = i.getExecutionContext().createDataContainer(inTable1.getSpec());
                try (final var inCursor1 = inTable1.cursor()) {
                    while (inCursor1.canForward()) {
                        container.addRowToTable(
                            new DefaultRow(inCursor1.forward().getRowKey().getString(), new DataCell[0]));
                    }
                }
                try (final var inCursor2 = inTable2.cursor()) {

                    while (inCursor2.canForward()) {
                        container.addRowToTable(new DefaultRow(
                            inCursor2.forward().getRowKey().getString() + " (second table)", new DataCell[0]));
                    }
                }
                container.close();
                final var outTable = container.getTable();
                o.setOutData(0, outTable);
                o.setOutData(1, InactiveBranchPortObject.INSTANCE);
            }))));

        m_wfm.addConnection(createNC.getID(), 1, appendNC.getID(), 1);
        m_wfm.addConnection(createNC.getID(), 1, appendNC.getID(), 2);

        m_wfm.executeAllAndWaitUntilDone();

        final var output = (BufferedDataTable)appendNC.getOutPort(1).getPortObject();

        assertThat(output.size()).isEqualTo(2);
    }

    @Test
    void testRearrangingColumns() {
        final var createNC = addNode(createOneRow);

        final var appendStringColumnNC = addNode(createNodeFactoryFromStage(RequirePorts.class, s -> s.ports(p -> p//
            .addInputTable("Input", "").addOutputTable("Rearranged output", "")).model(m -> m//
                .withoutSettings().rearrangeColumns((i, o) -> {

                    final var columnRearranger = i.getColumnRearranger();
                    columnRearranger.append(
                        new SingleCellFactory(new DataColumnSpecCreator("New Column", StringCell.TYPE).createSpec()) {
                        });
                    o.setColumnRearranger(columnRearranger);
                }))));
        m_wfm.addConnection(createNC.getID(), 1, appendStringColumnNC.getID(), 1);

        final var output = (DataTableSpec)appendStringColumnNC.getOutPort(1).getPortObjectSpec();

        assertThat(output.getColumnSpec(0).getName()).isEqualTo("New Column");

    }

    @Test
    void testThrowsIfNoRearrangerIsSet() {
        final var createNC = addNode(createOneRow);
        final var forgotToAddRearrangerNC = addNode(createNodeFactoryFromStage(RequirePorts.class, s -> s.ports(p -> p//
            .addInputTable("Input", "").addOutputTable("Rearranged output", "")).model(m -> m//
                .withoutSettings().rearrangeColumns((i, o) -> {

                    final var columnRearranger = i.getColumnRearranger();
                    columnRearranger.append(
                        new SingleCellFactory(new DataColumnSpecCreator("New Column", StringCell.TYPE).createSpec()) {

                        });
                }))));
        m_wfm.addConnection(createNC.getID(), 1, forgotToAddRearrangerNC.getID(), 1);

        assertTrue(forgotToAddRearrangerNC.getNodeContainerState().isIdle());

    }

    // USER ERRORS AND WARNINGS

    @Test
    void testInvalidSettingsInConfigure() {

        final var expectedException = "This is an exception we expect";

        final var nc = createAndAddNodeWithModel(m -> m //
            .withoutSettings().configure((i, o) -> {
                throw new InvalidSettingsException(expectedException);
            }).execute(NOOP_EXECUTE));

        m_wfm.executeAllAndWaitUntilDone();

        assertTrue(nc.getNodeContainerState().isIdle());
        assertThat(nc.getNodeMessage().getMessage()).isEqualTo(expectedException);
    }

    @Test
    void testWarningInExecute() {
        final var expectedWarning = "This is an expected warning";

        final var nc = createAndAddNodeWithModel(m -> m //
            .withoutSettings().configure(NOOP_CONFIGURE).execute((i, o) -> {
                o.setWarningMessage(expectedWarning);
            }));

        m_wfm.executeAllAndWaitUntilDone();

        assertTrue(nc.getNodeContainerState().isExecuted());
        assertThat(nc.getNodeMessage().getMessage()).isEqualTo(expectedWarning);
    }

    @Test
    void testErrorInExecute() {
        final var executeError = "Error thrown in execute";
        final var nc = createAndAddNodeWithModel(m -> m //
            .withoutSettings().configure(NOOP_CONFIGURE).execute((i, o) -> {
                throw new IllegalArgumentException(executeError);
            }));

        m_wfm.executeAllAndWaitUntilDone();

        assertTrue(nc.getNodeContainerState().isConfigured());
        assertThat(nc.getNodeMessage().getMessage()).contains(executeError);
    }

    // ILLEGAL STATES

    @Test
    void testExecuteWithoutOutput() {
        final var nc = addNode(createNodeFactoryFromStage(RequirePorts.class, s -> s.ports(p -> p//
            .addOutputTable("Output", "")).model(m -> m//
                .withoutSettings().configure((i, o) -> {
                }).execute((i, o) -> {
                    // No output set
                }))));
        m_wfm.executeAllAndWaitUntilDone();

        assertTrue(nc.getNodeContainerState().isConfigured());
        assertThat(nc.getNodeMessage().getMessage()).contains("data at output 0 is null");
    }

    @Test
    void testRearrangerWithWrongPorts() {
        assertThrows(IllegalStateException.class, () -> createAndAddNodeWithModel(m -> m //
            .withoutSettings().rearrangeColumns((i, o) -> {
            })));
    }

    // MODEL SETTINGS

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
    void testModelSettings() {
        final var settingsClass = TestSettings.class;
        final var nc = createAndAddNodeWithModel(m -> m //
            .settingsClass(settingsClass).configure((i, o) -> {
                final var settings = i.getSettings();
                assertThat(settings).isInstanceOf(settingsClass);
                ((TestSettings)settings).m_testString = "modified in configure";
            }).execute((i, o) -> {
                final var settings = i.getSettings();
                assertThat(settings).isInstanceOf(settingsClass);
                assertThat(((TestSettings)settings).m_testString).isEqualTo("modified in configure");
            }));
        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(nc.getNodeContainerState().isExecuted());
    }

    @Test
    void testModelSettingsInRearranger() {
        final var settingsClass = TestSettings.class;
        final var createNC = addNode(createOneRow);
        final var nc = addNode(createNodeFactoryFromStage(RequirePorts.class, s -> s.ports(p -> p//
            .addInputTable("Input", "").addOutputTable("Output", "")).model(m -> m//
                .settingsClass(settingsClass).rearrangeColumns((i, o) -> {
                    final var settings = i.getSettings();
                    assertThat(settings).isInstanceOf(settingsClass);
                    o.setColumnRearranger(i.getColumnRearranger());
                }))));
        m_wfm.addConnection(createNC.getID(), 1, nc.getID(), 1);
        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(nc.getNodeContainerState().isExecuted());

    }

    @Test
    void testLoadSettings() throws InvalidSettingsException {
        final var settingsClass = TestSettings.class;
        String loadedValue = "modified externally";

        final var nc = createAndAddNodeWithModel(m -> m //
            .settingsClass(settingsClass).configure((i, o) -> {
            }).execute((i, o) -> {
                final var settings = i.getSettings();
                assertThat(settings).isInstanceOf(settingsClass);
                assertThat(((TestSettings)settings).m_testString).isEqualTo(loadedValue);

            }));
        var nodeSettings = new NodeSettings("configuration");
        m_wfm.saveNodeSettings(nc.getID(), nodeSettings);

        setSettingsWithValue(nodeSettings, loadedValue);

        m_wfm.loadNodeSettings(nc.getID(), nodeSettings);

        m_wfm.executeAllAndWaitUntilDone();

        assertTrue(nc.getNodeContainerState().isExecuted());
    }

    @Test
    void testValidateSettings() throws InvalidSettingsException {
        final var settingsClass = TestSettings.class;

        final var nc = createAndAddNodeWithModel(m -> m //
            .settingsClass(settingsClass).configure((i, o) -> {
            }).execute((i, o) -> {
            }));
        var nodeSettings = new NodeSettings("configuration");
        m_wfm.saveNodeSettings(nc.getID(), nodeSettings);
        setSettingsWithValue(nodeSettings, TestSettings.INVALID_VALUE);

        assertThrows(InvalidSettingsException.class, () -> {
            m_wfm.loadNodeSettings(nc.getID(), nodeSettings);
        });
    }

    private static void setSettingsWithValue(final NodeSettings nodeSettings, final String value) {
        final var settings = new TestSettings();
        settings.m_testString = value;
        DefaultNodeSettings.saveSettings(TestSettings.class, settings, nodeSettings.addNodeSettings("model"));
    }

}
