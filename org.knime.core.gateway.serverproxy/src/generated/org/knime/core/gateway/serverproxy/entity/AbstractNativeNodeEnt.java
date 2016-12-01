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
package org.knime.core.gateway.serverproxy.entity;

import org.knime.core.gateway.v0.workflow.entity.NodeFactoryIDEnt;
import org.knime.core.gateway.v0.workflow.entity.EntityID;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import java.util.List;

import org.knime.core.gateway.v0.workflow.entity.NativeNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.NativeNodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.EntityID;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class AbstractNativeNodeEnt implements NativeNodeEnt {

	private NodeFactoryIDEnt m_NodeFactoryID;
	private EntityID m_Parent;
	private JobManagerEnt m_JobManager;
	private NodeMessageEnt m_NodeMessage;
	private List<NodeInPortEnt> m_InPorts;
	private List<NodeOutPortEnt> m_OutPorts;
	private String m_Name;
	private String m_NodeID;
	private String m_NodeType;
	private BoundsEnt m_Bounds;
	private boolean m_IsDeletable;
	private String m_NodeState;

    /**
     *
     */
    protected AbstractNativeNodeEnt(final AbstractNativeNodeEntBuilder builder) {
		m_NodeFactoryID = builder.m_NodeFactoryID;
		m_Parent = builder.m_Parent;
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
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public NodeFactoryIDEnt getNodeFactoryID() {
        return m_NodeFactoryID;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public EntityID getParent() {
        return m_Parent;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public JobManagerEnt getJobManager() {
        return m_JobManager;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public NodeMessageEnt getNodeMessage() {
        return m_NodeMessage;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public List<NodeInPortEnt> getInPorts() {
        return m_InPorts;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public List<NodeOutPortEnt> getOutPorts() {
        return m_OutPorts;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public String getName() {
        return m_Name;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public String getNodeID() {
        return m_NodeID;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public String getNodeType() {
        return m_NodeType;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public BoundsEnt getBounds() {
        return m_Bounds;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public boolean getIsDeletable() {
        return m_IsDeletable;
    }
    
	/**
    * {@inheritDoc}
    */
    @Override
    public String getNodeState() {
        return m_NodeState;
    }
    



    public static abstract class AbstractNativeNodeEntBuilder implements NativeNodeEntBuilder {

		private NodeFactoryIDEnt m_NodeFactoryID;
		private EntityID m_Parent;
		private JobManagerEnt m_JobManager;
		private NodeMessageEnt m_NodeMessage;
		private List<NodeInPortEnt> m_InPorts;
		private List<NodeOutPortEnt> m_OutPorts;
		private String m_Name;
		private String m_NodeID;
		private String m_NodeType;
		private BoundsEnt m_Bounds;
		private boolean m_IsDeletable;
		private String m_NodeState;

        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setNodeFactoryID(final NodeFactoryIDEnt NodeFactoryID) {
        	m_NodeFactoryID = NodeFactoryID;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setParent(final EntityID Parent) {
        	m_Parent = Parent;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setJobManager(final JobManagerEnt JobManager) {
        	m_JobManager = JobManager;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setNodeMessage(final NodeMessageEnt NodeMessage) {
        	m_NodeMessage = NodeMessage;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setInPorts(final List<NodeInPortEnt> InPorts) {
        	m_InPorts = InPorts;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setOutPorts(final List<NodeOutPortEnt> OutPorts) {
        	m_OutPorts = OutPorts;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setName(final String Name) {
        	m_Name = Name;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setNodeID(final String NodeID) {
        	m_NodeID = NodeID;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setNodeType(final String NodeType) {
        	m_NodeType = NodeType;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setBounds(final BoundsEnt Bounds) {
        	m_Bounds = Bounds;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setIsDeletable(final boolean IsDeletable) {
        	m_IsDeletable = IsDeletable;
            return this;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public NativeNodeEntBuilder setNodeState(final String NodeState) {
        	m_NodeState = NodeState;
            return this;
        }
        



    }

}
