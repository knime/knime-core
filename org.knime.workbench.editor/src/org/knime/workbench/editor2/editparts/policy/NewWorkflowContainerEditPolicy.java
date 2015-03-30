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
 * -------------------------------------------------------------------
 *
 * History
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts.policy;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.CreateDropRequest;
import org.knime.workbench.editor2.CreateDropRequest.RequestType;
import org.knime.workbench.editor2.ReaderNodeSettings;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.actions.CreateSpaceAction;
import org.knime.workbench.editor2.actions.CreateSpaceAction.CreateSpaceDirection;
import org.knime.workbench.editor2.commands.CreateMetaNodeCommand;
import org.knime.workbench.editor2.commands.CreateMetaNodeTemplateCommand;
import org.knime.workbench.editor2.commands.CreateNodeCommand;
import org.knime.workbench.editor2.commands.DropNodeCommand;
import org.knime.workbench.editor2.commands.InsertMetaNodeCommand;
import org.knime.workbench.editor2.commands.InsertMetaNodeTempalteCommand;
import org.knime.workbench.editor2.commands.InsertNodeCommand;
import org.knime.workbench.editor2.commands.ReplaceMetaNodeCommand;
import org.knime.workbench.editor2.commands.ReplaceMetaNodeTemplateCommand;
import org.knime.workbench.editor2.commands.ReplaceNodeCommand;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * Container policy, handles the creation of new nodes that are inserted into the workflow. The request contains the
 *
 * @author Florian Georg, University of Konstanz
 * @author Tim-Oliver Buchholz, KNIME.com AG, Zurich, Switzerland
 */
public class NewWorkflowContainerEditPolicy extends ContainerEditPolicy {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NewWorkflowContainerEditPolicy.class);

    public static final String REQ_LINK_METANODE_TEMPLATE = "link meta node template";

    public static final String REQ_COPY_METANODE_TEMPLATE = "copy meta node template";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Command getCreateCommand(final CreateRequest request) {
        // point where the command occurred
        // The node/description should be initially located here
        Point location = request.getLocation();


        WorkflowRootEditPart workflowPart = (WorkflowRootEditPart)this.getHost();
        WorkflowManager manager = workflowPart.getWorkflowManager();

        RequestType requestType = null;
        EditPart editPart = null;
        boolean createSpace = false;
        CreateSpaceDirection direction = null;
        int distance = 0;
        if (request instanceof CreateDropRequest) {
            CreateDropRequest cdr = (CreateDropRequest)request;
            requestType = cdr.getRequestType();
            editPart = cdr.getEditPart();
            createSpace = cdr.createSpace();
            direction = cdr.getDirection();
            distance = cdr.getDistance();
        }

        Object obj = request.getNewObject();
        boolean snapToGrid = WorkflowEditor.getActiveEditorSnapToGrid();
        if (obj instanceof NodeFactory) {
            NodeFactory<? extends NodeModel> factory = (NodeFactory<? extends NodeModel>)obj;
            if (requestType.equals(RequestType.CREATE)) {
                // create a new node
                return new CreateNodeCommand(manager, factory, location, snapToGrid);
            } else if (requestType.equals(RequestType.INSERT)) {
                // insert new node into connection
                InsertNodeCommand insertCommand =
                    new InsertNodeCommand(manager, factory, location, WorkflowEditor.getActiveEditorSnapToGrid(),
                        (ConnectionContainerEditPart)editPart);

                if (createSpace) {
                    CreateSpaceAction csa =
                        new CreateSpaceAction((WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage().getActiveEditor(), direction, distance);
                    return insertCommand.chain(csa.createCompoundCommand(csa.selectedParts()));
                } else {
                    return insertCommand;
                }
            } else if (requestType.equals(RequestType.REPLACE)) {
                // replace node with a node
                return new ReplaceNodeCommand(manager, factory, location, WorkflowEditor.getActiveEditorSnapToGrid(),
                    (NodeContainerEditPart)editPart);
            }
        } else if (obj instanceof AbstractExplorerFileStore) {
            // i
            AbstractExplorerFileStore fs = (AbstractExplorerFileStore)obj;
            if (AbstractExplorerFileStore.isWorkflowTemplate(fs)) {
                if (requestType.equals(RequestType.CREATE)) {
                    // create meta node from template
                    return new CreateMetaNodeTemplateCommand(manager, fs, location, snapToGrid);
                } else if (requestType.equals(RequestType.INSERT)) {
                    // insert meta node from template into connection
                    InsertMetaNodeTempalteCommand insertCommand =
                            new InsertMetaNodeTempalteCommand(manager, fs, location, WorkflowEditor.getActiveEditorSnapToGrid(),
                                (ConnectionContainerEditPart)editPart);

                        if (createSpace) {
                            CreateSpaceAction csa =
                                new CreateSpaceAction((WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                    .getActivePage().getActiveEditor(), direction, distance);
                            return insertCommand.chain(csa.createCompoundCommand(csa.selectedParts()));
                        } else {
                            return insertCommand;
                        }
                }else if (requestType.equals(RequestType.REPLACE)) {
                    // replace node with meta node from template
                    return new ReplaceMetaNodeTemplateCommand(manager, fs, location, snapToGrid,
                        (NodeContainerEditPart)editPart);
                }
            }
        } else if (obj instanceof ReaderNodeSettings) {
            ReaderNodeSettings settings = (ReaderNodeSettings)obj;
            return new DropNodeCommand(manager, settings.getFactory(), new NodeCreationContext(settings.getUrl()),
                location, snapToGrid);
        } else if (obj instanceof WorkflowPersistor) {
            WorkflowPersistor copy = (WorkflowPersistor)obj;

            if (requestType.equals(RequestType.CREATE)) {
                // create meta node from node repository
                return new CreateMetaNodeCommand(manager, copy, location, WorkflowEditor.getActiveEditorSnapToGrid());
            } else if (requestType.equals(RequestType.INSERT)) {
                // insert meta node from node repository into connection
                InsertMetaNodeCommand insertCommand =
                    new InsertMetaNodeCommand(manager, copy, location, WorkflowEditor.getActiveEditorSnapToGrid(),
                        (ConnectionContainerEditPart)editPart);

                if (createSpace) {
                    CreateSpaceAction csa =
                        new CreateSpaceAction((WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage().getActiveEditor(), direction, distance);
                    return insertCommand.chain(csa.createCompoundCommand(csa.selectedParts()));
                } else {
                    return insertCommand;
                }
            } else if (requestType.equals(RequestType.REPLACE)) {
                // replace node with meta node from repository
                return new ReplaceMetaNodeCommand(manager, copy, location, WorkflowEditor.getActiveEditorSnapToGrid(),
                    (NodeContainerEditPart)editPart);
            }
        } else {
            LOGGER.error("Illegal drop object: " + obj);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EditPart getTargetEditPart(final Request request) {
        if (REQ_CREATE.equals(request.getType())) {
            return getHost();
        }
        if (REQ_ADD.equals(request.getType())) {
            return getHost();
        }
        if (REQ_MOVE.equals(request.getType())) {
            return getHost();
        }
        if (REQ_COPY_METANODE_TEMPLATE.equals(request.getType())) {
            return getHost();
        }
        if (REQ_LINK_METANODE_TEMPLATE.equals(request.getType())) {
            return getHost();
        }
        return super.getTargetEditPart(request);
    }
}
