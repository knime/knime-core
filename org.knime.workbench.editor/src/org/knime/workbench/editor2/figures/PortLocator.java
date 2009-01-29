/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * -------------------------------------------------------------------
 *
 * History
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.knime.core.node.port.PortType;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public abstract class PortLocator implements Locator {

    private final boolean m_isInport;

    private final int m_maxPorts;

    private final int m_portIndex;

    private final PortType m_portType;

    /**
     *
     * @param type port type
     * @param portIndex port index
     * @param isInPort true if it's an in port, false if it's an out port
     * @param nrPorts total number of  ports
     */
    public PortLocator(final PortType type, final int portIndex,
            final boolean isInPort, final int nrPorts) {
        m_isInport = isInPort;
        m_portType = type;
        m_maxPorts = nrPorts;
        m_portIndex = portIndex;
    }

    /**
     *
     * @return type of the port (data, model, database)
     */
    protected PortType getType() {
        return m_portType;
    }

    /**
     *
     * @return total number of ports
     */
    protected int getNrPorts() {
        return m_maxPorts;
    }

    /**
     *
     * @return port index
     */
    protected int getPortIndex() {
        return m_portIndex;
    }

    /**
     *
     * @return true if in port, false if out port
     */
    protected boolean isInPort() {
        return m_isInport;
    }

    /**
     * {@inheritDoc}
     */
    public abstract void relocate(IFigure target);

}
