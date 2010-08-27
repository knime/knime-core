/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 *
 * History
 *   28.08.2005 (Florian Georg): created
 */
package org.knime.product.rcp;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.internal.ide.model.WorkbenchAdapterBuilder;

/**
 * This advisor is used for configuring the workbench window and creating the
 * action bar advisor.
 *
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEApplicationWorkbenchWindowAdvisor extends
        WorkbenchWindowAdvisor {

    /**
     * Creates a new workbench window advisor for configuring a workbench window
     * via the given workbench window configurer.
     *
     * @param configurer an object for configuring the workbench window
     */
    public KNIMEApplicationWorkbenchWindowAdvisor(
            final IWorkbenchWindowConfigurer configurer) {
        super(configurer);

    }

    /**
     * Creates our <code>KNIMEActionBarAdvisor</code> that form the action
     * bars.
     *
     * @param configurer the action bar configurer for the window
     * @return the action bar advisor for the window
     *
     * @see KNIMEApplicationActionBarAdvisor
     * @see org.eclipse.ui.application.WorkbenchWindowAdvisor
     *      #createActionBarAdvisor
     *      (org.eclipse.ui.application.IActionBarConfigurer)
     */
    @Override
    public ActionBarAdvisor createActionBarAdvisor(
            final IActionBarConfigurer configurer) {
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

        // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=36961
        // We want to use ResourceNavigator, so we have to introduce this
        // dependency to org.eclipse.ui.ide (otherwise we don't see our
        // Resources)
        WorkbenchAdapterBuilder.registerAdapters();
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void postWindowOpen() {
        PlatformUI.getWorkbench().addWorkbenchListener(
                new IWorkbenchListener() {
            @Override
            public void postShutdown(final IWorkbench workbench) {
                // do nothing
            }
            @Override
            public boolean preShutdown(final IWorkbench workbench, 
                    final boolean forced) {
                // Remove consoles manually in time. Otherwise they are removed,
                // when the display is already disposed and this causes 
                // exceptions
                // this is a workaround for bug 
                // https://bugs.eclipse.org/bugs/show_bug.cgi?id=257970
                // reported here:
                // http://dev.eclipse.org/newslists/news.eclipse.platform.rcp/msg35729.html

                ConsolePlugin.getDefault().getConsoleManager().removeConsoles(
                        ConsolePlugin.getDefault().getConsoleManager()
                        .getConsoles());
                return true;
            }
        });
    }
    
}
