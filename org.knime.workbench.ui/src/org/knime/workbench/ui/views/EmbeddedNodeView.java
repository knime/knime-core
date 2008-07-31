/*
 * -------------------------------------------------------------------
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
 *   23.12.2005 (georg): created
 */
package org.knime.workbench.ui.views;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Label;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.NodeView;

/**
 * View for showing Workbench-embedded KNIME Node-Views.
 *
 * @author Florian Georg, University of Konstanz
 */
public class EmbeddedNodeView extends ViewPart implements
        PropertyChangeListener {

    /** The view ID, needed to open instances of this view programatically. */
    public static final String ID =
            "org.knime.workbench.ui.views.EmbeddedNodeView";

    private Action m_action1;

    private Action m_action2;

    private Composite m_viewContainer;

    private Frame m_awtFrame;

    private Component m_content;

    private GenericNodeView<?> m_nodeView;

    /**
     * The constructor.
     */
    public EmbeddedNodeView() {
    }

    /**
     * Creates the view part control.
     *
     * @see org.eclipse.ui.IWorkbenchPart
     *      #createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(final Composite parent) {

        m_viewContainer = new Composite(parent, SWT.EMBEDDED | SWT.FOCUSED);
        m_viewContainer.setLayout(new GridLayout(1, true));
        m_awtFrame = SWT_AWT.new_Frame(m_viewContainer);

        makeActions();

        m_content = new Label("Nothing to display");
        m_awtFrame.add(m_content);
        m_awtFrame.toFront();
    }

    /**
     * Sets the content node view that should be embedded in this view.
     *
     * @param view The node view to embedd
     */
    public void setNodeView(final GenericNodeView<?> view) {
        if (m_nodeView != null) {
            releaseNodeView();
            m_content.removePropertyChangeListener(this);
        }
        m_nodeView = view;
        contributeToActionBars();

        // opens the view component
        Component comp = m_nodeView.openViewComponent();

        m_awtFrame.add(comp);
        m_content = comp;
        m_content.addPropertyChangeListener(this);

        // update title
        setPartName(m_nodeView.getViewTitle() + ":"
                + getViewSite().getSecondaryId());

    }

    private void contributeToActionBars() {
        //IActionBars bars = getViewSite().getActionBars();
        //fillLocalPullDown(bars.getMenuManager());
        // fillLocalToolBar(bars.getToolBarManager());
    }

//    private void fillLocalPullDown(final IMenuManager manager) {
//
//        if (m_nodeView == null) {
//            return;
//        }
//        JMenuBar menuBar = m_nodeView.getJMenuBar();
//        for (int i = 0; i < menuBar.getMenuCount(); i++) {
//            JMenu menu = menuBar.getMenu(i);
//
//            MenuManager managerNew =
//                    new MenuManager(menu.getName(), menu.getName());
//            for(MenuItem item : menu.getmen)
//            {
//
//            }
//            manager.add(managerNew);
//        }
//        // manager.add(m_action1);
//        // manager.add(new Separator());
//        // manager.add(m_action2);
//    }

    // private void fillLocalToolBar(final IToolBarManager manager) {
    // manager.add(m_action1);
    // manager.add(m_action2);
    // }

    private void makeActions() {
        m_action1 = new Action() {
            @Override
            public void run() {
                showMessage("Action 1 executed");
            }
        };
        m_action1.setText("Action 1");
        m_action1.setToolTipText("Action 1 tooltip");
        m_action1.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages().getImageDescriptor(
                        ISharedImages.IMG_OBJS_INFO_TSK));

        m_action2 = new Action() {
            @Override
            public void run() {
                showMessage("Action 2 executed");
            }
        };
        m_action2.setText("Action 2");
        m_action2.setToolTipText("Action 2 tooltip");
        m_action2.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages().getImageDescriptor(
                        ISharedImages.IMG_OBJS_INFO_TSK));
    }

    // private Action createAction() {
    //
    // Action action = new Action() {
    // @Override
    // public void run() {
    // showMessage("Action 1 executed");
    // }
    // };
    // action.setText("Action 1");
    // action.setToolTipText("Action 1 tooltip");
    // action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
    // .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
    //
    // return action;
    // }

    private void showMessage(final String message) {
        MessageDialog.openInformation(this.getViewSite().getShell(),
                "Node View", message);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        m_viewContainer.setFocus();
    }

    /**
     * releases the underlying node view, i.e. unregistering from node model.
     *
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        releaseNodeView();
        super.dispose();
    }

    /** Calls m_nodeView.closeViewComponent(). */
    private void releaseNodeView() {
        if (m_nodeView != null) {
            m_nodeView.closeView();
            m_content.removePropertyChangeListener(this);
            m_awtFrame.remove(m_content);
        }
        m_nodeView = null;
        m_content = null;
    }

    /**
     * Cares about events that come from the node view (more precisely from the
     * content pane m_content and will close this view.
     *
     * @see PropertyChangeListener
     *      #propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(NodeView.PROP_CHANGE_CLOSE)) {
            // do close the view here - how?
        }
    }
}
