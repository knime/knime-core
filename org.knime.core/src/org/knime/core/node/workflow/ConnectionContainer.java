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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   14.03.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Holds all information related to one connection between specific ports
 * of two nodes. It also holds additional information, which can be adjusted
 * from the outside (bend points on a layout, for example).
 * 
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public class ConnectionContainer {

    private final NodeID m_source;
    private final int m_sourcePort;
    private final NodeID m_dest;
    private final int m_destPort;
    private boolean m_isDeletable = true;
    private UIInformation m_uiInfo;
    private final CopyOnWriteArraySet<ConnectionUIInformationListener> 
        m_uiListeners = 
            new CopyOnWriteArraySet<ConnectionUIInformationListener>();

    
    enum ConnectionType { STD, WFMIN, WFMOUT, WFMTHROUGH;
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
    };
    private final ConnectionType m_type;
    
    /** Creates new connection.
     * 
     * @param src source node
     * @param srcPort port of source node
     * @param dest destination node
     * @param destPort port of destination node
     * @param type of connection
     */
    public ConnectionContainer(final NodeID src, final int srcPort,
            final NodeID dest, final int destPort, final ConnectionType type) { 
        if (src == null || dest == null || type == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
        if (srcPort < 0 || destPort < 0) {
            throw new IndexOutOfBoundsException("Port index must not be < 0:"
                    + Math.min(srcPort, destPort));
        }
        m_source = src;
        m_sourcePort = srcPort;
        m_dest = dest;
        m_destPort = destPort;
        m_uiInfo = null;
        m_type = type;
    }

    /////////////////
    // Getter Methods
    /////////////////
    
    /** 
     * @return the uiInfo
     */
    public UIInformation getUIInfo() {
        return m_uiInfo;
    }

    /**
     * @return the dest
     */
    public NodeID getDest() {
        return m_dest;
    }

    /**
     * @return the destPort
     */
    public int getDestPort() {
        return m_destPort;
    }

    /**
     * @return the source
     */
    public NodeID getSource() {
        return m_source;
    }

    /**
     * @return the sourcePort
     */
    public int getSourcePort() {
        return m_sourcePort;
    }
    
    /**
     * @return the isDeletable
     */
    public boolean isDeletable() {
        return m_isDeletable;
    }
    
    /**
     * @param isDeletable the isDeletable to set
     */
    void setDeletable(final boolean isDeletable) {
        m_isDeletable = isDeletable;
    }

    /**
     * @return type of the connection
     */
    public ConnectionType getType() {
        return m_type;
    }

    /////////////////////////
    
    /**
     * @param uiInfo the uiInfo to set
     */
    public void setUIInfo(final UIInformation uiInfo) {
        m_uiInfo = uiInfo;
        notifyUIListeners(new ConnectionUIInformationEvent(this, m_uiInfo));
    }
    
    /** Add a listener to the list of registered listeners.
     * @param l The listener to add, must not be null.
     */
    public void addUIInformationListener(
            final ConnectionUIInformationListener l) {
        if (l == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_uiListeners.add(l);
    }

    /** Remove a registered listener from the listener list.
     * @param l The listener to remove.
     */
    public void removeUIInformationListener(
            final ConnectionUIInformationListener l) {
        m_uiListeners.remove(l);
    }
    
    /** Removes all registered listeners in order to release references on
     * this object. */
    public void cleanup() {
        m_uiListeners.clear();
    }

    /** Notifies all registered listeners with the argument event.
     * @param evt The event to fire.
     */
    protected void notifyUIListeners(final ConnectionUIInformationEvent evt) {
        for (ConnectionUIInformationListener l : m_uiListeners) {
            l.connectionUIInformationChanged(evt);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ConnectionContainer)) {
            return false;
        }
        ConnectionContainer cc = (ConnectionContainer)obj;
        return m_dest.equals(cc.m_dest) && (m_destPort == cc.m_destPort)
        && m_source.equals(cc.m_source) && (m_sourcePort == cc.m_sourcePort)
                && m_type.equals(cc.m_type);
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_dest.hashCode() + m_source.hashCode() + m_destPort
        + m_sourcePort + m_type.hashCode();
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getType() + "[" + getSource() + "(" + getSourcePort() + ") -> "
            + getDest() + "( " + getDestPort() + ")]" 
            + (isDeletable() ? "" : " non deletable");
    }
}
