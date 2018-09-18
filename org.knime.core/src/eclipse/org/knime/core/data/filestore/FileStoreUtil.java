/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jul 11, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.FileStoreProxy;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.WriteFileStoreHandler;
import org.knime.core.node.NodeLogger;

/** Internal helper class, not to be used by clients.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.6
 */
public final class FileStoreUtil {

    private FileStoreUtil() {
        // no op
    }


    /** @noreference This method is not intended to be referenced by clients. */
    public static FileStoreKey getFileStoreKey(final FileStore store) {
        return store.getKey();
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static FileStoreKey getFileStoreKey(final FileStoreCell cell) {
        return cell.getFileStoreKey();
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static FileStoreKey[] getFileStoreKeys(final FileStoreCell cell) {
        return cell.getFileStoreKeys();
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static FileStore getFileStore(final FileStoreCell cell) {
        return cell.getFileStore();
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static FileStore[] getFileStores(final FileStoreCell cell) {
        return cell.getFileStores();
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static int getNumFileStores(final FileStoreCell cell) {
        return cell.getNumFileStores();
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static List<FileStore> getFileStores(final FileStorePortObject po) {
        return IntStream.range(0, po.getFileStoreCount()).mapToObj(i -> po.getFileStore(i)).collect(Collectors.toList());
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static IFileStoreHandler getFileStoreHandler(final FileStoreCell cell) {
        return getFileStoreHandler(cell.getFileStore());
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static IFileStoreHandler getFileStoreHandler(final FileStore filestore) {
        return filestore.getFileStoreHandler();
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static void invokeFlush(final FlushCallback flushCallback) throws IOException {
        if (flushCallback instanceof FileStoreCell) {
            ((FileStoreCell)flushCallback).callFlushIfNeeded();
        } else if (flushCallback instanceof FileStorePortObject) {
            ((FileStorePortObject)flushCallback).callFlushIfNeeded();
        } else {
            NodeLogger.getLogger(FileStoreUtil.class).coding("Unknown implementation of a "
                    + FlushCallback.class.getSimpleName() + ": "
                    + flushCallback == null ? "<null>" : flushCallback.getClass().getName());
        }
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static void retrieveFileStoreHandlersFrom(final FileStoreCell fsCell,
        final FileStoreKey[] fileStoreKeys,
        final FileStoreHandlerRepository fileStoreHandlerRepository) throws IOException {
        fsCell.retrieveFileStoreHandlersFrom(fileStoreKeys, fileStoreHandlerRepository);
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static void retrieveFileStoreHandlerFrom(final FileStorePortObject object,
        final List<FileStoreKey> keys, final FileStoreHandlerRepository repos) throws IOException {
        object.retrieveFileStoreHandlerFrom(keys, repos);
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static void retrieveFileStoreHandlers(
        final FileStorePortObject sourceFSObj, final FileStorePortObject resultFSObj,
        final IWriteFileStoreHandler newHandler) throws IOException {
        List<FileStoreProxy> sourceFSProxies = sourceFSObj.getFileStoreProxies();
        List<FileStoreKey> sourceFSKeys = new ArrayList<FileStoreKey>(sourceFSProxies.size());
        FileStoreHandlerRepository commonFSHandlerRepo = null;
        for (FileStoreProxy proxy : sourceFSProxies) {
            FileStoreKey newKey;
            if (newHandler != null) {
                newKey = newHandler.translateToLocal(proxy.getFileStore(), resultFSObj);
            } else {
                newKey = proxy.getFileStoreKey();
            }
            sourceFSKeys.add(newKey);
            FileStoreHandlerRepository fsHandlerRepo = proxy.getFileStoreHandler().getFileStoreHandlerRepository();
            if (commonFSHandlerRepo == null) {
                commonFSHandlerRepo = fsHandlerRepo;
            } else {
                assert commonFSHandlerRepo == fsHandlerRepo : "File Stores in port object have different file "
                        + "store handler repositories: " + commonFSHandlerRepo + " vs. " + fsHandlerRepo;
            }
        }
        FileStoreHandlerRepository resultRepos = newHandler != null
                ? newHandler.getFileStoreHandlerRepository() : commonFSHandlerRepo;
        resultFSObj.retrieveFileStoreHandlerFrom(sourceFSKeys, resultRepos);
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public static FileStore createFileStore(
            final WriteFileStoreHandler handler, final FileStoreKey key) {
        return new FileStore(handler, key);
    }

}
