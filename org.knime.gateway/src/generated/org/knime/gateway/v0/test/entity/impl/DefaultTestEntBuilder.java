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
package org.knime.gateway.v0.test.entity.impl;

import java.util.List;
import java.util.Map;
import org.knime.gateway.v0.test.entity.TestEnt;
import org.knime.gateway.v0.test.entity.builder.TestEntBuilder;
import org.knime.gateway.v0.workflow.entity.XYEnt;
import org.knime.gateway.v0.workflow.entity.impl.DefaultXYEntBuilder;

import org.knime.gateway.entities.EntityBuilderFactory;
import org.knime.gateway.entities.EntityBuilderManager;

/**
 * Default implementation of the TestEntBuilder-interface. E.g. used if no other {@link EntityBuilderFactory}
 * implementation (provided via the respective extension point, see {@link EntityBuilderManager}) is available.
 *
 * @author Martin Horn, University of Konstanz
 */
 public class DefaultTestEntBuilder implements TestEntBuilder {
    
	XYEnt m_XY;
	List<XYEnt> m_XYList;
	String m_Other;
	List<String> m_PrimitiveList;
	Map<String, XYEnt> m_XYMap;
	Map<Integer, String> m_PrimitiveMap;

	@Override
    public TestEnt build() {
        return new DefaultTestEnt(this);
    }

	@Override
    public TestEntBuilder setXY(final XYEnt XY) {
		m_XY = XY;			
        return this;
    }
        
	@Override
    public TestEntBuilder setXYList(final List<XYEnt> XYList) {
		m_XYList = XYList;			
        return this;
    }
        
	@Override
    public TestEntBuilder setOther(final String Other) {
		m_Other = Other;			
        return this;
    }
        
	@Override
    public TestEntBuilder setPrimitiveList(final List<String> PrimitiveList) {
		m_PrimitiveList = PrimitiveList;			
        return this;
    }
        
	@Override
    public TestEntBuilder setXYMap(final Map<String, XYEnt> XYMap) {
		m_XYMap = XYMap;			
        return this;
    }
        
	@Override
    public TestEntBuilder setPrimitiveMap(final Map<Integer, String> PrimitiveMap) {
		m_PrimitiveMap = PrimitiveMap;			
        return this;
    }
        
}

