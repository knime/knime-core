package org.knime.core.data.container.fast;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.TimerTask;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.container.fast.AdapterRegistry.DataSpecAdapter;
import org.knime.core.data.table.column.ColumnType;
import org.knime.core.data.table.store.TableReadStore;
import org.knime.core.data.table.store.TableStoreFactory;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.KNIMETimer;

/**
 * Fast table which is lazily loaded. Similar to 'ContainerTable' with delayed 'CopyTask'
 *
 * @author Christian Dietz, KNIME GmbH
 * @since 4.2
 */
class LazyFastTable extends AbstractFastTable {

    private AccessTask m_readTask;

    private TableReadStore m_store;

    private IDataRepository m_repository;

    LazyFastTable(final IDataRepository repository, final ReferencedFile fileRef, final int tableId,
        final DataTableSpec spec, final TableStoreFactory factory, final DataSpecAdapter adapter,
        final long size, final boolean isRowKeys) {
        super(tableId, spec, isRowKeys, adapter);
        m_repository = repository;
        m_readTask = new AccessTask(fileRef, factory, adapter.getColumnTypes(), size);
    }

    @Override
    public void ensureOpen() {
        // TODO revise logic here, especially sync logic
        AccessTask readTask = m_readTask;
        if (readTask == null) {
            return;
        }
        synchronized (m_readTask) {
            // synchronized may have blocked when another thread was
            // executing the copy task. If so, there is nothing else to
            // do here
            if (m_readTask == null) {
                return;
            }
            m_store = m_readTask.createTableStore();
            m_readTask = null;
        }
    }

    @Override
    public void clear() {
        try {
            // just close the store. no need to clean anything up
            // TODO null check required?
            if (m_store != null) {
                m_store.close();
            }
        } catch (Exception ex) {
        }

        if (m_readTask != null) {
            m_repository.removeTable(getTableId());
        }
    }

    @Override
    public TableReadStore getStore() {
        ensureOpen();
        return m_store;
    }

    @Override
    public long size() {
        ensureOpen();
        return m_store.size();
    }

    @Override
    public void saveToFile(final File f, final NodeSettingsWO settings, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        throw new IllegalStateException("Lazy fast tables have already been saved. This is an implementation error!");
    }

    final static class AccessTask {

        private final NodeLogger LOGGER = NodeLogger.getLogger(AccessTask.class);

        private final ReferencedFile m_fileRef;

        private final TableStoreFactory m_factory;

        private final ColumnType<?, ?>[] m_columnTypes;

        private final long m_size;

        /**
         * Delay im ms until copying process is reported to LOGGER, small files won't report their copying (if faster
         * than this threshold).
         */
        private static final long NOTIFICATION_DELAY = 3000;

        AccessTask(final ReferencedFile file, final TableStoreFactory factory, final ColumnType<?, ?>[] columnTypes,
            final long size) {
            m_columnTypes = columnTypes;
            m_factory = factory;
            m_fileRef = file;
            m_size = size;
        }

        TableReadStore createTableStore() {
            // timer task which prints a INFO message that the copying
            // is in progress.
            TimerTask timerTask = null;
            m_fileRef.lock();
            try {
                final File file = m_fileRef.getFile();
                timerTask = new TimerTask() {
                    /** {@inheritDoc} */
                    @Override
                    public void run() {
                        double sizeInMB = file.length() / (double)(1 << 20);
                        String size = NumberFormat.getInstance().format(sizeInMB);
                        LOGGER.debug(
                            "Extracting data file \"" + file.getAbsolutePath() + "\" to temp dir (" + size + "MB)");
                    }
                };
                KNIMETimer.getInstance().schedule(timerTask, NOTIFICATION_DELAY);

                // TODO why do we have to copy if we make sure we don't delete?
                return m_factory.create(m_columnTypes, file, m_size);
            } catch (Exception ex) {
                throw new RuntimeException(
                    "Exception while accessing file: \"" + m_fileRef.getFile().getName() + "\": " + ex.getMessage(),
                    ex);
            } finally {
                if (timerTask != null) {
                    timerTask.cancel();
                }
                m_fileRef.unlock();
            }
        }
    }
}