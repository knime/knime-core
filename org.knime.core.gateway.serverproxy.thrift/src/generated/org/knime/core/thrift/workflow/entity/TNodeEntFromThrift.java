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
package org.knime.core.thrift.workflow.entity;

import org.knime.core.gateway.v0.workflow.entity.EntityID;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import java.util.List;

import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeEntBuilder;

import org.knime.core.thrift.workflow.entity.TNodeEnt.TNodeEntBuilder;

import java.util.stream.Collectors;
import java.util.HashMap;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class TNodeEntFromThrift implements NodeEnt {

	private final TNodeEnt m_e;

	public TNodeEntFromThrift(final TNodeEnt e) {
		m_e = e;
	}

    @Override
    public EntityID getParent() {
    	        return new TEntityIDFromThrift(m_e.getParent());
            }
    
    @Override
    public JobManagerEnt getJobManager() {
    	        return new TJobManagerEntFromThrift(m_e.getJobManager());
            }
    
    @Override
    public NodeMessageEnt getNodeMessage() {
    	        return new TNodeMessageEntFromThrift(m_e.getNodeMessage());
            }
    
    @Override
    public List<NodeInPortEnt> getInPorts() {
    	    	return m_e.getInPorts().stream().map(l -> new TNodeInPortEntFromThrift(l)).collect(Collectors.toList());
    	    }
    
    @Override
    public List<NodeOutPortEnt> getOutPorts() {
    	    	return m_e.getOutPorts().stream().map(l -> new TNodeOutPortEntFromThrift(l)).collect(Collectors.toList());
    	    }
    
    @Override
    public String getName() {
    	    	return m_e.getName();
    	    }
    
    @Override
    public String getNodeID() {
    	    	return m_e.getNodeID();
    	    }
    
    @Override
    public String getNodeType() {
    	    	return m_e.getNodeType();
    	    }
    
    @Override
    public BoundsEnt getBounds() {
    	        return new TBoundsEntFromThrift(m_e.getBounds());
            }
    
    @Override
    public boolean getIsDeletable() {
    	    	return m_e.getIsDeletable();
    	    }
    
    @Override
    public String getNodeState() {
    	    	return m_e.getNodeState();
    	    }
    
    @Override
    public boolean getHasDialog() {
    	    	return m_e.getHasDialog();
    	    }
    
    @Override
    public NodeAnnotationEnt getNodeAnnotation() {
    	        return new TNodeAnnotationEntFromThrift(m_e.getNodeAnnotation());
            }
    

	@Override
    public String toString() {
        return m_e.toString();
    }

    public static class TNodeEntBuilderFromThrift implements NodeEntBuilder {
    
		private TNodeEntBuilder m_b;
	
		public TNodeEntBuilderFromThrift(final TNodeEntBuilder b) {
			m_b = b;
		}
	
        public NodeEnt build() {
            return new TNodeEntFromThrift(m_b.build());
        }

		@Override
        public TNodeEntBuilderFromThrift setParent(final EntityID Parent) {
                	m_b.setParent(new TEntityIDToThrift(Parent));
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setJobManager(final JobManagerEnt JobManager) {
                	m_b.setJobManager(new TJobManagerEntToThrift(JobManager));
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setNodeMessage(final NodeMessageEnt NodeMessage) {
                	m_b.setNodeMessage(new TNodeMessageEntToThrift(NodeMessage));
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setInPorts(final List<NodeInPortEnt> InPorts) {
                	m_b.setInPorts(InPorts.stream().map(e -> new TNodeInPortEntToThrift(e)).collect(Collectors.toList()));
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setOutPorts(final List<NodeOutPortEnt> OutPorts) {
                	m_b.setOutPorts(OutPorts.stream().map(e -> new TNodeOutPortEntToThrift(e)).collect(Collectors.toList()));
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setName(final String Name) {
                	m_b.setName(Name);
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setNodeID(final String NodeID) {
                	m_b.setNodeID(NodeID);
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setNodeType(final String NodeType) {
                	m_b.setNodeType(NodeType);
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setBounds(final BoundsEnt Bounds) {
                	m_b.setBounds(new TBoundsEntToThrift(Bounds));
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setIsDeletable(final boolean IsDeletable) {
                	m_b.setIsDeletable(IsDeletable);
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setNodeState(final String NodeState) {
                	m_b.setNodeState(NodeState);
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setHasDialog(final boolean HasDialog) {
                	m_b.setHasDialog(HasDialog);
                    return this;
        }
        
		@Override
        public TNodeEntBuilderFromThrift setNodeAnnotation(final NodeAnnotationEnt NodeAnnotation) {
                	m_b.setNodeAnnotation(new TNodeAnnotationEntToThrift(NodeAnnotation));
                    return this;
        }
        
    }

}
