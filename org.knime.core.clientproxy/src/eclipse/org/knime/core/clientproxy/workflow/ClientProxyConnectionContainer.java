/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   Oct 13, 2016 (hornm): created
 */
package org.knime.core.clientproxy.workflow;

import java.util.List;

import org.knime.core.api.node.workflow.ConnectionID;
import org.knime.core.api.node.workflow.ConnectionProgressEvent;
import org.knime.core.api.node.workflow.ConnectionProgressListener;
import org.knime.core.api.node.workflow.ConnectionUIInformation;
import org.knime.core.api.node.workflow.ConnectionUIInformationListener;
import org.knime.core.api.node.workflow.IConnectionContainer;
import org.knime.core.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.core.gateway.v0.workflow.entity.XYEnt;
import org.knime.core.node.workflow.NodeID;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class ClientProxyConnectionContainer implements IConnectionContainer {

    private ConnectionEnt m_connection;

    /**
     *
     */
    public ClientProxyConnectionContainer(final ConnectionEnt conn) {
        m_connection = conn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void progressChanged(final ConnectionProgressEvent pe) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionUIInformation getUIInfo() {
        List<? extends XYEnt> bendPoints = m_connection.getBendPoints();
        ConnectionUIInformation.Builder builder = ConnectionUIInformation.builder();
        for (int i = 0; i < bendPoints.size(); i++) {
            XYEnt xy = bendPoints.get(i);
            builder.addBendpoint(xy.getX(), xy.getY(), i);
        }
        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeID getDest() {
        return NodeID.fromString(m_connection.getDest());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDestPort() {
        return m_connection.getDestPort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeID getSource() {
        return NodeID.fromString(m_connection.getSource());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSourcePort() {
        return m_connection.getSourcePort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeletable() {
        return m_connection.getIsDeleteable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionType getType() {
        return ConnectionType.valueOf(m_connection.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionID getID() {
        return new ConnectionID(getDest(), getDestPort());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUIInfo(final ConnectionUIInformation uiInfo) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUIInformationListener(final ConnectionUIInformationListener l) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeUIInformationListener(final ConnectionUIInformationListener l) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProgressListener(final ConnectionProgressListener listener) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeProgressListener(final ConnectionProgressListener listener) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        // TODO Auto-generated method stub

    }

}
