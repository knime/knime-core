package org.knime.core.data.filestore.internal;

import java.io.IOException;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 *
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class VirtualProxyWriteFileStoreHandler extends DelegateWriteFileStoreHandler {

    private FileStoresInLoopCache m_allFileStoresCache;

    private FileStoresInLoopCache m_localFileStoresCache;

    public VirtualProxyWriteFileStoreHandler(final IWriteFileStoreHandler delegate) {
        super(delegate);
    }

    @Override
    public void open(final ExecutionContext exec) {
        super.open(exec);
        m_allFileStoresCache = new FileStoresInLoopCache(exec);
        m_localFileStoresCache = new FileStoresInLoopCache(exec);
    }

    @Override
    public boolean isReference() {
        return super.getDelegate().isReference();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileStore createFileStore(final String name) throws IOException {
        // filter duplicates?
        FileStore fs = super.createFileStore(name);
        m_allFileStoresCache.add(fs);
        return fs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileStoreKey translateToLocal(final FileStore fs, final FlushCallback flushCallback) {
        FileStoreKey fsk = super.translateToLocal(fs, flushCallback);
        m_localFileStoresCache.add(fsk);
        return fsk;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (m_allFileStoresCache != null) {
            try {
                m_allFileStoresCache.close();
                m_localFileStoresCache.close();
                m_allFileStoresCache.deletableUnusedFileStores(m_localFileStoresCache, this);
            } catch (CanceledExecutionException ex) {
                // TODO
                throw new RuntimeException("Canceled", ex);
            } finally {
                m_allFileStoresCache.dispose();
                m_localFileStoresCache.dispose();
                m_allFileStoresCache = null;
                m_localFileStoresCache = null;
            }
        }
        super.close();
    }

}