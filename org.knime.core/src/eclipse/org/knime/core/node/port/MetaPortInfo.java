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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 20, 2012 (wiswedel): created
 */
package org.knime.core.node.port;

/** Object describing a meta node port. Used in the action to modify meta node
 * port orders, types, etc. It comprises the port type, whether it's connected
 * (only if created from the WFM) and what its index in the list of all
 * in/out ports is.
 *
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.6
 */
public final class MetaPortInfo {

    private final PortType m_type;
    private final boolean m_isConnected;
    private final String m_message;
    private int m_oldIndex;
    private int m_newIndex;

    /** Created from the WFM. Fills the object according the arguments. A port
     * may be connected or not.
     * @param type ...
     * @param isConnected .. if connected somewhere in (or outside) the flow
     * @param message The tooltip (only if isConnected)
     * @param oldIndex the port index. */
    public MetaPortInfo(final PortType type, final boolean isConnected,
            final String message, final int oldIndex) {
        m_type = type;
        m_isConnected = isConnected;
        m_message = message;
        m_oldIndex = oldIndex;
        m_newIndex = -1;
    }

    /** Called from the UI to define a new port (not connected).
     * @param type ...
     * @param newIndex Index of port. */
    public MetaPortInfo(final PortType type, final int newIndex) {
        m_type = type;
        m_newIndex = newIndex;
        m_oldIndex = -1;
        m_isConnected = false;
        m_message = null;
    }

    /** @return the type */
    public PortType getType() {
        return m_type;
    }

    /** @return the isConnected */
    public boolean isConnected() {
        return m_isConnected;
    }

    /** @return the message */
    public String getMessage() {
        return m_message;
    }

    /** @return the oldIndex */
    public int getOldIndex() {
        return m_oldIndex;
    }

    /** @return the newIndex */
    public int getNewIndex() {
        return m_newIndex;
    }

    /** @param newIndex the newIndex to set */
    public void setNewIndex(final int newIndex) {
        m_newIndex = newIndex;
    }

}
