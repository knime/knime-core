/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   Sep 17, 2008 (wiswedel): created
 */
package org.knime.core.node.port.flowvariable;

import java.io.IOException;

import javax.swing.JComponent;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class FlowVariablePortObject implements PortObject {
    
    public static final PortType TYPE = 
        new PortType(FlowVariablePortObject.class);
    
    public static PortObjectSerializer<FlowVariablePortObject> 
    getPortObjectSerializer() {
        return new PortObjectSerializer<FlowVariablePortObject>() {

            @Override
            public FlowVariablePortObject loadPortObject(
                    final PortObjectZipInputStream in, 
                    final PortObjectSpec spec, final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
                return new FlowVariablePortObject();
            }

            @Override
            public void savePortObject(final FlowVariablePortObject portObject,
                    final PortObjectZipOutputStream out, 
                    final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                
            }
            
        };
    }

    /** {@inheritDoc} */
    @Override
    public PortObjectSpec getSpec() {
        return FlowVariablePortObjectSpec.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return "Variables connection";
    }
    
    /** {@inheritDoc} */
    @Override
    public JComponent[] getViews() {
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return getClass().equals(obj.getClass());
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
