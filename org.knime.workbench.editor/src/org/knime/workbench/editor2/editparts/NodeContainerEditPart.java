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
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartListener;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.JobManagerChangedEvent;
import org.knime.core.node.workflow.JobManagerChangedListener;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.core.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowManagerInput;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.directnodeedit.NodeEditManager;
import org.knime.workbench.editor2.directnodeedit.UserNodeNameCellEditorLocator;
import org.knime.workbench.editor2.directnodeedit.UserNodeNameDirectEditPolicy;
import org.knime.workbench.editor2.editparts.policy.PortGraphicalRoleEditPolicy;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
import org.knime.workbench.editor2.figures.ProgressFigure;
import org.knime.workbench.ui.SyncExecQueueDispatcher;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 * Edit part for node containers. This also listens to interesting events, like
 * changed extra infos or execution states
 * Model: {@link NodeContainer}
 * View: {@link NodeContainerFigure}
 * Controller: {@link NodeContainerEditPart}
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class NodeContainerEditPart extends AbstractWorkflowEditPart implements
        NodeStateChangeListener, NodeProgressListener, NodeMessageListener,
        NodeUIInformationListener, EditPartListener, ConnectableEditPart,
        JobManagerChangedListener {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(NodeContainerEditPart.class);

    /**
     * true, if the figure was initialized from the node extra info object.
     */
    private boolean m_figureInitialized;

    /**
     * The manager for the direct editing of the node name.
     */
    private NodeEditManager m_directEditManager;

    private boolean m_showFlowVarPorts = false;

    /**
     * @return The <code>NodeContainer</code>(= model)
     */
    @Override
    public NodeContainer getNodeContainer() {
        return (NodeContainer)getModel();
    }

    /**
     * Returns the parent WFM.
     *
     * @return The hosting WFM
     */
    public WorkflowManager getWorkflowManager() {
        return (WorkflowManager)getParent().getModel();
    }

    public boolean getShowImplFlowVarPorts() {
        return m_showFlowVarPorts;
    }

    public void setShowImplFlowVarPorts(final boolean showEm) {
        m_showFlowVarPorts = showEm;
        ((NodeContainerFigure)getFigure()).setShowFlowVarPorts(showEm);
        getFigure().repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate() {
        super.activate();

        // listen to node container (= model object)
        getNodeContainer().addNodeStateChangeListener(this);
        getNodeContainer().addNodeMessageListener(this);
        getNodeContainer().addProgressListener(this);
        getNodeContainer().addUIInformationListener(this);
        getNodeContainer().addJobManagerChangedListener(this);
        addEditPartListener(this);

        // If we already have extra info, init figure now
        //
        //
        if (getNodeContainer().getUIInformation() != null) {
            initFigureFromUIInfo((NodeUIInformation)getNodeContainer()
                    .getUIInformation());
        } else {
            // set the initial settings to the figure on the next "stateChanged"
            // event.
            m_figureInitialized = false;
            // create default extra info and set it
            // NOTE: This is done for nodes that are created from code
            // e.g. cross validation node creates a partitioner and
            // has no knowledge about a extrainfo
            NodeUIInformation info = new NodeUIInformation();
            info.setNodeLocation(0, 0, -1, -1);
            getNodeContainer().setUIInformation(info);
        }
        NodeContainer cont = getNodeContainer();
        if (cont != null && cont.findJobManager() != null) {
            URL iconURL = getNodeContainer().findJobManager().getIcon();
            setJobManagerIcon(iconURL);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deactivate() {
        super.deactivate();
        NodeContainer nc = getNodeContainer();
        nc.removeNodeStateChangeListener(this);
        nc.removeNodeMessageListener(this);
        nc.removeNodeProgressListener(this);
        nc.removeUIInformationListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {

        // create the visuals for the node container.
        final NodeContainerFigure nodeFigure =
                new NodeContainerFigure(new ProgressFigure());
        // init the user specified node name
        nodeFigure.setCustomName(getCustomName());
        return nodeFigure;
    }

    /**
     * Enable direct edit: edit custom name.
     */
    public void performDirectEdit() {
        if (m_directEditManager == null) {
            m_directEditManager =
                    new NodeEditManager(this,
                            new UserNodeNameCellEditorLocator(
                                    (NodeContainerFigure)getFigure()));
        }
        m_directEditManager.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performRequest(final Request request) {
        if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) {
            performDirectEdit();
        } else if (request.getType() == RequestConstants.REQ_OPEN) {
            // caused by a double click on this edit part
            openDialog();
        }
    }

    /**
     * Installs the COMPONENT_ROLE for this edit part.
     *
     * {@inheritDoc}
     */
    @Override
    protected void createEditPolicies() {

        // This policy provides create/reconnect commands for connections that
        // are associated with ports of this node
        this.installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
                new PortGraphicalRoleEditPolicy());

        // Installs the edit policy to directly edit the user node name
        // inside the node figture (by a CellEditor)
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE,
                new UserNodeNameDirectEditPolicy());
    }

    /**
     * Returns the model children (= the ports) of the
     * <code>NodeContainer</code> managed by this edit part. Note that in/out
     * ports are handled the same.
     *
     * @see org.eclipse.gef.editparts.AbstractEditPart#getModelChildren()
     */
    @Override
    protected List<NodePort> getModelChildren() {
        ArrayList<NodePort> ports = new ArrayList<NodePort>();
        NodeContainer container = getNodeContainer();

        for (int i = 0; i < container.getNrInPorts(); i++) {
            ports.add(container.getInPort(i));
        }
        for (int i = 0; i < container.getNrOutPorts(); i++) {
            ports.add(container.getOutPort(i));
        }
        return ports;
    }

    /**
     * Refreshes the visuals for this node representation.
     * {@inheritDoc}
     */
    @Override
        protected void refreshVisuals() {
        super.refreshVisuals();
    }

    /** {@inheritDoc} */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            @Override
            public void run() {
                NodeContainerFigure fig = (NodeContainerFigure)getFigure();
                fig.setState(state.getState());
                updateNodeStatus();

                // reset the tooltip text of the outports
                for (Object part : getChildren()) {

                    if (part instanceof NodeOutPortEditPart
                            || part instanceof WorkflowInPortEditPart
                            || part instanceof MetaNodeOutPortEditPart) {
                        AbstractPortEditPart outPortPart =
                                (AbstractPortEditPart)part;
                        outPortPart.rebuildTooltip();
                    }
                }
                // always refresh visuals
                refreshVisuals();
            }
        });

    }

    /** {@inheritDoc} */
    @Override
    public void progressChanged(final NodeProgressEvent pe) {
        // forward the new progress to our progress figure
        ((NodeContainerFigure)getFigure()).getProgressFigure().progressChanged(
                pe.getNodeProgress());
    }

    /** {@inheritDoc} */
    @Override
    public void messageChanged(final NodeMessageEvent messageEvent) {
        //
        // As this code updates the UI it must be executed in the UI thread.
        //
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            @Override
            public void run() {
                NodeContainerFigure fig = (NodeContainerFigure)getFigure();
                fig.setMessage(messageEvent.getMessage());
                updateNodeStatus();
                // always refresh visuals
                refreshVisuals();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {

        //
        // As this code updates the UI it must be executed in the UI thread.
        //
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                NodeContainerFigure fig = (NodeContainerFigure)getFigure();

                    // case NodeContainer.EVENT_EXTRAINFO_CHANGED:
                    LOGGER.debug("ExtraInfo changed, "
                            + "updating bounds and visuals...");

                    WorkflowRootEditPart parent = null;
                    parent = (WorkflowRootEditPart)getParent();

                    // provide some info from the extra info object to the
                    // figure
                    NodeUIInformation ei = (NodeUIInformation)
                        getNodeContainer().getUIInformation();

                    //
                    // if not already initialized, do this now.
                    // Necessary 'cause the extra info is only available after
                    // the node
                    // has been added to the WFM via the CreateNodeCommand
                    // All of this data is static and should not change after
                    // creation
                    // (icon, type)
                    if (!m_figureInitialized) {
                        initFigureFromUIInfo(ei);
                        m_figureInitialized = true;
                    }
                    // set the new constraints in the parent edit part
                    int[] bounds = ei.getBounds();
                    parent.setLayoutConstraint(NodeContainerEditPart.this,
                            getFigure(), new Rectangle(bounds[0], bounds[1],
                                    bounds[2], bounds[3]));

                    // check status of node
                    updateNodeStatus();
                    fig.setCustomName(getCustomName());
                    fig.setCustomDescription(evt.getDescription());
                updateNodeStatus();

                // reset the tooltip text of the outports
                for (Object part : getChildren()) {

                    if (part instanceof NodeOutPortEditPart) {
                        NodeOutPortEditPart outPortPart =
                                (NodeOutPortEditPart)part;
                        outPortPart.rebuildTooltip();
                    }
                }

                // always refresh visuals
                refreshVisuals();

            }

        });

    }

    protected String getCustomName() {
        String customName = getNodeContainer().getCustomName();
        if (customName == null) {
            customName = "Node " + getNodeContainer().getID().getIndex();
        }
        return customName;
    }

    /**
     * Initializes the figure with data from the node ui info object. This
     * must be done only once, but after the node has been added to the WFM
     * (otherwise the ui info object is not available).
     *
     * @param uiInfo UI info to provide to the figure
     */
    private void initFigureFromUIInfo(final NodeUIInformation uiInfo) {

        LOGGER.debug("Initializing figure from NodeExtraInfo..");

        /*
         * If the figure wasn't yet initialized (has relative coords)
         * convert from absolute to take scrolling into account
         */
        NodeContainerFigure f = (NodeContainerFigure)getFigure();
        int[] b = uiInfo.getBounds(); // this is a copy
        if (!uiInfo.hasAbsoluteCoordinates()) { // make it absolute coordinates
            Point p = new Point(b[0], b[1]);
            f.translateToRelative(p);
            b[0] = p.x;
            b[1] = p.y;
            NodeUIInformation newUI = new NodeUIInformation(
                    b[0], b[1], b[2], b[3], true);
            getNodeContainer().setUIInformation(newUI);
        }
        f.setBounds(new Rectangle(b[0], b[1], b[2], b[3]));
        m_figureInitialized = true;

        // String plugin = ei.getPluginID();
        // String iconPath = ei.getIconPath();
        NodeType type = getNodeContainer().getType();
        String name = getNodeContainer().getName();
        String description = getNodeContainer().getCustomDescription();

        // get the icon
        Image icon = ImageRepository.getScaledImage(
                getNodeContainer().getIcon(), 16, 16);
        // get default image if null
        if (icon == null) {
            icon =
                    ImageRepository.getScaledImage(
                            NodeFactory.getDefaultIcon(), 16, 16);
        }
        if (icon != null) {
            f.setIcon(icon);
        }
        f.setType(type);
        f.setLabelText(name);
        f.setCustomName(getCustomName());
        f.setCustomDescription(description);

        // TODO FIXME construct initial state here (after loading) - this should
        // be made nicer
        f.setState(getNodeContainer().getState());
        updateNodeStatus();
    }

    /**
     * Checks the status of the this node and if there is a message in the
     * <code>NodeStatus</code> object the messsage is set. Otherwise the
     * currently displayed message is removed.
     */
    private void updateNodeStatus() {
        NodeContainerFigure containerFigure = (NodeContainerFigure)getFigure();
        NodeMessage nodeMessage = getNodeContainer().getNodeMessage();
        if (nodeMessage != null) {
            containerFigure.setMessage(nodeMessage);
        }
    }

    /**
     * Marks this node parts figure. Used to hilite it from the rest of the
     * parts.
     *
     * @see NodeContainerEditPart#unmark()
     */
    public void mark() {
        ((NodeContainerFigure)getFigure()).mark();
    }

    /**
     * Resets the marked part.
     *
     * @see NodeContainerEditPart#mark()
     */
    public void unmark() {

        ((NodeContainerFigure)getFigure()).unmark();
    }

    /**
     * Overridden to return a custom <code>DragTracker</code> for
     * NodeContainerEditParts.
     *
     * @see org.eclipse.gef.EditPart#getDragTracker(Request)
     */
    @Override
    public DragTracker getDragTracker(final Request request) {
        return new WorkflowSelectionDragEditPartsTracker(this);
    }

    /**
     * @return all outgoing connections of this node part
     */
    public ConnectionContainerEditPart[] getOutgoingConnections() {

        Vector<ConnectionContainerEditPart> result =
                new Vector<ConnectionContainerEditPart>();

        for (Object part : getChildren()) {

            if (part instanceof NodeOutPortEditPart) {
                result.addAll(((AbstractPortEditPart)part)
                        .getSourceConnections());
            }
        }

        return result.toArray(new ConnectionContainerEditPart[result.size()]);
    }

    /**
     * @return all incoming connections of this node part
     */
    public ConnectionContainerEditPart[] getIncomingConnections() {

        Vector<ConnectionContainerEditPart> result =
                new Vector<ConnectionContainerEditPart>();

        for (Object part : getChildren()) {

            if (part instanceof NodeInPortEditPart) {
                result
                        .addAll(((NodeInPortEditPart)part)
                                .getTargetConnections());
            }
        }

        return result.toArray(new ConnectionContainerEditPart[result.size()]);
    }

    /**
     * @return all connections of this node part
     */
    public ConnectionContainerEditPart[] getAllConnections() {

        ConnectionContainerEditPart[] out = getOutgoingConnections();
        ConnectionContainerEditPart[] in = getIncomingConnections();

        ConnectionContainerEditPart[] result =
                new ConnectionContainerEditPart[out.length + in.length];
        System.arraycopy(in, 0, result, 0, in.length);
        System.arraycopy(out, 0, result, in.length, out.length);

        return result;
    }

    /**
     * Opens the node dialog on double click.
     *
     */
    public void openDialog() {
        NodeContainer container = (NodeContainer)getModel();

        if (container instanceof WorkflowManager) {
            openSubWorkflowEditor();
            return;
        }
        // if this node does not have a dialog
        if (!container.hasDialog()) {

            LOGGER.debug(container.getName()
                    + ": Opening node dialog after double "
                    + "click not possible");
            return;
        }

        LOGGER.debug(container.getName()
                + ": Opening node dialog after double click...");

        //
        // This is embedded in a special JFace wrapper dialog
        //
        try {
            WrappedNodeDialog dlg =
                    new WrappedNodeDialog(
                            Display.getCurrent().getActiveShell(), container);
            dlg.open();
        } catch (NotConfigurableException ex) {
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_WARNING | SWT.OK);
            mb.setText("Dialog cannot be opened");
            mb.setMessage("The dialog cannot be opened for the following"
                    + " reason:\n" + ex.getMessage());
            mb.open();
        } catch (Throwable t) {
            LOGGER.error("The dialog pane for node '"
                    + container.getNameWithID() + "' has thrown a '"
                    + t.getClass().getSimpleName()
                    + "'. That is most likely an implementation error.", t);
        }

        }

    public void openSubWorkflowEditor() {

        // open new editor for subworkflow
        LOGGER.debug("opening new editor for sub-workflow");
        try {
            NodeContainer container = (NodeContainer)getModel();
            final WorkflowEditor parent = (WorkflowEditor)PlatformUI
                .getWorkbench().getActiveWorkbenchWindow()
                    .getActivePage().getActiveEditor();
            WorkflowManagerInput input = new WorkflowManagerInput(
                    (WorkflowManager)container, parent);
            PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().openEditor(input,
                        "org.knime.workbench.editor.WorkflowEditor");
        } catch (PartInitException e) {
            LOGGER.error("Error while opening new editor", e);
            e.printStackTrace();
        }
        return;
    }

    @Override
    public void jobManagerChanged(final JobManagerChangedEvent e) {
        URL iconURL = getNodeContainer().findJobManager().getIcon();
        setJobManagerIcon(iconURL);
    }

    private void setJobManagerIcon(final URL iconURL) {
        Image icon = null;
        if (iconURL != null) {
            icon = ImageDescriptor.createFromURL(iconURL).createImage();
        }
        ((NodeContainerFigure)getFigure()).setJobExecutorIcon(icon);
    }

    /** {@inheritDoc} */
    @Override
    public void childAdded(final EditPart child, final int index) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    @Override
    public void partActivated(final EditPart editpart) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void partDeactivated(final EditPart editpart) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void removingChild(final EditPart child, final int index) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void selectedStateChanged(final EditPart editpart) {
        LOGGER.debug(getNodeContainer().toString());
    }

}
