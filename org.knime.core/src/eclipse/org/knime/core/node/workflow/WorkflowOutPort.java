/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   18.09.2007 (mb): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;

/**
 *
 * @author M. Berthold, University of Konstanz
 */
public class WorkflowOutPort extends NodeOutPortWrapper  {

    private final NodeInPort m_simulatedInPort;

    /**
     * Creates a new output port with a fixed type and index (should unique
     * to all other output ports of this node) for the given node.
     * @param portIndex This port index
     * @param pType The port's type
     */
    WorkflowOutPort(final int portIndex, final PortType pType) {
        super(portIndex, pType);
        m_simulatedInPort = new NodeInPort(portIndex, pType);
    }

    /** Return a NodeInPort for the WFM's output ports so that the Outport
     * of a node within the WFM can connect to it as an "input". Since InPorts
     * only wrap name/type this is really all it does: it wraps this information
     * as specified during WFM construction into an InPort.
     *
     * @return fake InPort.
     */
    NodeInPort getSimulatedInPort() {
        return m_simulatedInPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject getPortObject() {
        // don't test for execution in the WFM, this will be done by
        // the individual ports
        return super.getPortObject();
    }

}
