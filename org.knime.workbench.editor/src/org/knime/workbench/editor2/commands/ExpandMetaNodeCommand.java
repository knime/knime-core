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
 *   07.05.2011 (mb): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;

/**
 *
 * @author M. Berthold, University of Konstanz
 */
public class ExpandMetaNodeCommand extends AbstractKNIMECommand {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ExpandMetaNodeCommand.class);

    private final NodeID m_id;
    private NodeID[] m_pastedNodes;
    private WorkflowAnnotation[] m_pastedAnnotations;
    private WorkflowPersistor m_undoCopyPersistor;

    /**
     * @param wfm the workflow manager holding the new metanode
     * @param id of node to be expanded.
     */
    public ExpandMetaNodeCommand(final WorkflowManager wfm, final NodeID id) {
        super(wfm);
        m_id = id;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        return getHostWFM().canExpandMetaNode(m_id) == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            WorkflowManager hostWFM = getHostWFM();
            WorkflowCopyContent cnt = new WorkflowCopyContent();
            cnt.setNodeIDs(m_id);
            cnt.setIncludeInOutConnections(true);
            m_undoCopyPersistor = hostWFM.copy(true, cnt);
            WorkflowCopyContent wcc = hostWFM.expandMetaNode(m_id);
            m_pastedNodes = wcc.getNodeIDs();
            m_pastedAnnotations = wcc.getAnnotations();
        } catch (Exception e) {
            String error = "Expanding Metanode failed: " + e.getMessage();
            LOGGER.error(error, e);
            MessageDialog.openError(Display.getCurrent().getActiveShell(),
                    "Expand failed", error);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        if (m_undoCopyPersistor != null) {
            WorkflowManager hostWFM = getHostWFM();
            for (NodeID id : m_pastedNodes) {
                if (!hostWFM.canRemoveNode(id)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        WorkflowManager hostWFM = getHostWFM();
        for (NodeID id : m_pastedNodes) {
            hostWFM.removeNode(id);
        }
        for (WorkflowAnnotation anno : m_pastedAnnotations) {
            hostWFM.removeAnnotation(anno);
        }
        hostWFM.paste(m_undoCopyPersistor);
        m_pastedNodes = null;
        m_pastedAnnotations = null;
        m_undoCopyPersistor = null;
    }

}
