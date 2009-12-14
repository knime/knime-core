/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 18, 2006 (sieb): created
 */
package org.knime.workbench.help.intro;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;

/**
 * Custom action to open the install wizard.
 *
 * @author Christoph, University of Konstanz
 */
public class InvokeInstallSiteAction extends Action {
    private static final String ID = "INVOKE_INSTALL_SITE_ACTION";

    /** P2 profile id. */
    public static final String KNIME_PROFILE_ID = "KNIMEProfile";

    
    
    public InvokeInstallSiteAction() {
        // FIXME: update this hardcoded update site!!!
        // FIXME: as of Eclipse 3.5 the update sites defined in the features
        // should be added automatically to the p2 update/install dialog
        // then this hack becomes obsolete
        String urlString = "http://www.knime.org/update/2.1/";
        try {
            // very similar to run() we use reflection to overcome compile errors
            // eclipse 3.5
            URL url = new URL(urlString);
            Class<?> class34 = Class.forName("org.eclipse.equinox.internal.provisional.p2.ui.operations.AddColocatedRepositoryOperation");
            Constructor<?> constructor = class34.getConstructor(String.class, URL.class);
            Object instance = constructor.newInstance("KNIME", url);
            Method m = class34.getMethod("execute", IProgressMonitor.class, IAdaptable.class);
            m.setAccessible(true);
            m.invoke(instance, new NullProgressMonitor(), null);
            // AddColocatedRepositoryOperation op 
            //   = new AddColocatedRepositoryOperation("KNIME", url);
            // op.execute(new NullProgressMonitor(), null);
        } catch (Exception e) {
            NodeLogger.getLogger(getClass()).warn("Unable to add " 
                    + "KNIME update site (" + urlString + " to repository," 
                    + "(3.4/3.5 problem?)", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            /* the following code does not compile on eclipse 3.5, we try to use
             * reflection here to overcome the compile errors. The 
             * UpdateAndInstallDialog class has been removed; replaced by
             * UpdateWizard class.
             * http://wiki.eclipse.org/Equinox/p2/Adding_Self-Update_to_an_RCP_Application 
             */
//            UpdateAndInstallDialog dialog = new UpdateAndInstallDialog(
//                    Display.getDefault().getActiveShell(), KNIME_PROFILE_ID);
//            dialog.open();
            Class<?> class34 = Class.forName("org.eclipse.equinox.internal.p2.ui.sdk.UpdateAndInstallDialog");
            Constructor<?> constructor = class34.getConstructor(Shell.class, String.class);
            Dialog dialog = (Dialog)constructor.newInstance(Display.getDefault().getActiveShell(), KNIME_PROFILE_ID);
            dialog.open();
        } catch (Exception e) {
            NodeLogger.getLogger(getClass()).warn("Unable to invoke update " 
                    + "action, could not instantiate dialog class " 
                    + "(3.4/3.5 problem?)", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
        try {
            Class.forName("org.eclipse.equinox.internal.p2.ui.sdk.UpdateAndInstallDialog");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return super.isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Opens the KNIME update site to install "
                + "additional KNIME features.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Update KNIME...";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }
}
