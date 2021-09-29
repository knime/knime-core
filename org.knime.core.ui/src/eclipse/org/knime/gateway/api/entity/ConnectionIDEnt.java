/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 */
package org.knime.gateway.api.entity;

import java.util.Objects;

import org.knime.core.node.workflow.ConnectionID;

/**
 * Represents a connection id as used by gateway entities and services. Equivalent to the core's
 * {@link org.knime.core.node.workflow.ConnectionID}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class ConnectionIDEnt {

    private NodeIDEnt m_destNodeID;
    private int m_destPortIdx;

    /**
     * @param destNodeID the destination node id of the connection
     * @param destPortIdx the destination port index
     */
    public ConnectionIDEnt(final NodeIDEnt destNodeID, final int destPortIdx) {
        m_destNodeID = destNodeID;
        m_destPortIdx = destPortIdx;
    }

    /**
     * Creates a new connection id entity from a {@link ConnectionID}.
     *
     * @param connectionId
     */
    public ConnectionIDEnt(final ConnectionID connectionId) {
        this(new NodeIDEnt(connectionId.getDestinationNode()), connectionId.getDestinationPort());
    }

    /**
     * Deserialization constructor.
     *
     * @param s string representation as returned by {@link #toString()}
     */
    public ConnectionIDEnt(final String s) {
        String[] split = s.split("_");
        m_destNodeID = new NodeIDEnt(split[0]);
        m_destPortIdx = Integer.parseInt(split[1]);
    }

    /**
     * @return the destination node id entity
     */
    public NodeIDEnt getDestNodeIDEnt() {
        return m_destNodeID;
    }

    /**
     * @return the destination port index
     */
    public int getDestPortIdx() {
        return m_destPortIdx;
    }

    @Override
    public String toString() {
        return m_destNodeID.toString() + "_" + m_destPortIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_destNodeID.hashCode(), m_destNodeID);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        ConnectionIDEnt ent = (ConnectionIDEnt)o;
        return Objects.equals(m_destNodeID, ent.m_destNodeID) && m_destPortIdx == ent.m_destPortIdx;
    }
}
