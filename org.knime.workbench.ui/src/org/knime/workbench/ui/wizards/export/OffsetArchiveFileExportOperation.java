/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   18.08.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.export;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileExportOperation;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.IFileExporter;
import org.eclipse.ui.internal.wizards.datatransfer.ZipFileExporter;

/**
 * This class is necessary in order to export workflows that are located within 
 * a workflow group - but should strip the leading segments of the path in order
 * to export the workflow directory only.
 * 
 * @see WorkflowExportWizard
 *  
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class OffsetArchiveFileExportOperation extends
        ArchiveFileExportOperation {


    private IFileExporter m_exporter;
    
    private int m_offset = 0;
    
    private final String m_fileName;

    private IProgressMonitor m_monitor;
    
    /**
     * @param res the original resource container (workflow within a 
     *  workflow group) 
     * @param resources list of resources to export
     * @param filename the destination file name
     */
    public OffsetArchiveFileExportOperation(final IResource res, 
            final List resources, final String filename) {
        super(res, resources, filename);
        m_fileName = filename;
    }

    /**
     * 
     * @param offset the number of segments that should be striped for the 
     * destination path within the archive file
     */
    public void setOffset(final int offset) {
        m_offset = offset;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void initialize() throws IOException {
        m_exporter = new ZipFileExporter(m_fileName, false);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void run(final IProgressMonitor progressMonitor)
            throws InvocationTargetException, InterruptedException {
        m_monitor = progressMonitor;

        try {
            initialize();
        } catch (IOException e) {
            throw new InvocationTargetException(e, NLS.bind(
                    DataTransferMessages.ZipExport_cannotOpen, e.getMessage()));
        }

        try {
            // ie.- a single resource for recursive export was specified
            int totalWork = IProgressMonitor.UNKNOWN;
            try {
                totalWork = countSelectedResources();
            } catch (CoreException e) {
                // Should not happen
            }
            m_monitor.beginTask(
                    DataTransferMessages.DataTransfer_exportingTitle, 
                    totalWork);
                // ie.- a list of specific resources to export was specified
            exportSpecifiedResources();

            try {
                m_exporter.finished();
            } catch (IOException e) {
                throw new InvocationTargetException(
                        e,
                        NLS.bind(DataTransferMessages.ZipExport_cannotClose, 
                                e.getMessage()));
            }
        } finally {
            m_monitor.done();
        }
    }
    
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void exportResource(final IResource exportResource, 
            final int leadupDepth)
            throws InterruptedException {
        if (!exportResource.isAccessible()) {
            return;
        }
        String destinationName;
        IPath fullPath = exportResource.getFullPath();
        destinationName = fullPath.makeRelative().toString();
        if (m_offset > 0) {
            destinationName = fullPath.removeFirstSegments(m_offset - 1)
                .toString();
        }
        try {
            if (exportResource instanceof IFile) {
                m_exporter.write((IFile) exportResource, destinationName);
            }
        } catch (IOException e) {
            addError(NLS.bind(DataTransferMessages.DataTransfer_errorExporting, 
                    exportResource.getFullPath().makeRelative(), 
                    e.getMessage()), e);
        } catch (CoreException e) {
            addError(NLS.bind(DataTransferMessages.DataTransfer_errorExporting, 
                    exportResource.getFullPath().makeRelative(), 
                    e.getMessage()), e);
        }
        if (exportResource instanceof IContainer) {
            IResource[] children = null;
            try {
                children = ((IContainer) exportResource).members();
            } catch (CoreException e) {
                // this should never happen because an #isAccessible check is 
                // done before #members is invoked
                addError(NLS.bind(DataTransferMessages
                        .DataTransfer_errorExporting, 
                        exportResource.getFullPath()), e);
            }
            for (int i = 0; i < children.length; i++) {
                // this passed offset argument is anyway ignored
                exportResource(children[i], m_offset);
            }
        }
    }
    
}
