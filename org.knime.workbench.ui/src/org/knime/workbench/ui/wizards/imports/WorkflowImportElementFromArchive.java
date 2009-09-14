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

import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;

/**
 * Implementation of a workflow import element from an archive file 
 * (zip or tar).
 *  
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class WorkflowImportElementFromArchive 
    extends AbstractWorkflowImportElement {
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean isWorkflow() {
        ILeveledImportStructureProvider provider = getProvider();
        Object zipEntry = getEntry();
        if (provider.isFolder(zipEntry)) {
            List children = provider.getChildren(zipEntry);
            if (children == null) {
                return false;
            }
            for (Object o : children) {
                String elementLabel = provider.getLabel(o);
                if (elementLabel.equals(WorkflowPersistor.WORKFLOW_FILE)) {
                    return true;
                }
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
        ILeveledImportStructureProvider provider = getProvider(); 
        Object zipEntry = getEntry();    
        if (provider.isFolder(zipEntry)) {
            List children = provider.getChildren(zipEntry);
            if (children == null) {
                return false;
            }
            for (Object o : children) {
                String elementLabel = provider.getLabel(o);
                if (elementLabel.equals(MetaInfoFile.METAINFO_FILE)) {
                    return true;
                }
            }
        }
        return false;        
    }

    private final ILeveledImportStructureProvider m_provider; 
    
    // this is either org.eclipse.ui.internal.wizards.datatransfer.ZipFile or 
    // org.eclipse.ui.internal.wizards.datatransfer.TarFile
    private final Object m_entry;
    
    private String m_name;
    
    private final int m_level;
   
    /**
     * 
     * @param provider a provider to handle the archive
     * @param entry the archive file entry ({@link TarFile} or {@link ZipFile})
     * @param level indicates the level within the archive file hierarchy
     */
    public WorkflowImportElementFromArchive(
            final ILeveledImportStructureProvider provider, 
            final Object entry, final int level) {
        m_entry = entry;
        m_provider = provider;
        m_level = level;
        m_name = provider.getLabel(m_entry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getContents() {
        return m_provider.getContents(m_entry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_name;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void setName(final String newName) {
        m_name = newName;
    }

    /**
     * 
     * @return the level of this entry
     */
    public int getLevel() {
        return m_level;
    }
    
    /**
     * 
     * @return the provider of this archive file entry
     */
    public ILeveledImportStructureProvider getProvider() {
        return m_provider;
    }
    
    /**
     * 
     * @return the archive file entry ({@link TarFile} or {@link ZipFile})
     */
    public Object getEntry() {
        return m_entry;
    }
    
}
