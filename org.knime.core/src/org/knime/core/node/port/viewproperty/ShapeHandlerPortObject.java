/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 */
package org.knime.core.node.port.viewproperty;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.PortType;

/**
 * <code>PortObject</code> implementation for {@link ShapeHandlerPortObject}
 * which are part of a <code>DataTableSpec</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ShapeHandlerPortObject extends ViewPropertyPortObject {
    
    /** Convenience access method for port type. */
    public static final PortType TYPE = 
        new PortType(ShapeHandlerPortObject.class);

    /** Public no arg constructor required by super class. 
     * <p>
     * <b>This constructor should only be used by the framework.</b> */
    public ShapeHandlerPortObject() {
    }
    
    /** Constructor used to instantiate this object during a node's execute
     * method.
     * @param spec The accompanying spec
     * @param portSummary A summary returned in the {@link #getSummary()}
     * method.
     * @throws NullPointerException If spec argument is <code>null</code>.
     */
    public ShapeHandlerPortObject(
            final DataTableSpec spec, final String portSummary) {
        super(spec, portSummary);
    }

}
