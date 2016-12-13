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

import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;

import org.knime.core.thrift.workflow.entity.TBoundsEnt.TBoundsEntBuilder;
import org.knime.core.thrift.workflow.entity.TBoundsEntFromThrift.TBoundsEntBuilderFromThrift;
import org.knime.core.thrift.TEntityBuilderFactory.ThriftEntityBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
@ThriftStruct(builder = TBoundsEntBuilder.class)
public class TBoundsEnt {



	private int m_X;
	private int m_Y;
	private int m_Width;
	private int m_Height;

    /**
     * @param builder
     */
    private TBoundsEnt(final TBoundsEntBuilder builder) {
		m_X = builder.m_X;
		m_Y = builder.m_Y;
		m_Width = builder.m_Width;
		m_Height = builder.m_Height;
    }
    
    protected TBoundsEnt() {
    	//
    }

    @ThriftField(1)
    public int getX() {
        return m_X;
    }
    
    @ThriftField(2)
    public int getY() {
        return m_Y;
    }
    
    @ThriftField(3)
    public int getWidth() {
        return m_Width;
    }
    
    @ThriftField(4)
    public int getHeight() {
        return m_Height;
    }
    

	public static TBoundsEntBuilder builder() {
		return new TBoundsEntBuilder();
	}
	
    public static class TBoundsEntBuilder implements ThriftEntityBuilder<BoundsEnt> {
    
		private int m_X;
		private int m_Y;
		private int m_Width;
		private int m_Height;

        @ThriftConstructor
        public TBoundsEnt build() {
            return new TBoundsEnt(this);
        }
        
        @Override
        public GatewayEntityBuilder<BoundsEnt> wrap() {
            return new TBoundsEntBuilderFromThrift(this);
        }

        @ThriftField
        public TBoundsEntBuilder setX(final int X) {
			m_X = X;			
            return this;
        }
        
        @ThriftField
        public TBoundsEntBuilder setY(final int Y) {
			m_Y = Y;			
            return this;
        }
        
        @ThriftField
        public TBoundsEntBuilder setWidth(final int Width) {
			m_Width = Width;			
            return this;
        }
        
        @ThriftField
        public TBoundsEntBuilder setHeight(final int Height) {
			m_Height = Height;			
            return this;
        }
        
    }

}
