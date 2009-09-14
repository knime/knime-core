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
