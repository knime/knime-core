/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdateMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdatePlugin;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdateScheduler;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.EclipseUtil;
import org.knime.product.rcp.intro.IntroPage;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.Preferences;

/**
 * Provides the initial workbench perspective ID (KNIME perspective).
 *
 * @author Florian Georg, University of Konstanz
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class KNIMEApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

    private KNIMEOpenDocumentEventProcessor m_openDocProcessor;

    /**
     * Simple constructor to store the {@code KNIMEOpenDocumentEventProcessor}
     *
     * @param openDocProcessor the KNIMEOpenDocumentEventProcessor handling the opening of KNIME files
     *
     */
    public KNIMEApplicationWorkbenchAdvisor(final KNIMEOpenDocumentEventProcessor openDocProcessor) {
        m_openDocProcessor = openDocProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitialWindowPerspectiveId() {
        return "org.knime.workbench.ui.ModellerPerspective";
    }

    /**
     * Initializes the application. At the moment it just forces the product to save and restore the window and
     * perspective settings (remembers whether editors are open, etc.).
     *
     * @param configurer an object for configuring the workbench
     *
     *
     * @see org.eclipse.ui.application.WorkbenchAdvisor #initialize(org.eclipse.ui.application.IWorkbenchConfigurer)
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
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer) {
        return new KNIMEApplicationWorkbenchWindowAdvisor(configurer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preStartup() {
        super.preStartup();

        if (!EclipseUtil.isRunFromSDK()) {
            if (IntroPage.INSTANCE.isFreshWorkspace()) {
                KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(new ExampleWorkflowExtractor());
            }
            changeDefaultPreferences();
        }
    }

    @SuppressWarnings("restriction")
    private void changeDefaultPreferences() {
        // enable automatic check for updates every day at 11:00
        Preferences node = DefaultScope.INSTANCE.getNode(AutomaticUpdatePlugin.PLUGIN_ID);
        node.putBoolean(org.eclipse.equinox.internal.p2.ui.sdk.scheduler.PreferenceConstants.PREF_AUTO_UPDATE_ENABLED,
            true);
        node.put(org.eclipse.equinox.internal.p2.ui.sdk.scheduler.PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE,
            org.eclipse.equinox.internal.p2.ui.sdk.scheduler.PreferenceConstants.PREF_UPDATE_ON_SCHEDULE);
        node.put(AutomaticUpdateScheduler.P_DAY, AutomaticUpdateMessages.SchedulerStartup_day);
        node.put(AutomaticUpdateScheduler.P_HOUR, AutomaticUpdateMessages.SchedulerStartup_11AM);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void eventLoopIdle(final Display display) {
        m_openDocProcessor.openFiles();
        super.eventLoopIdle(display);
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
        // showIntroPage();
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
                Status error = new Status(IStatus.ERROR, "org.knime.product", "Error while saving workspace", ex);
                Platform.getLog(myself).log(error);
            }
        }
    }
}
