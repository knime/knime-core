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

import org.knime.core.gateway.v0.workflow.entity.AnnotationEnt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link AnnotationEnt} interface that can be deserialized from a json object (json-annotated constructor).
 *
 * @author Martin Horn, University of Konstanz
 */
public class AnnotationEntFromJson implements AnnotationEnt{

	private String m_Text;
	private int m_BackgroundColor;
	private int m_X;
	private int m_Y;
	private int m_Width;
	private int m_Height;
	private String m_TextAlignment;
	private int m_BorderSize;
	private int m_BorderColor;
	private int m_DefaultFontSize;
	private int m_Version;

	@JsonCreator
	private AnnotationEntFromJson(
	@JsonProperty("Text") String Text,	@JsonProperty("BackgroundColor") int BackgroundColor,	@JsonProperty("X") int X,	@JsonProperty("Y") int Y,	@JsonProperty("Width") int Width,	@JsonProperty("Height") int Height,	@JsonProperty("TextAlignment") String TextAlignment,	@JsonProperty("BorderSize") int BorderSize,	@JsonProperty("BorderColor") int BorderColor,	@JsonProperty("DefaultFontSize") int DefaultFontSize,	@JsonProperty("Version") int Version	) {
		m_Text = Text;
		m_BackgroundColor = BackgroundColor;
		m_X = X;
		m_Y = Y;
		m_Width = Width;
		m_Height = Height;
		m_TextAlignment = TextAlignment;
		m_BorderSize = BorderSize;
		m_BorderColor = BorderColor;
		m_DefaultFontSize = DefaultFontSize;
		m_Version = Version;
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
    public int getX() {
        	return m_X;
            
    }
    
	@Override
    public int getY() {
        	return m_Y;
            
    }
    
	@Override
    public int getWidth() {
        	return m_Width;
            
    }
    
	@Override
    public int getHeight() {
        	return m_Height;
            
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
    

}
