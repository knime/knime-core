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

import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowAnnotationEntBuilder;

import org.knime.core.gateway.entities.EntityBuilderFactory;
import org.knime.core.gateway.entities.EntityBuilderManager;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Default implementation of the WorkflowAnnotationEnt-interface. E.g. used if no other {@link EntityBuilderFactory}
 * implementation (provided via the respective extension point, see {@link EntityBuilderManager}) is available.
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultWorkflowAnnotationEnt implements WorkflowAnnotationEnt {

	private String m_Text;
	private BoundsEnt m_Bounds;
	private int m_BgColor;
	private int m_BorderSize;
	private int m_BorderColor;
	private int m_FontSize;
	private String m_Alignment;

    /**
     * @param builder
     */
    private DefaultWorkflowAnnotationEnt(final DefaultWorkflowAnnotationEntBuilder builder) {
		m_Text = builder.m_Text;
		m_Bounds = builder.m_Bounds;
		m_BgColor = builder.m_BgColor;
		m_BorderSize = builder.m_BorderSize;
		m_BorderColor = builder.m_BorderColor;
		m_FontSize = builder.m_FontSize;
		m_Alignment = builder.m_Alignment;
    }

	@Override
    public String getText() {
        return m_Text;
    }
    
	@Override
    public BoundsEnt getBounds() {
        return m_Bounds;
    }
    
	@Override
    public int getBgColor() {
        return m_BgColor;
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
    public int getFontSize() {
        return m_FontSize;
    }
    
	@Override
    public String getAlignment() {
        return m_Alignment;
    }
    

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this);
	}

	public static DefaultWorkflowAnnotationEntBuilder builder() {
		return new DefaultWorkflowAnnotationEntBuilder();
	}
	
	/**
	* Default implementation of the WorkflowAnnotationEntBuilder-interface.
	*/
	public static class DefaultWorkflowAnnotationEntBuilder implements WorkflowAnnotationEntBuilder {
    
		private String m_Text;
		private BoundsEnt m_Bounds;
		private int m_BgColor;
		private int m_BorderSize;
		private int m_BorderColor;
		private int m_FontSize;
		private String m_Alignment;

        public WorkflowAnnotationEnt build() {
            return new DefaultWorkflowAnnotationEnt(this);
        }

		@Override
        public WorkflowAnnotationEntBuilder setText(final String Text) {
			m_Text = Text;			
            return this;
        }
        
		@Override
        public WorkflowAnnotationEntBuilder setBounds(final BoundsEnt Bounds) {
			m_Bounds = Bounds;			
            return this;
        }
        
		@Override
        public WorkflowAnnotationEntBuilder setBgColor(final int BgColor) {
			m_BgColor = BgColor;			
            return this;
        }
        
		@Override
        public WorkflowAnnotationEntBuilder setBorderSize(final int BorderSize) {
			m_BorderSize = BorderSize;			
            return this;
        }
        
		@Override
        public WorkflowAnnotationEntBuilder setBorderColor(final int BorderColor) {
			m_BorderColor = BorderColor;			
            return this;
        }
        
		@Override
        public WorkflowAnnotationEntBuilder setFontSize(final int FontSize) {
			m_FontSize = FontSize;			
            return this;
        }
        
		@Override
        public WorkflowAnnotationEntBuilder setAlignment(final String Alignment) {
			m_Alignment = Alignment;			
            return this;
        }
        
    }
}
