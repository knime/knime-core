/*
 * ------------------------------------------------------------------ *
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
 * -------------------------------------------------------------------
 *
 * History
 *   24.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.knime.core.node.PortType;

/**
 * TODO: make this class obsolete
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractWorkflowPortFigure extends AbstractPortFigure {

    /** Constant for the width and height of the port figure. */
    protected static final int SIZE = 20;
    private final int m_portIndex;

    /**
     *
     * @param type port type
     * @param nrOfPorts total number of ports
     * @param portIndex port index
     */
    public AbstractWorkflowPortFigure(final PortType type,
            final int nrOfPorts, final int portIndex) {
        super(type, nrOfPorts);
        m_portIndex = portIndex;
    }
    
    /**
     *
     * @return index of this port
     */
    protected int getPortIndex() {
        return m_portIndex;
    }


}
