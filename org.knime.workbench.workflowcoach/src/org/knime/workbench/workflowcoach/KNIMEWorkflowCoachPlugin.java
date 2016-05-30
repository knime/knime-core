/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.workflowcoach;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class KNIMEWorkflowCoachPlugin extends AbstractUIPlugin {

    private static NodeLogger LOGGER = NodeLogger.getLogger(KNIMEWorkflowCoachPlugin.class);

    /**
     * the id of the plug-in.
     */
    public static final String PLUGIN_ID = "org.knime.workbench.workflowcoach";

    /** Preference store keys */
    public static final String P_COMMUNITY_NODE_TRIPLE_PROVIDER = "community_node_triple_provider";

    public static final String P_LAST_STATISTICS_UPDATE = "last_community_statistics_update";

    public static final String P_AUTO_UPDATE_SCHEDULE = "auto_update_schedule";

    public static final int NO_AUTO_UPDATE = 0;

    public static final int WEEKLY_UPDATE = 1;

    public static final int MONTHLY_UPDATE = 2;

    private static KNIMEWorkflowCoachPlugin plugin;

    /**
     * Creates a new activator for the explorer plugin.
     */
    public KNIMEWorkflowCoachPlugin() {
        plugin = this;
    }

    /**
     * Returns the shared instance.
     *
     * @return Singleton instance of the Explorer Plugin
     */
    public static KNIMEWorkflowCoachPlugin getDefault() {
        return plugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        super.stop(bundleContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPreferenceStore getPreferenceStore() {
        IPreferenceStore prefStore = super.getPreferenceStore();
        return prefStore;
    }

    public void recommendationsUpdated() {
        //store the todays date of the update
        DateTimeFormatter f = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
        String d = f.format(LocalDateTime.now());
        IEclipsePreferences prefs =
                InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
        prefs.put(P_LAST_STATISTICS_UPDATE, d);
    }
}
