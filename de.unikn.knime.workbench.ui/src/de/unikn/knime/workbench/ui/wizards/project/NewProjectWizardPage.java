/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.02.2005 (georg): created
 */
package de.unikn.knime.workbench.ui.wizards.project;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.swtdesigner.ResourceManager;

import de.unikn.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Page that is used as the "New Project" wizards' GUI.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewProjectWizardPage extends WizardPage {

    private Button m_checkCreateWorkflow;

    private Button m_checkAddDataset;

    private Text m_projectName;

    /**
     * Create & init the page.
     * 
     */
    public NewProjectWizardPage() {
        super("wizardPage");
        setTitle("New KNIME Project Wizard");
        setDescription("Create a new project layout for data analysis."
                + "\r\nThis may serve as container for KNIME workflows.");
        setImageDescriptor(ResourceManager.getPluginImageDescriptor(
                KNIMEUIPlugin.getDefault(), "icons/logo32x32.png"));
    }

    /**
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage
     *      #createControl(org.eclipse.swt.widgets.Composite)
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
        m_projectName.setLayoutData(new GridData(
                GridData.FILL_HORIZONTAL));
        m_projectName.setText("new_project");

        final Group group = new Group(container, SWT.NONE);
        final GridLayout gridLayout1 = new GridLayout();
        group.setLayout(gridLayout1);
        group.setText("Options");
        final GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);

        m_checkCreateWorkflow = new Button(group, SWT.CHECK);
        m_checkCreateWorkflow.setSelection(true);
        m_checkCreateWorkflow.setText("Create (empty) workflow file");

        m_checkAddDataset = new Button(group, SWT.CHECK);
        m_checkAddDataset.setEnabled(false);
        m_checkAddDataset.setText("Add example dataset (iris)");
    }

    /**
     * @return The project name
     */
    public String getProjectName() {
        return m_projectName.getText();
    }

    /**
     * @return The state of the "create workflow" checkbox
     */
    public boolean getCreateWorkflowFile() {
        return m_checkCreateWorkflow.getSelection();
    }

    /**
     * @return The state of the "add dataset" checkbox
     */
    public boolean getAddDataset() {
        return m_checkAddDataset.getSelection();
    }
}
