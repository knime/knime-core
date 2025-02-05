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
 *   Nov 20, 2024 (Tobias Kampmann): created
 */
package org.knime.testing.util;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import org.apache.xmlbeans.XmlException;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NoDescriptionProxy;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.workflow.WorkflowManager;
import org.xml.sax.SAXException;

/**
 * Some helpers for providing input data to a node in a workflow, for testing purposes.
 *
 * @author Tobias Kampmann
 */
public final class InputTableNode {

    private InputTableNode() {
        // no instances
    }

    /**
     * A simple node that provides a table to its output port. The table is provided by a supplier that is passed to the
     * constructor.
     */
    public static class InputDataNodeModel extends NodeModel {

        Supplier<BufferedDataTable> m_tableSupplier;

        /**
         * Creates a new InputDataNodeModel that will provide the table from the given supplier.
         *
         * @param tableSupplier the supplier that will provide the table.
         */
        public InputDataNodeModel(final Supplier<BufferedDataTable> tableSupplier) {
            super(0, 1);

            m_tableSupplier = tableSupplier;
        }

        @Override
        protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
            var table = m_tableSupplier.get();
            if (table == null) {
                throw new IllegalStateException("Table supplier returned null");
            }
            return new DataTableSpec[]{m_tableSupplier.get().getSpec()};
        }

        @Override
        protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) {
            var table = m_tableSupplier.get();
            if (table == null) {
                throw new IllegalStateException("Table supplier returned null");
            }
            return new BufferedDataTable[]{m_tableSupplier.get()};
        }

        @Override
        protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            // does nothing
        }

        @Override
        protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            // does nothing
        }

        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) {
            // does nothing
        }

        @Override
        protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            // does nothing
        }

        @Override
        protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            // does nothing
        }

        @Override
        protected void reset() {
            // does nothing
        }
    }

    /**
     * A simple factory to create the node with the model {@link InputDataNodeModel}. You might pass this to
     * {@link WorkflowManagerUtil#createAndAddNode(WorkflowManager, NodeFactory)} to add a general input node to a test
     * workflow.
     */
    public static class InputDataNodeFactory extends NodeFactory<InputDataNodeModel> {

        private final Supplier<BufferedDataTable> m_tableSupplier;

        /**
         * Creates a new factory that will create a node with the given table supplier.
         *
         * @param tableSupplier the supplier that will provide the table.
         */
        public InputDataNodeFactory(final Supplier<BufferedDataTable> tableSupplier) {
            m_tableSupplier = tableSupplier;
        }

        @Override
        protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
            return new NoDescriptionProxy(this.getClass()); // Reduce logging spam for tests
        }

        @Override
        public InputDataNodeModel createNodeModel() {
            return new InputDataNodeModel(m_tableSupplier);
        }

        @Override
        protected int getNrNodeViews() {
            return 0;
        }

        @Override
        public NodeView<InputDataNodeModel> createNodeView(final int viewIndex, final InputDataNodeModel nodeModel) {
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
}
