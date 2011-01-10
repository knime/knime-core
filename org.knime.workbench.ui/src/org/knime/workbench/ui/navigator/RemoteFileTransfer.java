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
package org.knime.workbench.ui.navigator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.knime.core.node.NodeLogger;

/**
 * Transfer for a drag and drop of files (actually zip archive files containing
 * a workflow) that first need to be created after they have been dropped,
 * before the drop can be finished (i.e. the workflow can be imported). Allows
 * for a delayed copy of the files. The drop target initiates the copy before
 * accessing the file content (actually it can decide whether the file needs to
 * be downloaded/copied, or how the drop can be finished otherwise).<br />
 * The data set in the event is an array of URIs, denoting the remote files to
 * transfer. The locations of the downloaded/copied files are specified by the
 * corresponding callback/copy method.
 *
 * @author ohl, University of Konstanz
 */
public class RemoteFileTransfer extends ByteArrayTransfer {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(RemoteFileTransfer.class);

    private static final RemoteFileTransfer INSTANCE = new RemoteFileTransfer();

    private static final String TYPE_NAME = "downloaded-file-transfer";

    private static final int TYPE_ID = registerType(TYPE_NAME);

    private final CopyOnWriteArraySet<FileContentProvider> m_callbacks =
            new CopyOnWriteArraySet<FileContentProvider>();

    private Object m_tag;

    /**
     * Avoid explicit instantiation.
     */
    private RemoteFileTransfer() {
        // don't instantiate, use the global instance.
    }

    /**
     * @return the single instance.
     */
    public static RemoteFileTransfer getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void javaToNative(final Object object,
            final TransferData transferData) {
        if (object == null || !(object instanceof URI[]))
            return;

        if (isSupportedType(transferData)) {
            URI[] remoteFiles = (URI[])object;
            try {
                // write data to a byte array,
                // then ask super to convert to pMedium
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream writeOut = new DataOutputStream(out);
                for (int i = 0; i < remoteFiles.length; i++) {
                    String uriStr = remoteFiles[i].toString();
                    byte[] name = uriStr.getBytes();
                    writeOut.writeInt(name.length);
                    writeOut.write(name);
                }
                byte[] buffer = out.toByteArray();
                writeOut.close();
                super.javaToNative(buffer, transferData);
            } catch (IOException e) {
                LOGGER.error("IO Exception", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object nativeToJava(final TransferData transferData) {

        if (isSupportedType(transferData)) {

            byte[] buffer = (byte[])super.nativeToJava(transferData);
            if (buffer == null) {
                return null;
            }
            ArrayList<URI> remoteFiles = new ArrayList<URI>();
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(buffer);
                DataInputStream readIn = new DataInputStream(in);
                while (readIn.available() > 1) {
                    int size = readIn.readInt();
                    byte[] name = new byte[size];
                    readIn.read(name);
                    String uriStr = new String(name);
                    remoteFiles.add(new URI(uriStr));
                }
                readIn.close();
            } catch (IOException ex) {
                LOGGER.error("IO Exception", ex);
                return null;
            } catch (URISyntaxException use) {
                LOGGER.error("URI Syntax Exception", use);
                return null;
            }
            return remoteFiles.toArray(new URI[remoteFiles.size()]);
        }
        return null;
    }

    /**
     * When the drag starts, set a unique tag. Check this tag when the Transfer
     * calls your callback notifying you to provide the file content. Only store
     * the file when the tag is yours. Also, clear the tag after the drag
     * finished.
     *
     * @param tag unique tag for the initiator of the drag to verify and to
     *            recognize that it is him to provide content for the transfer
     */
    public void setTag(final Object tag) {
        m_tag = tag;
    }

    /**
     * @return the tag previously set by {@link #setTag(Object)}
     */
    public Object getTag() {
        return m_tag;
    }

    /**
     * All drag sources supporting this transfer must register a callback, that
     * is called, when the drop is performed and the data of the file is to be
     * written (delayed copy). The dragSetData only sets a temp file name (empty
     * file). It also sets a tag for the drag initiator to recognize that it is
     * him to provide the file data.
     *
     * @param dataSource the callback called when the file content it to be
     *            written.
     */
    public void addDragSource(final FileContentProvider dataSource) {
        if (dataSource != null) {
            m_callbacks.add(dataSource);
        }
    }

    /**
     * If a drag initiator disappears, remove its callback.
     *
     * @param dataSource the callback interface to remove
     */
    public void removeDragSource(final FileContentProvider dataSource) {
        m_callbacks.remove(dataSource);
    }

    /**
     * The drop target should call this, before accessing the files specified in
     * the event.data. During the call of this method the content of the files
     * is transfered and written into the files (delayed copy).
     *
     * @param eventData that is the event.data
     * @return String[] the absolute paths to the files written. The files are
     *         deleted by the drop source, if necessary.
     */
    public String[] requestFileContent(final URI[] eventData) {
        String[] files = null;
        for (FileContentProvider callback : m_callbacks) {
            files = callback.writeFile(eventData, m_tag);
            if (files != null) {
                break;
            }
        }

        if (files == null) {
            LOGGER.debug("Implementation error: none of the registered drag"
                    + " sources provided content for the dropped file "
                    + "(RemoteFileTransfer");
        } else {
            for (String f : files) {
                if (f == null) {
                    LOGGER.debug("Implementation error: provided filename of"
                            + " dragged file (RemoteFileTransfer) is null");
                } else if (f.isEmpty()) {
                    LOGGER.debug("Implementation error: provided filename of"
                            + " dragged file (RemoteFileTransfer) is empty");
                }
            }
        }
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean validate(final Object object) {
        if ((!(object instanceof URI[])) || (((URI[])object).length == 0)) {
            return false;
        }
        for (URI s : (URI[])object) {
            if (s == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int[] getTypeIds() {
        return new int[]{TYPE_ID};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getTypeNames() {
        return new String[]{TYPE_NAME};
    }

    /**
     * Register this interface with the transfer. When the drop is performed the
     * target calls all(!) registered providers to fill the file with data.
     * Check the tag. If it is the one you set when the drag started it is you
     * that should write the file (but only then!).
     *
     * @author ohl, University of Konstanz
     */
    public interface FileContentProvider {
        /**
         * Called by the drop target to actually get the file filled with data
         * (delayed copy). Only copy the file, if the tag is the one set by you
         * during drag start.
         *
         * @param eventData the data set by setEventData in the drag listener
         * @param tag the tag set by the drag source during drag start
         * @return the absolute paths of the files. The files will be deleted if
         *         necessary by the target.
         */
        public String[] writeFile(URI[] eventData, Object tag);
    }
}
