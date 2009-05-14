/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.workflow.JobManagerChangedEvent;
import org.knime.core.node.workflow.JobManagerChangedListener;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
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
import org.knime.workbench.editor2.editparts.policy.NodeContainerComponentEditPolicy;
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
    /**
     * The time (in ms) within two clicks are treated as double click. TODO: get
     * the system double click time
     */
    // private static final long DOUBLE_CLICK_TIME = 400;
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(NodeContainerEditPart.class);

    /**
     * Remembers the time of the last <code>MousePressed</code> event. This is
     * neccessary to implement manually the double click, as the
     * <code>DoubleClick</code> event is not processed by draw2D for some
     * reason.
     */
    // private long m_lastClick;
    /**
     * true, if the figure was initialized from the node extra info object.
     */
    private boolean m_figureInitialized;

    /**
     * this is set while executing. DeleteCommands mustn't be executed while
     * node is busy *
     */
    private boolean m_isLocked;

    /**
     * The manager for the direct editing of the node name.
     */
    private NodeEditManager m_directEditManager;

    /**
     * @return The <code>NodeContainer</code>(= model)
     */
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

    /**
     * @return Returns if this edit part is locked (=busy). Important for
     *         commands because e.g. the node mustn't be deleted while
     *         executing.
     */
    public boolean isLocked() {
        return m_isLocked;
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
            initFigureFromExtraInfo((NodeUIInformation)getNodeContainer()
                    .getUIInformation());
            m_figureInitialized = true;
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
     * Return the content pane for the model children (= ports).
     *
     * @see org.eclipse.gef.GraphicalEditPart#getContentPane()
     */
    @Override
    public IFigure getContentPane() {
        return ((NodeContainerFigure)getFigure()).getContentFigure();

    }

    private void performDirectEdit() {

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
     * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
     */
    @Override
    protected void createEditPolicies() {

        // Handles the creation of DeleteCommands for nodes in a workflow
        this.installEditPolicy(EditPolicy.COMPONENT_ROLE,
                new NodeContainerComponentEditPolicy());

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
    protected List getModelChildren() {
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
     *
     * @see org.eclipse.gef.editparts.AbstractEditPart#refreshVisuals()
     */
    @Override
    protected void refreshVisuals() {
        // TODO Auto-generated method stub
        super.refreshVisuals();
    }



    public void stateChanged(final NodeStateEvent state) {
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            public void run() {
                NodeContainerFigure fig = (NodeContainerFigure)getFigure();
                fig.setState(state.getState());
                updateNodeStatus();

                // reset the tooltip text of the outports
                for (Object part : getChildren()) {

                    if (part instanceof NodeOutPortEditPart
                            || part instanceof WorkflowInPortEditPart
                            || part instanceof SubWorkFlowOutPortEditPart) {
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

    /**
     * {@inheritDoc}
     */
    public void progressChanged(final NodeProgressEvent pe) {
        // forward the new progress to our progress figure
        ((NodeContainerFigure)getFigure()).getProgressFigure().progressChanged(
                pe.getNodeProgress());
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void messageChanged(final NodeMessageEvent messageEvent) {
        //
        // As this code updates the UI it must be executed in the UI thread.
        //
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            public void run() {
                NodeContainerFigure fig = (NodeContainerFigure)getFigure();
                fig.setMessage(messageEvent.getMessage());
                updateNodeStatus();
                // always refresh visuals
                refreshVisuals();
            }
        });
    }

    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {

        //
        // As this code updates the UI it must be executed in the UI thread.
        //
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                NodeContainerFigure fig = (NodeContainerFigure)getFigure();

                    // case NodeContainer.EVENT_EXTRAINFO_CHANGED:
                    LOGGER.debug("ExtraInfo changed, "
                            + "updating bounds and visuals...");

                    WorkflowRootEditPart parent = null;
                    parent = (WorkflowRootEditPart)getParent();

                    // provide some info from the extra info object to the
                    // figure
                    NodeUIInformation ei = null;
                    ei =
                            (NodeUIInformation)getNodeContainer()
                                    .getUIInformation();

                    //
                    // if not already initialized, do this now.
                    // Necessary 'cause the extra info is only available after
                    // the node
                    // has been added to the WFM via the CreateNodeCommand
                    // All of this data is static and should not change after
                    // creation
                    // (icon, type)
                    if (!m_figureInitialized) {
                        initFigureFromExtraInfo(ei);
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
     * Initializes the figure with data from the node extra info object. This
     * must be done only once, but after the node has been added to the WFM
     * (otherwise the extra info object is not available).
     *
     * @param ei Extra info to provide to the figure
     */
    private void initFigureFromExtraInfo(final NodeUIInformation ei) {

        LOGGER.debug("Initializing figure from NodeExtraInfo..");
        m_figureInitialized = true;

        /*
         * If the figure wasn't yet initialized 
         * (see that the width and height are -1)
         * convert from absolute to take scrolling into account 
         */
        NodeContainerFigure f = (NodeContainerFigure)getFigure();
        int[] b = ei.getBounds();
        if (b[2] == -1 || b[2] == -1) {
            Point p = new Point(b[0], b[1]);
            f.translateToRelative(p);
//            LOGGER.debug("after: " + p);
            b[0] = p.x;
            b[1] = p.y;
        }
        f.setBounds(new Rectangle(b[0], b[1], b[2], b[3]));
        
        // String plugin = ei.getPluginID();
        // String iconPath = ei.getIconPath();
        NodeType type = getNodeContainer().getType();
        String name = getNodeContainer().getName();
        String description = getNodeContainer().getCustomDescription();

        // get the icon
        Image icon = null;
        // URL iconURL = getNodeContainer().getIcon();
        // try {
        // icon = new Image(Workbench.getInstance().getDisplay(),
        // iconURL.openStream());
        // } catch (Exception e) {
        // try {
        // icon = new Image(Workbench.getInstance().getDisplay(),
        // NodeFactory.getDefaultIcon().openStream());
        // } catch (Exception innerE) {
        // // in this case the icon is null
        // icon = null;
        // }
        // }
        icon =
                ImageRepository.getScaledImage(getNodeContainer().getIcon(),
                        16, 16);
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
//        boolean isExecuted = getNodeContainer().getState().equals(
//                NodeContainer.State.EXECUTED);
//        if (isExecuted) {
//            f.setState(NodeContainerFigure.STATE_EXECUTED, null);
//        } else {
//            if (getNodeContainer().getState().equals(
//                    NodeContainer.State.EXECUTING)) {
//                f.setState(NodeContainerFigure.STATE_EXECUTING, null);
//            } else if (getNodeContainer().getState().equals(
//                    NodeContainer.State.QUEUED)) {
//                f.setState(NodeContainerFigure.STATE_QUEUED, null);
//                // TODO: check this
//            } else if (getNodeContainer().getState().equals(
//                    NodeContainer.State.CONFIGURED)) {
//                f.setState(NodeContainerFigure.STATE_READY, null);
//            } else {
//                f.setState(NodeContainerFigure.STATE_NOT_CONFIGURED, null);
//            }
//        }
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
    public void jobManagerChanged(JobManagerChangedEvent e) {
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

    public void childAdded(final EditPart child, final int index) {
        // TODO Auto-generated method stub

    }

    public void partActivated(final EditPart editpart) {
        // TODO Auto-generated method stub

    }

    public void partDeactivated(final EditPart editpart) {
        // TODO Auto-generated method stub

    }

    public void removingChild(final EditPart child, final int index) {
        // TODO Auto-generated method stub

    }

    public void selectedStateChanged(final EditPart editpart) {
        LOGGER.debug(getNodeContainer().toString());
    }

}
