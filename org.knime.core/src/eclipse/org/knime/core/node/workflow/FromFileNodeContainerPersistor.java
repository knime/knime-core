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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 26, 2012 by wiswedel
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformation;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 *
 * @author wiswedel
 */
interface FromFileNodeContainerPersistor extends NodeContainerPersistor {

    void preLoadNodeContainer(final WorkflowPersistor parentPersistor,
                              final NodeSettingsRO parentSettings, LoadResult loadResult)
                              throws InvalidSettingsException, IOException;

    /** Called on single node persistors if their factory can't be loaded (extension not installed). They will
     * check their up- and downstream nodes and guess their port types from. (Port types are defined in the node
     * code, which is missing here).
     * @param nodeInfo Info stored in xml
     * @param additionalFactorySettings possible settings for dynamic node factory (possibly null)
     * @param upstreamNodes The ordered list of upstream nodes, may contain null (not connected).
     * @param downstreamNodes the ordered list of downstream nodes (multiple connections to output possible).
     *        May contain null.
     */
    void guessPortTypesFromConnectedNodes(final NodeAndBundleInformation nodeInfo,
          NodeSettingsRO additionalFactorySettings, final ArrayList<PersistorWithPortIndex> upstreamNodes,
          final ArrayList<List<PersistorWithPortIndex>> downstreamNodes);

    /** Used in {@link #guessPortTypesFromConnectedNodes(
     * NodeAndBundleInformation, NodeSettingsRO, ArrayList, ArrayList)} to retrieve
     * the port types of up- and downstream nodes. Might return null (if this node is missing, too).
     *
     * <p>Downstream port types are input ports for SNC but output ports for WFMs.
     * @param index Port index.
     * @return port type or null.
     */
    PortType getDownstreamPortType(final int index);

    /** Counterpart to {@link #getDownstreamPortType(int)}.
     *
     * <p>Upstream port types are output ports for SNC but input ports for WFMs.
     * @param index ...
     * @return ...
     */
    PortType getUpstreamPortType(final int index);

    /** Represents the persistor of a connected node with its port (port at the connected node!). */
    static final class PersistorWithPortIndex {
        private final FromFileNodeContainerPersistor m_persistor;
        private final int m_portIndex;

        /** ...
         * @param persistor ...
         * @param portIndex ...
         */
        PersistorWithPortIndex(final FromFileNodeContainerPersistor persistor, final int portIndex) {
            if (persistor == null || portIndex < 0) {
                throw new IllegalArgumentException(
                   String.format("Invalid constructor call: persistor %s; port %d", persistor, portIndex));
            }
            m_persistor = persistor;
            m_portIndex = portIndex;
        }

        /**@return the persistor  ... */
        FromFileNodeContainerPersistor getPersistor() {
            return m_persistor;
        }

        /** @return the portIndex ... */
        int getPortIndex() {
            return m_portIndex;
        }

    }

}
