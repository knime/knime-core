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

import javax.swing.JComponent;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public final class FlowVariablePortObjectSpec implements PortObjectSpec {

    public static PortObjectSpecSerializer<FlowVariablePortObjectSpec> 
    getPortObjectSpecSerializer() { 
        return new PortObjectSpecSerializer<FlowVariablePortObjectSpec>() {

            @Override
            public FlowVariablePortObjectSpec loadPortObjectSpec(
                    final PortObjectSpecZipInputStream in) {
                return INSTANCE;
            }

            @Override
            public void savePortObjectSpec(
                    final FlowVariablePortObjectSpec portObjectSpec,
                    final PortObjectSpecZipOutputStream out) {
            }
        };
    }
    
    public static final FlowVariablePortObjectSpec INSTANCE = 
        new FlowVariablePortObjectSpec();
    
    private FlowVariablePortObjectSpec() {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return null;
    }
}
