/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
