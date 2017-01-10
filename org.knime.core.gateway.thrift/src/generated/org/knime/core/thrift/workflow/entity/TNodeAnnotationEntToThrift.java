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


import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;

import org.knime.core.thrift.workflow.entity.TNodeAnnotationEntFromThrift.TNodeAnnotationEntBuilderFromThrift;
import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;

import java.util.stream.Collectors;
import java.util.HashMap;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class TNodeAnnotationEntToThrift extends TNodeAnnotationEnt {

	private final NodeAnnotationEnt m_e;
	
	public TNodeAnnotationEntToThrift(final NodeAnnotationEnt e) {
		m_e = e;
	}

	@Override
    public String getNode() {
        	return m_e.getNode();
        }
    
	@Override
    public String getText() {
        	return m_e.getText();
        }
    
	@Override
    public int getBackgroundColor() {
        	return m_e.getBackgroundColor();
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
    public String getTextAlignment() {
        	return m_e.getTextAlignment();
        }
    
	@Override
    public int getBorderSize() {
        	return m_e.getBorderSize();
        }
    
	@Override
    public int getBorderColor() {
        	return m_e.getBorderColor();
        }
    
	@Override
    public int getDefaultFontSize() {
        	return m_e.getDefaultFontSize();
        }
    
	@Override
    public int getVersion() {
        	return m_e.getVersion();
        }
    

	@Override
	public String toString() {
	    return m_e.toString();
	}

    public static class TNodeAnnotationEntBuilderToThrift extends TNodeAnnotationEntBuilder {
    
    	private NodeAnnotationEntBuilder m_b;
    	
    	public TNodeAnnotationEntBuilderToThrift(final NodeAnnotationEntBuilder b) {
    		m_b = b;
    	}

    
    	@Override
        public TNodeAnnotationEnt build() {
            return new TNodeAnnotationEntToThrift(m_b.build());
        }
        
        @Override
        public GatewayEntityBuilder<NodeAnnotationEnt> wrap() {
            return new TNodeAnnotationEntBuilderFromThrift(this);
        }

		@Override
        public TNodeAnnotationEntBuilderToThrift setNode(final String Node) {
					m_b.setNode(Node);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setText(final String Text) {
					m_b.setText(Text);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setBackgroundColor(final int BackgroundColor) {
					m_b.setBackgroundColor(BackgroundColor);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setX(final int X) {
					m_b.setX(X);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setY(final int Y) {
					m_b.setY(Y);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setWidth(final int Width) {
					m_b.setWidth(Width);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setHeight(final int Height) {
					m_b.setHeight(Height);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setTextAlignment(final String TextAlignment) {
					m_b.setTextAlignment(TextAlignment);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setBorderSize(final int BorderSize) {
					m_b.setBorderSize(BorderSize);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setBorderColor(final int BorderColor) {
					m_b.setBorderColor(BorderColor);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setDefaultFontSize(final int DefaultFontSize) {
					m_b.setDefaultFontSize(DefaultFontSize);
		            return this;
        }
        
		@Override
        public TNodeAnnotationEntBuilderToThrift setVersion(final int Version) {
					m_b.setVersion(Version);
		            return this;
        }
        
    }

}
