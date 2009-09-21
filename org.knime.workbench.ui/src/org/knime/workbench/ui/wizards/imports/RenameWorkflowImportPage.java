/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   14.08.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.imports;

import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A page where workflows and workflow groups which should be imported can be
 * renamed. This is necessary if a resource with the same name already exists in
 * the target destination.
 *
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class RenameWorkflowImportPage extends WizardPage {

    private final Collection<IWorkflowImportElement>m_invalids;

    private final WorkflowImportSelectionPage m_previousPage;

    /** Identifier for this page. */
    public static final String NAME = "Rename duplicate workflows";

    private boolean m_isNameValid = true;

    private boolean m_nameExists = false;

    /**
     *
     * @param invalids the duplicate workflows
     * @param previousPage the previous import selection page to update when
     *  the name(s) are changed
     */
    public RenameWorkflowImportPage(
            final WorkflowImportSelectionPage previousPage,
            final Collection<IWorkflowImportElement> invalids) {
        super(NAME);
        m_previousPage = previousPage;
        m_invalids = invalids;
        setTitle("Rename Page");
        setDescription("Rename the workflows to import");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Group overall = new Group(parent, SWT.SHADOW_ETCHED_IN);
        overall.setLayout(new GridLayout(1, false));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        overall.setLayoutData(gridData);
        overall.setText("Duplicate workflows:");

        if (m_invalids == null || m_invalids.isEmpty()) {
            Label ok = new Label(overall, SWT.NONE);
            ok.setText("No duplicate workflows found!");
            setControl(overall);
            return;
        } // else
        GridData horizontalFill = new GridData(GridData.FILL_HORIZONTAL);
        for (final IWorkflowImportElement element : m_invalids) {
            Group row = new Group(overall, SWT.FILL);
            row.setLayout(new GridLayout(2, true));
            row.setLayoutData(horizontalFill);
            // path // name // overwrite? checkbox
            Label path = new Label(row, SWT.NONE);
            path.setText(element.getOriginalPath().toString());
            final Text name = new Text(row, SWT.FILL | SWT.BORDER);
            name.setText(element.getName());
            name.setLayoutData(horizontalFill);
            name.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    String newName = name.getText();
                    checkIsNameValid(newName);
                    checkNameExists(newName);
                    if (m_isNameValid) {
                        // if the name is valid -> set it
                        // such that it is visible on the previous page
                        // (in the tree viewer)
                        // Convenient if the element is renamed to another
                        // existing resource in the target location
                        element.setName(newName);
                    }
                    if (m_isNameValid && !m_nameExists) {
                        setErrorMessage(null);
                        element.setInvalid(false);
                        setPageComplete(canFinish());
                    }
                    // in any case validate the workflows
                    m_previousPage.validateWorkflows();
                    getWizard().getContainer().updateButtons();
                }
            });
        }
        setControl(overall);
    }

    private boolean checkIsNameValid(final String name) {
        IStatus isValid = ResourcesPlugin.getWorkspace().validateName(
                name, IResource.FOLDER);
        m_isNameValid = isValid.isOK();
        if (!m_isNameValid) {
            setErrorMessage(name + " is not a valid name!");
            setPageComplete(false);
        }
        return m_isNameValid;
    }

    private boolean checkNameExists(final String name) {
        IPath destination = m_previousPage.getDestinationPath();
        destination = destination.append(name);
        IResource exists = ResourcesPlugin.getWorkspace().getRoot().findMember(
                destination);
        m_nameExists = exists != null;
        if (m_nameExists) {
            setErrorMessage(name + " already exists in target destination!");
            setPageComplete(false);
        }
        return m_nameExists;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFlipToNextPage() {
        return false;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getPreviousPage() {
        // not possible to return to previous page....
        return super.getPreviousPage();
    }

    /**
     *
     * @return true if all workflows have been renamed
     */
    public boolean canFinish() {
        if (!m_isNameValid) {
            return false;
        }
        if (m_nameExists) {
            return false;
        }
        if (m_invalids == null) {
            return true;
        }
        for (IWorkflowImportElement e : m_invalids) {
            if (e.isInvalid()) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getNextPage() {
        return null;
    }

}
