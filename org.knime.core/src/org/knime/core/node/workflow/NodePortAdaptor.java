/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * History
 *   Mar 20, 2008 (berthold): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.port.PortType;

/**
 * 
 * @author berthold, University of Konstanz
 */
public class NodePortAdaptor implements NodePort {
    /** This ports ID assigned from the underlying node. */
    private final int m_portIndex;
    
    /** The type of this port. */
    private final PortType m_portType;

    /** The port name which can be used for displaying purposes. */
    private String m_portName;

    /**
     * Creates a new node port with an ID assigned from the underlying node. The
     * default port name is "Port [portID]" and can be changed via
     * <code>#setPortName(String)</code>.
     * 
     * @param portID the port's id, greater or equal zero
     * 
     * @see #setPortName(String)
     */
    public NodePortAdaptor(final int portID, final PortType pType) {
        assert (portID >= 0);
        assert (pType != null);
        m_portIndex = portID;
        m_portType = pType;
        setPortName(null);
    }

    /**
     * @return The port id.
     */
    public final int getPortIndex() {
        return m_portIndex;
    }
    
    /**
     * @return The port type. 
     */
    public final PortType getPortType() {
        return m_portType;
    }

    /**
     * @return The port name.
     */
    public final String getPortName() {
        return m_portName;
    }

    /**
     * Sets a new name for this port. If null or an empty string is passed, the
     * default name will be generated: "Port [" + portID + "]".
     * 
     * @param portName The new name for this port. If null is passed, the
     *            default name will be generated.
     */
    public final void setPortName(final String portName) {
        if (portName == null || portName.trim().length() == 0) {
            if (this instanceof NodeInPort) {
                m_portName = "Inport " + m_portIndex;
            } else {
                m_portName = "Outport " + m_portIndex;
            }
        } else {
            m_portName = portName.trim();
        }
    }

}
