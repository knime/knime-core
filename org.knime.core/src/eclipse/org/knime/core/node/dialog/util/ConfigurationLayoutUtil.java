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
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author Daniel Bogenrieder, KNIME.com GmbH, Konstanz, Germany
 * @since 4.3
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ConfigurationLayoutUtil {

    private static final ServiceTracker serviceConfigurationTracker;

    static {
        Bundle coreBundle = FrameworkUtil.getBundle(ConfigurationLayoutUtil.class);
        if (coreBundle != null) {
            serviceConfigurationTracker = new ServiceTracker(coreBundle.getBundleContext(),
                DefaultConfigurationLayoutCreator.class.getName(), null);
            serviceConfigurationTracker.open();
        } else {
            serviceConfigurationTracker = null;
        }
    }

    /**
     * Creates a default layout structure as a serialized JSON string.
     *
     * @param configurationNodes the configuration nodes to include in the layout.
     * @return a default layout structure as JSON string.
     * @throws IOException If no service is registered or the default layout cannot be created.
     */
    public static String createDefaultLayout(final Map<NodeIDSuffix, DialogNode> configurationNodes)
        throws IOException {
        if (serviceConfigurationTracker == null) {
            throw new IOException("Core bundle is not active, can't create default layout.");
        }
        DefaultConfigurationLayoutCreator creator =
            (DefaultConfigurationLayoutCreator)serviceConfigurationTracker.getService();
        if (creator == null) {
            throw new IOException("Can't create default configuration layout; no appropriate service registered.");
        }
        return creator.createDefaultConfigurationLayout(configurationNodes);
    }

    /**
     * Creates extra rows/columns at the bottom of the layout for all unreferenced nodes.
     *
     * @param configurationStringProvider the configuration layout provider, who's layout needs to be already expanded.
     * @param allNodes a map of all nodes with a configuration dialog.
     * @throws IOException If no service is registered or the layout cannot be amended.
     */
    public static void addUnreferencedDialogNodes(
        final SubnodeContainerConfigurationStringProvider configurationStringProvider,
        final Map<NodeIDSuffix, DialogNode> allNodes) throws IOException {
        if (serviceConfigurationTracker == null) {
            throw new IOException("Core bundle is not active, can't add unreferenced views to layout.");
        }
        DefaultConfigurationLayoutCreator creator =
            (DefaultConfigurationLayoutCreator)serviceConfigurationTracker.getService();
        if (creator == null) {
            throw new IOException("Can't add unreferenced views to layout; no appropriate service registered.");
        }
        creator.addUnreferencedDialogNodes(configurationStringProvider, allNodes);
    }

    /**
     * Updates a configuration layout if needed.
     *
     * @param configurationStringProvider the configuration layout provider, who's layout needs to be already expanded.
     */
    public static void updateLayout(final SubnodeContainerConfigurationStringProvider configurationStringProvider) {
        if (serviceConfigurationTracker == null) {
            throw new IllegalStateException("Core bundle is not active, can't update layout.");
        }
        DefaultConfigurationLayoutCreator creator =
            (DefaultConfigurationLayoutCreator)serviceConfigurationTracker.getService();
        if (creator == null) {
            throw new IllegalStateException("Can't update layout; no appropriate service registered.");
        }
        creator.updateConfigurationLayout(configurationStringProvider);
    }

    /**
     * Returns the order of the configuration nodes
     *
     * @param configurationStringProvider the configuration layout provider, who's layout needs to be already expanded.
     * @param configurationNodes the configuration nodes to consider
     * @param wfm the workflow manager of the subnode
     * @return returns the order of the configuration nodes as a list of strings
     */
    public static List<Integer> getConfigurationOrder(
        final SubnodeContainerConfigurationStringProvider configurationStringProvider,
        final Map<NodeID, MetaNodeDialogNode> configurationNodes, final WorkflowManager wfm) {
        if (serviceConfigurationTracker == null) {
            throw new IllegalStateException("Core bundle is not active, can't get configuration order.");
        }
        DefaultConfigurationLayoutCreator creator =
            (DefaultConfigurationLayoutCreator)serviceConfigurationTracker.getService();
        if (creator == null) {
            throw new IllegalStateException("Can't update layout; no appropriate service registered.");
        }
        return creator.getConfigurationOrder(configurationStringProvider, configurationNodes, wfm);
    }
}
