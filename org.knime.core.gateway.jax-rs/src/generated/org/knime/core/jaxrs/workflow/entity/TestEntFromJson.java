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
package org.knime.core.jaxrs.workflow.entity;

import org.knime.core.gateway.v0.workflow.entity.XYEnt;
import java.util.List;
import java.util.Map;

import org.knime.core.gateway.v0.workflow.entity.TestEnt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link TestEnt} interface that can be deserialized from a json object (json-annotated constructor).
 *
 * @author Martin Horn, University of Konstanz
 */
public class TestEntFromJson implements TestEnt{

	private XYEntFromJson m_xy;
	private List<XYEntFromJson> m_xylist;
	private String m_other;
	private List<String> m_primitivelist;
	private Map<String, XYEntFromJson> m_xymap;
	private Map<Integer, String> m_primitivemap;

	@JsonCreator
	private TestEntFromJson(
	@JsonProperty("xy") XYEntFromJson xy,	@JsonProperty("xylist") List<XYEntFromJson> xylist,	@JsonProperty("other") String other,	@JsonProperty("primitivelist") List<String> primitivelist,	@JsonProperty("xymap") Map<String, XYEntFromJson> xymap,	@JsonProperty("primitivemap") Map<Integer, String> primitivemap	) {
		m_xy = xy;
		m_xylist = xylist;
		m_other = other;
		m_primitivelist = primitivelist;
		m_xymap = xymap;
		m_primitivemap = primitivemap;
	}


	@Override
    public XYEnt getxy() {
            return (XYEnt) m_xy;
            
    }
    
	@Override
    public List<XYEnt> getxylist() {
        	return m_xylist.stream().map(l -> (XYEnt) l ).collect(Collectors.toList());
            
    }
    
	@Override
    public String getother() {
        	return m_other;
            
    }
    
	@Override
    public List<String> getprimitivelist() {
        	return m_primitivelist;
            
    }
    
	@Override
    public Map<String, XYEnt> getxymap() {
        	//TODO support non-primitive keys
    	Map<String, XYEnt> res = new HashMap<>();
        m_xymap.entrySet().stream().forEach(e -> res.put(e.getKey(), (XYEnt) e.getValue()));
        return res;
            
    }
    
	@Override
    public Map<Integer, String> getprimitivemap() {
        	return m_primitivemap;
            
    }
    

}
