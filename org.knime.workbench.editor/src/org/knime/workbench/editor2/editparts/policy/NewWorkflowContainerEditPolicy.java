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
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts.policy;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ReaderNodeSettings;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.CreateMetaNodeTemplateCommand;
import org.knime.workbench.editor2.commands.CreateNodeCommand;
import org.knime.workbench.editor2.commands.DropNodeCommand;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;

/**
 * Container policy, handles the creation of new nodes that are inserted into
 * the workflow. The request contains the
 *
 * @author Florian Georg, University of Konstanz
 */
public class NewWorkflowContainerEditPolicy extends ContainerEditPolicy {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NewWorkflowContainerEditPolicy.class);

    public static final String REQ_LINK_METANODE_TEMPLATE =
        "link meta node template";

    public static final String REQ_COPY_METANODE_TEMPLATE =
        "copy meta node template";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Command getCreateCommand(final CreateRequest request) {
        // point where the command occured
        // The node/description should be initially located here
        Point location = request.getLocation();

        // adapt the location according to the viewport location and the zoom
        // factor
        // this seems to be a workaround for a bug in the framework
        ZoomManager zoomManager = (ZoomManager)getTargetEditPart(request)
                .getRoot().getViewer()
                .getProperty(ZoomManager.class.toString());

        // adjust the location according to the viewport position
        // seems to be a workaround for a bug in the framework
        // (should imediately deliver the correct view position and not
        // the position of the viewport)
        WorkflowEditor.adaptZoom(zoomManager, location, true);

        WorkflowRootEditPart workflowPart =
            (WorkflowRootEditPart)this.getHost();
        WorkflowManager manager = workflowPart.getWorkflowManager();

        Object obj = request.getNewObject();
        // create a new node
        if (obj instanceof NodeFactory) {
            NodeFactory<? extends NodeModel> factory
                = (NodeFactory<? extends NodeModel>)obj;
            return new CreateNodeCommand(manager, factory, location);
        } else if (obj instanceof ExplorerFileStore) {
            ExplorerFileStore fs = (ExplorerFileStore)obj;
            if (ExplorerFileStore.isWorkflowTemplate(fs)) {
                return new CreateMetaNodeTemplateCommand(manager, fs, location);
            }
        } else if (obj instanceof ReaderNodeSettings) {
            ReaderNodeSettings settings = (ReaderNodeSettings)obj;
            return new DropNodeCommand(manager, settings.getFactory(),
                    new NodeCreationContext(settings.getUrl()), location);
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
