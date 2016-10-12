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
import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.ui.PlatformUI;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.CreateDropRequest;
import org.knime.workbench.editor2.CreateDropRequest.RequestType;
import org.knime.workbench.editor2.ReaderNodeSettings;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.actions.CreateSpaceAction;
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

    public static final String REQ_LINK_METANODE_TEMPLATE = "link metanode template";

    public static final String REQ_COPY_METANODE_TEMPLATE = "copy metanode template";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Command getCreateCommand(final CreateRequest request) {
        // point where the command occurred
        // The node/description should be initially located here
        boolean snapToGrid = WorkflowEditor.getActiveEditorSnapToGrid();


        WorkflowRootEditPart workflowPart = (WorkflowRootEditPart)this.getHost();
        IWorkflowManager manager = workflowPart.getWorkflowManager();

        if (request instanceof CreateDropRequest) {
            Object obj = request.getNewObject();
            CreateDropRequest cdr = (CreateDropRequest)request;
            if (obj instanceof NodeFactory) {
                return handleNodeDrop(manager, (NodeFactory<? extends NodeModel>)obj, cdr);
            } else if (obj instanceof AbstractExplorerFileStore) {
                AbstractExplorerFileStore fs = (AbstractExplorerFileStore)obj;
                if (AbstractExplorerFileStore.isWorkflowTemplate(fs)) {
                    return handleMetaNodeTemplateDrop(manager, cdr, fs);
                }
            } else if (obj instanceof WorkflowPersistor) {
                return handleMetaNodeDrop(manager, (WorkflowPersistor)obj, cdr);
            } else if (obj instanceof ReaderNodeSettings) {
                    ReaderNodeSettings settings = (ReaderNodeSettings)obj;
                    return new DropNodeCommand(manager, settings.getFactory(), new NodeCreationContext(settings.getUrl()),
                        request.getLocation(), snapToGrid);
            }else {
                LOGGER.error("Illegal drop object: " + obj);
            }
        }
        return null;
    }

    /**
     * @param manager the workflow manager
     * @param content the metanode content
     * @param request the drop request
     */
    private Command handleMetaNodeDrop(final IWorkflowManager manager, final WorkflowPersistor content, final CreateDropRequest request) {
        Point location = request.getLocation();
        if (request.getRequestType().equals(RequestType.CREATE)) {
            // create metanode from node repository
            return new CreateMetaNodeCommand(manager, content, location, WorkflowEditor.getActiveEditorSnapToGrid());
        } else {
            AbstractEditPart editPart = request.getEditPart();
            if (request.getRequestType().equals(RequestType.INSERT)) {
                // insert metanode from node repository into connection
                InsertMetaNodeCommand insertCommand =
                    new InsertMetaNodeCommand(manager, content, location, WorkflowEditor.getActiveEditorSnapToGrid(),
                        (ConnectionContainerEditPart)editPart);

                if (request.createSpace()) {
                    CreateSpaceAction csa =
                        new CreateSpaceAction((WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage().getActiveEditor(), request.getDirection(), request.getDistance());
                    return insertCommand.chain(csa.createCompoundCommand(csa.selectedParts()));
                } else {
                    return insertCommand;
                }
            } else if (request.getRequestType().equals(RequestType.REPLACE)) {
                // replace node with metanode from repository
                return new ReplaceMetaNodeCommand(manager, content, location, WorkflowEditor.getActiveEditorSnapToGrid(),
                    (NodeContainerEditPart)editPart);
            } else {
                return null;
            }
        }
    }

    /**
     * @param manager the workflow manager
     * @param request the drop request
     * @param filestore the location of the metanode template
     */
    private Command handleMetaNodeTemplateDrop(final IWorkflowManager manager,
        final CreateDropRequest request, final AbstractExplorerFileStore filestore) {
            RequestType requestType = request.getRequestType();
            Point location = request.getLocation();
            boolean snapToGrid = WorkflowEditor.getActiveEditorSnapToGrid();
            if (requestType.equals(RequestType.CREATE)) {
                // create metanode from template
                return new CreateMetaNodeTemplateCommand(manager, filestore, location, snapToGrid);
            } else {
                AbstractEditPart editPart = request.getEditPart();
                if (requestType.equals(RequestType.INSERT)) {
                    // insert metanode from template into connection
                    InsertMetaNodeTempalteCommand insertCommand =
                            new InsertMetaNodeTempalteCommand(manager, filestore, location, snapToGrid,
                                (ConnectionContainerEditPart)editPart);

                        if (request.createSpace()) {
                            CreateSpaceAction csa =
                                new CreateSpaceAction((WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                    .getActivePage().getActiveEditor(), request.getDirection(), request.getDistance());
                            return insertCommand.chain(csa.createCompoundCommand(csa.selectedParts()));
                        } else {
                            return insertCommand;
                        }
                }else if (requestType.equals(RequestType.REPLACE)) {
                    // replace node with metanode from template
                    return new ReplaceMetaNodeTemplateCommand(manager, filestore, location, snapToGrid,
                        (NodeContainerEditPart)editPart);
                } else {
                    return null;
                }
            }
    }

    /**
     * @param manager the workflow manager
     * @param factory the ndoe factory
     * @param request the drop request
     */
    private Command handleNodeDrop( final IWorkflowManager manager, final NodeFactory<? extends NodeModel> factory,
        final CreateDropRequest request) {
        RequestType requestType = request.getRequestType();
        Point location = request.getLocation();
        boolean snapToGrid = WorkflowEditor.getActiveEditorSnapToGrid();
        if (requestType.equals(RequestType.CREATE)) {
            // create a new node
            return new CreateNodeCommand(manager, factory, location, snapToGrid);
        } else {
            AbstractEditPart editPart = request.getEditPart();
            if (requestType.equals(RequestType.INSERT)) {
                // insert new node into connection
                InsertNodeCommand insertCommand =
                        new InsertNodeCommand(manager, factory, location, snapToGrid,
                            (ConnectionContainerEditPart)editPart);

                if (request.createSpace()) {
                    CreateSpaceAction csa =
                            new CreateSpaceAction((WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getActivePage().getActiveEditor(), request.getDirection(), request.getDistance());
                    return insertCommand.chain(csa.createCompoundCommand(csa.selectedParts()));
                } else {
                    return insertCommand;
                }
            } else if (requestType.equals(RequestType.REPLACE)) {
                // replace node with a node
                return new ReplaceNodeCommand(manager, factory, location, snapToGrid,
                    (NodeContainerEditPart)editPart);
            } else {
                return null;
            }
        }
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
