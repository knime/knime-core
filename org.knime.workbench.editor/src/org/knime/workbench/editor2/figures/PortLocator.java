/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

    private int m_maxPorts;

    private int m_portIndex;

    private final boolean m_isMetaNodePort;

    private final PortType m_type;

    /**
     *
     * @param type port type
     * @param portIndex port index
     * @param isInPort true if it's an in port, false if it's an out port
     * @param nrPorts total number of ports
     * @param isMetaNodePort whether this is hanging off a meta node
     */
    public PortLocator(final PortType type, final int portIndex,
            final boolean isInPort, final int nrPorts,
            final boolean isMetaNodePort) {
        m_isInport = isInPort;
        m_maxPorts = nrPorts;
        m_portIndex = portIndex;
        m_isMetaNodePort = isMetaNodePort;
        m_type = type;
    }

    /**
     *
     * @return type of the port (data, model, database)
     */
    protected PortType getType() {
        return m_type;
    }

    /**
     *
     * @return total number of ports
     */
    protected int getNrPorts() {
        return m_maxPorts;
    }

    /**
     * @param numberOfPorts new number of ports of the node
     */
    public void setNrPorts(final int numberOfPorts) {
        m_maxPorts = numberOfPorts;
    }

    /**
     *
     * @return port index
     */
    protected int getPortIndex() {
        return m_portIndex;
    }

    /**
     * @param idx the new index
     */
    public void setPortIndex(final int idx) {
        m_portIndex = idx;
    }
    /**
     *
     * @return true if in port, false if out port
     */
    protected boolean isInPort() {
        return m_isInport;
    }

    /**
     * @return true if port is hanging off a meta node.
     */
    protected boolean isMetaNodePort() {
        return m_isMetaNodePort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void relocate(IFigure target);

}
