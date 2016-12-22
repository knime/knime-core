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


import org.apache.commons.lang3.builder.ToStringBuilder;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeMessageEntBuilder;

import org.knime.core.thrift.workflow.entity.TNodeMessageEnt.TNodeMessageEntBuilder;
import org.knime.core.thrift.workflow.entity.TNodeMessageEntFromThrift.TNodeMessageEntBuilderFromThrift;
import org.knime.core.thrift.TEntityBuilderFactory.ThriftEntityBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
@ThriftStruct(builder = TNodeMessageEntBuilder.class)
public class TNodeMessageEnt {



	private String m_Message;
	private String m_Type;

    /**
     * @param builder
     */
    private TNodeMessageEnt(final TNodeMessageEntBuilder builder) {
		m_Message = builder.m_Message;
		m_Type = builder.m_Type;
    }
    
    protected TNodeMessageEnt() {
    	//
    }

    @ThriftField(1)
    public String getMessage() {
        return m_Message;
    }
    
    @ThriftField(2)
    public String getType() {
        return m_Type;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static TNodeMessageEntBuilder builder() {
		return new TNodeMessageEntBuilder();
	}
	
    public static class TNodeMessageEntBuilder implements ThriftEntityBuilder<NodeMessageEnt> {
    
		private String m_Message;
		private String m_Type;

        @ThriftConstructor
        public TNodeMessageEnt build() {
            return new TNodeMessageEnt(this);
        }
        
        @Override
        public GatewayEntityBuilder<NodeMessageEnt> wrap() {
            return new TNodeMessageEntBuilderFromThrift(this);
        }

        @ThriftField
        public TNodeMessageEntBuilder setMessage(final String Message) {
			m_Message = Message;			
            return this;
        }
        
        @ThriftField
        public TNodeMessageEntBuilder setType(final String Type) {
			m_Type = Type;			
            return this;
        }
        
    }

}
