/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Sep 4, 2017 (hornm): created
 */
package org.knime.core.ui.wrapper;

import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionProgressEvent;
import org.knime.core.node.workflow.ConnectionProgressListener;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.ConnectionUIInformationListener;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;

/**
 * UI-interface implementation that wraps a {@link ConnectionContainer}.
 *
 * @author Martin Horn, University of Konstanz
 */
public final class ConnectionContainerWrapper extends AbstractWrapper<ConnectionContainer>
    implements ConnectionContainerUI {

    /**
     * @param delegate
     */
    protected ConnectionContainerWrapper(final ConnectionContainer delegate) {
        super(delegate);
    }

    /**
     * Wraps the object via {@link Wrapper#wrapOrGet(Object, java.util.function.Function)}.
     *
     * @param cc the object to be wrapped
     * @return a new wrapper or a already existing one
     */
    public static ConnectionContainerWrapper wrap(final ConnectionContainer cc) {
        return (ConnectionContainerWrapper)Wrapper.wrapOrGet(cc, o -> new ConnectionContainerWrapper(o));
    }

    @Override
    public ConnectionUIInformation getUIInfo() {
        return unwrap().getUIInfo();
    }

    @Override
    public NodeID getDest() {
        return unwrap().getDest();
    }

    @Override
    public int getDestPort() {
        return unwrap().getDestPort();
    }

    @Override
    public NodeID getSource() {
        return unwrap().getSource();
    }

    @Override
    public int getSourcePort() {
        return unwrap().getSourcePort();
    }

    @Override
    public boolean isDeletable() {
        return unwrap().isDeletable();
    }

    @Override
    public ConnectionType getType() {
        return unwrap().getType();
    }

    @Override
    public ConnectionID getID() {
        return unwrap().getID();
    }

    @Override
    public void setUIInfo(final ConnectionUIInformation uiInfo) {
        unwrap().setUIInfo(uiInfo);
    }

    @Override
    public void addUIInformationListener(final ConnectionUIInformationListener l) {
        unwrap().addUIInformationListener(l);
    }

    @Override
    public void removeUIInformationListener(final ConnectionUIInformationListener l) {
        unwrap().removeUIInformationListener(l);
    }

    @Override
    public void addProgressListener(final ConnectionProgressListener listener) {
        unwrap().addProgressListener(listener);
    }

    @Override
    public void removeProgressListener(final ConnectionProgressListener listener) {
        unwrap().removeProgressListener(listener);
    }

    @Override
    public void progressChanged(final ConnectionProgressEvent pe) {
        unwrap().progressChanged(pe);
    }

    @Override
    public void cleanup() {
        unwrap().cleanup();
    }

    @Override
    public boolean equals(final Object obj) {
        return unwrap().equals(obj);
    }

    @Override
    public int hashCode() {
        return unwrap().hashCode();
    }

    @Override
    public String toString() {
        return unwrap().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFlowVariablePortConnection() {
        return unwrap().isFlowVariablePortConnection();
    }
}
