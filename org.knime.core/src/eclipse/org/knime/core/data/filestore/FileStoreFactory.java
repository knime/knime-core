/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 */

package org.knime.core.data.filestore;

import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.node.ExecutionContext;

/**
 * This class allows creating new {@link FileStore} objects that can be
 * used to instantiate a {@link FileStoreCell}.
 *
 * <p>It's used as a wrapper around an {@link ExecutionContext}, which is
 * used in some API classes (e.g. the abstract group-by aggregator) to
 * hide the complexity of an {@link ExecutionContext} from the client
 * implementation.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.6
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class FileStoreFactory {

    /** Constructor with package scope to prevent extensions outside this package. */
    FileStoreFactory() {
    }

    /** Creates the file store object. This either is part of the workflow or not (depending upon how this factory
     * is created). For details see {@link ExecutionContext#createFileStore(String)} (including declared exceptions).
     * @param relativePath ...
     * @return ...
     * @throws IOException ...
     * @noreference Pending API. Feel free to use the method but keep in mind
     * that it might change in a future version of KNIME.
     */
    public abstract FileStore createFileStore(final String relativePath) throws IOException;

    /** Can be called by the client to disallow further creation of file stores.
     * @since 2.7
     */
    public abstract void close();

    /** Creates a factory whose file stores are part of the workflow. The factory delegates to
     * {@link ExecutionContext#createFileStore(String)}.
     * @param exec The non-null execution context.
     * @return A file store factory that creates file stores as part of the workflow.
     * @since 2.7
     */
    public static final FileStoreFactory createWorkflowFileStoreFactory(final ExecutionContext exec) {
        return new WorkflowFileStoreFactory(exec);
    }

    /** Creates a factory whose generated file stores are not part of the workflow. It's used in isolated
     * data tables (such as used in views).
     * @return Such a new factory.
     * @since 2.7
     */
    public static final FileStoreFactory createNotInWorkflowFileStoreFactory() {
        NotInWorkflowWriteFileStoreHandler fsh = new NotInWorkflowWriteFileStoreHandler(UUID.randomUUID());
        fsh.open();
        return new NotInWorkflowFileStoreFactory(fsh);
    }

    /** Implementation that creates file stores associated with the workflow (execution context). */
    private static final class WorkflowFileStoreFactory extends FileStoreFactory {

        private ExecutionContext m_exec;

        WorkflowFileStoreFactory(final ExecutionContext exec) {
            if (exec == null) {
                throw new NullPointerException("exec must not be null");
            }
            m_exec = exec;
        }

        @Override
        public FileStore createFileStore(final String relativePath) throws IOException {
            return m_exec.createFileStore(relativePath);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            // empty, leave it to the framework to decide when a node execution is over.
        }
    }

    /** Implementation that creates file stores outside the workflow scope. */
    private static final class NotInWorkflowFileStoreFactory extends FileStoreFactory {

        private final NotInWorkflowWriteFileStoreHandler m_notInWorkflowWriteFileStoreHandler;

        NotInWorkflowFileStoreFactory(final NotInWorkflowWriteFileStoreHandler fsh) {
            if (fsh == null) {
                throw new NullPointerException("exec must not be null");
            }
            m_notInWorkflowWriteFileStoreHandler = fsh;
        }

        @Override
        public FileStore createFileStore(final String relativePath) throws IOException {
            return m_notInWorkflowWriteFileStoreHandler.createFileStore(relativePath);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            m_notInWorkflowWriteFileStoreHandler.close();
        }
    }


}
