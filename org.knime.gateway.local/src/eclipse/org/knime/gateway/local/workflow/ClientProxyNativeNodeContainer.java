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
package org.knime.gateway.local.workflow;

import java.net.URL;

import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.gateway.local.util.ObjectCache;
import org.knime.gateway.services.ServerServiceConfig;
import org.knime.gateway.v0.workflow.entity.NativeNodeEnt;
import org.knime.gateway.v0.workflow.entity.NodeFactoryIDEnt;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.NodeTemplate;
import org.w3c.dom.Element;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class ClientProxyNativeNodeContainer extends ClientProxySingleNodeContainer {

    private NativeNodeEnt m_nativeNode;

    /**
     * @param node
     * @param objCache
     * @param serviceConfig
     */
    public ClientProxyNativeNodeContainer(final NativeNodeEnt node, final ObjectCache objCache,
        final ServerServiceConfig serviceConfig) {
        super(node, objCache, serviceConfig);
        m_nativeNode = node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getXMLDescription() {
        //get xml description from underlying node model
        NodeFactoryIDEnt nodeFactoryID = m_nativeNode.getNodeFactoryID();
        try {
            String id = nodeFactoryID.getClassName() + nodeFactoryID.getNodeName().map(n -> "#" + n).orElse("");
            if (RepositoryManager.INSTANCE.getNodeTemplate(id) == null) {
                //some nodes cannot be found via the repository manager, e.g. WrappedNode Input/Output
                //try instantiating the factory here from the class name
                NodeFactory nodeFactory = ((Class<NodeFactory>)Class.forName(nodeFactoryID.getClassName())).newInstance();
                if(nodeFactory instanceof DynamicNodeFactory) {
                    //in case of a dynamic node factory needs to be initialized in order to read the node description
                    nodeFactory.init();
                }
                return nodeFactory.getXMLDescription();
            } else {
                return RepositoryManager.INSTANCE.getNodeTemplate(id).createFactoryInstance().getXMLDescription();
            }
        } catch (Exception ex) {
            // TODO better exception handling
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getIcon() {
        //get the icon url via the node factory
        NodeFactoryIDEnt nodeFactoryID = m_nativeNode.getNodeFactoryID();
        try {
            String id = nodeFactoryID.getClassName() + nodeFactoryID.getNodeName().map(n -> "#" + n).orElse("");
            NodeTemplate nodeTemplate = RepositoryManager.INSTANCE.getNodeTemplate(id);
            if(nodeTemplate == null) {
                //can happen, e.g., in case of virtual nodes, such as WrappedNode Input/Output
                //TODO possibly use another placholder icon
                return SubNodeContainer.class.getResource("virtual/subnode/empty.png");
            } else {
                return nodeTemplate.createFactoryInstance().getIcon();
            }
        } catch (Exception ex) {
            // TODO better exception handling
            throw new RuntimeException(ex);
        }
    }

}
