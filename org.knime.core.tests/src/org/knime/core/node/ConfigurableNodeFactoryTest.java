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
 *   10 Oct 2022 (carlwitt): created
 */
package org.knime.core.node;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.virtual.AbstractPortObjectRepositoryNodeModel;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class ConfigurableNodeFactoryTest {

    private static class TestNodeModel extends AbstractPortObjectRepositoryNodeModel {
        TestNodeModel(final NodeCreationConfiguration creationConfig) {
            super(creationConfig.getPortConfig().get().getInputPorts(),
                creationConfig.getPortConfig().get().getOutputPorts());
        }

        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) {
        }

        @Override
        protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        }

        @Override
        protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        }
    }

    private static class TestNodeFactory extends ConfigurableNodeFactory<TestNodeModel> {
        @Override
        public TestNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
            return new TestNodeModel(creationConfig);
        }

        @Override
        protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
            final var b = new PortsConfigurationBuilder();
            Predicate<PortType> allPorts = p -> true;
            b.addOptionalInputPortGroup("Optional", PortObject.TYPE);
            b.addExtendableInputPortGroup("Inputs", allPorts);
            b.addExtendableOutputPortGroup("Outputs", allPorts);
            // non-interactive means this cannot be controlled by the user via the user interface. Instead, the node dialog
            // updates the input and output ports according to the selected callee workflow.
            b.addNonInteractiveExtendableInputPortGroup("InputsNonModifiable", allPorts);
            b.addNonInteractiveExtendableOutputPortGroup("OutputsNonModifiable", allPorts);
            return Optional.of(b);
        }

        @Override
        public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
            return null;
        }

        @Override
        public int getNrNodeViews() {
            return 0;
        }

        @Override
        public NodeView<TestNodeModel> createNodeView(final int viewIndex, final TestNodeModel nodeModel) {
            return null;
        }

        @Override
        public boolean hasDialog() {
            return false;
        }
    }

    /**
     * Test that non-interactive port groups added via a builder show up as non-interactive after ports configuration
     * creation via the factory.
     *
     * Test that non-interactive survives a copy (which is frequently done) of the ports configuration.
     */
    @Test
    void testNonInteractiveExtendablePortGroup() {
        var factory = new TestNodeFactory();
        // inputs
        final ModifiablePortsConfiguration portsConfig = factory.createNodeCreationConfig().getPortConfig().get();
        assertTrue("", portsConfig.isInteractive("Optional"));
        assertTrue("", portsConfig.isInteractive("Inputs"));
        assertFalse("", portsConfig.isInteractive("InputsNonModifiable"));

        // outputs
        assertTrue("", portsConfig.isInteractive("Outputs"));
        assertFalse("", portsConfig.isInteractive("OutputsNonModifiable"));

        // survives copy
        final var copiedPortsConfig = portsConfig.copy();
        assertTrue("", copiedPortsConfig.isInteractive("Optional"));
        assertTrue("", copiedPortsConfig.isInteractive("Inputs"));
        assertFalse("", copiedPortsConfig.isInteractive("InputsNonModifiable"));

        // outputs
        assertTrue("", copiedPortsConfig.isInteractive("Outputs"));
        assertFalse("", copiedPortsConfig.isInteractive("OutputsNonModifiable"));
    }
}
