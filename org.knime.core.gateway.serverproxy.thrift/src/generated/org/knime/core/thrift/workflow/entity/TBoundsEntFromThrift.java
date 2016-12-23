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


import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;

import org.knime.core.thrift.workflow.entity.TBoundsEnt.TBoundsEntBuilder;

import java.util.stream.Collectors;
import java.util.HashMap;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class TBoundsEntFromThrift implements BoundsEnt {

	private final TBoundsEnt m_e;

	public TBoundsEntFromThrift(final TBoundsEnt e) {
		m_e = e;
	}

    @Override
    public int getX() {
    	    	return m_e.getX();
    	    }
    
    @Override
    public int getY() {
    	    	return m_e.getY();
    	    }
    
    @Override
    public int getWidth() {
    	    	return m_e.getWidth();
    	    }
    
    @Override
    public int getHeight() {
    	    	return m_e.getHeight();
    	    }
    

	@Override
    public String toString() {
        return m_e.toString();
    }

    public static class TBoundsEntBuilderFromThrift implements BoundsEntBuilder {
    
		private TBoundsEntBuilder m_b;
	
		public TBoundsEntBuilderFromThrift(final TBoundsEntBuilder b) {
			m_b = b;
		}
	
        public BoundsEnt build() {
            return new TBoundsEntFromThrift(m_b.build());
        }

		@Override
        public TBoundsEntBuilderFromThrift setX(final int X) {
                	m_b.setX(X);
                    return this;
        }
        
		@Override
        public TBoundsEntBuilderFromThrift setY(final int Y) {
                	m_b.setY(Y);
                    return this;
        }
        
		@Override
        public TBoundsEntBuilderFromThrift setWidth(final int Width) {
                	m_b.setWidth(Width);
                    return this;
        }
        
		@Override
        public TBoundsEntBuilderFromThrift setHeight(final int Height) {
                	m_b.setHeight(Height);
                    return this;
        }
        
    }

}
