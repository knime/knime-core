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
 *   Dec 11, 2024 (hornm): created
 */
package org.knime.node;

import java.io.File;
import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.NodeViewFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * quasi final
 */
public abstract class DefaultNodeFactory extends ConfigurableNodeFactory<NodeModel>
    implements NodeDialogFactory, NodeViewFactory<NodeModel> {

    private final DefaultNode m_node;

    /**
     * TODO
     */
    protected DefaultNodeFactory(final DefaultNode node) {
        m_node = node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        // TODO create xml doc??
        return new NodeDescription() {

            @Override
            public String getIconPath() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getInportDescription(final int index) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getInportName(final int index) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getInteractiveViewName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getNodeName() {
                return m_node.m_name;
            }

            @Override
            public String getOutportDescription(final int index) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getOutportName(final int index) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public NodeType getType() {
                // TODO null check?
                return m_node.m_nodeType;
            }

            @Override
            public int getViewCount() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getViewDescription(final int index) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getViewName(final int index) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Element getXMLDescription() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final NodeDialog createNodeDialog() {
        // TODO view settings
        return new DefaultNodeDialog(SettingsType.MODEL, m_node.m_modelSettingsClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final NodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        // TODO configurable ports
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final NodeModel createNodeModel() {
        var inPorts =
            m_node.m_inputPortDescriptions.stream().map(portDesc -> portDesc.getType()).toArray(PortType[]::new);
        var outPorts =
            m_node.m_outputPortDescriptions.stream().map(portDesc -> portDesc.getType()).toArray(PortType[]::new);
        return new NodeModel(inPorts, outPorts) {

            private DefaultNodeSettings m_modelSettings;

            @Override
            protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
                // TODO
                return null;
            }

            @Override
            protected PortObject[] execute(final PortObject[] inObjects,
                final org.knime.core.node.ExecutionContext exec) throws Exception {
                // TODO
                return null;
            }

            @Override
            protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                // TODO Auto-generated method stub

            }

            @Override
            protected void saveSettingsTo(final NodeSettingsWO settings) {
                // TODO view settings
                DefaultNodeSettings.saveSettings(m_node.m_modelSettingsClass, m_modelSettings, settings);

            }

            @Override
            protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
                // TODO Auto-generated method stub

            }

            @Override
            protected void reset() {
                // TODO Auto-generated method stub

            }

            @Override
            protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
                // TODO view settings
                m_modelSettings = DefaultNodeSettings.loadSettings(settings, m_node.m_modelSettingsClass);
            }

            @Override
            protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
                // TODO view data
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final NodeView createNodeView(final NodeModel nodeModel) {
        // TODO
        return null;
    }

}
