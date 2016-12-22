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

import org.knime.core.gateway.v0.workflow.entity.PortTypeEnt;

import org.knime.core.gateway.v0.workflow.entity.MetaPortEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.MetaPortEntBuilder;

import org.knime.core.thrift.workflow.entity.TMetaPortEnt.TMetaPortEntBuilder;

import java.util.stream.Collectors;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class TMetaPortEntFromThrift implements MetaPortEnt {

	private final TMetaPortEnt m_e;

	public TMetaPortEntFromThrift(final TMetaPortEnt e) {
		m_e = e;
	}

    @Override
    public PortTypeEnt getPortType() {
    	        return new TPortTypeEntFromThrift(m_e.getPortType());
            }
    
    @Override
    public boolean getIsConnected() {
    	    	return m_e.getIsConnected();
    	    }
    
    @Override
    public String getMessage() {
    	    	return m_e.getMessage();
    	    }
    
    @Override
    public int getOldIndex() {
    	    	return m_e.getOldIndex();
    	    }
    
    @Override
    public int getNewIndex() {
    	    	return m_e.getNewIndex();
    	    }
    

	@Override
    public String toString() {
        return m_e.toString();
    }

    public static class TMetaPortEntBuilderFromThrift implements MetaPortEntBuilder {
    
		private TMetaPortEntBuilder m_b;
	
		public TMetaPortEntBuilderFromThrift(final TMetaPortEntBuilder b) {
			m_b = b;
		}
	
        public MetaPortEnt build() {
            return new TMetaPortEntFromThrift(m_b.build());
        }

		@Override
        public TMetaPortEntBuilderFromThrift setPortType(final PortTypeEnt PortType) {
                	m_b.setPortType(new TPortTypeEntToThrift(PortType));
                    return this;
        }
        
		@Override
        public TMetaPortEntBuilderFromThrift setIsConnected(final boolean IsConnected) {
                	m_b.setIsConnected(IsConnected);
                    return this;
        }
        
		@Override
        public TMetaPortEntBuilderFromThrift setMessage(final String Message) {
                	m_b.setMessage(Message);
                    return this;
        }
        
		@Override
        public TMetaPortEntBuilderFromThrift setOldIndex(final int OldIndex) {
                	m_b.setOldIndex(OldIndex);
                    return this;
        }
        
		@Override
        public TMetaPortEntBuilderFromThrift setNewIndex(final int NewIndex) {
                	m_b.setNewIndex(NewIndex);
                    return this;
        }
        
    }

}
