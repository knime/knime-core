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
 * -------------------------------------------------------------------
 *
 * History
 *   14.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.workbench.KNIMEEditorPlugin;

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
        } else if (KNIMEEditorPlugin.PMML_PORT_TYPE.isSuperTypeOf(m_type)) {
            return m_name + "(PMML)";
        } else if (m_type.equals(DatabasePortObject.TYPE)) {
            return m_name + "(Data base)";
        } else {
            return m_name + "(unknown)";
        }
    }
}
