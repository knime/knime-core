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
 *   28.08.2005 (Florian Georg): created
 */
package org.knime.product.rcp;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.knime.product.rcp.intro.IntroPageAction;
import org.knime.workbench.ui.navigator.actions.ExportKnimeWorkflowAction;
import org.knime.workbench.ui.navigator.actions.ImportKnimeWorkflowAction;
import org.knime.workbench.ui.p2.actions.InvokeInstallSiteAction;
import org.knime.workbench.ui.p2.actions.InvokeUpdateAction;
import org.knime.workbench.ui.preferences.ExportPreferencesAction;
import org.knime.workbench.ui.preferences.ImportPreferencesAction;

/**
 * This advisor is responsible for creating the workbench actions and fills them
 * into the menu / cool bar of the workbench window.
 *
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEApplicationActionBarAdvisor extends ActionBarAdvisor {
    private IWorkbenchAction m_newAction;

    private IWorkbenchAction m_exitAction;

    private IWorkbenchAction m_preferencesAction;

    private IAction m_exportPrefAction;

    private IAction m_importPrefAction;

    private IAction m_print;

    // private IWorkbenchAction m_introAction;

    private IWorkbenchAction m_aboutAction;

    private IWorkbenchAction m_helpAction;

    private IWorkbenchAction m_helpSearchAction;

    private IAction m_introAction;

    private IWorkbenchAction m_cutAction;

    private IWorkbenchAction m_copyAction;

    private IWorkbenchAction m_pasteAction;

    private IWorkbenchAction m_undoAction;

    private IWorkbenchAction m_redoAction;

    private IWorkbenchAction m_deleteAction;

    private IWorkbenchAction m_selectAllAction;

    private IWorkbenchAction m_newWizardDropdownAction;

    private IWorkbenchAction m_saveAction;

    private IWorkbenchAction m_saveAsAction;

    private IWorkbenchAction m_saveAllAction;

    private IWorkbenchAction m_closeAllAction;

    private IWorkbenchAction m_changeWorkspaceAction;

    private IAction m_updateKnimeAction;

    private IAction m_installFeaturesAction;

    private IAction m_exportWorkflowAction;

    private IAction m_importWorkflowAction;

    private IContributionItem m_showViewShortlistContributionItem;

    private List<IAction> m_multiInstanceViews;

    private IAction m_resetPerspective;

    /**
     * Creates a new action bar advisor to configure a workbench window's action
     * bars via the given action bar configurer.
     *
     * @param configurer the action bar configurer
     */
    public KNIMEApplicationActionBarAdvisor(
            final IActionBarConfigurer configurer) {
        super(configurer);
    }

    /**
     * This creates all the actions that are available for the knime workbench
     * action bars.
     *
     * @see org.eclipse.ui.application.ActionBarAdvisor
     *      #makeActions(org.eclipse.ui.IWorkbenchWindow)
     */
    @Override
    protected void makeActions(final IWorkbenchWindow window) {
        // Creates the actions and registers them.
        // Registering is needed to ensure that key bindings work.
        // The corresponding commands keybindings are defined in the plugin.xml
        // file.
        // Registering also provides automatic disposal of the actions when
        // the window is closed.

        // File actions
        m_newAction = ActionFactory.NEW.create(window);
        m_newAction.setText("&New...");
        register(m_newAction);
        m_saveAction = ActionFactory.SAVE.create(window);
        register(m_saveAction);
        m_saveAsAction = ActionFactory.SAVE_AS.create(window);
        register(m_saveAsAction);
        m_saveAllAction = ActionFactory.SAVE_ALL.create(window);
        register(m_saveAllAction);
        m_exitAction = ActionFactory.QUIT.create(window);
        register(m_exitAction);
        m_changeWorkspaceAction =
                IDEActionFactory.OPEN_WORKSPACE.create(window);
        register(m_changeWorkspaceAction);
        m_updateKnimeAction = new InvokeUpdateAction();
        register(m_updateKnimeAction);
        m_installFeaturesAction = new InvokeInstallSiteAction();
        register(m_installFeaturesAction);
        m_preferencesAction = ActionFactory.PREFERENCES.create(window);
        register(m_preferencesAction);
        m_exportPrefAction = new ExportPreferencesAction(window);
        register(m_exportPrefAction);
        m_importPrefAction = new ImportPreferencesAction(window);
        register(m_importPrefAction);

        m_print = ActionFactory.PRINT.create(window);
        register(m_print);

        // Edit Actions
        m_cutAction = ActionFactory.CUT.create(window);
        register(m_cutAction);
        m_copyAction = ActionFactory.COPY.create(window);
        register(m_copyAction);
        m_pasteAction = ActionFactory.PASTE.create(window);
        register(m_pasteAction);
        m_undoAction = ActionFactory.UNDO.create(window);
        register(m_undoAction);
        m_redoAction = ActionFactory.REDO.create(window);
        register(m_redoAction);
        m_deleteAction = ActionFactory.DELETE.create(window);
        register(m_deleteAction);
        m_selectAllAction = ActionFactory.SELECT_ALL.create(window);
        register(m_selectAllAction);
        m_closeAllAction = ActionFactory.CLOSE_ALL.create(window);
        register(m_closeAllAction);

        // View Actions

        // Use Eclipse view short cuts
        // (downside: don't know how to control ordering, instantiates views
        // only once at a time)
        m_showViewShortlistContributionItem =
                ContributionItemFactory.VIEWS_SHORTLIST.create(window);
        // create actions for views that register with the mult_inst_view point
        m_multiInstanceViews = createMultiInstanceViewActions();

        m_resetPerspective = ActionFactory.RESET_PERSPECTIVE.create(window);
        register(m_resetPerspective);

        // temporarily disable due to eclipse bug
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=211184
        // (Code will be enabled if bug is closed, corresponding task #1453)
        // Help Actions
        /*
         * m_introAction = ActionFactory.INTRO.create(window);
         * m_introAction.setText("Show &Intro page"); register(m_introAction);
         */

        m_helpAction = ActionFactory.HELP_CONTENTS.create(window);
        register(m_helpAction);

        m_helpSearchAction = ActionFactory.HELP_SEARCH.create(window);
        register(m_helpSearchAction);

        m_introAction = new IntroPageAction();
        register(m_introAction);

        m_aboutAction = ActionFactory.ABOUT.create(window);
        register(m_aboutAction);

        // Toolbar actions
        m_newWizardDropdownAction =
                ActionFactory.NEW_WIZARD_DROP_DOWN.create(window);
        register(m_newWizardDropdownAction);

        m_exportWorkflowAction = new ExportKnimeWorkflowAction(window);
        register(m_exportWorkflowAction);
        m_importWorkflowAction = new ImportKnimeWorkflowAction(window);
        register(m_importWorkflowAction);
    }

    /**
     * Fills the menu bar with the main menus for the windows. Some anchors were
     * added below the last action of each division for convenience.
     * These anchors are named after the action above them, e.g.:
     * "file/ImportPreferences" is located below "Import Preferences..."
     * but above the separator.
     *
     * {@inheritDoc}
     */
    @Override
    protected void fillMenuBar(final IMenuManager menuBar) {
        menuBar.remove(IWorkbenchActionConstants.MB_ADDITIONS);

        final MenuManager fileMenu = new MenuManager("&File",
                IWorkbenchActionConstants.M_FILE);

        MenuManager editMenu =
                new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
        MenuManager viewMenu =
                new MenuManager("&View", IWorkbenchActionConstants.M_VIEW);
        MenuManager helpMenu =
                new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);

        // 1. File menu
        menuBar.add(fileMenu);
        // 2. Edit menu
        menuBar.add(editMenu);
        // 3. View menu
        menuBar.add(viewMenu);
        // Add a group marker indicating where action set menus will appear.
        GroupMarker marker = new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS);
        marker.setVisible(false);
        menuBar.add(marker);
        // last: Help menu
        menuBar.add(helpMenu);

        // File menu
        fileMenu.add(m_newAction);
        fileMenu.add(m_saveAction);
        fileMenu.add(m_saveAsAction);
        fileMenu.add(m_saveAllAction);
        fileMenu.add(m_closeAllAction);

        fileMenu.add(m_print);
        fileMenu.add(m_importWorkflowAction);
        fileMenu.add(m_exportWorkflowAction);
        fileMenu.add(new GroupMarker("ExportWorkflow"));
        fileMenu.add(new Separator());
        fileMenu.add(m_changeWorkspaceAction);
        fileMenu.add(new GroupMarker("SwitchWorkspace"));
        fileMenu.add(new Separator());
        fileMenu.add(m_preferencesAction);
        fileMenu.add(m_exportPrefAction);
        fileMenu.add(m_importPrefAction);
        fileMenu.add(new GroupMarker("ImportPreferences"));
        fileMenu.add(new Separator());
        fileMenu.add(m_installFeaturesAction);
        fileMenu.add(m_updateKnimeAction);
        fileMenu.add(new GroupMarker("UpdateKNIME"));
        fileMenu.add(new Separator());
        fileMenu.add(m_exitAction);

        // Edit menu
        editMenu.add(m_undoAction);
        editMenu.add(m_redoAction);
        fileMenu.add(new GroupMarker("Redo"));
        editMenu.add(new Separator());
        editMenu.add(m_cutAction);
        editMenu.add(m_copyAction);
        editMenu.add(m_pasteAction);
        fileMenu.add(new GroupMarker("Paste"));
        editMenu.add(new Separator());
        editMenu.add(m_deleteAction);
        editMenu.add(m_selectAllAction);

        // View menu
        addMultiViewsToMenu(viewMenu);
        viewMenu.add(m_showViewShortlistContributionItem);
        viewMenu.add(m_resetPerspective);

        // Help menu
        // helpMenu.add(m_introAction);
        helpMenu.add(m_helpAction);
        helpMenu.add(m_helpSearchAction);
        helpMenu.add(m_introAction);
        // menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        helpMenu.add(m_aboutAction);

        final String[] unwanted = new String[] {
                "org.eclipse.birt.report.designer.ui.LibraryPublisAction",
                "org.eclipse.birt.report.designer.ui.TemplatepublisAction"};

        fileMenu.addMenuListener(new IMenuListener() {
            /** {@inheritDoc} */
            @Override
            public void menuAboutToShow(final IMenuManager mgr) {
                for (String id : unwanted) {
                    fileMenu.remove(id);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillCoolBar(final ICoolBarManager coolBar) {
        // create a toolbar and add it to the coolbar :)
        IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        coolBar.removeAll();
        coolBar.add(new ToolBarContributionItem(toolbar, "main"));

        // add tools to the toolbar
        toolbar.add(m_newWizardDropdownAction);
        toolbar.add(m_saveAction);
        toolbar.add(m_saveAsAction);
        toolbar.add(m_saveAllAction);
    }

    private List<IAction> createMultiInstanceViewActions() {

        List<IAction> result = new LinkedList<IAction>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point =
                registry.getExtensionPoint(
                        "org.knime.workbench.ui.multipleInstanceViews");
        if (point == null) {
            return result;
        }

        IExtension[] ext = point.getExtensions();
        if (ext == null) {
            return result;
        }

        for (IExtension extension : ext) {
            IConfigurationElement[] conf = extension.getConfigurationElements();
            if (conf == null) {
                continue;
            }
            for (IConfigurationElement c : conf) {
                String viewId = c.getAttribute("id");
                if (viewId == null || viewId.isEmpty()) {
                    continue;
                }
                IAction viewAction = createViewAction(viewId);
                if (viewAction == null) {
                    continue;
                }
                result.add(viewAction);
            }
        }

        // sort actions by view name
        Collections.sort(result, new Comparator<IAction>() {
            @Override
            public int compare(final IAction o1, final IAction o2) {
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                return o1.getText().compareTo(o2.getText());
            }
        });

        return result;
    }

    private void addMultiViewsToMenu(final MenuManager viewMenu) {
        for (IAction a : m_multiInstanceViews) {
            viewMenu.add(a);
        }
    }

    private OpenKnimeViewAction createViewAction(final String viewId) {
        IViewRegistry viewReg = PlatformUI.getWorkbench().getViewRegistry();
        IViewDescriptor vDesc = viewReg.find(viewId);
        if (vDesc == null) {
            return null;
        }
        if (!vDesc.getAllowMultiple()) {
            // views that can not be instantiated multiple times are ignored
            return null;
        }

        OpenKnimeViewAction result = new OpenKnimeViewAction(viewId);
        result.setText(vDesc.getLabel());
        result.setImageDescriptor(vDesc.getImageDescriptor());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("restriction")
    @Override
    public void fillActionBars(final int flags) {
        super.fillActionBars(flags);
        ActionSetRegistry reg =
                WorkbenchPlugin.getDefault().getActionSetRegistry();
        IActionSetDescriptor[] actionSets = reg.getActionSets();

        // remove open file menu action
        String actionSetId = "org.eclipse.ui.actionSet.openFiles";
        for (int i = 0; i < actionSets.length; i++) {
            if (!actionSets[i].getId().equals(actionSetId)) {
                continue;
            }
            IExtension ext = actionSets[i].getConfigurationElement()
                    .getDeclaringExtension();
            reg.removeExtension(ext, new Object[]{actionSets[i]});
        }

        // remove convert line delimiters menu action
        actionSetId
                = "org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo";
        for (int i = 0; i < actionSets.length; i++) {
            if (!actionSets[i].getId().equals(actionSetId)) {
                continue;
            }
            IExtension ext = actionSets[i].getConfigurationElement()
                    .getDeclaringExtension();
            reg.removeExtension(ext, new Object[]{actionSets[i]});
        }
    }

}
