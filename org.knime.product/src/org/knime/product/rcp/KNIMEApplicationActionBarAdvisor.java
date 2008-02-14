/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   28.08.2005 (Florian Georg): created
 */
package org.knime.product.rcp;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.ide.IDEActionFactory;
import org.knime.workbench.help.intro.InvokeInstallSiteAction;

/**
 * This advisor is resposible for creating the workbench actions and fills them
 * into the menu / cool bar of the workbench window.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEApplicationActionBarAdvisor extends ActionBarAdvisor {
    private IWorkbenchAction m_newAction;

    private IWorkbenchAction m_exitAction;

    private IWorkbenchAction m_preferencesAction;

    // private IWorkbenchAction m_introAction;

    private IWorkbenchAction m_aboutAction;

    private IWorkbenchAction m_helpAction;

    private IWorkbenchAction m_helpSearchAction;

    private IWorkbenchAction m_cutAction;

    private IWorkbenchAction m_copyAction;

    private IWorkbenchAction m_pasteAction;

    private IWorkbenchAction m_undoAction;

    private IWorkbenchAction m_redoAction;

    private IWorkbenchAction m_deleteAction;
    
    private IWorkbenchAction m_selectAllAction;

    private IWorkbenchAction m_newWizardDropdownAction;

    private IWorkbenchAction m_saveAction;
    
    private IWorkbenchAction m_saveAllAction;
    
    private IWorkbenchAction m_changeWorkspaceAction;
    
    private IAction m_updateKnimeAction;

    // private IAction m_openOutlineViewAction;
    //    
    // private IAction m_openNavigatorViewAction;
    //    
    // private IAction m_openRepositoryViewAction;
    //    
    // private IAction m_openRepositoryHelpViewAction;
    //    
    // private IAction m_openConsoleViewAction;
    //    
    // private IAction m_openProgressViewAction;

    private IContributionItem m_showViewShortlistContributionItem;

    /**
     * @param configurer
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
        m_saveAllAction = ActionFactory.SAVE_ALL.create(window);
        register(m_saveAllAction);
        m_exitAction = ActionFactory.QUIT.create(window);
        register(m_exitAction);
        m_changeWorkspaceAction =
                IDEActionFactory.OPEN_WORKSPACE.create(window);
        register(m_changeWorkspaceAction);
        m_updateKnimeAction = new InvokeInstallSiteAction();
        register(m_updateKnimeAction);
        m_preferencesAction = ActionFactory.PREFERENCES.create(window);
        register(m_preferencesAction);

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

        // View Actions
        // m_openOutlineViewAction = new OpenOutlineViewAction();
        // m_openConsoleViewAction = new OpenConsoleViewAction();
        // m_openNavigatorViewAction = new OpenNavigatorViewAction();
        // m_openProgressViewAction = new OpenProgressViewAction();
        // m_openRepositoryHelpViewAction = new OpenRepositoryHelpViewAction();
        // m_openRepositoryViewAction = new OpenRepositoryViewAction();

        // Don't need this actions, as the following contribution item creates
        // them all
        // for us (downside: don't know how to control ordering)
        // Contribution item for view shortlist
        m_showViewShortlistContributionItem = ContributionItemFactory.VIEWS_SHORTLIST
                .create(window);

        // Help Actions
        // m_introAction = ActionFactory.INTRO.create(window);
        // m_introAction.setText("Show &Intro page");
        // register(m_introAction);

        m_helpAction = ActionFactory.HELP_CONTENTS.create(window);
        register(m_helpAction);

        m_helpSearchAction = ActionFactory.HELP_SEARCH.create(window);
        register(m_helpSearchAction);

        m_aboutAction = ActionFactory.ABOUT.create(window);
        register(m_aboutAction);

        // Toolbar actions
        m_newWizardDropdownAction = ActionFactory.NEW_WIZARD_DROP_DOWN
                .create(window);
        register(m_newWizardDropdownAction);

    }

    /**
     * Fills the actions into the menu bar.
     * 
     * @see org.eclipse.ui.application.ActionBarAdvisor
     *      #fillMenuBar(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillMenuBar(final IMenuManager menuBar) {
        MenuManager fileMenu = new MenuManager("&File");
        MenuManager editMenu = new MenuManager("&Edit",
                IWorkbenchActionConstants.M_EDIT);
        MenuManager viewMenu = new MenuManager("&View",
                IWorkbenchActionConstants.M_VIEW);
        MenuManager helpMenu = new MenuManager("&Help",
                IWorkbenchActionConstants.M_HELP);

        // 1. File menu
        menuBar.add(fileMenu);
        // 2. Edit menu
        menuBar.add(editMenu);
        // 3. View menu
        menuBar.add(viewMenu);
        // Add a group marker indicating where action set menus will appear.
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        // last: Help menu
        menuBar.add(helpMenu);

        // File menu
        fileMenu.add(m_newAction);
        fileMenu.add(m_saveAction);
        fileMenu.add(m_saveAllAction);
        fileMenu.add(new Separator());
        fileMenu.add(m_changeWorkspaceAction);
        fileMenu.add(new Separator());
        fileMenu.add(m_preferencesAction);
        fileMenu.add(m_updateKnimeAction);
        fileMenu.add(new Separator());
        fileMenu.add(m_exitAction);

        // Edit menu
        editMenu.add(m_cutAction);
        editMenu.add(m_copyAction);
        editMenu.add(m_pasteAction);
        editMenu.add(new Separator());
        editMenu.add(m_deleteAction);
        editMenu.add(m_selectAllAction);
        editMenu.add(new Separator());
        editMenu.add(m_undoAction);
        editMenu.add(m_redoAction);

        // View menu (contribution item contributes all views registered via
        // "perspectiveExtension" in ui plugin

        // viewMenu.add(m_openOutlineViewAction);
        // viewMenu.add(m_openConsoleViewAction);
        // viewMenu.add(m_openNavigatorViewAction);
        // viewMenu.add(m_openProgressViewAction);
        // viewMenu.add(m_openRepositoryHelpViewAction);
        // viewMenu.add(m_openRepositoryViewAction);
        // viewMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        viewMenu.add(m_showViewShortlistContributionItem);

        // Help menu
        // helpMenu.add(m_introAction);
        helpMenu.add(m_helpAction);
        helpMenu.add(m_helpSearchAction);
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        helpMenu.add(m_aboutAction);

    }

    /**
     * Fills the cool bar (under the menu bar) with tool shortcuts.
     * 
     * @see org.eclipse.ui.application.ActionBarAdvisor
     *      #fillCoolBar(org.eclipse.jface.action.ICoolBarManager)
     */
    @Override
    protected void fillCoolBar(final ICoolBarManager coolBar) {
        // create a toolbar and add it to the coolbar :)
        IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        coolBar.add(new ToolBarContributionItem(toolbar, "main"));

        // add tools to the toolbar
        toolbar.add(m_newWizardDropdownAction);
        toolbar.add(m_saveAction);
        toolbar.add(m_saveAllAction);
    }
}
