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
package org.knime.gateway.v0.workflow.entity.impl;

import java.util.List;
import org.knime.gateway.v0.workflow.entity.AnnotationEnt;
import org.knime.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.gateway.v0.workflow.entity.StyleRangeEnt;
import org.knime.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;

import org.knime.gateway.entities.EntityBuilderFactory;
import org.knime.gateway.entities.EntityBuilderManager;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Default implementation of the NodeAnnotationEnt-interface. E.g. used if no other {@link EntityBuilderFactory}
 * implementation (provided via the respective extension point, see {@link EntityBuilderManager}) is available.
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultNodeAnnotationEnt implements NodeAnnotationEnt {

	private boolean m_IsDefault;
	private String m_Text;
	private int m_BackgroundColor;
	private BoundsEnt m_Bounds;
	private String m_TextAlignment;
	private int m_BorderSize;
	private int m_BorderColor;
	private int m_DefaultFontSize;
	private int m_Version;
	private List<StyleRangeEnt> m_StyleRanges;

    /**
     * @param builder
     */
    DefaultNodeAnnotationEnt(final DefaultNodeAnnotationEntBuilder builder) {
		m_IsDefault = builder.m_IsDefault;
		m_Text = builder.m_Text;
		m_BackgroundColor = builder.m_BackgroundColor;
		m_Bounds = builder.m_Bounds;
		m_TextAlignment = builder.m_TextAlignment;
		m_BorderSize = builder.m_BorderSize;
		m_BorderColor = builder.m_BorderColor;
		m_DefaultFontSize = builder.m_DefaultFontSize;
		m_Version = builder.m_Version;
		m_StyleRanges = builder.m_StyleRanges;
    }

	@Override
    public boolean getIsDefault() {
        return m_IsDefault;
    }
    
	@Override
    public String getText() {
        return m_Text;
    }
    
	@Override
    public int getBackgroundColor() {
        return m_BackgroundColor;
    }
    
	@Override
    public BoundsEnt getBounds() {
        return m_Bounds;
    }
    
	@Override
    public String getTextAlignment() {
        return m_TextAlignment;
    }
    
	@Override
    public int getBorderSize() {
        return m_BorderSize;
    }
    
	@Override
    public int getBorderColor() {
        return m_BorderColor;
    }
    
	@Override
    public int getDefaultFontSize() {
        return m_DefaultFontSize;
    }
    
	@Override
    public int getVersion() {
        return m_Version;
    }
    
	@Override
    public List<StyleRangeEnt> getStyleRanges() {
        return m_StyleRanges;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static DefaultNodeAnnotationEntBuilder builder() {
		return new DefaultNodeAnnotationEntBuilder();
	}
}
