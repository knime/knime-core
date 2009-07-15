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
 */
package org.knime.workbench.ui.masterkey;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class MasterKeyDialog extends Dialog {

    private MasterKeyPreferencePage m_prefPage;

    /**
     * @param parentShell the parent shell
     */
    public MasterKeyDialog(final Shell parentShell) {
        super(parentShell);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        // create OK button only
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);
        composite.setSize(400, 600);
        m_prefPage = new MasterKeyPreferencePage();
        m_prefPage.initPrefStore();
        m_prefPage.createControl(composite);
        m_prefPage.initialize();
        composite.getShell().setText("Master Key");
        return composite;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {
        if (m_prefPage.performOk()) {
            super.okPressed();
        }
    }

}
