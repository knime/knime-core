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
 *   Oct 25, 2023 (hornm): created
 */
package org.knime.core.node;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.node.v41.KnimeNodeDocument;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeFactoryIdTestNodeSetFactory implements NodeSetFactory {

    @Override
    public Collection<String> getNodeFactoryIds() {
        return List.of("dynamic_1", "dynamic_2", "normal_2");
    }

    @Override
    public Class<? extends NodeFactory<? extends NodeModel>> getNodeFactory(final String id) {
        if (id.equals("dynamic_1")) {
            return NodeFactoryIdTestDynamicNodeFactory1.class;
        } else if (id.equals("dynamic_2")) {
            return NodeFactoryIdTestDynamicNodeFactory2.class;
        } else {
            return NodeFactoryIdTestNodeFactory.class;
        }
    }

    @Override
    public String getCategoryPath(final String id) {
        return null;
    }

    @Override
    public String getAfterID(final String id) {
        return null;
    }

    @Override
    public ConfigRO getAdditionalSettings(final String id) {
        return null;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    public static class NodeFactoryIdTestNodeFactory extends NodeFactory<NodeModel> {

        @Override
        public NodeModel createNodeModel() {
            return new DummyNodeModel();
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

        @Override
        protected NodeDescription createNodeDescription() {
            return createDummyNodeDescription("normal node name");
        }

    }

    public static class NodeFactoryIdTestDynamicNodeFactory1 extends DynamicNodeFactory<NodeModel> {

        public NodeFactoryIdTestDynamicNodeFactory1() {
            super();
        }

        @Override
        public NodeModel createNodeModel() {
            return new DummyNodeModel();
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

        @Override
        protected NodeDescription createNodeDescription() {
            return createDummyNodeDescription("dynamic node name 1");
        }

        @Override
        public void saveAdditionalFactorySettings(final ConfigWO config) {
            config.addString("dynamic node 1 setting", "foo");
        }

        @Override
        public String getFactoryIdUniquifier() {
            return "factory-id-uniquifier-1";
        }
    }

    public static class NodeFactoryIdTestDynamicNodeFactory2 extends DynamicNodeFactory<NodeModel> {

        @Override
        public DummyNodeModel createNodeModel() {
            return new DummyNodeModel();
        }

        @Override
        public NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
            return null;
        }

        @Override
        protected int getNrNodeViews() {
            return 0;
        }

        @Override
        protected boolean hasDialog() {
            return false;
        }

        @Override
        protected NodeDialogPane createNodeDialogPane() {
            return null;
        }

        @Override
        protected NodeDescription createNodeDescription() {
            return createDummyNodeDescription("dynamic node name 2");
        }

        @Override
        public void saveAdditionalFactorySettings(final ConfigWO config) {
            config.addString("dynamic node 2 setting", "bar");
        }
    }

    private static NodeDescription createDummyNodeDescription(final String name) {
        var doc = KnimeNodeDocument.Factory.newInstance();
        var node = doc.addNewKnimeNode();
        node.setName(name);
        node.addNewPorts();
        return new NodeDescription41Proxy(doc);
    }

    static class DummyNodeModel extends NodeModel {

        protected DummyNodeModel() {
            super(0, 0);
        }

        @Override
        protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            //
        }

        @Override
        protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            //
        }

        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) {
            //
        }

        @Override
        protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            //
        }

        @Override
        protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            //
        }

        @Override
        protected void reset() {
            //
        }

    }

}