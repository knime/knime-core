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

import org.knime.core.gateway.v0.workflow.entity.MetaPortEnt;
import org.knime.core.gateway.v0.workflow.entity.PortTypeEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.MetaPortEntBuilder;

import org.knime.core.gateway.entities.EntityBuilderFactory;
import org.knime.core.gateway.entities.EntityBuilderManager;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Default implementation of the MetaPortEnt-interface. E.g. used if no other {@link EntityBuilderFactory}
 * implementation (provided via the respective extension point, see {@link EntityBuilderManager}) is available.
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultMetaPortEnt implements MetaPortEnt {

	private PortTypeEnt m_PortType;
	private boolean m_IsConnected;
	private String m_Message;
	private int m_OldIndex;
	private int m_NewIndex;

    /**
     * @param builder
     */
    private DefaultMetaPortEnt(final DefaultMetaPortEntBuilder builder) {
		m_PortType = builder.m_PortType;
		m_IsConnected = builder.m_IsConnected;
		m_Message = builder.m_Message;
		m_OldIndex = builder.m_OldIndex;
		m_NewIndex = builder.m_NewIndex;
    }

	@Override
    public PortTypeEnt getPortType() {
        return m_PortType;
    }
    
	@Override
    public boolean getIsConnected() {
        return m_IsConnected;
    }
    
	@Override
    public String getMessage() {
        return m_Message;
    }
    
	@Override
    public int getOldIndex() {
        return m_OldIndex;
    }
    
	@Override
    public int getNewIndex() {
        return m_NewIndex;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static DefaultMetaPortEntBuilder builder() {
		return new DefaultMetaPortEntBuilder();
	}
	
	/**
	* Default implementation of the MetaPortEntBuilder-interface.
	*/
	public static class DefaultMetaPortEntBuilder implements MetaPortEntBuilder {
    
		private PortTypeEnt m_PortType;
		private boolean m_IsConnected;
		private String m_Message;
		private int m_OldIndex;
		private int m_NewIndex;

        public MetaPortEnt build() {
            return new DefaultMetaPortEnt(this);
        }

		@Override
        public MetaPortEntBuilder setPortType(final PortTypeEnt PortType) {
			m_PortType = PortType;			
            return this;
        }
        
		@Override
        public MetaPortEntBuilder setIsConnected(final boolean IsConnected) {
			m_IsConnected = IsConnected;			
            return this;
        }
        
		@Override
        public MetaPortEntBuilder setMessage(final String Message) {
			m_Message = Message;			
            return this;
        }
        
		@Override
        public MetaPortEntBuilder setOldIndex(final int OldIndex) {
			m_OldIndex = OldIndex;			
            return this;
        }
        
		@Override
        public MetaPortEntBuilder setNewIndex(final int NewIndex) {
			m_NewIndex = NewIndex;			
            return this;
        }
        
    }
}
