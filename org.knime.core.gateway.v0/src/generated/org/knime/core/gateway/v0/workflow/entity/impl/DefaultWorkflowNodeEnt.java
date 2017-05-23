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
package org.knime.core.gateway.v0.workflow.entity.impl;

import java.util.List;
import java.util.Optional;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowNodeEntBuilder;

import org.knime.core.gateway.entities.EntityBuilderFactory;
import org.knime.core.gateway.entities.EntityBuilderManager;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Default implementation of the WorkflowNodeEnt-interface. E.g. used if no other {@link EntityBuilderFactory}
 * implementation (provided via the respective extension point, see {@link EntityBuilderManager}) is available.
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultWorkflowNodeEnt implements WorkflowNodeEnt {

	private List<NodeOutPortEnt> m_WorkflowIncomingPorts;
	private List<NodeInPortEnt> m_WorkflowOutgoingPorts;
	private boolean m_IsEncrypted;
	private Optional<String> m_ParentNodeID;
	private String m_RootWorkflowID;
	private Optional<JobManagerEnt> m_JobManager;
	private NodeMessageEnt m_NodeMessage;
	private List<NodeInPortEnt> m_InPorts;
	private List<NodeOutPortEnt> m_OutPorts;
	private String m_Name;
	private String m_NodeID;
	private String m_NodeType;
	private BoundsEnt m_Bounds;
	private boolean m_IsDeletable;
	private String m_NodeState;
	private boolean m_HasDialog;
	private NodeAnnotationEnt m_NodeAnnotation;

    /**
     * @param builder
     */
    private DefaultWorkflowNodeEnt(final DefaultWorkflowNodeEntBuilder builder) {
		m_WorkflowIncomingPorts = builder.m_WorkflowIncomingPorts;
		m_WorkflowOutgoingPorts = builder.m_WorkflowOutgoingPorts;
		m_IsEncrypted = builder.m_IsEncrypted;
		m_ParentNodeID = builder.m_ParentNodeID;
		m_RootWorkflowID = builder.m_RootWorkflowID;
		m_JobManager = builder.m_JobManager;
		m_NodeMessage = builder.m_NodeMessage;
		m_InPorts = builder.m_InPorts;
		m_OutPorts = builder.m_OutPorts;
		m_Name = builder.m_Name;
		m_NodeID = builder.m_NodeID;
		m_NodeType = builder.m_NodeType;
		m_Bounds = builder.m_Bounds;
		m_IsDeletable = builder.m_IsDeletable;
		m_NodeState = builder.m_NodeState;
		m_HasDialog = builder.m_HasDialog;
		m_NodeAnnotation = builder.m_NodeAnnotation;
    }

	@Override
    public List<NodeOutPortEnt> getWorkflowIncomingPorts() {
        return m_WorkflowIncomingPorts;
    }
    
	@Override
    public List<NodeInPortEnt> getWorkflowOutgoingPorts() {
        return m_WorkflowOutgoingPorts;
    }
    
	@Override
    public boolean getIsEncrypted() {
        return m_IsEncrypted;
    }
    
	@Override
    public Optional<String> getParentNodeID() {
        return m_ParentNodeID;
    }
    
	@Override
    public String getRootWorkflowID() {
        return m_RootWorkflowID;
    }
    
	@Override
    public Optional<JobManagerEnt> getJobManager() {
        return m_JobManager;
    }
    
	@Override
    public NodeMessageEnt getNodeMessage() {
        return m_NodeMessage;
    }
    
	@Override
    public List<NodeInPortEnt> getInPorts() {
        return m_InPorts;
    }
    
	@Override
    public List<NodeOutPortEnt> getOutPorts() {
        return m_OutPorts;
    }
    
	@Override
    public String getName() {
        return m_Name;
    }
    
	@Override
    public String getNodeID() {
        return m_NodeID;
    }
    
	@Override
    public String getNodeType() {
        return m_NodeType;
    }
    
	@Override
    public BoundsEnt getBounds() {
        return m_Bounds;
    }
    
	@Override
    public boolean getIsDeletable() {
        return m_IsDeletable;
    }
    
	@Override
    public String getNodeState() {
        return m_NodeState;
    }
    
	@Override
    public boolean getHasDialog() {
        return m_HasDialog;
    }
    
	@Override
    public NodeAnnotationEnt getNodeAnnotation() {
        return m_NodeAnnotation;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static DefaultWorkflowNodeEntBuilder builder() {
		return new DefaultWorkflowNodeEntBuilder();
	}
	
	/**
	* Default implementation of the WorkflowNodeEntBuilder-interface.
	*/
	public static class DefaultWorkflowNodeEntBuilder implements WorkflowNodeEntBuilder {
    
		private List<NodeOutPortEnt> m_WorkflowIncomingPorts;
		private List<NodeInPortEnt> m_WorkflowOutgoingPorts;
		private boolean m_IsEncrypted;
		private Optional<String> m_ParentNodeID = Optional.empty();
		private String m_RootWorkflowID;
		private Optional<JobManagerEnt> m_JobManager = Optional.empty();
		private NodeMessageEnt m_NodeMessage;
		private List<NodeInPortEnt> m_InPorts;
		private List<NodeOutPortEnt> m_OutPorts;
		private String m_Name;
		private String m_NodeID;
		private String m_NodeType;
		private BoundsEnt m_Bounds;
		private boolean m_IsDeletable;
		private String m_NodeState;
		private boolean m_HasDialog;
		private NodeAnnotationEnt m_NodeAnnotation;

        public WorkflowNodeEnt build() {
            return new DefaultWorkflowNodeEnt(this);
        }

		@Override
        public WorkflowNodeEntBuilder setWorkflowIncomingPorts(final List<NodeOutPortEnt> WorkflowIncomingPorts) {
			m_WorkflowIncomingPorts = WorkflowIncomingPorts;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setWorkflowOutgoingPorts(final List<NodeInPortEnt> WorkflowOutgoingPorts) {
			m_WorkflowOutgoingPorts = WorkflowOutgoingPorts;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setIsEncrypted(final boolean IsEncrypted) {
			m_IsEncrypted = IsEncrypted;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setParentNodeID(final Optional<String> ParentNodeID) {
			m_ParentNodeID = ParentNodeID;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setRootWorkflowID(final String RootWorkflowID) {
			m_RootWorkflowID = RootWorkflowID;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setJobManager(final Optional<JobManagerEnt> JobManager) {
			m_JobManager = JobManager;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setNodeMessage(final NodeMessageEnt NodeMessage) {
			m_NodeMessage = NodeMessage;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setInPorts(final List<NodeInPortEnt> InPorts) {
			m_InPorts = InPorts;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setOutPorts(final List<NodeOutPortEnt> OutPorts) {
			m_OutPorts = OutPorts;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setName(final String Name) {
			m_Name = Name;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setNodeID(final String NodeID) {
			m_NodeID = NodeID;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setNodeType(final String NodeType) {
			m_NodeType = NodeType;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setBounds(final BoundsEnt Bounds) {
			m_Bounds = Bounds;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setIsDeletable(final boolean IsDeletable) {
			m_IsDeletable = IsDeletable;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setNodeState(final String NodeState) {
			m_NodeState = NodeState;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setHasDialog(final boolean HasDialog) {
			m_HasDialog = HasDialog;			
            return this;
        }
        
		@Override
        public WorkflowNodeEntBuilder setNodeAnnotation(final NodeAnnotationEnt NodeAnnotation) {
			m_NodeAnnotation = NodeAnnotation;			
            return this;
        }
        
    }
}
