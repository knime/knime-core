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

import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.JobManagerEntBuilder;

import org.knime.core.thrift.workflow.entity.TJobManagerEnt.TJobManagerEntBuilder;
import org.knime.core.thrift.workflow.entity.TJobManagerEntFromThrift.TJobManagerEntBuilderFromThrift;
import org.knime.core.thrift.TEntityBuilderFactory.ThriftEntityBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
@ThriftStruct(builder = TJobManagerEntBuilder.class)
public class TJobManagerEnt {



	private String m_Name;
	private String m_JobManagerID;

    /**
     * @param builder
     */
    private TJobManagerEnt(final TJobManagerEntBuilder builder) {
		m_Name = builder.m_Name;
		m_JobManagerID = builder.m_JobManagerID;
    }
    
    protected TJobManagerEnt() {
    	//
    }

    @ThriftField(1)
    public String getName() {
        return m_Name;
    }
    
    @ThriftField(2)
    public String getJobManagerID() {
        return m_JobManagerID;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static TJobManagerEntBuilder builder() {
		return new TJobManagerEntBuilder();
	}
	
    public static class TJobManagerEntBuilder implements ThriftEntityBuilder<JobManagerEnt> {
    
		private String m_Name;
		private String m_JobManagerID;

        @ThriftConstructor
        public TJobManagerEnt build() {
            return new TJobManagerEnt(this);
        }
        
        @Override
        public GatewayEntityBuilder<JobManagerEnt> wrap() {
            return new TJobManagerEntBuilderFromThrift(this);
        }

        @ThriftField
        public TJobManagerEntBuilder setName(final String Name) {
			m_Name = Name;			
            return this;
        }
        
        @ThriftField
        public TJobManagerEntBuilder setJobManagerID(final String JobManagerID) {
			m_JobManagerID = JobManagerID;			
            return this;
        }
        
    }

}
