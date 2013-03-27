/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ----------------------------------------------------------------------------
 */
package org.knime.product.rcp;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.framework.Bundle;

/**
 * Provides the initial workbench perspective ID (KNIME perspective).
 *
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEApplicationWorkbenchAdvisor extends WorkbenchAdvisor {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(KNIMEApplicationWorkbenchAdvisor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitialWindowPerspectiveId() {
        return "org.knime.workbench.ui.ModellerPerspective";
    }

    /**
     * Initializes the application. At the moment it just forces the product to
     * save and restore the window and perspective settings (remembers whether
     * editors are open, etc.).
     *
     * @param configurer an object for configuring the workbench
     *
     *
     * @see org.eclipse.ui.application.WorkbenchAdvisor
     *      #initialize(org.eclipse.ui.application.IWorkbenchConfigurer)
     */
    @Override
    public void initialize(final IWorkbenchConfigurer configurer) {
        super.initialize(configurer);

        configurer.setSaveAndRestore(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
            final IWorkbenchWindowConfigurer configurer) {
        return new KNIMEApplicationWorkbenchWindowAdvisor(configurer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postStartup() {
        super.postStartup();
        // initialize org.eclipse.core.net so that the Authenticator
        // for the Update Manager is set and it asks the user for a password
        // if the Update Site is password protected
        IProxyService.class.getName();

        // show a tips&tricks dialog only if the intro page is not shown
        // and if KNIME is not started from within Eclipse
        String installLocation = Platform.getInstallLocation().getURL().toString();
        String configurationLocation = Platform.getConfigurationLocation().getURL().toString();
        if ((PlatformUI.getWorkbench().getIntroManager().getIntro() == null)
                && configurationLocation.contains(installLocation)) {
            // try to open T&T in a separate thread because if DNS resolution
            // does not work properly this blocks KNIME startup
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    tryOpenTipsAndTricks();
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postShutdown() {
        super.postShutdown();
        if (ResourcesPlugin.getWorkspace() != null) {
            try {
                ResourcesPlugin.getWorkspace().save(true, null);
            } catch (CoreException ex) {
                Bundle myself = Platform.getBundle("org.knime.product");
                Status error =
                        new Status(IStatus.ERROR, "org.knime.product",
                                "Error while saving workspace", ex);
                Platform.getLog(myself).log(error);
            }
        }
    }

    private void tryOpenTipsAndTricks() {
        boolean showTipsAndTricks = true;
        try {
            HttpURLConnection conn =
                    (HttpURLConnection)TipsAndTricksDialog.TIPS_AND_TRICKS_URL
                            .openConnection();
            conn.setConnectTimeout(500);
            conn.connect();
            conn.disconnect();
            IPreferenceStore pStore =
                    KNIMEUIPlugin.getDefault().getPreferenceStore();
            showTipsAndTricks =
                    !pStore.getBoolean(PreferenceConstants.P_HIDE_TIPS_AND_TRICKS);
        } catch (IOException ex) {
            // no internet connection
            LOGGER.info("Cannot connect to knime.org, not showing tips&tricks",
                    ex);
            showTipsAndTricks = false;
        } catch (Exception ex) {
            // likely no license classes found
            LOGGER.info("Error while reading preferences", ex);
        }

        if (showTipsAndTricks) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    TipsAndTricksAction.openTipsAndTricks();
                }
            });
        }
    }
}
