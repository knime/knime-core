/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
    
    private WizardProjectsImportPage m_importPage;
    private WizardProjectRenameDuplicatesPage m_renamePage;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench,
            final IStructuredSelection currentSelection) {

        super.init(workbench, currentSelection);

        setWindowTitle("Import");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        super.addPages();
        m_importPage =
                (WizardProjectsImportPage)getPages()[0];

        m_importPage.setTitle("KNIME workflow projects");
        m_importPage.setDescription("This wizard imports KNIME workflow " 
                + "projects given as an archive or given as a folder within"
                + " the file system.");

        m_renamePage =
                new WizardProjectRenameDuplicatesPage(m_importPage);
        // bugfix 1263
        m_renamePage.setTitle("Duplicate project names");
        m_renamePage.setDescription("Shows projects which have the same name "
                + "as a workspace project. You can edit the automatic " 
                + "proposals.");
        addPage(m_renamePage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
//        WizardProjectRenameDuplicatesPage renamePage =
//                (WizardProjectRenameDuplicatesPage)getPages()[1];
        if (m_importPage.isPageComplete()) {
            if (!m_importPage.getDoublesToImport()) {
                return true;
            } else if (m_importPage.getRenamePageShown()
                    // bugfix 1263
                    && m_renamePage.isPageComplete()) {
                return true;
            }
        }

        return false;
    }
}
