/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 18, 2006 (sieb): created
 */
package org.knime.workbench.help.intro;

import java.lang.reflect.Constructor;
import java.net.URL;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.AddColocatedRepositoryOperation;
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
        String urlString = "http://merkur02.inf.uni-konstanz.de/knime/trunk/2009-09-22/update/www.knime.org/update_2.x/";
        try {
            URL url = new URL(urlString);
            AddColocatedRepositoryOperation op 
                = new AddColocatedRepositoryOperation("KNIME", url);
            op.execute(new NullProgressMonitor(), null);
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
            return;
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
