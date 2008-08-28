/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   14.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;

import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.extrainfo.ModellingNodeExtraInfo;

/**
 * One page wizard to create a meta node by defining the number and type of in
 * and out ports.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class AddMetaNodeWizard extends Wizard {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            AddMetaNodeWizard.class);


    private final WorkflowEditor m_wfEditor;

    private SelectMetaNodePage m_selectPage;
    private AddMetaNodePage m_addPage;


    /**
     *
     * @param wfEditor the underlying workflow editor
     */
    public AddMetaNodeWizard(final WorkflowEditor wfEditor) {
        super();
        m_wfEditor = wfEditor;
        // TODO: remove as soon as there is some help available
        // would be  great to have some description of how to create meta nodes,
        // what to do with meta nodes and how to deploy them
        setHelpAvailable(false);
    }
   

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        // add the one and only page to enter the in- and out ports
        setWindowTitle("Add Meta Node Wizard");
        setDefaultPageImageDescriptor(ImageDescriptor.createFromImage(
                ImageRepository.getImage("icons/meta/meta_node_wizard2.png")));
        m_selectPage = new SelectMetaNodePage();
        m_addPage = new AddMetaNodePage();
        addPage(m_selectPage);
        addPage(m_addPage);
    }

    

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return m_addPage.isPageComplete() || m_selectPage.isPageComplete();
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        if (m_addPage.isPageComplete()) {
            performCustomizedFinish();
            return true;
        } else {
            return performPredefinedFinish();
        }
    }

    protected boolean performPredefinedFinish() {
        String metaNodeType = m_selectPage.getSelectedMetaNode();
        if (metaNodeType.equals(SelectMetaNodePage.ZERO_ONE)) {
            // create 0:1 meta node
            createMetaNode(0, 1);
            return true;
        } else if (metaNodeType.equals(SelectMetaNodePage.ONE_ONE)) {
            // create 1:1 meta node
            createMetaNode(1, 1);
            return true;
        } else if (metaNodeType.equals(SelectMetaNodePage.ONE_TWO)) {
            // create 1:2 meta node
            createMetaNode(1, 2);
            return true;
        } else if (metaNodeType.equals(SelectMetaNodePage.TWO_ONE)) {
            // create 2:1 meta node
            createMetaNode(2, 1);
            return true;
        } else if (metaNodeType.equals(SelectMetaNodePage.TWO_TWO)) {
            // create 2:2 meta node
            createMetaNode(2, 2);
            return true;
        }
        return false;
    }

    private void createMetaNode(final int nrInPorts, final int nrOutPorts) {
        PortType[] inPorts = new PortType[nrInPorts];
        for (int i = 0; i < nrInPorts; i++) {
            inPorts[i] = BufferedDataTable.TYPE;
        }
        PortType[] outPorts = new PortType[nrOutPorts];
        for (int i = 0; i < nrOutPorts; i++) {
            outPorts[i] = BufferedDataTable.TYPE;
        }
        // removed the port "ratio" from the name
        String name = "MetaNode"; // + nrInPorts + ":" + nrOutPorts;
        createMetaNodeFromPorts(inPorts, outPorts, name);
    }

    private void createMetaNodeFromPorts(final PortType[] inPorts,
            final PortType[] outPorts, final String name) {
        WorkflowManager meta = m_wfEditor.getWorkflowManager()
        .createAndAddSubWorkflow(inPorts, outPorts);
        NodeContainer cont = m_wfEditor.getWorkflowManager().getNodeContainer(
                meta.getID());
        ((WorkflowManager)cont).setName(name);
        // create extra info and set it
        ModellingNodeExtraInfo info = new ModellingNodeExtraInfo();

        Viewport viewPort = ((ScalableFreeformRootEditPart)m_wfEditor
                .getViewer().getRootEditPart()).getZoomManager().getViewport();

        // get the currently visible area
        Rectangle rect = viewPort.getClientArea();
        // translate it to absolute coordinates (with respect to the scrolled
        // editor viewer)
        viewPort.translateToAbsolute(rect.getLocation());

        // now we have an absolute point for the new metanode location
        org.eclipse.draw2d.geometry.Point location
            = new org.eclipse.draw2d.geometry.Point(rect.x  + (rect.width / 2),
                    rect.y + (rect.height / 2));
        // in order to get a local (relative position we have to substract the
        // invisible offset
        Point adaptedLoc = new Point(location.x - rect.x, location.y - rect.y);

        EditPart ep;
        // iterate until we have found a free place on the editor
        do {
            // we have to use the adaptedLoc (which is the relative position)
            ep = m_wfEditor.getViewer().findObjectAt(adaptedLoc);
            if (ep instanceof NodeContainerEditPart) {
                // offset the absolute location
                location.x += ((NodeContainerEditPart)ep).getFigure()
                                .getBounds().width;
                location.y += ((NodeContainerEditPart)ep).getFigure()
                                .getBounds().height;
                // offset the relative position
                adaptedLoc.x += ((NodeContainerEditPart)ep).getFigure()
                                    .getBounds().width;
                adaptedLoc.y += ((NodeContainerEditPart)ep).getFigure()
                                    .getBounds().height;
            }
        } while (ep instanceof NodeContainerEditPart);
        // now position the node to the absolute position
        info.setNodeLocation(location.x, location.y, -1, -1);
        cont.setUIInformation(info);
    }

    private void performCustomizedFinish() {
        // create subworkflow with the number and types
        // of the entered in- and out ports
        PortType[] inPorts = new PortType[m_addPage.getInports().size()];
        PortType[] outPorts = new PortType[m_addPage.getOutPorts().size()];
        int i = 0;
        for (Port p : m_addPage.getInports()) {
            inPorts[i++] = p.getType();
        }
        i = 0;
        for (Port p : m_addPage.getOutPorts()) {
            outPorts[i++] = p.getType();
        }
        String name = "";
        if (m_addPage.getMetaNodeName() != null
                && m_addPage.getMetaNodeName().length() > 0) {
            name = m_addPage.getMetaNodeName();
        }
        createMetaNodeFromPorts(inPorts, outPorts, name);
    }

}
