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
 *   13.08.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.imports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;

/**
 * Represents a workflow import element from a directory or file.
 *
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class WorkflowImportElementFromFile
    extends AbstractWorkflowImportElement {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowImportElementFromFile.class);

    /**
     *
     * @param dir the directory to test
     * @return true if the directory is a workflow
     */
    public static boolean isWorkflow(final File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        File workflowFile = new File(dir, WorkflowPersistor.WORKFLOW_FILE);
        // if itself contains a .knime file -> return this
        if (workflowFile.exists()) {
            File parentWorkflowFile = new File(dir.getParent(),
                    WorkflowPersistor.WORKFLOW_FILE);
            if (!parentWorkflowFile.exists()) {
                // check whether the parent does not contain a workflow file
                // in order to prevent importing of meta nodes
                return true;
            }
        }
        return false;
    }

    private final File m_file;

    private final boolean m_isWorkflowSelected;

    /**
     *
     * @param dir workflow folder or workflow group
     */
    public WorkflowImportElementFromFile(final File dir) {
        this(dir, false);
    }

    /**
     *
     * @param dir the workflow folder containing the workflow
     * @param isWorkflowSelected true if a workflow was selected as the tree
     *  root (then an artificial parent has to be created and this has to be
     *  ignored in {@link #getOriginalPath()}) and {@link #getRenamedPath()}
     */
    public WorkflowImportElementFromFile(final File dir,
            final boolean isWorkflowSelected) {
        super(dir.getName());
        m_file = dir;
        m_isWorkflowSelected = isWorkflowSelected;
    }

    /**
     *
     * @return the wrapped directory
     */
    public File getFile() {
        return m_file;
    }

    /**
     * {@inheritDoc}
     * @throws FileNotFoundException
     */
    @Override
    public InputStream getContents() {
        try {
            return new FileInputStream(m_file);
        } catch (FileNotFoundException e) {
            // file was not found
            LOGGER.error("File not found " + m_file.getName(), e);
        }
        return null;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public IPath getRenamedPath() {
        if (m_isWorkflowSelected) {
            return new Path(getName());
        }
        return super.getRenamedPath();
    }

    /**
    *
    * {@inheritDoc}
    */
   @Override
   public IPath getOriginalPath() {
       if (m_isWorkflowSelected) {
           return new Path(getOriginalName());
       }
       return super.getOriginalPath();
   }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isWorkflow() {
        File dir = getFile();
        if (!dir.isDirectory()) {
            return false;
        }
        File workflowFile = new File(dir, WorkflowPersistor.WORKFLOW_FILE);
        // if itself contains a .knime file -> return this
        if (workflowFile.exists()) {
            File parentWorkflowFile = new File(dir.getParent(),
                    WorkflowPersistor.WORKFLOW_FILE);
            if (!parentWorkflowFile.exists()) {
                // check whether the parent does not contain a workflow file
                // in order to prevent importing of meta nodes
                return true;
            }
        }
        return false;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isWorkflowGroup() {
        File dir = getFile();
        if (!dir.isDirectory()) {
            return false;
        }
        File workflowGroupFile = new File(dir, MetaInfoFile.METAINFO_FILE);
        // if itself contains a .knime file -> return this
        if (workflowGroupFile.exists()) {
            return true;
        }
        return false;
    }

}
