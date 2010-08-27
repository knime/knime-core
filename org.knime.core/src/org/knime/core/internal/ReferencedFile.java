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
 *   Mar 25, 2008 (wiswedel): created
 */
package org.knime.core.internal;

import java.io.File;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@link File} wrapper with modifiable parent location. This class is used
 * in cases in which nested elements keep a file reference and the file location
 * (particular of the parent or the parent of the parent) may change.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ReferencedFile {
    
    private final ReferencedFileDelegate m_delegate;
    
    private ReferencedFile(final ReferencedFileDelegate delegate) {
        m_delegate = delegate;
    }
    
    /** Creates new root element. 
     * @param rootDir The parent directory of the referenced file location.
     * @throws NullPointerException If the argument is null
     */
    public ReferencedFile(final File rootDir) {
        this(new RootFileDelegate(rootDir));
    }
    
    /** Creates new sub-element.
     * @param parent The parent location
     * @param base The name of this file
     * @throws NullPointerException If either argument is null
     */
    public ReferencedFile(final ReferencedFile parent, final String base) {
        this(new HierarchyElementFileDelegate(parent, base));
    }
    
    /** Locks this file location. Asynchronous invocations of
     * {@link #rename(String)} will block until {@link #unlock()} is called.
     * It will also disable the renaming of any element further up the 
     * hierarchy. Parallel <i>reading</i> of the resource is still possibly. */
    public void lock() {
        m_delegate.readLock();
    }
    
    /** Unlocks this file hierarchy. (Counterpart to {@link #lock()}).
     * @throws IllegalMonitorStateException 
     *          If monitor is not held by current thread. */
    public void unlock() {
        m_delegate.readUnlock();
    }
    
    /** Renames this (base) element as an atomic operation. &quot;This&quot;
     * element refers the current element in the hierarchy. The operation will
     * block until all read/write operations have finished. If the associated
     * file location exists (i.e. the file returned by {@link #getFile()} 
     * {@link File#exists() exists}, it will also be renamed. 
     * @param newBaseName The new name
     * @return whether the rename was successful: it returns true in two cases: 
     * (i) the file exists and was successfully renamed or (ii) 
     * if it does not exist (being positive that it has not been created just
     * yet) 
     * @throws NullPointerException If argument is null
     */
    public boolean rename(final String newBaseName) {
        return m_delegate.rename(newBaseName);
    }
    
    /** Get the {@link File} representing the full path of this referenced
     * file element. Please note that the returned file may be renamed after
     * this method returns. In order to circumvent this, you typically write
     * code as follows:
     * <pre>
     * ReferencedFile m_refFile = ... // this element
     * m_refFile.lock();
     * try {
     *   File file = m_refFile.getFile();
     *   // do something with this file
     * } finally {
     *   m_refFile.unlock();
     * }
     * </pre>
     * @return The file representing the full path of this referenced file */
    public File getFile() {
        return m_delegate.getFile(); 
    }
    
    /** Get the parent of this element or null if the file's parent is not
     * represented as a <code>ReferencedFile</code> object.
     * @return The parent or <code>null</code>. */
    public ReferencedFile getParent() {
        return m_delegate.getParent();
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        return m_delegate.equals(obj);
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_delegate.hashCode();
    }
    
    /** Get absolute path of the represented file.
     * {@inheritDoc} */
    @Override
    public String toString() {
        return m_delegate.toString();
    }
    
    /** Implementing class of {@link ReferencedFile}. */
    private abstract static class ReferencedFileDelegate {
        
        /** Acquire read lock. */
        abstract void readLock();
        /** Release read lock. */
        abstract void readUnlock();
        /** Acquire write lock. */
        abstract void writeLock();
        /** Release write lock. */
        abstract void writeUnlock();
        /** @param newName new name of hierarchy element
         * @return true if renaming was successful or file does'nt exist */
        abstract boolean rename(final String newName);
        /** @return the file representing this hierarchy element (including
         * full path. */
        abstract File getFile();
        /** @return parent referenced file or null if not available. */
        abstract ReferencedFile getParent();
        
        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            File file;
            if (obj instanceof ReferencedFile) {
                file = ((ReferencedFile)obj).getFile();
            } else if (obj instanceof ReferencedFileDelegate) {
                file = ((ReferencedFileDelegate)obj).getFile();
            } else {
                return false;
            }
            return getFile().equals(file);
        }
        
        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return getFile().hashCode();
        }
        
        /** {@inheritDoc} */
        @Override
        public String toString() {
            return getFile().getAbsolutePath();
        }
        
        /** Final implementation of the rename function.
         * @param file The file representing the full path
         * @param newName The new name of the base element (child name)
         * @return whether the rename was successful */
        final boolean renameFile(final File file, final String newName) {
            if (newName == null) {
                throw new NullPointerException("file name must not be null");
            }
            writeLock();
            // whatever (will) happen here should be surrounded by a try/finally
            try {
                boolean result = false;
                if (file.exists()) {
                    File newFile = new File(file.getParentFile(), newName);
                    result = file.renameTo(newFile);
                } else {
                    // we are positive ... the file hasn't been created just yet
                    result = true;
                }
                return result;
            } finally {
                writeUnlock();
            }
        }
        
    }
    
    /** Represents the parent of all hierarchical files. */
    private static final class RootFileDelegate extends ReferencedFileDelegate {
        private File m_rootFile;
        private final ReentrantReadWriteLock m_lock;
        
        /** @param root root directory of the hierarchy */
        public RootFileDelegate(final File root) {
            if (root == null) {
                throw new NullPointerException("Root file must not be null.");
            }
            m_rootFile = root;
            m_lock = new ReentrantReadWriteLock();
        }

        /** {@inheritDoc} */
        @Override
        File getFile() {
            return m_rootFile;
        }
        
        /** {@inheritDoc} */
        @Override
        ReferencedFile getParent() {
            return null;
        }
        
        /** {@inheritDoc} */
        @Override
        boolean rename(final String name) {
            boolean result = renameFile(getFile(), name);
            if (result) {
                m_rootFile = new File(getFile().getParentFile(), name);
            }
            return result;
        }

        /** {@inheritDoc} */
        @Override
        void readLock() {
            m_lock.readLock().lock();
        }

        /** {@inheritDoc} */
        @Override
        void readUnlock() {
            m_lock.readLock().unlock();
        }
        
        /** {@inheritDoc} */
        @Override
        void writeLock() {
            m_lock.writeLock().lock();
        }
        
        /** {@inheritDoc} */
        @Override
        void writeUnlock() {
            m_lock.writeLock().unlock();
        }
    }
    
    /** The parent file element if this object represents not the origin
     * but an element in the hierarchy. */
    private static final class HierarchyElementFileDelegate 
            extends ReferencedFileDelegate {
        private final ReferencedFile m_referencedFileParent;
        private String m_baseName;

        /** @param parent parent hierarchy element
         * @param baseName base name */
        HierarchyElementFileDelegate(
                final ReferencedFile parent, final String baseName) {
            m_referencedFileParent = parent;
            if (baseName == null) {
                throw new NullPointerException("Argument must not be null");
            }
            m_baseName = baseName;
        }

        /** {@inheritDoc} */
        @Override
        File getFile() {
            return new File(m_referencedFileParent.getFile(), m_baseName);
        }
        
        /** {@inheritDoc} */
        @Override
        ReferencedFile getParent() {
            return m_referencedFileParent;
        }
        
        /** {@inheritDoc} */
        @Override
        boolean rename(final String newName) {
            writeLock();
            try {
                if (renameFile(getFile(), newName)) {
                    m_baseName = newName;
                    return true;
                }
                return false;
            } finally {
                writeUnlock();
            }
        }

        /** {@inheritDoc} */
        @Override
        void readLock() {
            m_referencedFileParent.lock();
        }

        /** {@inheritDoc} */
        @Override
        void readUnlock() {
            m_referencedFileParent.unlock();
        }

        /** {@inheritDoc} */
        @Override
        void writeLock() {
            m_referencedFileParent.m_delegate.writeLock();
        }

        /** {@inheritDoc} */
        @Override
        void writeUnlock() {
            m_referencedFileParent.m_delegate.writeUnlock();
        }
    }

}
