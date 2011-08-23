/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;
import org.knime.workbench.ui.wizards.project.NewProjectWizard;

/**
 * This action is called when the user clicks "Open KNIME Workbench" in the
 * intro page. It creates a new project with the standard name "KNIME_project"
 * if no project exists in the workspace.
 *
 * @see NewProjectWizard
 *
 * @author Fabian Dill, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class NewProjectWizardIntroAction implements IIntroAction {

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final IIntroSite site, final Properties params) {
        try {
            // close the intro page
            IIntroManager introManager =
                    PlatformUI.getWorkbench().getIntroManager();
            IIntroPart introPart = introManager.getIntro();
            if (introPart != null) {
                introManager.closeIntro(introPart);
            }
            if (ResourcesPlugin.getWorkspace().getRoot().getProjects().length == 0) {
                PlatformUI.getWorkbench().getProgressService()
                        .busyCursorWhile(new IRunnableWithProgress() {
                            @Override
                            public void run(final IProgressMonitor monitor)
                                    throws InvocationTargetException,
                                    InterruptedException {
                                try {
                                    // call static method on NewProjectWizard
                                    NewProjectWizard.doFinish(new Path(
                                            "KNIME_project"), monitor);
                                } catch (CoreException ce) {
                                    throw new RuntimeException(ce);
                                }
                            }
                        });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
