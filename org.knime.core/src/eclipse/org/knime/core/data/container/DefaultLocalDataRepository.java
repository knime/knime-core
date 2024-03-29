/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   May 5, 2020 (dietzc): created
 */
package org.knime.core.data.container;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default implementation of a {@link ILocalDataRepository}.
 *
 * @author Christian Dietz, KNIME GmbH
 *
 * @since 4.2
 * @noreference This class is not intended to be referenced by clients.
 */
public class DefaultLocalDataRepository implements ILocalDataRepository {

    private final Map<Integer, ContainerTable> m_localRepository;

    private final Map<ICancellationListener, ICancellationListener> m_cancellationListeners;

    /**
     * Create an empty local repository
     */
    public DefaultLocalDataRepository() {
        this(new LinkedHashMap<>());
    }

    /**
     * Initialized local repository
     *
     * @param localRepository initialization map
     */
    public DefaultLocalDataRepository(final Map<Integer, ContainerTable> localRepository) {
        m_localRepository = localRepository;
        m_cancellationListeners = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContainerTable getTable(final int id) {
        return m_localRepository.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeTable(final int id) {
        m_localRepository.remove(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCancellationListener(final ICancellationListener listener) {
        m_cancellationListeners.put(listener, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCancellationListener(final ICancellationListener listener) {
        m_cancellationListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTable(final ContainerTable table) {
        m_localRepository.put(table.getTableId(), table);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        for (final ContainerTable table : m_localRepository.values()) {
            table.clear();
        }
        m_localRepository.clear();
        m_cancellationListeners.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel() {
        for (final ICancellationListener listener : m_cancellationListeners.values()) {
            listener.onCancel();
        }
        clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, ContainerTable> toMap() {
        return m_localRepository;
    }
}
