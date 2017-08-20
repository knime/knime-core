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
package org.knime.core.jsonrpc.workflow.entity;

import java.util.List;
import java.util.Optional;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.WrappedWorkflowNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.WrappedWorkflowNodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.impl.DefaultWrappedWorkflowNodeEntBuilder;


import org.knime.core.jsonrpc.JsonRpcUtil;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * MixIn class for entity builder implementations that adds jackson annotations for the de-/serialization.
 *
 * @author Martin Horn, University of Konstanz
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = JsonRpcUtil.ENTITY_TYPE_KEY,
    defaultImpl = DefaultWrappedWorkflowNodeEntBuilder.class)
@JsonSubTypes({
    @Type(value = DefaultWrappedWorkflowNodeEntBuilder.class, name="WrappedWorkflowNodeEnt")
})
// AUTO-GENERATED CODE; DO NOT MODIFY
public interface WrappedWorkflowNodeEntBuilderMixIn extends WrappedWorkflowNodeEntBuilder {

    @Override
    public WrappedWorkflowNodeEnt build();
    
	@Override
	@JsonProperty("WorkflowIncomingPorts")
    public WrappedWorkflowNodeEntBuilder setWorkflowIncomingPorts(final List<NodeOutPortEnt> WorkflowIncomingPorts);
    
	@Override
	@JsonProperty("WorkflowOutgoingPorts")
    public WrappedWorkflowNodeEntBuilder setWorkflowOutgoingPorts(final List<NodeInPortEnt> WorkflowOutgoingPorts);
    
	@Override
	@JsonProperty("IsEncrypted")
    public WrappedWorkflowNodeEntBuilder setIsEncrypted(final boolean IsEncrypted);
    
	@Override
	@JsonProperty("VirtualInNodeID")
    public WrappedWorkflowNodeEntBuilder setVirtualInNodeID(final String VirtualInNodeID);
    
	@Override
	@JsonProperty("VirtualOutNodeID")
    public WrappedWorkflowNodeEntBuilder setVirtualOutNodeID(final String VirtualOutNodeID);
    
	@Override
	@JsonProperty("ParentNodeID")
    public WrappedWorkflowNodeEntBuilder setParentNodeID(final Optional<String> ParentNodeID);
    
	@Override
	@JsonProperty("RootWorkflowID")
    public WrappedWorkflowNodeEntBuilder setRootWorkflowID(final String RootWorkflowID);
    
	@Override
	@JsonProperty("JobManager")
    public WrappedWorkflowNodeEntBuilder setJobManager(final Optional<JobManagerEnt> JobManager);
    
	@Override
	@JsonProperty("NodeMessage")
    public WrappedWorkflowNodeEntBuilder setNodeMessage(final NodeMessageEnt NodeMessage);
    
	@Override
	@JsonProperty("InPorts")
    public WrappedWorkflowNodeEntBuilder setInPorts(final List<NodeInPortEnt> InPorts);
    
	@Override
	@JsonProperty("OutPorts")
    public WrappedWorkflowNodeEntBuilder setOutPorts(final List<NodeOutPortEnt> OutPorts);
    
	@Override
	@JsonProperty("Name")
    public WrappedWorkflowNodeEntBuilder setName(final String Name);
    
	@Override
	@JsonProperty("NodeID")
    public WrappedWorkflowNodeEntBuilder setNodeID(final String NodeID);
    
	@Override
	@JsonProperty("NodeType")
    public WrappedWorkflowNodeEntBuilder setNodeType(final String NodeType);
    
	@Override
	@JsonProperty("Bounds")
    public WrappedWorkflowNodeEntBuilder setBounds(final BoundsEnt Bounds);
    
	@Override
	@JsonProperty("IsDeletable")
    public WrappedWorkflowNodeEntBuilder setIsDeletable(final boolean IsDeletable);
    
	@Override
	@JsonProperty("NodeState")
    public WrappedWorkflowNodeEntBuilder setNodeState(final String NodeState);
    
	@Override
	@JsonProperty("HasDialog")
    public WrappedWorkflowNodeEntBuilder setHasDialog(final boolean HasDialog);
    
	@Override
	@JsonProperty("NodeAnnotation")
    public WrappedWorkflowNodeEntBuilder setNodeAnnotation(final NodeAnnotationEnt NodeAnnotation);
    

}
