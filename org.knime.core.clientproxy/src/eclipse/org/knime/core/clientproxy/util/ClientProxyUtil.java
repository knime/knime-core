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
 *   Apr 25, 2017 (hornm): created
 */
package org.knime.core.clientproxy.util;

import static org.knime.core.gateway.services.ServiceManager.service;

import java.util.Optional;

import org.knime.core.clientproxy.workflow.ClientProxyConnectionContainer;
import org.knime.core.clientproxy.workflow.ClientProxyNativeNodeContainer;
import org.knime.core.clientproxy.workflow.ClientProxyNodeContainer;
import org.knime.core.clientproxy.workflow.ClientProxyNodeInPort;
import org.knime.core.clientproxy.workflow.ClientProxyNodeOutPort;
import org.knime.core.clientproxy.workflow.ClientProxySubNodeContainer;
import org.knime.core.clientproxy.workflow.ClientProxyWorkflowAnnotation;
import org.knime.core.clientproxy.workflow.ClientProxyWorkflowInPort;
import org.knime.core.clientproxy.workflow.ClientProxyWorkflowManager;
import org.knime.core.clientproxy.workflow.ClientProxyWorkflowOutPort;
import org.knime.core.gateway.services.ServerServiceConfig;
import org.knime.core.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.core.gateway.v0.workflow.entity.NativeNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.WrappedWorkflowNodeEnt;
import org.knime.core.gateway.v0.workflow.service.NodeService;

/**
 * Collection of utility methods helping to create the client-proxy classes (e.g. {@link ClientProxyWorkflowManager}) from
 * the respective entity classes (e.g. {@link WorkflowEnt}).
 *
 * @author Martin Horn, University of Konstanz
 */
public class ClientProxyUtil {

    /**
     *
     */
    private ClientProxyUtil() {
        // utility class
    }

    /**
     * @param rootWorkflowID
     * @param nodeID
     * @param objCache the cache that allows one to re-use class instances
     * @param serviceConfig
     * @return the {@link ClientProxyWorkflowManager} - either the cached one or newly created
     */
    public static ClientProxyWorkflowManager getWorkflowManager(final String rootWorkflowID,
        final Optional<String> nodeID, final ObjectCache objCache, final ServerServiceConfig serviceConfig) {
        return objCache.getOrCreate(rootWorkflowID + "_" + nodeID.orElse(""), we -> {
            NodeEnt node = service(NodeService.class, serviceConfig).getNode(rootWorkflowID, nodeID);
            assert node instanceof WorkflowNodeEnt;
            return new ClientProxyWorkflowManager((WorkflowNodeEnt)node, objCache, serviceConfig);
        });
    }

    /**
     * @param nodeEnt the node entity to be wrapped
     * @param workflowEnt must be provided if the node entity is a metanode (i.e. {@link MetaNodeEnt}) - it represents
     *            the workflow associated with the meta node
     * @param key a unique key representing the node entity to ensure that the very same object instance is returned for
     *            the same key
     * @param objCache the cache that allows one to re-use class instances
     * @param serviceConfig
     * @return the {@link ClientProxyNodeContainer} - either the cached one or newly created
     */
    public static ClientProxyNodeContainer getNodeContainer(final NodeEnt nodeEnt,
        final Optional<WorkflowEnt> workflowEnt, final Object key, final ObjectCache objCache, final ServerServiceConfig serviceConfig) {
        //return exactly the same node container instance for the same node entity
        return objCache.getOrCreate(key, k -> {
            if (nodeEnt instanceof NativeNodeEnt) {
                return new ClientProxyNativeNodeContainer((NativeNodeEnt)nodeEnt, objCache, serviceConfig);
            }
            if (nodeEnt instanceof WorkflowNodeEnt) {
                return new ClientProxyWorkflowManager((WorkflowNodeEnt)nodeEnt, objCache, serviceConfig);
            }
            if(nodeEnt instanceof WrappedWorkflowNodeEnt) {
                return new ClientProxySubNodeContainer((WrappedWorkflowNodeEnt)nodeEnt, objCache, serviceConfig);
            }
            throw new IllegalStateException("Node entity type " + nodeEnt.getClass().getName() + " not supported.");
        }, ClientProxyNodeContainer.class);
    }

    /**
     * @param wa the entity to get the client-proxy wrapper for
     * @param objCache the cache that allows one to re-use class instances
     * @return the {@link ClientProxyWorkflowAnnotation} - either the cached one or newly created
     */
    public static ClientProxyWorkflowAnnotation getWorkflowAnnotation(final WorkflowAnnotationEnt wa,
        final ObjectCache objCache) {
        return objCache.getOrCreate(wa, o -> new ClientProxyWorkflowAnnotation(o), ClientProxyWorkflowAnnotation.class);
    }

    /**
     * @param c the entity to get the client-proxy wrapper for
     * @param objCache the cache that allows one to re-use class instances
     * @return the {@link ClientProxyConnectionContainer} - either the cached one or newly created
     */
    public static ClientProxyConnectionContainer getConnectionContainer(final ConnectionEnt c,
        final ObjectCache objCache) {
        return objCache.getOrCreate(c, o -> new ClientProxyConnectionContainer(c),
            ClientProxyConnectionContainer.class);
    }

    /**
     * @param p the entity to get the client-proxy wrapper for
     * @param objCache the cache that allows one to re-use class instances
     * @return the {@link ClientProxyNodeInPort} - either the cached one or newly created
     */
    public static ClientProxyNodeInPort getNodeInPort(final NodeInPortEnt p, final ObjectCache objCache) {
        //possibly return the same node in port instance for the same index
        return objCache.getOrCreate(p, o -> new ClientProxyNodeInPort(o), ClientProxyNodeInPort.class);
    }

    /**
     * @param p the entity to get the client-proxy wrapper for
     * @param objCache the cache that allows one to re-use class instances
     * @return the {@link ClientProxyNodeOutPort} - either the cached one or newly created
     */
    public static ClientProxyNodeOutPort getNodeOutPort(final NodeOutPortEnt p, final ObjectCache objCache) {
        //possibly return the same node out port instance for the same index
        return objCache.getOrCreate(p, o -> new ClientProxyNodeOutPort(o), ClientProxyNodeOutPort.class);
    }

    /**
     * @param p the entity to get the client-proxy wrapper for
     * @param underlyingPort
     * @param objCache the cache that allows one to re-use class instances
     * @return the {@link ClientProxyWorkflowInPort} - either the cached one or newly created
     */
    public static ClientProxyWorkflowInPort getWorkflowInPort(final NodeInPortEnt p,
        final NodeOutPortEnt underlyingPort, final ObjectCache objCache) {
        //possibly return the same node in port instance for the same index
        return objCache.getOrCreate(p, o -> new ClientProxyWorkflowInPort(o, underlyingPort, objCache),
            ClientProxyWorkflowInPort.class);
    }

    /**
     * @param p the entity to get the client-proxy wrapper for
     * @param objCache the cache that allows one to re-use class instances
     * @return the {@link ClientProxyWorkflowOutPort} - either the cached one or newly created
     */
    public static ClientProxyWorkflowOutPort getWorkflowOutPort(final NodeOutPortEnt p, final ObjectCache objCache) {
        //possibly return the same node out port instance for the same index
        return objCache.getOrCreate(p, o -> new ClientProxyWorkflowOutPort(o), ClientProxyWorkflowOutPort.class);
    }

}
