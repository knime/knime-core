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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 26, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.knime.core.node.NodeLogger;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.6
 */
public final class FileStoreHandlerRepository {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(FileStoreHandler.class);

    private final ConcurrentHashMap<UUID, DefaultFileStoreHandler> m_handlerMap;

    /**
     *  */
    public FileStoreHandlerRepository() {
        m_handlerMap = new ConcurrentHashMap<UUID, DefaultFileStoreHandler>();
    }

    void addFileStoreHandler(final DefaultFileStoreHandler handler) {
        m_handlerMap.put(handler.getStoreUUID(), handler);
    }

    void removeFileStoreHandler(final DefaultFileStoreHandler handler) {
        FileStoreHandler old = m_handlerMap.remove(handler.getStoreUUID());
        if (old == null) {
            throw new IllegalArgumentException(
                    "No such file store hander: " + handler);
        }
    }

    /** Get handler to id, never null.
     * @param storeHandlerUUID the store id
     * @return the handler.
     * @throws IllegalStateException If store is not registered. */
    public FileStoreHandler getHandler(final UUID storeHandlerUUID) {
        FileStoreHandler h = m_handlerMap.get(storeHandlerUUID);
        if (h == null) {
            final String s =
                "Unknown file store handler to UUID " + storeHandlerUUID;
            LOGGER.error(s);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Valid file store handlers are:");
                LOGGER.debug("--------- Start --------------");
                for (FileStoreHandler fsh : m_handlerMap.values()) {
                    LOGGER.debug("  " + fsh);
                }
                LOGGER.debug("--------- End ----------------");
            }
            throw new IllegalStateException(s);
        }
        return h;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "File store handler repository ("
            + m_handlerMap.size() + " handler(s))";
    }


}
