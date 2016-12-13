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


import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import org.knime.core.gateway.v0.workflow.entity.PortTypeEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.PortTypeEntBuilder;

import org.knime.core.thrift.workflow.entity.TPortTypeEnt.TPortTypeEntBuilder;
import org.knime.core.thrift.workflow.entity.TPortTypeEntFromThrift.TPortTypeEntBuilderFromThrift;
import org.knime.core.thrift.TEntityBuilderFactory.ThriftEntityBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
@ThriftStruct(builder = TPortTypeEntBuilder.class)
public class TPortTypeEnt {



	private String m_Name;
	private String m_PortObjectClassName;
	private boolean m_IsOptional;
	private int m_Color;
	private boolean m_IsHidden;

    /**
     * @param builder
     */
    private TPortTypeEnt(final TPortTypeEntBuilder builder) {
		m_Name = builder.m_Name;
		m_PortObjectClassName = builder.m_PortObjectClassName;
		m_IsOptional = builder.m_IsOptional;
		m_Color = builder.m_Color;
		m_IsHidden = builder.m_IsHidden;
    }
    
    protected TPortTypeEnt() {
    	//
    }

    @ThriftField(1)
    public String getName() {
        return m_Name;
    }
    
    @ThriftField(2)
    public String getPortObjectClassName() {
        return m_PortObjectClassName;
    }
    
    @ThriftField(3)
    public boolean getIsOptional() {
        return m_IsOptional;
    }
    
    @ThriftField(4)
    public int getColor() {
        return m_Color;
    }
    
    @ThriftField(5)
    public boolean getIsHidden() {
        return m_IsHidden;
    }
    

	public static TPortTypeEntBuilder builder() {
		return new TPortTypeEntBuilder();
	}
	
    public static class TPortTypeEntBuilder implements ThriftEntityBuilder<PortTypeEnt> {
    
		private String m_Name;
		private String m_PortObjectClassName;
		private boolean m_IsOptional;
		private int m_Color;
		private boolean m_IsHidden;

        @ThriftConstructor
        public TPortTypeEnt build() {
            return new TPortTypeEnt(this);
        }
        
        @Override
        public GatewayEntityBuilder<PortTypeEnt> wrap() {
            return new TPortTypeEntBuilderFromThrift(this);
        }

        @ThriftField
        public TPortTypeEntBuilder setName(final String Name) {
			m_Name = Name;			
            return this;
        }
        
        @ThriftField
        public TPortTypeEntBuilder setPortObjectClassName(final String PortObjectClassName) {
			m_PortObjectClassName = PortObjectClassName;			
            return this;
        }
        
        @ThriftField
        public TPortTypeEntBuilder setIsOptional(final boolean IsOptional) {
			m_IsOptional = IsOptional;			
            return this;
        }
        
        @ThriftField
        public TPortTypeEntBuilder setColor(final int Color) {
			m_Color = Color;			
            return this;
        }
        
        @ThriftField
        public TPortTypeEntBuilder setIsHidden(final boolean IsHidden) {
			m_IsHidden = IsHidden;			
            return this;
        }
        
    }

}
