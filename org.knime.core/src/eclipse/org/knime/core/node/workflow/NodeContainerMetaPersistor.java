/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

interface NodeContainerMetaPersistor {

    /** Key for this node's custom description. */
    static final String KEY_CUSTOM_DESCRIPTION = "customDescription";

    /** Key for this node's user name. */
    static final String KEY_CUSTOM_NAME = "customName";

    /**
     * @return the load helper as set during load (not part of the workflow being loaded but a setting during load).
     */
    WorkflowLoadHelper getLoadHelper();

    /**
     * File reference to the node container directory. Something like &lt;workflow_space&gt;/File Reader (#xy). This
     * value is non-null when (i) loading from disk or (ii) if pasted into a workflow as part of an undo of a delete
     * command. It's null if node is copied&amp;pasted. If the value is non-null the referenced file will be removed
     * from the list of obsolete node directories (must not clear directories as they may contain a "drop" folder).
     *
     * @return The node container dir or <code>null</code>
     */
    ReferencedFile getNodeContainerDirectory();

    int getNodeIDSuffix();

    void setNodeIDSuffix(final int nodeIDSuffix);

    NodeAnnotationData getNodeAnnotationData();

    String getCustomDescription();

    /**
     * For instance, a special job manager for streamed node execution.
     * @return null for the default node execution job manager
     */
    NodeExecutionJobManager getExecutionJobManager();

    NodeSettingsRO getExecutionJobSettings();

    InternalNodeContainerState getState();

    NodeUIInformation getUIInfo();

    NodeMessage getNodeMessage();

    // TODO Res or Def?
    NodeLocks getNodeLocks();

    boolean isDirtyAfterLoad();

    void setUIInfo(final NodeUIInformation uiInfo);

    /**
     * Load content, gets both the current settings (first argument) and the "parent settings", which are only used in
     * 1.3.x flows and will be ignored in any version after that.
     *
     * @param settings The settings object that is usually read from
     * @param parentSettings The parent settings, mostly ignored.
     * @param loadResult Where to add errors and warnings to.
     * @return Whether errors occurred that require a reset of the node.
     */
    boolean load(final NodeSettingsRO settings, final NodeSettingsRO parentSettings, final LoadResult loadResult);

}
