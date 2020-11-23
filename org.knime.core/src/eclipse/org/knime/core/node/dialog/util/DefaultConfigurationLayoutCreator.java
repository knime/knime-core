/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   10 Nov 2020 (bogenrieder): created
 */
package org.knime.core.node.dialog.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.MetaNodeDialogNode;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubnodeContainerConfigurationStringProvider;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * A service interface to create a default configuration layout from a given set of {@link DialogNode}.
 *
 * @author Daniel Bogenrieder, KNIME.com GmbH, Konstanz, Germany
 * @since 4.3
 */
public interface DefaultConfigurationLayoutCreator {

    /**
     * Creates a default layout structure as a serialized JSON string.
     *
     * @param configurationNodes the nodes to include in the layout.
     * @return a default configuration layout structure as JSON string.
     * @throws IOException on creation error.
     */
    @SuppressWarnings("rawtypes")
    public String createDefaultConfigurationLayout(final Map<NodeIDSuffix, DialogNode> configurationNodes)
        throws IOException;

    /**
     * Creates extra rows/columns at the bottom of the configuration layout for all unreferenced nodes.
     *
     * @param configurationStringProvider the configuration layout provider, who's layout needs to be already expanded.
     * @param allNodes a map of all nodes with a configuration dialog.
     */
    @SuppressWarnings("rawtypes")
    public void addUnreferencedViews(final SubnodeContainerConfigurationStringProvider configurationStringProvider,
        final Map<NodeIDSuffix, DialogNode> allNodes);

    /**
     * Updates a configuration layout.
     *
     * @param configurationStringProvider the configuration layout provider, who's layout needs to be already expanded.
     */
    public void
        updateConfigurationLayout(final SubnodeContainerConfigurationStringProvider configurationStringProvider);

    /**
     * Returns the order of the configuration nodes.
     *
     * @param configurationStringProvider the configuration layout provider, who's layout needs to be already expanded.
     * @param nodes all available configuration nodes, which are then sorted
     * @param wfm the workflow manager
     * @return returns a List of NodeID's in the configured order
     */
    public List<Integer> getConfigurationOrder(
        final SubnodeContainerConfigurationStringProvider configurationStringProvider,
        final Map<NodeID, MetaNodeDialogNode> nodes, final WorkflowManager wfm);
}
