/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created: Mar 29, 2011
 * Author: ohl
 */
package org.knime.core.def.node.workflow;

import org.knime.core.node.workflow.NodeID;

/**
 *
 * @author ohl, University of Konstanz
 */
public final class ConnectionID {

    private final NodeID m_destinationNode;

    private final int m_destinationPort;

    /**
     * A connection is identified by its destination node and port index.
     *
     * @param destNode the endpoint of the connection
     * @param destPort the port index of the destination node
     */
    public ConnectionID(final NodeID destNode, final int destPort) {
        if (destNode == null) {
            throw new NullPointerException("Destination node ID can't be null");
        }
        if (destPort < 0) {
            throw new NullPointerException(
                    "Destination port index can't be negative");
        }
        m_destinationNode = destNode;
        m_destinationPort = destPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_destinationNode.hashCode();
        result = prime * result + m_destinationPort;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConnectionID)) {
            return false;
        }
        ConnectionID other = (ConnectionID)obj;

        if (!m_destinationNode.equals(other.m_destinationNode)) {
            return false;
        }
        if (m_destinationPort != other.m_destinationPort) {
            return false;
        }
        return true;
    }

    /**
     * @return the destinationNode
     */
    public NodeID getDestinationNode() {
        return m_destinationNode;
    }

    /**
     * @return the destinationPort
     */
    public int getDestinationPort() {
        return m_destinationPort;
    }

}
