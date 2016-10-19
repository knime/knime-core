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
package org.knime.core.node.workflow.wrapped;

import org.knime.core.api.node.workflow.ConnectionID;
import org.knime.core.api.node.workflow.ConnectionProgressEvent;
import org.knime.core.api.node.workflow.ConnectionProgressListener;
import org.knime.core.api.node.workflow.ConnectionUIInformation;
import org.knime.core.api.node.workflow.ConnectionUIInformationListener;
import org.knime.core.api.node.workflow.IConnectionContainer;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class ConnectionContainerWrapper implements IConnectionContainer {

    private final IConnectionContainer m_delegate;

    /**
     * @param delegate the {@link ConnectionContainer} implementation to delegate to
     */
    private ConnectionContainerWrapper(final IConnectionContainer delegate) {
        m_delegate = delegate;
    }

    public static final ConnectionContainerWrapper wrap(final IConnectionContainer cc) {
        return WrapperMapUtil.getOrCreate(cc, o -> new ConnectionContainerWrapper(o), ConnectionContainerWrapper.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_delegate.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        return m_delegate.equals(obj);
    }

    @Override
    public void progressChanged(final ConnectionProgressEvent pe) {
        m_delegate.progressChanged(pe);
    }

    @Override
    public ConnectionUIInformation getUIInfo() {
        return m_delegate.getUIInfo();
    }

    @Override
    public NodeID getDest() {
        return m_delegate.getDest();
    }

    @Override
    public int getDestPort() {
        return m_delegate.getDestPort();
    }

    @Override
    public NodeID getSource() {
        return m_delegate.getSource();
    }

    @Override
    public int getSourcePort() {
        return m_delegate.getSourcePort();
    }

    @Override
    public boolean isDeletable() {
        return m_delegate.isDeletable();
    }

    @Override
    public ConnectionType getType() {
        return m_delegate.getType();
    }

    @Override
    public ConnectionID getID() {
        return m_delegate.getID();
    }

    @Override
    public void setUIInfo(final ConnectionUIInformation uiInfo) {
        m_delegate.setUIInfo(uiInfo);
    }

    @Override
    public void addUIInformationListener(final ConnectionUIInformationListener l) {
        m_delegate.addUIInformationListener(l);
    }

    @Override
    public void removeUIInformationListener(final ConnectionUIInformationListener l) {
        m_delegate.removeUIInformationListener(l);
    }

    @Override
    public void addProgressListener(final ConnectionProgressListener listener) {
        m_delegate.addProgressListener(listener);
    }

    @Override
    public void removeProgressListener(final ConnectionProgressListener listener) {
        m_delegate.removeProgressListener(listener);
    }

    @Override
    public void cleanup() {
        m_delegate.cleanup();
    }

}
