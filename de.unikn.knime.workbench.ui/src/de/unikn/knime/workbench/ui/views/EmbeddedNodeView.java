/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   23.12.2005 (georg): created
 */
package de.unikn.knime.workbench.ui.views;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Label;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.unikn.knime.core.node.NodeView;

/**
 * View for showing Workbench-embedded KNIME Node-Views.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class EmbeddedNodeView extends ViewPart {

    /** The view ID, needed to open instances of this view programatically.* */
    public static final String ID = "de.unikn.knime.workbench.ui.views.EmbeddedNodeView";

    private Action m_action1;

    private Action m_action2;

    private Composite m_viewContainer;

    private Frame m_awtFrame;

    private Component m_content;

    private NodeView m_nodeView;

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
    public void createPartControl(final Composite parent) {

        m_viewContainer = new Composite(parent, SWT.EMBEDDED);
        m_viewContainer.setLayout(new GridLayout(1, true));
        m_awtFrame = SWT_AWT.new_Frame(m_viewContainer);

        makeActions();
        contributeToActionBars();

        m_content = new Label("Nothing to display");
        m_awtFrame.add(m_content);
    }

    /**
     * Sets the content node view that should be embedded in this view.
     * 
     * @param view The node view to embedd
     */
    public void setNodeView(final NodeView view) {
        m_nodeView = view;

        // opens the view component
        Component comp = m_nodeView.openViewComponent();

        if (m_content != null) {
            m_awtFrame.remove(m_content);
        }
        m_awtFrame.add(comp);
        m_content = comp;

        // update title
        setPartName(m_nodeView.getViewName() + ":"
                + getViewSite().getSecondaryId());

    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(final IMenuManager manager) {
        manager.add(m_action1);
        manager.add(new Separator());
        manager.add(m_action2);
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        manager.add(m_action1);
        manager.add(m_action2);
    }

    private void makeActions() {
        m_action1 = new Action() {
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

    private void showMessage(final String message) {
        MessageDialog.openInformation(this.getViewSite().getShell(),
                "Node View", message);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        m_viewContainer.setFocus();
    }
}