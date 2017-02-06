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
 */
package org.knime.core.gateway.v0.workflow.entity.builder;

import java.util.List;
import java.util.Map;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.core.gateway.v0.workflow.entity.EntityID;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.MetaPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEnt;

/**
 * Builder for {@link WorkflowEnt}.
 * @author Martin Horn, University of Konstanz
 */
// AUTO-GENERATED CODE; DO NOT MODIFY
public interface WorkflowEntBuilder extends GatewayEntityBuilder<WorkflowEnt> {

    /**
     * @param nodes The node map.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setNodes(Map<String, NodeEnt> nodes);

    /**
     * @param connections The list of connections.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setConnections(List<ConnectionEnt> connections);

    /**
     * @param metaInPorts The inputs of a metanode (if this workflow is one).
     * @return <code>this</code>
     */
	WorkflowEntBuilder setMetaInPorts(List<MetaPortEnt> metaInPorts);

    /**
     * @param metaOutPorts The outputs of a metanode (if this workflow is one).
     * @return <code>this</code>
     */
	WorkflowEntBuilder setMetaOutPorts(List<MetaPortEnt> metaOutPorts);

    /**
     * @param parent The parent of the node.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setParent(EntityID parent);

    /**
     * @param jobManager The job manager (e.g. cluster or streaming).
     * @return <code>this</code>
     */
	WorkflowEntBuilder setJobManager(JobManagerEnt jobManager);

    /**
     * @param nodeMessage The current node message (warning, error, none).
     * @return <code>this</code>
     */
	WorkflowEntBuilder setNodeMessage(NodeMessageEnt nodeMessage);

    /**
     * @param inPorts The list of inputs.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setInPorts(List<NodeInPortEnt> inPorts);

    /**
     * @param outPorts The list of outputs.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setOutPorts(List<NodeOutPortEnt> outPorts);

    /**
     * @param name The name.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setName(String name);

    /**
     * @param nodeID The ID of the node.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setNodeID(String nodeID);

    /**
     * @param nodeTypeID The ID of the node type (metanode, native nodes, etc).
     * @return <code>this</code>
     */
	WorkflowEntBuilder setNodeTypeID(String nodeTypeID);

    /**
     * @param nodeType The type of the node as string.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setNodeType(String nodeType);

    /**
     * @param bounds The bounds / rectangle on screen of the node.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setBounds(BoundsEnt bounds);

    /**
     * @param isDeletable Whether node is deletable.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setIsDeletable(boolean isDeletable);

    /**
     * @param nodeState The state of the node.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setNodeState(String nodeState);

    /**
     * @param hasDialog Whether the node has a configuration dialog / user settings.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setHasDialog(boolean hasDialog);

    /**
     * @param nodeAnnotation The annotation underneath the node.
     * @return <code>this</code>
     */
	WorkflowEntBuilder setNodeAnnotation(NodeAnnotationEnt nodeAnnotation);

}
