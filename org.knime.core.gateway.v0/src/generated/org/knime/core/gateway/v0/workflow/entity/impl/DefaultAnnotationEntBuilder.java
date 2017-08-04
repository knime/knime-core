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

import java.util.List;
import org.knime.core.gateway.v0.workflow.entity.AnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.StyleRangeEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.AnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowAnnotationEntBuilder;

import org.knime.core.gateway.entities.EntityBuilderFactory;
import org.knime.core.gateway.entities.EntityBuilderManager;

/**
 * Default implementation of the AnnotationEntBuilder-interface. E.g. used if no other {@link EntityBuilderFactory}
 * implementation (provided via the respective extension point, see {@link EntityBuilderManager}) is available.
 *
 * @author Martin Horn, University of Konstanz
 */
 public class DefaultAnnotationEntBuilder implements AnnotationEntBuilder {
    
	String m_Text;
	int m_BackgroundColor;
	BoundsEnt m_Bounds;
	String m_TextAlignment;
	int m_BorderSize;
	int m_BorderColor;
	int m_DefaultFontSize;
	int m_Version;
	List<StyleRangeEnt> m_StyleRanges;

	@Override
    public AnnotationEnt build() {
        return new DefaultAnnotationEnt(this);
    }

	@Override
    public AnnotationEntBuilder setText(final String Text) {
		m_Text = Text;			
        return this;
    }
        
	@Override
    public AnnotationEntBuilder setBackgroundColor(final int BackgroundColor) {
		m_BackgroundColor = BackgroundColor;			
        return this;
    }
        
	@Override
    public AnnotationEntBuilder setBounds(final BoundsEnt Bounds) {
		m_Bounds = Bounds;			
        return this;
    }
        
	@Override
    public AnnotationEntBuilder setTextAlignment(final String TextAlignment) {
		m_TextAlignment = TextAlignment;			
        return this;
    }
        
	@Override
    public AnnotationEntBuilder setBorderSize(final int BorderSize) {
		m_BorderSize = BorderSize;			
        return this;
    }
        
	@Override
    public AnnotationEntBuilder setBorderColor(final int BorderColor) {
		m_BorderColor = BorderColor;			
        return this;
    }
        
	@Override
    public AnnotationEntBuilder setDefaultFontSize(final int DefaultFontSize) {
		m_DefaultFontSize = DefaultFontSize;			
        return this;
    }
        
	@Override
    public AnnotationEntBuilder setVersion(final int Version) {
		m_Version = Version;			
        return this;
    }
        
	@Override
    public AnnotationEntBuilder setStyleRanges(final List<StyleRangeEnt> StyleRanges) {
		m_StyleRanges = StyleRanges;			
        return this;
    }
        
}

