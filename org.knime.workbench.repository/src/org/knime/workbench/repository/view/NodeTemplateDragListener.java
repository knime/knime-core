/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 *
 * History
 *   12.01.2005 (Flo): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 * Drag listener for dragging NodeTemplates out of the Repository tree viewer.
 *
 * @author Florian Georg, University of Konstanz
 */
public class NodeTemplateDragListener implements DragSourceListener {

//    private final NodeLogger LOGGER = NodeLogger.getLogger(
//            NodeTemplateDragListener.class);

    private final TreeViewer m_viewer;

    /**
     * @param viewer The viewer to add drag support to
     */
    public NodeTemplateDragListener(final TreeViewer viewer) {
        m_viewer = viewer;
    }

    /**
     * Only start the drag, if there's exactly one NodeTemplate selected in the
     * viewer.
     *
     * @see org.eclipse.swt.dnd.DragSourceListener
     *      #dragStart(org.eclipse.swt.dnd.DragSourceEvent)
     */
    public void dragStart(final DragSourceEvent event) {
        IStructuredSelection sel = (IStructuredSelection)m_viewer
                .getSelection();
        event.detail = DND.DROP_COPY;
        event.data = sel.getFirstElement();
        event.widget = m_viewer.getTree();
        LocalSelectionTransfer.getTransfer().setSelection(sel);
        // cancel event, if not an NodeTemplate, or not exactly one element
        // selected
        if (!(sel.getFirstElement() instanceof NodeTemplate
                || sel.getFirstElement() instanceof MetaNodeTemplate)
                || (sel.size() != 1)) {
            event.doit = false;
        } else {
            event.doit = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dragSetData(final DragSourceEvent event) {
    }

    /**
     * {@inheritDoc}
     */
    public void dragFinished(final DragSourceEvent event) {
    }
}
