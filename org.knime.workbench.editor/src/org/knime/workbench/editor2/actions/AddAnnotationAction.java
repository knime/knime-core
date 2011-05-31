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
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Point;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.AddAnnotationCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class AddAnnotationAction extends AbstractNodeAction {

    /** unique ID for this action. * */
    public static final String ID = "knime.action.addannotation";

    private static final int DEFAULT_XLOC = 20;

    private static final int DEFAULT_YLOC = 29;

    private int m_x;

    private int m_y;

    /**
     * Create a new 'Add Annotation' action.
     * @param editor underlying workflow editor
     */
    public AddAnnotationAction(final WorkflowEditor editor) {
        super(editor);
        m_x = DEFAULT_XLOC;
        m_y = DEFAULT_YLOC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean calculateEnabled() {
        return !getManager().isWriteProtected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "New Workflow Annotation";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository
        .getImageDescriptor("icons/annotation.png");
    }

    /**
     * @param xLoc
     * @param yLoc
     */
    public void setLocation(final int xLoc, final int yLoc) {
        m_x = xLoc;
        m_y = yLoc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        AddAnnotationCommand aac =
                new AddAnnotationCommand(getManager(), getEditor().getViewer(),
                        new Point(m_x, m_y));
        getCommandStack().execute(aac); // enables undo

        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }
}
