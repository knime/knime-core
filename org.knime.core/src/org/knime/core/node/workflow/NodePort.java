/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   02.05.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node.workflow;

import org.knime.core.node.PortType;


/**
 * Abstract node port implementation which keeps an index and a port name.
 * 
 * @author Michael Berthold & B. Wiswedel, University of Konstanz
 */
public interface NodePort {

    /**
     * @return The port index.
     */
    public int getPortIndex();
    
    /**
     * @return The port type. 
     */
    public PortType getPortType();

    /**
     * @return The port name.
     */
    public String getPortName();

    /**
     * Sets a new name for this port. If null or an empty string is passed, the
     * default name will be generated: "Port [" + portID + "]".
     * 
     * @param portName The new name for this port. If null is passed, the
     *            default name will be generated.
     */
    public void setPortName(final String portName);

}
