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
package org.knime.gateway.v0.workflow.entity.impl;

import java.util.List;
import java.util.Map;
import org.knime.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.gateway.v0.workflow.entity.MetaPortInfoEnt;
import org.knime.gateway.v0.workflow.entity.NodeEnt;
import org.knime.gateway.v0.workflow.entity.WorkflowAnnotationEnt;
import org.knime.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.gateway.v0.workflow.entity.WorkflowUIInfoEnt;
import org.knime.gateway.v0.workflow.entity.builder.WorkflowEntBuilder;

import org.knime.gateway.entities.EntityBuilderFactory;
import org.knime.gateway.entities.EntityBuilderManager;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Default implementation of the WorkflowEnt-interface. E.g. used if no other {@link EntityBuilderFactory}
 * implementation (provided via the respective extension point, see {@link EntityBuilderManager}) is available.
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultWorkflowEnt implements WorkflowEnt {

	private Map<String, NodeEnt> m_Nodes;
	private List<ConnectionEnt> m_Connections;
	private List<MetaPortInfoEnt> m_MetaInPortInfos;
	private List<MetaPortInfoEnt> m_MetaOutPortInfos;
	private List<WorkflowAnnotationEnt> m_WorkflowAnnotations;
	private WorkflowUIInfoEnt m_WorkflowUIInfo;

    /**
     * @param builder
     */
    DefaultWorkflowEnt(final DefaultWorkflowEntBuilder builder) {
		m_Nodes = builder.m_Nodes;
		m_Connections = builder.m_Connections;
		m_MetaInPortInfos = builder.m_MetaInPortInfos;
		m_MetaOutPortInfos = builder.m_MetaOutPortInfos;
		m_WorkflowAnnotations = builder.m_WorkflowAnnotations;
		m_WorkflowUIInfo = builder.m_WorkflowUIInfo;
    }

	@Override
    public Map<String, NodeEnt> getNodes() {
        return m_Nodes;
    }
    
	@Override
    public List<ConnectionEnt> getConnections() {
        return m_Connections;
    }
    
	@Override
    public List<MetaPortInfoEnt> getMetaInPortInfos() {
        return m_MetaInPortInfos;
    }
    
	@Override
    public List<MetaPortInfoEnt> getMetaOutPortInfos() {
        return m_MetaOutPortInfos;
    }
    
	@Override
    public List<WorkflowAnnotationEnt> getWorkflowAnnotations() {
        return m_WorkflowAnnotations;
    }
    
	@Override
    public WorkflowUIInfoEnt getWorkflowUIInfo() {
        return m_WorkflowUIInfo;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static DefaultWorkflowEntBuilder builder() {
		return new DefaultWorkflowEntBuilder();
	}
}
