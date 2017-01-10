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

import org.knime.core.thrift.workflow.entity.TTestEnt.TTestEntBuilder;

import java.util.stream.Collectors;
import java.util.HashMap;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class TTestEntFromThrift implements TestEnt {

	private final TTestEnt m_e;

	public TTestEntFromThrift(final TTestEnt e) {
		m_e = e;
	}

    @Override
    public XYEnt getxy() {
    	        return new TXYEntFromThrift(m_e.getxy());
            }
    
    @Override
    public List<XYEnt> getxylist() {
    	    	return m_e.getxylist().stream().map(l -> new TXYEntFromThrift(l)).collect(Collectors.toList());
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
    public Map<String, XYEnt> getxymap() {
    	    	//TODO support non-primitive keys
    	Map<String, XYEnt> res = new HashMap<>();
        m_e.getxymap().entrySet().forEach(e -> res.put(e.getKey(), new TXYEntFromThrift(e.getValue())));
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

    public static class TTestEntBuilderFromThrift implements TestEntBuilder {
    
		private TTestEntBuilder m_b;
	
		public TTestEntBuilderFromThrift(final TTestEntBuilder b) {
			m_b = b;
		}
	
        public TestEnt build() {
            return new TTestEntFromThrift(m_b.build());
        }

		@Override
        public TTestEntBuilderFromThrift setxy(final XYEnt xy) {
                	m_b.setxy(new TXYEntToThrift(xy));
                    return this;
        }
        
		@Override
        public TTestEntBuilderFromThrift setxylist(final List<XYEnt> xylist) {
                	m_b.setxylist(xylist.stream().map(e -> new TXYEntToThrift(e)).collect(Collectors.toList()));
                    return this;
        }
        
		@Override
        public TTestEntBuilderFromThrift setother(final String other) {
                	m_b.setother(other);
                    return this;
        }
        
		@Override
        public TTestEntBuilderFromThrift setprimitivelist(final List<String> primitivelist) {
                	m_b.setprimitivelist(primitivelist);
                    return this;
        }
        
		@Override
        public TTestEntBuilderFromThrift setxymap(final Map<String, XYEnt> xymap) {
                	//TODO support non-primitive keys
        	Map<String, TXYEnt> map = new HashMap<>();
		    xymap.entrySet().forEach(e -> map.put(e.getKey(), new TXYEntToThrift(e.getValue())));
            m_b.setxymap(map);
                    return this;
        }
        
		@Override
        public TTestEntBuilderFromThrift setprimitivemap(final Map<Integer, String> primitivemap) {
                	m_b.setprimitivemap(primitivemap);
                    return this;
        }
        
    }

}
