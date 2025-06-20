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
 *   Jun 20, 2025 (lw): created
 */
package org.knime.core.node.workflow;

/**
 * All constants used as JSON field keys in the {@link NodeTimer},
 * written to the file {@value NodeTimer.GlobalNodeStats#FILENAME}.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
final class NodeTimerConstants {

    static final String VERSION = "version";

    static final String CREATED_AT = "created";

    static final String NODE_STATS = "nodestats";

    static final String NODE_STATS_NODES = "nodes";

    static final String NODE_STATS_METANODES = "metaNodes";

    static final String NODE_STATS_COMPONENTS = "wrappedNodes";

    static final String NODE_CREATED_VIA = "createdVia";

    // -- SESSION-BASED NODE STATISTICS --

    static final String ID = "id";

    static final String NODE_NAME = "nodename";

    static final String NR_EXECS = "nrexecs";

    static final String NR_FAILS = "nrfails";

    static final String EXEC_TIME = "exectime";

    static final String NR_CREATED = "nrcreated";

    static final String NR_SETTINGS_CHANGED = "nrsettingsChanged";

    static final String SUCCESSOR_FACTORY = "successor";

    static final String SUCCESSOR_NAME = "successornodename";

    // -- INCREMENTAL, GLOBAL STATISTICS --

    static final String UP_TIME = "uptime";

    static final String LOCAL_WFS_OPENED = "workflowsOpened";

    static final String REMOTE_WFS_OPENED = "remoteWorkflowsOpened";

    static final String LOCAL_WFS_CREATED = "workflowsCreated";

    static final String REMOTE_WFS_CREATED = "remoteWorkflowsCreated";

    static final String COLUMNAR_WFS_OPENED = "columnarStorageWorkflowsOpened";

    static final String WFS_IMPORTED = "workflowsImported";

    static final String WFS_EXPORTED = "workflowsExported";

    static final String NR_MUI_PERSPECTIVE = "webUIPerspectiveSwitches";

    static final String NR_CUI_PERSPECTIVE = "javaUIPerspectiveSwitches";

    static final String LAST_USED_PERSPECTIVE = "lastUsedPerspective";

    static final String NR_LAUNCHES = "launches";

    static final String NR_CRASHES = "crashes";

    static final String LAST_APPLICATION_ID = "lastApplicationID";

    static final String LAST_START_TIME = "timeSinceLastStart";

    static final String WAS_PROPER_SHUTDOWN = "properlyShutDown";

    /**
     * Only a utility class.
     */
    private NodeTimerConstants() {
    }

}
