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
package org.knime.core.jaxrs.workflow.entity;

import java.util.List;
import java.util.Optional;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.NativeNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeFactoryIDEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link NativeNodeEnt} interface that can be deserialized from a json object (json-annotated constructor).
 *
 * @author Martin Horn, University of Konstanz
 */
// AUTO-GENERATED CODE; DO NOT MODIFY
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, 
  include = JsonTypeInfo.As.PROPERTY, 
  property = "EntityType")
@JsonSubTypes({ 
  @Type(value = NativeNodeEntFromJson.class, name = "NativeNodeEnt")
})
public class NativeNodeEntFromJson extends NodeEntFromJson implements NativeNodeEnt {

	private NodeFactoryIDEntFromJson m_NodeFactoryID;
	private String m_Parent;
	private Optional<JobManagerEntFromJson> m_JobManager;
	private NodeMessageEntFromJson m_NodeMessage;
	private List<NodeInPortEntFromJson> m_InPorts;
	private List<NodeOutPortEntFromJson> m_OutPorts;
	private String m_Name;
	private String m_NodeID;
	private String m_NodeType;
	private BoundsEntFromJson m_Bounds;
	private boolean m_IsDeletable;
	private String m_NodeState;
	private boolean m_HasDialog;
	private NodeAnnotationEntFromJson m_NodeAnnotation;

	@JsonCreator
	private NativeNodeEntFromJson(
	@JsonProperty("NodeFactoryID") NodeFactoryIDEntFromJson NodeFactoryID,	@JsonProperty("Parent") String Parent,	@JsonProperty("JobManager") JobManagerEntFromJson JobManager,	@JsonProperty("NodeMessage") NodeMessageEntFromJson NodeMessage,	@JsonProperty("InPorts") List<NodeInPortEntFromJson> InPorts,	@JsonProperty("OutPorts") List<NodeOutPortEntFromJson> OutPorts,	@JsonProperty("Name") String Name,	@JsonProperty("NodeID") String NodeID,	@JsonProperty("NodeType") String NodeType,	@JsonProperty("Bounds") BoundsEntFromJson Bounds,	@JsonProperty("IsDeletable") boolean IsDeletable,	@JsonProperty("NodeState") String NodeState,	@JsonProperty("HasDialog") boolean HasDialog,	@JsonProperty("NodeAnnotation") NodeAnnotationEntFromJson NodeAnnotation	) {
		m_NodeFactoryID = NodeFactoryID;
		m_Parent = Parent;
		m_JobManager = Optional.ofNullable(JobManager);
		m_NodeMessage = NodeMessage;
		m_InPorts = InPorts;
		m_OutPorts = OutPorts;
		m_Name = Name;
		m_NodeID = NodeID;
		m_NodeType = NodeType;
		m_Bounds = Bounds;
		m_IsDeletable = IsDeletable;
		m_NodeState = NodeState;
		m_HasDialog = HasDialog;
		m_NodeAnnotation = NodeAnnotation;
	}
	
	protected NativeNodeEntFromJson() {
		//just a dummy constructor for subclasses
	}


	@Override
    public NodeFactoryIDEnt getNodeFactoryID() {
            return (NodeFactoryIDEnt) m_NodeFactoryID;
            
    }
    
	@Override
    public String getParent() {
        	return m_Parent;
            
    }
    
	@Override
    public Optional<JobManagerEnt> getJobManager() {
        	return m_JobManager.map(o -> (JobManagerEnt) o);
            
    }
    
	@Override
    public NodeMessageEnt getNodeMessage() {
            return (NodeMessageEnt) m_NodeMessage;
            
    }
    
	@Override
    public List<NodeInPortEnt> getInPorts() {
        	return m_InPorts.stream().map(l -> (NodeInPortEnt) l ).collect(Collectors.toList());
            
    }
    
	@Override
    public List<NodeOutPortEnt> getOutPorts() {
        	return m_OutPorts.stream().map(l -> (NodeOutPortEnt) l ).collect(Collectors.toList());
            
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
            return (BoundsEnt) m_Bounds;
            
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
            return (NodeAnnotationEnt) m_NodeAnnotation;
            
    }
    

}
