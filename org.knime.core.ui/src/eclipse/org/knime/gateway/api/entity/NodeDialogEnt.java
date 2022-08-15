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
 *  NodeDialog, and NodeDialog) and that only interoperate with KNIME through
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
 *   Oct 15, 2021 (hornm): created
 */
package org.knime.gateway.api.entity;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.webui.node.NNCWrapper;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.SNCWrapper;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.page.PageUtil.PageType;

/**
 * Node dialog entity containing the info required by the UI (i.e. frontend) to be able to display a node dialog.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeDialogEnt extends NodeUIExtensionEnt<NodeWrapper<? extends NodeContainer>> {

    private final FlowVariableSettingsEnt m_flowVariableSettings;

    /**
     * @param nc
     */
    public NodeDialogEnt(final NativeNodeContainer nc) {
        super(NNCWrapper.of(nc), NodeDialogManager.getInstance(), NodeDialogManager.getInstance(), PageType.DIALOG);
        CheckUtils.checkArgument(NodeDialogManager.hasNodeDialog(nc), "The provided node doesn't have a node dialog");
        m_flowVariableSettings = new FlowVariableSettingsEnt(nc);
    }


    /**
     * @param nc
     */
    public NodeDialogEnt(final SingleNodeContainer nc) {
        super(SNCWrapper.of(nc), NodeDialogManager.getInstance(), NodeDialogManager.getInstance(), PageType.DIALOG);
        CheckUtils.checkArgument(NodeDialogManager.hasNodeDialog(nc), "The provided node doesn't have a node dialog");
        m_flowVariableSettings = new FlowVariableSettingsEnt(nc);
    }

    /**
     * @return a representation of the flow variable settings
     */
    public FlowVariableSettingsEnt getFlowVariableSettings() {
        return m_flowVariableSettings;
    }

}
