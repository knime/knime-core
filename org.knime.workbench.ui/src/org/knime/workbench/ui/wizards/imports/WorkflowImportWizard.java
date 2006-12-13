/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
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
 *   04.07.2006 (sieb): created
 */
package org.knime.workbench.ui.wizards.imports;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

/**
 * Wizard to import a knime workflow project. This project may be an archive or
 * a file structure. The import wizard completely uses the
 * <code>ExternalProjectImportWizard</code> from eclipse; just naming stuff is
 * changed.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowImportWizard extends ExternalProjectImportWizard {
    /**
     * @see org.eclipse.ui.IWorkbenchWizard# init(org.eclipse.ui.IWorkbench,
     *      org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init(final IWorkbench workbench,
            final IStructuredSelection currentSelection) {

        super.init(workbench, currentSelection);

        setWindowTitle("Import");

    }

    /**
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        super.addPages();

        getPages()[0].setTitle("Knime workflow projects");
        getPages()[0]
                .setDescription("This wizard imports Knime workflow projects"
                        + " given as an archive or given as a folder within"
                        + " the file system.");
    }
}
