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

import org.knime.core.gateway.v0.workflow.entity.NodeFactoryIDEnt;
import org.knime.core.gateway.v0.workflow.entity.EntityID;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.knime.core.gateway.v0.workflow.entity.NativeNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.NativeNodeEntBuilder;

import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;

import java.util.stream.Collectors;
import java.util.HashMap;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class NativeNodeEntToRS {

	private final NativeNodeEnt m_e;
	
	public NativeNodeEntToRS(final NativeNodeEnt e) {
		m_e = e;
	}

	@JsonProperty("NodeFactoryID")
    public NodeFactoryIDEntToRS getNodeFactoryID() {
            return new NodeFactoryIDEntToRS(m_e.getNodeFactoryID());
        }
    
	@JsonProperty("Parent")
    public EntityIDToRS getParent() {
            return new EntityIDToRS(m_e.getParent());
        }
    
	@JsonProperty("JobManager")
    public JobManagerEntToRS getJobManager() {
            return new JobManagerEntToRS(m_e.getJobManager());
        }
    
	@JsonProperty("NodeMessage")
    public NodeMessageEntToRS getNodeMessage() {
            return new NodeMessageEntToRS(m_e.getNodeMessage());
        }
    
	@JsonProperty("InPorts")
    public List<NodeInPortEntToRS> getInPorts() {
        	return m_e.getInPorts().stream().map(l -> new NodeInPortEntToRS(l)).collect(Collectors.toList());
        }
    
	@JsonProperty("OutPorts")
    public List<NodeOutPortEntToRS> getOutPorts() {
        	return m_e.getOutPorts().stream().map(l -> new NodeOutPortEntToRS(l)).collect(Collectors.toList());
        }
    
	@JsonProperty("Name")
    public String getName() {
        	return m_e.getName();
        }
    
	@JsonProperty("NodeID")
    public String getNodeID() {
        	return m_e.getNodeID();
        }
    
	@JsonProperty("NodeTypeID")
    public String getNodeTypeID() {
        	return m_e.getNodeTypeID();
        }
    
	@JsonProperty("NodeType")
    public String getNodeType() {
        	return m_e.getNodeType();
        }
    
	@JsonProperty("Bounds")
    public BoundsEntToRS getBounds() {
            return new BoundsEntToRS(m_e.getBounds());
        }
    
	@JsonProperty("IsDeletable")
    public boolean getIsDeletable() {
        	return m_e.getIsDeletable();
        }
    
	@JsonProperty("NodeState")
    public String getNodeState() {
        	return m_e.getNodeState();
        }
    
	@JsonProperty("HasDialog")
    public boolean getHasDialog() {
        	return m_e.getHasDialog();
        }
    
	@JsonProperty("NodeAnnotation")
    public NodeAnnotationEntToRS getNodeAnnotation() {
            return new NodeAnnotationEntToRS(m_e.getNodeAnnotation());
        }
    

	@Override
	public String toString() {
	    return m_e.toString();
	}

}
