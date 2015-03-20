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

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.ide.IDEInternalPreferences;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.EclipseUtil;
import org.knime.product.rcp.intro.IntroPage;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.knime.workbench.ui.startup.StartupMessage;

/**
 * This advisor is used for configuring the workbench window and creating the action bar advisor.
 *
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    /**
     * Creates a new workbench window advisor for configuring a workbench window via the given workbench window
     * configurer.
     *
     * @param configurer an object for configuring the workbench window
     */
    public KNIMEApplicationWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer) {
        super(configurer);

    }

    /**
     * Creates our <code>KNIMEActionBarAdvisor</code> that form the action bars.
     *
     * @param configurer the action bar configurer for the window
     * @return the action bar advisor for the window
     *
     * @see KNIMEApplicationActionBarAdvisor
     * @see org.eclipse.ui.application.WorkbenchWindowAdvisor #createActionBarAdvisor
     *      (org.eclipse.ui.application.IActionBarConfigurer)
     */
    @Override
    public ActionBarAdvisor createActionBarAdvisor(final IActionBarConfigurer configurer) {
        return new KNIMEApplicationActionBarAdvisor(configurer);
    }

    /**
     * Configures the initial settings of the application window.
     */
    @Override
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

        // configurer.setInitialSize(new Point(1024, 768));

        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setShowProgressIndicator(true);
        configurer.setTitle(computeTitle());

        // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=36961
        // We want to use ResourceNavigator, so we have to introduce this
        // dependency to org.eclipse.ui.ide (otherwise we don't see our
        // Resources)
        org.eclipse.ui.ide.IDE.registerAdapters();
    }

    /**
     *
     */
    private void showIntroPage() {
        IPreferenceStore pStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        boolean showTipsAndTricks = !pStore.getBoolean(PreferenceConstants.P_HIDE_TIPS_AND_TRICKS);

//        if (!EclipseUtil.isRunFromSDK() && showTipsAndTricks) {
            IntroPage.INSTANCE.show(false);
            if (IntroPage.INSTANCE.isFreshWorkspace()) {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setMaximized(true);
            }
//        }

        if (!EclipseUtil.isRunFromSDK() && IntroPage.INSTANCE.isFreshWorkspace()) {
            for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
                for (IWorkbenchPage page : window.getPages()) {
                    for (IViewReference ref : page.getViewReferences()) {
                        if (ExplorerView.ID.equals(ref.getId())) {
                            final ExplorerView explorer = (ExplorerView)ref.getView(true);
                            explorer.getViewer().getControl().getDisplay().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    explorer.getViewer().expandAll();
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("restriction")
    @Override
    public void postWindowOpen() {
        super.postWindowOpen();
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IMenuManager menuManager = ((WorkbenchWindow)workbenchWindow).getMenuBarManager();
        menuManager.remove("org.eclipse.search.menu");
        menuManager.remove("org.eclipse.ui.run");
        menuManager.remove("org.eclipse.ui.run"); // yes, it's in there twice
        menuManager.remove("navigate");
        menuManager.updateAll(true);

        Collection<String> toRemove =
            Arrays.asList("org.eclipse.debug.ui.launchActionSet",
                "org.eclipse.ui.edit.text.actionSet.annotationNavigation",
                "org.eclipse.ui.edit.text.actionSet.navigation");

        ICoolBarManager toolbarManager = ((WorkbenchWindow)workbenchWindow).getCoolBarManager2();
        Stream.of(toolbarManager.getItems()).filter(item -> toRemove.contains(item.getId()))
            .forEach(item -> toolbarManager.remove(item));
        toolbarManager.update(true);

        showIntroPage();
        showStartupMessages();
    }

    private void showStartupMessages() {
        if (!StartupMessage.getAllStartupMessages().isEmpty()) {
            // show view only if there are messages
            try {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .showView("org.knime.workbench.ui.startupMessages");
            } catch (PartInitException ex) {
                NodeLogger.getLogger(getClass()).error("Could not open startup messages view: " + ex.getMessage(), ex);
            }
        }
    }

    @SuppressWarnings("restriction")
    private String computeTitle() {
        String title = null;
        IProduct product = Platform.getProduct();
        if (product != null) {
            title = product.getName();
        }
        if (title == null) {
            title = "KNIME";
        }

        String[] cmdLineArgs = Platform.getCommandLineArgs();
        String customName = null;
        String workspaceLocation = null;
        for (int i = 0; i < cmdLineArgs.length; i++) {
            if ("-showlocation".equalsIgnoreCase(cmdLineArgs[i])) {
                if (cmdLineArgs.length > i + 1) {
                    customName = cmdLineArgs[i + 1];
                }

                workspaceLocation = Platform.getLocation().toOSString();
                break;
            }
        }

        String workspaceName =
            IDEWorkbenchPlugin.getDefault().getPreferenceStore().getString(IDEInternalPreferences.WORKSPACE_NAME);
        if ((customName == null) && (workspaceName != null) && (workspaceName.length() > 0)) {
            customName = workspaceName;
        }

        return (customName != null ? customName : title) + (workspaceLocation != null ? " - " + workspaceLocation : "");
    }
}
