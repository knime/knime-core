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


import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import org.knime.core.gateway.serverproxy.entity.AbstractPortTypeEnt;
import org.knime.core.gateway.v0.workflow.entity.PortTypeEnt;
import org.knime.core.thrift.workflow.entity.TPortTypeEnt.TPortTypeEntBuilder;


/**
 *
 * @author Martin Horn, University of Konstanz
 */
@ThriftStruct(builder = TPortTypeEntBuilder.class)
public class TPortTypeEnt extends AbstractPortTypeEnt {

    /**
     * @param builder
     */
    protected TPortTypeEnt(final AbstractPortTypeEntBuilder builder) {
        super(builder);
    }

    @Override
    @ThriftField
    public String getName() {
        return super.getName();
    }
    
    @Override
    @ThriftField
    public String getPortObjectClassName() {
        return super.getPortObjectClassName();
    }
    
    @Override
    @ThriftField
    public boolean getIsOptional() {
        return super.getIsOptional();
    }
    
    @Override
    @ThriftField
    public int getColor() {
        return super.getColor();
    }
    
    @Override
    @ThriftField
    public boolean getIsHidden() {
        return super.getIsHidden();
    }
    

    public static class TPortTypeEntBuilder extends AbstractPortTypeEntBuilder {

        @Override
        @ThriftConstructor
        public TPortTypeEnt build() {
            return new TPortTypeEnt(this);
        }

        @Override
        @ThriftField
        public TPortTypeEntBuilder setName(final String Name) {
            super.setName(Name);
            return this;
        }
        
        @Override
        @ThriftField
        public TPortTypeEntBuilder setPortObjectClassName(final String PortObjectClassName) {
            super.setPortObjectClassName(PortObjectClassName);
            return this;
        }
        
        @Override
        @ThriftField
        public TPortTypeEntBuilder setIsOptional(final boolean IsOptional) {
            super.setIsOptional(IsOptional);
            return this;
        }
        
        @Override
        @ThriftField
        public TPortTypeEntBuilder setColor(final int Color) {
            super.setColor(Color);
            return this;
        }
        
        @Override
        @ThriftField
        public TPortTypeEntBuilder setIsHidden(final boolean IsHidden) {
            super.setIsHidden(IsHidden);
            return this;
        }
        
    }

}
