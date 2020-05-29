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
 *   May 9, 2020 (dietzc): created
 */
package org.knime.core.data.container;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;

/**
 * Factory to create {@link RowContainer}s.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz
 *
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @since 4.2
 */
public interface RowContainerFactory {

    /**
     * Make sure to call this method before
     * {@link #create(DataTableSpec, DataContainerSettings, IDataRepository, ILocalDataRepository, IWriteFileStoreHandler)}.
     *
     * @param spec the {@link DataTableSpec}, which is checked for compatibility with the {@link RowContainerFactory}.
     * @return <source>true</source> if {@link RowContainerFactory} can create a {@link RowContainer} for the provided
     *         spec.
     */
    boolean supports(DataTableSpec spec);

    /**
     * Creates a new {@link RowContainer}. Call {@link #supports(DataTableSpec)} before creating a {@link RowContainer}
     * to ensure compatibility of the {@link DataTableSpec} with the created {@link RowContainer}.
     *
     * @param spec used to create {@link RowContainer}.
     * @param settings of the {@link RowContainer}
     * @param repository the global data repository (per workflow)
     * @param localRepository the local data repository (per node)
     * @param fileStoreHandler the file store handler
     *
     * @return the created {@link RowContainer}
     */
    RowContainer create(final DataTableSpec spec, final DataContainerSettings settings,
        final IDataRepository repository, final ILocalDataRepository localRepository,
        final IWriteFileStoreHandler fileStoreHandler);

    /** @return non-blank short name shown in the preference page, e.g. "Arrow". */
    public String getName();
}
