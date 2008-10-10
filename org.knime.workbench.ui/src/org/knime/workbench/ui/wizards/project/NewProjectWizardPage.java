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
 *   07.02.2005 (georg): created
 */
package org.knime.workbench.ui.wizards.project;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.ui.KNIMEUIPlugin;

import com.swtdesigner.ResourceManager;

/**
 * Page that is used as the "New Project" wizards' GUI.
 * 
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class NewProjectWizardPage extends WizardPage {
    private static final String INITIAL_PROJECT_NAME = "KNIME_project";

    private Text m_projectName;
    
    /**
     * Create & init the page.
     * 
     */
    public NewProjectWizardPage() {
        super("wizardPage");
        setTitle("New KNIME Project Wizard");
        setDescription("Create a new KNIME workflow project.");
        setImageDescriptor(ResourceManager.getPluginImageDescriptor(
                KNIMEUIPlugin.getDefault(), "icons/logo32x32.png"));
    }

    /**
     * {@inheritDoc}
     */
    public void createControl(final Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        container.setLayout(gridLayout);
        //
        setControl(container);

        final Label label = new Label(container, SWT.NONE);
        label.setText("&Name of the project to create:");

        m_projectName = new Text(container, SWT.BORDER);
        m_projectName.addModifyListener(new ModifyListener() {
            public void modifyText(final ModifyEvent e) {
                dialogChanged();
            }
        });
        m_projectName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        // initialize the new project name field
        // set a default file name which is not already available
        String projectName = INITIAL_PROJECT_NAME;

        // if the default initial project name is already set, the name is
        // appended by numbers until a new name has been found
        IContainer project = (IContainer)ResourcesPlugin.getWorkspace()
                .getRoot().findMember(new Path(projectName));

        for (int i = 2; project != null; i++) {
            projectName = INITIAL_PROJECT_NAME + i;
            project = (IContainer)ResourcesPlugin.getWorkspace().getRoot()
                    .findMember(new Path(projectName));
        }

        m_projectName.setText(projectName);
    }

    private void updateStatus(final String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    /**
     * Ensures that the text field is set properly.
     */
    private void dialogChanged() {

        String projectName = m_projectName.getText();
        IContainer container = (IContainer)ResourcesPlugin.getWorkspace()
                .getRoot().findMember(new Path(projectName));

        if (projectName.trim().length() == 0) {
            updateStatus("Project name must be specified");
            return;
        }

        if (container != null) {
            updateStatus("Project name already exists.");
            return;
        }

        updateStatus(null);
    }

    /**
     * @return The project name
     */
    public String getProjectName() {
        return m_projectName.getText();
    }
}
