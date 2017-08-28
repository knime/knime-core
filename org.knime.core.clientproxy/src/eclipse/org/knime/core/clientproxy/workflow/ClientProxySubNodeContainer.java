/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   Aug 18, 2017 (hornm): created
 */
package org.knime.core.clientproxy.workflow;

import java.net.URL;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.XmlException;
import org.knime.core.clientproxy.util.ObjectCache;
import org.knime.core.def.node.workflow.ISubNodeContainer;
import org.knime.core.def.node.workflow.IWorkflowManager;
import org.knime.core.gateway.services.ServerServiceConfig;
import org.knime.core.gateway.services.ServiceManager;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.core.gateway.v0.workflow.entity.WrappedWorkflowNodeEnt;
import org.knime.core.node.NodeDescription27Proxy;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class ClientProxySubNodeContainer extends ClientProxySingleNodeContainer implements ISubNodeContainer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ClientProxyNodeContainer.class);

    private WorkflowEnt m_workflowEnt;
    private WrappedWorkflowNodeEnt m_wrappedWorkflowNodeEnt;

    /**
     * @param node
     * @param objCache
     * @param serviceConfig
     */
    public ClientProxySubNodeContainer(final WrappedWorkflowNodeEnt node, final ObjectCache objCache,
        final ServerServiceConfig serviceConfig) {
        super(node, objCache, serviceConfig);
        m_wrappedWorkflowNodeEnt = node;
    }

    private WorkflowEnt getWorkflow() {
        if (m_workflowEnt == null) {
            Optional<String> nodeID = m_wrappedWorkflowNodeEnt.getParentNodeID().isPresent()
                ? Optional.of(m_wrappedWorkflowNodeEnt.getNodeID()) : Optional.empty();
            m_workflowEnt = ServiceManager.workflowService(m_serviceConfig)
                .getWorkflow(m_wrappedWorkflowNodeEnt.getRootWorkflowID(), nodeID);
        }
        return m_workflowEnt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeID getVirtualInNodeID() {
        return NodeID.fromString(m_wrappedWorkflowNodeEnt.getVirtualInNodeID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeID getVirtualOutNodeID() {
        return NodeID.fromString(m_wrappedWorkflowNodeEnt.getVirtualOutNodeID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWorkflowManager getWorkflowManager() {
        return m_objCache.getOrCreate(
            m_wrappedWorkflowNodeEnt.getRootWorkflowID() + "_" + m_wrappedWorkflowNodeEnt.getNodeID(), we -> {
                return new ClientProxyWrappedWorkflowManager(m_wrappedWorkflowNodeEnt, m_objCache, m_serviceConfig);
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getIcon() {
        return SubNodeContainer.class.getResource("virtual/subnode/empty.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLayoutJSONString() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLayoutJSONString(final String layoutJSONString) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getXMLDescription() {
        String noDescriptionAvailableText =
            "Node description for wrapped metanodes within remotely opened workflows not available, yet.";

        try {
            // Document
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation()
                .createDocument("http://knime.org/node2012", "knimeNode", null);
            // knimeNode
            Element knimeNode = doc.getDocumentElement();
            knimeNode.setAttribute("type", "Unknown");
            knimeNode.setAttribute("icon", "subnode.png");
            // name
            Element name = doc.createElement("name");
            knimeNode.appendChild(name);
            name.appendChild(doc.createTextNode(getName()));
            // shortDescription
            Element shortDescription = doc.createElement("shortDescription");
            knimeNode.appendChild(shortDescription);
            shortDescription.appendChild(doc.createTextNode(noDescriptionAvailableText));
            // fullDescription
            Element fullDescription = doc.createElement("fullDescription");
            knimeNode.appendChild(fullDescription);
            // intro
            Element intro = doc.createElement("intro");
            fullDescription.appendChild(intro);
            intro.appendChild(doc.createTextNode(noDescriptionAvailableText));

            // ports
            Element ports = doc.createElement("ports");
            knimeNode.appendChild(ports);
//            // inPort
//            for (int i = 0; i < inPortNames.length; i++) {
//                Element inPort = doc.createElement("inPort");
//                ports.appendChild(inPort);
//                inPort.setAttribute("index", "" + i);
//                inPort.setAttribute("name", inPortNames[i]);
//                String defaultText = NO_DESCRIPTION_SET;
//                if (i == 0) {
//                    defaultText += "\nChange this label by browsing the input node contained in the Wrapped Metanode "
//                            + "and changing its configuration.";
//                }
//                addText(inPort, inPortDescriptions[i], defaultText);
//            }
//            // outPort
//            for (int i = 0; i < outPortNames.length; i++) {
//                Element outPort = doc.createElement("outPort");
//                ports.appendChild(outPort);
//                outPort.setAttribute("index", "" + i);
//                outPort.setAttribute("name", outPortNames[i]);
//                String defaultText = NO_DESCRIPTION_SET;
//                if (i == 0) {
//                    defaultText += "\nChange this label by browsing the output node contained in the Wrapped Metanode "
//                            + "and changing its configuration.";
//                }
//                addText(outPort, outPortDescriptions[i], defaultText);
//            }

            return new NodeDescription27Proxy(doc).getXMLDescription();
        } catch (XmlException | DOMException | ParserConfigurationException ex) {
            LOGGER.warn("Could not generate Wrapped Metanode description (for a remotely opened workflow)", ex);
        }
        return null;
    }

}
