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
 *   Mar 15, 2016 (hornm): created
 */
package org.knime.workbench.workflowcoach.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.FrameworkUtil;

/**
 * Intializer for the general workflow coach preferences.
 *
 * @author Martin Horn, University of Konstanz
 */
public class WorkflowCoachPreferenceInitializer extends AbstractPreferenceInitializer {
    /**
     * Constant for monthly updates.
     */
    public static final int MONTHLY_UPDATE = 2;
    /**
     * Constant for weekly updates.
     */
    public static final int WEEKLY_UPDATE = 1;
    /**
     * Constant for no updates.
     */
    public static final int NO_AUTO_UPDATE = 0;

    /**
     * Preference key for the automatic update schedule.
     */
    public static final String P_AUTO_UPDATE_SCHEDULE = "auto_update_schedule";

    /**
     * Preference store key for the community recommendations provider.
     */
    public static final String P_COMMUNITY_NODE_TRIPLE_PROVIDER = "community_node_triple_provider";

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        IEclipsePreferences prefs =
            DefaultScope.INSTANCE.getNode(FrameworkUtil.getBundle(getClass()).getSymbolicName());
        //disable the community recommendations by default (because the 'send_statistics'-property is disabled by default, too)
        prefs.putBoolean(P_COMMUNITY_NODE_TRIPLE_PROVIDER, false);
        prefs.putInt(P_AUTO_UPDATE_SCHEDULE, MONTHLY_UPDATE);
    }
}
