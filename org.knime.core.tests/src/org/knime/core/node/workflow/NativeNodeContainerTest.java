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
 *   Sep 27, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.node.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author Paul Bärnreuther
 */
public class NativeNodeContainerTest {

    private WorkflowManager m_wfm;

    @BeforeEach
    void createEmptyWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    @AfterEach
    void disposeWorkflow() {
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
    }

    @Nested
    class ValidateViewSettingsTest {
        class TestNodeModel extends NodeModel {

            private final ViewSettingsValidator m_viewSettingsValidator;

            protected TestNodeModel(final ViewSettingsValidator viewSettingsValidator) {
                super(0, 0);
                m_viewSettingsValidator = viewSettingsValidator;
            }

            @Override
            protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
                // Do nothing
                return inSpecs;
            }


            @Override
            protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
                // Not used
            }

            @Override
            protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
                // Not used
            }

            @Override
            protected void saveSettingsTo(final NodeSettingsWO settings) {
                // Not used
            }

            @Override
            protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                // Not used
            }

            @Override
            protected void validateViewSettings(final NodeSettingsRO viewSettings) throws InvalidSettingsException {
                m_viewSettingsValidator.validateViewSettings(viewSettings);
            }

            @Override
            protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
                // Not used
            }

            @Override
            protected void reset() {
                // Not used
            }

        }

        class TestNodeFactory extends NodeFactory<NodeModel> {

            private final ViewSettingsValidator m_validator;

            TestNodeFactory(final ViewSettingsValidator validator) {
                m_validator = validator;
            }

            @Override
            public NodeModel createNodeModel() {
                return new TestNodeModel(m_validator);
            }

            @Override
            protected int getNrNodeViews() {
                return 0;
            }

            @Override
            public NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
                return null;
            }

            @Override
            protected boolean hasDialog() {
                return false;
            }

            @Override
            protected NodeDialogPane createNodeDialogPane() {
                return null;
            }

        }

        @Test
        void testValidatesViewSettings() throws InvalidSettingsException {
            final var validator = mock(ViewSettingsValidator.class);
            final var nnc = constructNativeNodeContainer(validator);
            final var nodeSettings = new NodeSettings("root");
            final var settingCfgKey = "setting";
            nodeSettings.addNodeSettings("view").addString(settingCfgKey, "value");
            final var variableTree = nodeSettings.addNodeSettings("view_variables").addNodeSettings("tree");
            addViewVariable(settingCfgKey, variableTree);

            m_wfm.loadNodeViewSettings(nnc.getID(), nodeSettings);

            doNothing().when(validator).validateViewSettings(any(NodeSettingsRO.class));

            nnc.callNodeConfigure(new PortObjectSpec[]{new DataTableSpecCreator().createSpec()}, false);
            verify(validator).validateViewSettings(nnc.getViewSettingsUsingFlowObjectStack().get());
            assertThat(nnc.getNodeMessage().getMessage()).isEmpty();

            final var errorMessage = "myErrorMessage";
            doThrow(new InvalidSettingsException(errorMessage)).when(validator)
                .validateViewSettings(any(NodeSettingsRO.class));

            nnc.callNodeConfigure(new PortObjectSpec[]{new DataTableSpecCreator().createSpec()}, false);
            assertThat(nnc.getNodeMessage().getMessage())
                .isEqualTo(String.format("Errors loading flow variables into node : %s", errorMessage));

        }

        private static void addViewVariable(final String settingsKey, final NodeSettingsWO variableTree) {
            var variableTreeNode1 = variableTree.addNodeSettings(settingsKey);
            variableTreeNode1.addString("used_variable", "knime.workspace");
            variableTreeNode1.addBoolean("used_variable_flawed", false);
            variableTreeNode1.addString("exposed_variable", null);
        }

        private NativeNodeContainer constructNativeNodeContainer(final ViewSettingsValidator validator) {
            return WorkflowManagerUtil.createAndAddNode(m_wfm, new TestNodeFactory(validator));
        }

    }

    @SuppressWarnings("javadoc")
    public static interface ViewSettingsValidator {
        void validateViewSettings(NodeSettingsRO settings) throws InvalidSettingsException;
    }

}
