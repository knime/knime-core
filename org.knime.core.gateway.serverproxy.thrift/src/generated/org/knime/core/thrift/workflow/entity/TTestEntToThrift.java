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
import java.util.Map;

import org.knime.core.gateway.v0.workflow.entity.TestEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.TestEntBuilder;

import org.knime.core.thrift.workflow.entity.TTestEntFromThrift.TTestEntBuilderFromThrift;
import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;

import java.util.stream.Collectors;
import java.util.HashMap;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class TTestEntToThrift extends TTestEnt {

	private final TestEnt m_e;
	
	public TTestEntToThrift(final TestEnt e) {
		m_e = e;
	}

	@Override
    public TXYEnt getxy() {
            return new TXYEntToThrift(m_e.getxy());
        }
    
	@Override
    public List<TXYEnt> getxylist() {
        	return m_e.getxylist().stream().map(l -> new TXYEntToThrift(l)).collect(Collectors.toList());
        }
    
	@Override
    public String getother() {
        	return m_e.getother();
        }
    
	@Override
    public List<String> getprimitivelist() {
        	return m_e.getprimitivelist();
        }
    
	@Override
    public Map<String, TXYEnt> getxymap() {
        	//TODO support non-primitive keys
    	Map<String, TXYEnt> res = new HashMap<>();
        m_e.getxymap().entrySet().stream().forEach(e -> res.put(e.getKey(), new TXYEntToThrift(e.getValue())));
        return res;
        }
    
	@Override
    public Map<Integer, String> getprimitivemap() {
        	return m_e.getprimitivemap();
        }
    

	@Override
	public String toString() {
	    return m_e.toString();
	}

    public static class TTestEntBuilderToThrift extends TTestEntBuilder {
    
    	private TestEntBuilder m_b;
    	
    	public TTestEntBuilderToThrift(final TestEntBuilder b) {
    		m_b = b;
    	}

    
    	@Override
        public TTestEnt build() {
            return new TTestEntToThrift(m_b.build());
        }
        
        @Override
        public GatewayEntityBuilder<TestEnt> wrap() {
            return new TTestEntBuilderFromThrift(this);
        }

		@Override
        public TTestEntBuilderToThrift setxy(final TXYEnt xy) {
					m_b.setxy(new TXYEntFromThrift(xy));
		            return this;
        }
        
		@Override
        public TTestEntBuilderToThrift setxylist(final List<TXYEnt> xylist) {
					m_b.setxylist(xylist.stream().map(e -> new TXYEntFromThrift(e)).collect(Collectors.toList()));
		            return this;
        }
        
		@Override
        public TTestEntBuilderToThrift setother(final String other) {
					m_b.setother(other);
		            return this;
        }
        
		@Override
        public TTestEntBuilderToThrift setprimitivelist(final List<String> primitivelist) {
					m_b.setprimitivelist(primitivelist);
		            return this;
        }
        
		@Override
        public TTestEntBuilderToThrift setxymap(final Map<String, TXYEnt> xymap) {
					//TODO support non-primitive keys
			Map<String, XYEnt> map = new HashMap<>();
		    xymap.entrySet().forEach(e -> map.put(e.getKey(), new TXYEntFromThrift(e.getValue())));
			m_b.setxymap(map);
		            return this;
        }
        
		@Override
        public TTestEntBuilderToThrift setprimitivemap(final Map<Integer, String> primitivemap) {
					m_b.setprimitivemap(primitivemap);
		            return this;
        }
        
    }

}
