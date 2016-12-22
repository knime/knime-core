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

import org.knime.core.gateway.v0.workflow.entity.XYEnt;
import java.util.List;

import org.knime.core.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.ConnectionEntBuilder;

import org.knime.core.thrift.workflow.entity.TConnectionEntFromThrift.TConnectionEntBuilderFromThrift;
import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;

import java.util.stream.Collectors;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class TConnectionEntToThrift extends TConnectionEnt {

	private final ConnectionEnt m_e;
	
	public TConnectionEntToThrift(final ConnectionEnt e) {
		m_e = e;
	}

	@Override
    public String getDest() {
        	return m_e.getDest();
        }
    
	@Override
    public int getDestPort() {
        	return m_e.getDestPort();
        }
    
	@Override
    public String getSource() {
        	return m_e.getSource();
        }
    
	@Override
    public int getSourcePort() {
        	return m_e.getSourcePort();
        }
    
	@Override
    public boolean getIsDeleteable() {
        	return m_e.getIsDeleteable();
        }
    
	@Override
    public List<TXYEnt> getBendPoints() {
        	return m_e.getBendPoints().stream().map(l -> new TXYEntToThrift(l)).collect(Collectors.toList());
        }
    
	@Override
    public String getType() {
        	return m_e.getType();
        }
    

	@Override
	public String toString() {
	    return m_e.toString();
	}

    public static class TConnectionEntBuilderToThrift extends TConnectionEntBuilder {
    
    	private ConnectionEntBuilder m_b;
    	
    	public TConnectionEntBuilderToThrift(final ConnectionEntBuilder b) {
    		m_b = b;
    	}

    
    	@Override
        public TConnectionEnt build() {
            return new TConnectionEntToThrift(m_b.build());
        }
        
        @Override
        public GatewayEntityBuilder<ConnectionEnt> wrap() {
            return new TConnectionEntBuilderFromThrift(this);
        }

		@Override
        public TConnectionEntBuilderToThrift setDest(final String Dest) {
					m_b.setDest(Dest);
		            return this;
        }
        
		@Override
        public TConnectionEntBuilderToThrift setDestPort(final int DestPort) {
					m_b.setDestPort(DestPort);
		            return this;
        }
        
		@Override
        public TConnectionEntBuilderToThrift setSource(final String Source) {
					m_b.setSource(Source);
		            return this;
        }
        
		@Override
        public TConnectionEntBuilderToThrift setSourcePort(final int SourcePort) {
					m_b.setSourcePort(SourcePort);
		            return this;
        }
        
		@Override
        public TConnectionEntBuilderToThrift setIsDeleteable(final boolean IsDeleteable) {
					m_b.setIsDeleteable(IsDeleteable);
		            return this;
        }
        
		@Override
        public TConnectionEntBuilderToThrift setBendPoints(final List<TXYEnt> BendPoints) {
					m_b.setBendPoints(BendPoints.stream().map(e -> new TXYEntFromThrift(e)).collect(Collectors.toList()));
		            return this;
        }
        
		@Override
        public TConnectionEntBuilderToThrift setType(final String Type) {
					m_b.setType(Type);
		            return this;
        }
        
    }

}
