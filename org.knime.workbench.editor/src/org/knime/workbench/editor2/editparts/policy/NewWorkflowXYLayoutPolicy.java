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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2.editparts.policy;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.handles.MoveHandle;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.graphics.Cursor;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.commands.ChangeAnnotationBoundsCommand;
import org.knime.workbench.editor2.commands.ChangeNodeBoundsCommand;
import org.knime.workbench.editor2.commands.ChangeWorkflowPortBarCommand;
import org.knime.workbench.editor2.editparts.AbstractWorkflowPortBarEditPart;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeAnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * Handles manual layout editing for the workflow, that is, creates the commands
 * that can change the visual constraints of the contents.
 *
 * Only available for XYLayoutManagers, not for automatic layout
 *
 * @author Florian Georg, University of Konstanz
 */
public class NewWorkflowXYLayoutPolicy extends XYLayoutEditPolicy {
    /**
     * {@inheritDoc}
     */
    @Override
    protected Command createAddCommand(final EditPart child,
            final Object constraint) {
        return null;
    }

    /**
     * Creates command to move / resize <code>NodeContainer</code> components on
     * the project's client area.
     *
     * {@inheritDoc}
     */
    @Override
    protected Command createChangeConstraintCommand(final EditPart child,
            final Object constraint) {

        // only rectangular constraints are supported
        if (!(constraint instanceof Rectangle)) {
            return null;
        }

        Command command = null;

        Rectangle rect = ((Rectangle)constraint).getCopy();
        if (child.getModel() instanceof NodeContainer) {
            NodeContainer container = (NodeContainer)child.getModel();
            NodeContainerEditPart nodePart = (NodeContainerEditPart)child;
            command =
                    new ChangeNodeBoundsCommand(container,
                            (NodeContainerFigure)nodePart.getFigure(), rect);
        } else if (child instanceof AbstractWorkflowPortBarEditPart) {
            command =
                    new ChangeWorkflowPortBarCommand(
                            (AbstractWorkflowPortBarEditPart)child, rect);
        } else if (child instanceof AnnotationEditPart) {
            AnnotationEditPart annoPart = (AnnotationEditPart)child;
            // TODO the workflow annotation could know what its WFM is?
            WorkflowRootEditPart root =
                    (WorkflowRootEditPart)annoPart.getParent();
            WorkflowManager wm = root.getWorkflowManager();
            command = new ChangeAnnotationBoundsCommand(wm, annoPart, rect);
        }
        return command;
    }

    /**
     * @param request The request
     * @return always null
     * @see org.eclipse.gef.editpolicies.LayoutEditPolicy
     *      #getCreateCommand(org.eclipse.gef.requests.CreateRequest)
     */
    @Override
    protected Command getCreateCommand(final CreateRequest request) {
        return null;
    }

    /**
     * @param request The request
     * @return always null
     * @see org.eclipse.gef.editpolicies.LayoutEditPolicy
     *      #getDeleteDependantCommand(org.eclipse.gef.Request)
     */
    @Override
    protected Command getDeleteDependantCommand(final Request request) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EditPolicy createChildEditPolicy(final EditPart child) {
        if (child instanceof NodeContainerEditPart) {
            return new NonResizeNoHandlesEditPolicy((GraphicalEditPart)child);
        }
        if (child instanceof NodeAnnotationEditPart) {
            NonResizableEditPolicy pol =
                    new NonResizeNoHandlesEditPolicy((GraphicalEditPart)child);
            pol.setDragAllowed(false);
            return pol;
        }
        return super.createChildEditPolicy(child);
    }

    /**
     * Policy that doesn't show any handles (black squares) when the editpart
     * is selected.
     *
     * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
     */
    class NonResizeNoHandlesEditPolicy extends NonResizableEditPolicy {

        private final GraphicalEditPart m_child;

        /**
         * @param child
         *
         */
        public NonResizeNoHandlesEditPolicy(final GraphicalEditPart child) {
            m_child = child;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("rawtypes")
        protected List createSelectionHandles() {
            if (isDragAllowed()) {
                return Collections.singletonList(new MoveHandle(m_child));
            } else {
                return Collections.singletonList(new Handle(m_child));
            }

        }

        /**
         * Handle that doesn't change the cursor.
         *
         * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
         */
        class Handle extends MoveHandle {

            /**
             * @param owner the editpart this handle is shown on
             */
            Handle(final GraphicalEditPart owner) {
                super(owner);
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public Cursor getCursor() {
                return getParent().getCursor();
            }
        }
    }
}
