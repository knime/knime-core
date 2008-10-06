/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   19.07.2007 (sieb): created
 */
package org.knime.workbench.ui.wizards.imports;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.ui.wizards.imports.WizardProjectsImportPage.ProjectRecord;

/**
 * Creates the page that is used to rename imported projects, that are
 * duplicates according to the workspace.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class WizardProjectRenameDuplicatesPage extends WizardPage {

    /**
     * Needed to retrieve the selected projects.
     */
    private WizardProjectsImportPage m_previousePage;

    private List<ProjectTextfieldAssoc> m_projectTextfieldAssocs;

    private Composite m_scroller;

    /**
     * Creates the rename page.
     */
    public WizardProjectRenameDuplicatesPage(
            final WizardProjectsImportPage previousePage) {
        super("Rename doubles");
        m_projectTextfieldAssocs = new ArrayList<ProjectTextfieldAssoc>();
        m_previousePage = previousePage;
    }

    /**
     * {@inheritDoc}
     */
    public void createControl(final Composite parent) {
        Composite workArea = new Composite(parent, SWT.NONE);
        setControl(workArea);

        workArea.setLayout(new GridLayout());
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH
                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        createRenameList(workArea);
    }

    /**
     * Create the list of textfields to rename the dobules.
     * 
     * @param workArea
     */
    private void createRenameList(final Composite workArea) {
        // Group for the list
        ScrolledComposite renameGroup =
                new ScrolledComposite(workArea, SWT.V_SCROLL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        renameGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.FILL_VERTICAL));
        m_scroller = new Composite(renameGroup, SWT.None);
        m_scroller.setLayout(layout);
        m_scroller.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));
        // m_scroller.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
        // | GridData.FILL_VERTICAL));
        renameGroup.setContent(m_scroller);
        updateRenameList();
    }

    private class ProjectTextfieldAssoc {
        private ProjectRecord m_record;

        private Text m_textfield;

        private String m_lastText;

        public ProjectTextfieldAssoc(final ProjectRecord record,
                final Text textfield) {
            m_record = record;
            m_textfield = textfield;
        }

        public ProjectRecord getRecord() {
            return m_record;
        }

        public void setRecord(ProjectRecord record) {
            m_record = record;
        }

        public Text getTextfield() {
            return m_textfield;
        }

        public void setTextfield(Text textfield) {
            m_textfield = textfield;
        }

        public String getLastText() {
            return m_lastText;
        }

        public void setLastText(String lastText) {
            m_lastText = lastText;
        }
    }

    /**
     * Updates the ranme list of textfields. If the changes result in more or
     * different projects, true is returned.
     * 
     * @return true, if the changes result in more or different projects
     */
    boolean updateRenameList() {

        if (m_scroller == null) {
            return false;
        }
        boolean additionalChanges = false;
        // fist get all projects to be renamed
        ProjectRecord[] renameProjects = m_previousePage.getProjectsToRename();

        // now remove those from the display group that are no longer
        // to be renamed
        removeMissingFromRenameGroup(renameProjects);

        // then filter those, that are already displayed
        ProjectRecord[] notDisplayedProjects =
                getNotDisplayedProjects(renameProjects);
        if (notDisplayedProjects.length > 0) {
            additionalChanges = true;
        }

        // now get (from the already available ones) or create all associations
        for (final ProjectRecord record : notDisplayedProjects) {
            ProjectTextfieldAssoc assoc = getAssoc(record);

            if (assoc == null) {
                final Text textfield =
                        new Text(m_scroller, SWT.SINGLE | SWT.BORDER);
                textfield.addModifyListener(new RenameModifyListener());
                textfield.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                textfield.setText(getAutomaticRenameProposal(record
                        .getProjectName()));
                m_projectTextfieldAssocs.add(new ProjectTextfieldAssoc(record,
                        textfield));
                moveToCorrectPosition(textfield);
            } else if (assoc.getTextfield().isDisposed()) {
                final Text textfield =
                        new Text(m_scroller, SWT.SINGLE | SWT.BORDER);
                textfield.addModifyListener(new RenameModifyListener());
                textfield.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                textfield.setText(assoc.getLastText());
                assoc.setTextfield(textfield);
                moveToCorrectPosition(textfield);
            }
        }

        m_scroller.pack();
        return additionalChanges;
    }

    private ProjectRecord[] getNotDisplayedProjects(
            final ProjectRecord[] projects) {
        if (m_scroller == null) {
            return null;
        }
        List<ProjectRecord> resultList = new ArrayList<ProjectRecord>();
        Control[] textfields = m_scroller.getChildren();
        for (ProjectRecord record : projects) {
            // check each project record against all text fields
            boolean found = false;
            for (Control c : textfields) {
                Text field = (Text)c;
                // get the project textfield association for the given textfield
                ProjectTextfieldAssoc assoc = getAssoc(field);
                if (assoc == null) {
                    continue;
                }
                if (assoc.getRecord().getProjectName().equals(
                        record.getProjectName())) {
                    found = true;
                }
            }
            if (!found) {
                resultList.add(record);
            }
        }

        return resultList.toArray(new ProjectRecord[resultList.size()]);
    }

    private ProjectTextfieldAssoc getAssoc(final Text field) {
        for (ProjectTextfieldAssoc assoc : m_projectTextfieldAssocs) {
            if (field == assoc.getTextfield()) {
                return assoc;
            }
        }
        return null;
    }

    private ProjectTextfieldAssoc getAssoc(final ProjectRecord record) {
        for (ProjectTextfieldAssoc assoc : m_projectTextfieldAssocs) {
            if (record == assoc.getRecord()) {
                return assoc;
            }
        }
        return null;
    }

    private void removeMissingFromRenameGroup(final ProjectRecord[] records) {
        if (m_scroller == null) {
            return;
        }
        Control[] textfields = m_scroller.getChildren();
        for (Control textfield : textfields) {
            ProjectTextfieldAssoc assoc = getAssoc((Text)textfield);
            if (assoc == null) {
                continue;
            }
            boolean found = false;
            for (ProjectRecord record : records) {
                if (assoc.getRecord() == record) {
                    found = true;
                }
            }
            if (!found) {
                assoc.setLastText(((Text)textfield).getText());
                textfield.dispose();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        super.dispose();
        for (ProjectTextfieldAssoc assoc : m_projectTextfieldAssocs) {
            // copy the text before disposing the text fields
            if (!assoc.getTextfield().isDisposed()) {
                assoc.setLastText(assoc.getTextfield().getText());
                assoc.getTextfield().dispose();
            }
        }
    }

    private String getAutomaticRenameProposal(final String projectName) {
        assert m_previousePage.isProjectInWorkspace(projectName) : "Project "
                + projectName + " should not be in the workspace.";
        int i = 2;
        String extension = "_" + i;
        while (m_previousePage.isProjectInWorkspace(projectName + " "
                + extension)) {
            i++;
            extension = "(" + i + ")";
        }
        return projectName + " " + extension;
    }

    /**
     * Puts the content of the rename text fields into the project records.
     */
    public void putRenameTextFieldContentIntoProjectRecord() {
        for (ProjectTextfieldAssoc assoc : m_projectTextfieldAssocs) {
            String newName = null;
            if (assoc.getTextfield().isDisposed()) {
                newName = assoc.getLastText().trim();
            } else {
                newName = assoc.getTextfield().getText().trim();
            }
            assoc.getRecord().projectName = newName;
        }
    }

    private void moveToCorrectPosition(final Text textfield) {
        Text insertAboveChild = null;
        Control[] otherTextfields = m_scroller.getChildren();
        boolean above = false;
        for (Control c : otherTextfields) {
            if (c == textfield) {
                continue;
            }
            insertAboveChild = (Text)c;
            if (getAssoc(insertAboveChild).getRecord().projectName.compareTo(
                    getAssoc(textfield).getRecord().projectName) > 0) {
                above = true;
                break;
            }
        }
        if (above) {
            textfield.moveAbove(insertAboveChild);
        } else {
            textfield.moveBelow(insertAboveChild);
        }
    }

    private class RenameModifyListener implements ModifyListener {

        /**
         * {@inheritDoc}
         */
        public void modifyText(ModifyEvent e) {
            setPageComplete(validatePage());
        }
    }

    private boolean validatePage() {
        Control[] controls = m_scroller.getChildren();
        for (Control c : controls) {
            Text field = (Text)c;
            if (m_previousePage.isProjectInWorkspace(field.getText().trim())) {
                setMessage(null);
                setErrorMessage("Project name '" + field.getText().trim()
                        + "' is already used in the workspace.");
                return false;
            }
        }
        setMessage(null);
        setErrorMessage(null);
        return true;
    }
}
