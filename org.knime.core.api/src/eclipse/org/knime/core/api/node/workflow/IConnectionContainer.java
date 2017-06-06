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
 *   Aug 30, 2016 (wiswedel): created
 */
package org.knime.core.api.node.workflow;

import org.knime.core.node.workflow.NodeID;

/**
 *
 * @author wiswedel
 */
public interface IConnectionContainer extends ConnectionProgressListener{

    /** Typ of the connection: metanode input, output, through or "standard" connection.
     * @noreference */
    public enum ConnectionType { STD, WFMIN, WFMOUT, WFMTHROUGH;
        /**
         * @return Whether this type is leaving a workflow (through or out)
         */
        public boolean isLeavingWorkflow() {
            switch (this) {
                case WFMOUT:
                case WFMTHROUGH: return true;
                default: return false;
            }
        }
    }

    /**
     * @return the uiInfo
     */
    ConnectionUIInformation getUIInfo();

    /**
     * @return the dest
     */
    NodeID getDest();

    /**
     * @return the destPort
     */
    int getDestPort();

    /**
     * @return the source
     */
    NodeID getSource();

    /**
     * @return the sourcePort
     */
    int getSourcePort();

    /**
     * @return the isDeletable
     */
    boolean isDeletable();

    /**
     * @return whether the connection connects two flow variable ports
     */
    boolean isFlowVariablePortConnection();

    /**
     * @return type of the connection
     */
    ConnectionType getType();

    /**
     * @return the ID for this connection.
     */
    ConnectionID getID();

    /**
     * @param uiInfo the uiInfo to set
     */
    void setUIInfo(ConnectionUIInformation uiInfo);

    /** Add a listener to the list of registered listeners.
     * @param l The listener to add, must not be null.
     */
    void addUIInformationListener(ConnectionUIInformationListener l);

    /** Remove a registered listener from the listener list.
     * @param l The listener to remove.
     */
    void removeUIInformationListener(ConnectionUIInformationListener l);

    /**
     * Adds a listener to the list of registered progress listeners.
     * @param listener The listener to add, must not be null.
     */
    void addProgressListener(ConnectionProgressListener listener);

    /**
     * Removes a listener from the list of registered progress listeners.
     * @param listener The listener to remove
     */
    void removeProgressListener(ConnectionProgressListener listener);

    /** Removes all registered listeners in order to release references on
     * this object. */
    public void cleanup();

}