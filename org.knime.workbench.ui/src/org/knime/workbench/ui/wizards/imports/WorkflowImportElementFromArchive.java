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
        super(provider.getLabel(entry));
        m_entry = entry;
        m_provider = provider;
        m_level = level;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getContents() {
        return m_provider.getContents(m_entry);
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
