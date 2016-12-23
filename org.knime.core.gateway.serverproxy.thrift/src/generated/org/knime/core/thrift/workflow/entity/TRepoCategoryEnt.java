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

import java.util.List;
import org.knime.core.gateway.v0.workflow.entity.RepoNodeTemplateEnt;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import org.knime.core.gateway.v0.workflow.entity.RepoCategoryEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.RepoCategoryEntBuilder;

import org.knime.core.thrift.workflow.entity.TRepoCategoryEnt.TRepoCategoryEntBuilder;
import org.knime.core.thrift.workflow.entity.TRepoCategoryEntFromThrift.TRepoCategoryEntBuilderFromThrift;
import org.knime.core.thrift.TEntityBuilderFactory.ThriftEntityBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
@ThriftStruct(builder = TRepoCategoryEntBuilder.class)
public class TRepoCategoryEnt {



	private String m_Name;
	private String m_IconURL;
	private List<TRepoCategoryEnt> m_Categories;
	private List<TRepoNodeTemplateEnt> m_Nodes;

    /**
     * @param builder
     */
    private TRepoCategoryEnt(final TRepoCategoryEntBuilder builder) {
		m_Name = builder.m_Name;
		m_IconURL = builder.m_IconURL;
		m_Categories = builder.m_Categories;
		m_Nodes = builder.m_Nodes;
    }
    
    protected TRepoCategoryEnt() {
    	//
    }

    @ThriftField(1)
    public String getName() {
        return m_Name;
    }
    
    @ThriftField(2)
    public String getIconURL() {
        return m_IconURL;
    }
    
    @ThriftField(3)
    public List<TRepoCategoryEnt> getCategories() {
        return m_Categories;
    }
    
    @ThriftField(4)
    public List<TRepoNodeTemplateEnt> getNodes() {
        return m_Nodes;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static TRepoCategoryEntBuilder builder() {
		return new TRepoCategoryEntBuilder();
	}
	
    public static class TRepoCategoryEntBuilder implements ThriftEntityBuilder<RepoCategoryEnt> {
    
		private String m_Name;
		private String m_IconURL;
		private List<TRepoCategoryEnt> m_Categories;
		private List<TRepoNodeTemplateEnt> m_Nodes;

        @ThriftConstructor
        public TRepoCategoryEnt build() {
            return new TRepoCategoryEnt(this);
        }
        
        @Override
        public GatewayEntityBuilder<RepoCategoryEnt> wrap() {
            return new TRepoCategoryEntBuilderFromThrift(this);
        }

        @ThriftField
        public TRepoCategoryEntBuilder setName(final String Name) {
			m_Name = Name;			
            return this;
        }
        
        @ThriftField
        public TRepoCategoryEntBuilder setIconURL(final String IconURL) {
			m_IconURL = IconURL;			
            return this;
        }
        
        @ThriftField
        public TRepoCategoryEntBuilder setCategories(final List<TRepoCategoryEnt> Categories) {
			m_Categories = Categories;			
            return this;
        }
        
        @ThriftField
        public TRepoCategoryEntBuilder setNodes(final List<TRepoNodeTemplateEnt> Nodes) {
			m_Nodes = Nodes;			
            return this;
        }
        
    }

}
