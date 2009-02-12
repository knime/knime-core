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
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.view;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.internal.help.WorkbenchHelpSystem;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;

import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.Root;

/**
 * This view shows the content of the repository that was loaded from the
 * contributing extensions. It mainly includes a tree viewer which shows the
 * hierarchy of categories / nodes.
 *
 * @author Florian Georg, University of Konstanz
 */
public class RepositoryView extends ViewPart {
    private TreeViewer m_viewer;

    private DrillDownAdapter m_drillDownAdapter;

    // private Action m_action1;
    //
    // private Action m_action2;
    //
    // private Action m_doubleClickAction;

    private Root m_root;

    private final IPropertySourceProvider m_propertyProvider = new PropertyProvider();

    private FilterViewContributionItem m_toolbarFilterCombo;

    /**
     * This fetches the root repository object from the RepositoryManager.
     *
     */
    private void initialize() {

        m_root = RepositoryManager.INSTANCE.getRoot();
    }

    /**
     * The constructor.
     */
    public RepositoryView() {
    }

    /**
     * This callback creates the contant of the view. The TreeViewer is
     * initialized.
     *
     * @see org.eclipse.ui.IWorkbenchPart
     *      #createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(final Composite parent) {
        this.initialize();

        //
        // Create and configure the tree viewer
        //
        m_viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL);
        // m_drillDownAdapter = new DrillDownAdapter(m_viewer);
        m_viewer.setContentProvider(new RepositoryContentProvider());
        m_viewer.setLabelProvider(new RepositoryLabelProvider());
        // m_viewer.setSorter(new NameRepositorySorter());
        m_viewer.setInput(m_root);

        // check if there were categories that could not be processed properly
        // i.e. the afer-relationship information was wrong
        List<Category> problemCategories = m_root.getProblemCategories();

        if (problemCategories.size() > 0) {

            StringBuffer message = new StringBuffer();
            message
                    .append("The following categories could not be inserted at a "
                            + "proper position in the node repository due to wrong "
                            + "positioning information.\n"
                            + "See the corresponding plugin.xml file.\n "
                            + "The categories were instead appended at the end "
                            + "in each level.\n\n");
            for (Category category : problemCategories) {
                message.append("ID: ").append(category.getID());
                message.append(" Name: ").append(category.getName());
                message.append(" After-ID: ").append(category.getAfterID());
                message.append("\n");
            }

            try {
                MessageBox mb = new MessageBox(Display.getDefault()
                        .getActiveShell(), SWT.ICON_INFORMATION | SWT.OK);
                mb.setText("Problem categories...");
                mb.setMessage(message.toString());
                mb.open();
            } catch (Exception e) {
                // do nothing as this is just an information box
            }
        }

        // The viewer provides the selection to the workbench.
        this.getSite().setSelectionProvider(m_viewer);
        // The viewer supports drag&drop
        // (well, actually only drag - objects are dropped into the editor ;-)
        Transfer[] transfers = new Transfer[]{LocalSelectionTransfer.getTransfer()};
        m_viewer.addDragSupport(DND.DROP_COPY, transfers,
                new NodeTemplateDragListener(m_viewer));

        //
        // Configure actions and context menus
        //
        // this.makeActions();
        this.hookContextMenu();
        // this.hookDoubleClickAction();
        this.contributeToActionBars();

        // add Help context
        WorkbenchHelpSystem.getInstance().setHelp(m_viewer.getControl(),
                "org.knime.workbench.help.repository_view_context");

    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(final IMenuManager manager) {
                RepositoryView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(m_viewer.getControl());

        m_viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, m_viewer);
    }

    private void contributeToActionBars() {
        // Create drill down adapter
        m_drillDownAdapter = new DrillDownAdapter(m_viewer);

        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());

    }

    private void fillLocalPullDown(final IMenuManager manager) {
        // register drill down actions
        m_drillDownAdapter.addNavigationActions(manager);

        manager.add(new Separator());
    }

    private void fillContextMenu(final IMenuManager manager) {
        // manager.add(m_action1);
        // manager.add(m_action2);
        // manager.add(new Separator());

        // register drill down actions
        m_drillDownAdapter.addNavigationActions(manager);

        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        // create the combo contribution item that can filter our view

        m_toolbarFilterCombo = new FilterViewContributionItem(m_viewer);
        manager.add(m_toolbarFilterCombo);
        manager.add(new Separator());

        // add drill down actions to local tool bar
        m_drillDownAdapter.addNavigationActions(manager);
    }

    /**
     * Passing the focus request to the m_viewer's control.
     */
    @Override
    public void setFocus() {
        m_viewer.getControl().setFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAdapter(final Class adapter) {
        if (adapter == IPropertySourceProvider.class) {
            return m_propertyProvider;
        }
        return super.getAdapter(adapter);
    }

    /**
     * Property source provider.
     *
     * @author Florian Georg, University of Konstanz
     */
    private class PropertyProvider implements IPropertySourceProvider {
        /**
         * Delegates the request, if the object is an IAdaptable.
         *
         * @see org.eclipse.ui.views.properties.IPropertySourceProvider#
         *      getPropertySource(java.lang.Object)
         */
        public IPropertySource getPropertySource(final Object object) {

            // Look if we can get an adapter to IPropertySource....
            if (object instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable)object;

                return (IPropertySource)adaptable
                        .getAdapter(IPropertySource.class);
            }

            // well, no :-(
            return null;
        }
    }
}
