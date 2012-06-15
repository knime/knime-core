/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *   04.06.2012 (meinl): created
 */
package org.knime.workbench.repository.view.custom;

import java.io.File;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.util.ImportFromFilePage;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.CustomRepositoryManager;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class CustomReposImportWizard extends Wizard {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CustomReposExportWizard.class);

    private final ImportFromFilePage m_page = new ImportFromFilePage(
            "Load custom repository",
            "Loads the custom repository structure from a file.");

    private final CustomRepositoryManager m_manager;

    private final TreeViewer m_treeViewer;

    /**
     * Creates a new export wizard.
     */
    CustomReposImportWizard(final CustomRepositoryManager manager,
            final TreeViewer treeViewer) {
        setWindowTitle("Export custom repository definition");
        m_page.addFileExtensionFilter("*.xml", "XML files");
        m_manager = manager;
        m_treeViewer = treeViewer;
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        addPage(m_page);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return m_page.isPageComplete();
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     *
     * @return <code>true</code> if finished successfully, <code>false</code>
     *         otherwise
     */
    @Override
    public boolean performFinish() {
        try {
            return export();
        } catch (Exception ex) {
            LOGGER.error("Error during import: " + ex.getMessage(), ex);
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
            mb.setText("Error during import");
            mb.setMessage("Could not load custom repository definition: "
                    + ex.getMessage());
            mb.open();
            return false;
        }
    }

    private boolean export() throws Exception {
        String fileSource = m_page.getFile();

        if (fileSource.isEmpty()) {
            m_page.setErrorMessage("No file specified!");
            return false;
        }

        if ((fileSource.trim().length() > 0)
                && ((fileSource.length() < 5) || (fileSource.lastIndexOf('.') < fileSource
                        .length() - 4))) {
            fileSource += ".xml";
        }

        File inFile = new File(fileSource);

        if (!inFile.exists()) {
            m_page.setErrorMessage("Selected file does not exist.");
            return false;
        }
        if (!inFile.isFile()) {
            m_page.setErrorMessage("Selected entry is not a file.");
            return false;
        }
        if (!inFile.canRead()) {
            m_page.setErrorMessage("Selected file is not readable.");
            return false;
        }

        m_manager.loadDefinition(inFile);
        m_treeViewer.setInput(m_manager
                .transformRepository(RepositoryManager.INSTANCE.getRoot()));
        return true;
    }
}
