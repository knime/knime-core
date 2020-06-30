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
 *   11 Nov 2016 (albrecht): created
 */
package org.knime.core.node.wizard.util;

import java.io.IOException;
import java.util.Map;

import org.knime.core.node.wizard.ViewHideable;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.3
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class LayoutUtil {

    private static final ServiceTracker serviceTracker;

    /**
     * In the 4.2 release we decided to components should no longer be saved without a layout configuration. This static
     * field acts as the "magic marker" we use (in combination with other information) to identify what would have
     * (before 4.2.0), been an empty layout.
     *
     * @since 4.2
     */
    public static final String EMPTY_LAYOUT_MARKER = "LAYOUT_REQUIRED";

    static {
        Bundle coreBundle = FrameworkUtil.getBundle(LayoutUtil.class);
        if (coreBundle != null) {
            serviceTracker = new ServiceTracker(coreBundle.getBundleContext(), DefaultLayoutCreator.class.getName(), null);
            serviceTracker.open();
        } else {
            serviceTracker = null;
        }
    }

    /**
     * Creates a default layout structure as a serialized JSON string.
     * @param viewNodes the nodes to include in the layout
     * @return a default layout structure as JSON string.
     * @throws IOException If no service is registered or the default layout cannot be created.
     */
    public static String createDefaultLayout(final Map<NodeIDSuffix, ViewHideable> viewNodes) throws IOException {
        if (serviceTracker == null) {
            throw new IOException("Core bundle is not active, can't create default layout.");
        }
        DefaultLayoutCreator creator = (DefaultLayoutCreator)serviceTracker.getService();
        if (creator == null) {
            throw new IOException("Can't create default layout; no appropriate service registered.");
        }
        return creator.createDefaultLayout(viewNodes);
    }

    /**
     * Expands nested layouts by inserting the appropriate sub-layouts in an original layout.
     * @param originalLayout the original not expanded layout
     * @param wfm the {@link WorkflowManager} of the containing {@link SubNodeContainer}
     * @return The expanded layout as JSON serialized string
     * @throws IOException If no service is registered or the layout cannot be expanded.
     * @since 3.7
     */
    public static String expandNestedLayout(final String originalLayout, final WorkflowManager wfm) throws IOException {
        if (serviceTracker == null) {
            throw new IOException("Core bundle is not active, can't expand nested layout.");
        }
        DefaultLayoutCreator creator = (DefaultLayoutCreator)serviceTracker.getService();
        if (creator == null) {
            throw new IOException("Can't expand nested layout; no appropriate service registered.");
        }
        return creator.expandNestedLayout(originalLayout, wfm);
    }

    /**
     * Creates extra rows/columns at the bottom of the layout for all unreferenced nodes
     * @param originalLayout the original layout, which needs to be already expanded
     * @param allNodes a map of all viewable nodes
     * @param allNestedViews a map of all {@link SubNodeContainer} which contain nested views
     * @param containerID the {@link NodeID} of the containing subnode container
     * @return The amended layout as a JSON serialized string
     * @throws IOException If no service is registered or the layout cannot be amended.
     * @since 3.7
     */
    public static String addUnreferencedViews(final String originalLayout, final Map<NodeIDSuffix, WizardNode> allNodes,
        final Map<NodeIDSuffix, SubNodeContainer> allNestedViews, final NodeID containerID) throws IOException {
        if (serviceTracker == null) {
            throw new IOException("Core bundle is not active, can't add unreferenced views to layout.");
        }
        DefaultLayoutCreator creator = (DefaultLayoutCreator)serviceTracker.getService();
        if (creator == null) {
            throw new IOException("Can't add unreferenced views to layout; no appropriate service registered.");
        }
        return creator.addUnreferencedViews(originalLayout, allNodes, allNestedViews, containerID);
    }

    /**
     * Updates a layout if needed.
     *
     * @param currentLayout the current layout, which needs to be already expanded
     * @param originalLayout the original layout, as provided by the {@link SubNodeContainer}
     * @return The updated layout
     * @throws IOException If no service is registered or the layout cannot be updated
     * @since 4.2
     */
    public static String updateLayout(final String currentLayout, final String originalLayout) throws IOException {
        if (serviceTracker == null) {
            throw new IOException("Core bundle is not active, can't update layout.");
        }
        DefaultLayoutCreator creator = (DefaultLayoutCreator)serviceTracker.getService();
        if (creator == null) {
            throw new IOException("Can't update layout; no appropriate service registered.");
        }
        return creator.updateLayout(currentLayout, originalLayout);
    }

    /**
     * Checks a layout string representation for the presence of a "magic marker" which indicates a full layout needs to
     * be created.
     *
     * @param currentLayout to check for the "magic marker" which
     * @return if the provided layout needs to be updated.
     *
     * @since 4.2
     */
    public static boolean requiresLayout(final String currentLayout) {
        if (currentLayout == null) {
            return true;
        }
        return currentLayout.contains(EMPTY_LAYOUT_MARKER);
    }
}
