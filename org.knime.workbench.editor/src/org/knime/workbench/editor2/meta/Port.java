/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 *
 * History
 *   14.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ModelPortObject;
import org.knime.core.node.PortType;

/**
 * Helper class to represent a port with name and type.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class Port {

    private final PortType m_type;
    private final String m_name;


    /**
     *
     * @param type the port type
     * @param name the port name
     */
    public Port(final PortType type, final String name) {
        m_type = type;
        m_name = name;
    }

    /**
     *
     * @return port type
     */
    public PortType getType() {
        return m_type;
    }

    /**
     *
     * @return port name
     */
    public String getName() {
        return m_name;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_type.equals(BufferedDataTable.TYPE)) {
            return m_name + "(Data)";
        } else if (m_type.equals(ModelPortObject.TYPE)) {
            return m_name + "(Model)";
        } else {
            return m_name + "(unknown)";
        }
    }
}
