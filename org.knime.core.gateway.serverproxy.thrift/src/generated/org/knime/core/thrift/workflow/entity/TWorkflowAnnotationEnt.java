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

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import org.knime.core.gateway.serverproxy.entity.AbstractWorkflowAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowAnnotationEnt;
import org.knime.core.thrift.workflow.entity.TWorkflowAnnotationEnt.TWorkflowAnnotationEntBuilder;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
@ThriftStruct(builder = TWorkflowAnnotationEntBuilder.class)
public class TWorkflowAnnotationEnt extends AbstractWorkflowAnnotationEnt {

    /**
     * @param builder
     */
    protected TWorkflowAnnotationEnt(final AbstractWorkflowAnnotationEntBuilder builder) {
        super(builder);
    }

    @Override
    @ThriftField
    public String getText() {
        return super.getText();
    }
    
    @Override
    @ThriftField
    public BoundsEnt getBounds() {
        return super.getBounds();
    }
    
    @Override
    @ThriftField
    public int getBgColor() {
        return super.getBgColor();
    }
    
    @Override
    @ThriftField
    public int getBorderSize() {
        return super.getBorderSize();
    }
    
    @Override
    @ThriftField
    public int getBorderColor() {
        return super.getBorderColor();
    }
    
    @Override
    @ThriftField
    public int getFontSize() {
        return super.getFontSize();
    }
    
    @Override
    @ThriftField
    public String getAlignment() {
        return super.getAlignment();
    }
    

    public static class TWorkflowAnnotationEntBuilder extends AbstractWorkflowAnnotationEntBuilder {

        @Override
        @ThriftConstructor
        public TWorkflowAnnotationEnt build() {
            return new TWorkflowAnnotationEnt(this);
        }

        @Override
        @ThriftField
        public TWorkflowAnnotationEntBuilder setText(final String Text) {
            super.setText(Text);
            return this;
        }
        
        @Override
        @ThriftField
        public TWorkflowAnnotationEntBuilder setBounds(final BoundsEnt Bounds) {
            super.setBounds(Bounds);
            return this;
        }
        
        @Override
        @ThriftField
        public TWorkflowAnnotationEntBuilder setBgColor(final int BgColor) {
            super.setBgColor(BgColor);
            return this;
        }
        
        @Override
        @ThriftField
        public TWorkflowAnnotationEntBuilder setBorderSize(final int BorderSize) {
            super.setBorderSize(BorderSize);
            return this;
        }
        
        @Override
        @ThriftField
        public TWorkflowAnnotationEntBuilder setBorderColor(final int BorderColor) {
            super.setBorderColor(BorderColor);
            return this;
        }
        
        @Override
        @ThriftField
        public TWorkflowAnnotationEntBuilder setFontSize(final int FontSize) {
            super.setFontSize(FontSize);
            return this;
        }
        
        @Override
        @ThriftField
        public TWorkflowAnnotationEntBuilder setAlignment(final String Alignment) {
            super.setAlignment(Alignment);
            return this;
        }
        
    }

}
