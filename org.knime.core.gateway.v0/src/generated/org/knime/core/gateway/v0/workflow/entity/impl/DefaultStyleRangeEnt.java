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
package org.knime.core.gateway.v0.workflow.entity.impl;

import org.knime.core.gateway.v0.workflow.entity.StyleRangeEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.StyleRangeEntBuilder;

import org.knime.core.gateway.entities.EntityBuilderFactory;
import org.knime.core.gateway.entities.EntityBuilderManager;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Default implementation of the StyleRangeEnt-interface. E.g. used if no other {@link EntityBuilderFactory}
 * implementation (provided via the respective extension point, see {@link EntityBuilderManager}) is available.
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultStyleRangeEnt implements StyleRangeEnt {

	private int m_Start;
	private int m_Length;
	private String m_FontName;
	private String m_FontStyle;
	private int m_FontSize;
	private int m_ForegroundColor;

    /**
     * @param builder
     */
    DefaultStyleRangeEnt(final DefaultStyleRangeEntBuilder builder) {
		m_Start = builder.m_Start;
		m_Length = builder.m_Length;
		m_FontName = builder.m_FontName;
		m_FontStyle = builder.m_FontStyle;
		m_FontSize = builder.m_FontSize;
		m_ForegroundColor = builder.m_ForegroundColor;
    }

	@Override
    public int getStart() {
        return m_Start;
    }
    
	@Override
    public int getLength() {
        return m_Length;
    }
    
	@Override
    public String getFontName() {
        return m_FontName;
    }
    
	@Override
    public String getFontStyle() {
        return m_FontStyle;
    }
    
	@Override
    public int getFontSize() {
        return m_FontSize;
    }
    
	@Override
    public int getForegroundColor() {
        return m_ForegroundColor;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static DefaultStyleRangeEntBuilder builder() {
		return new DefaultStyleRangeEntBuilder();
	}
}
