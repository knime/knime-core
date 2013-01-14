/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * Created on Nov 12, 2012 by wiswedel
 */
package org.knime.core.data.collection;

import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.filestore.FileStoreCell;

/**
 * Implemented by {@link DataCell}, which contain other cells. This is the case for extensions of {@link AdapterValue}
 * and {@link CollectionDataValue}. It helps the framework to decide whether the contained cell collection contains
 * {@link BlobDataCell}, which need special handling upon adding them to a {@link DataContainer}.
 *
 * <p>Apart from {@link BlobDataCell} getting deprecated status soon (use a {@link FileStoreCell} instead), this
 * interface should be implemented by any DataCell that writes other cells in its
 * {@link DataCellSerializer#serialize(DataCell, org.knime.core.data.DataCellDataOutput)} method.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.7
 */
public interface CellCollection extends Iterable<DataCell> {

    /**
     * Get whether the collection contains special {@link BlobWrapperDataCell} (framework use). This method gives a hint
     * to the framework (specifically to the classes {@link org.knime.core.data.container.DataContainer DataContainer}
     * and {@link org.knime.core.node.BufferedDataContainer BufferedDataContainer}) whether they should handle the cell
     * implementing this interface with care with respect to contained {@link BlobDataCell}.
     *
     * <p>
     * This method should return <code>true</code> only if
     * <ul>
     * <li>The iterator returned by {@link #iterator()} implements {@link BlobSupportDataCellIterator} (which allows the
     * framework to access the {@link BlobWrapperDataCell} without touching the contained {@link BlobDataCell} (which is
     * an expensive operation) and</li>
     * <li>at least one element returned by the iterator is indeed a {@link BlobWrapperDataCell}.</li>
     * </ul>
     *
     * <p>
     * Implementation note: This method is really only a helper for the framework. The same information can be retrieved
     * by accessing the iterator. This can be an expensive and unnecessary operation, however.
     *
     * @return Whether the collection contains blob wrapper cells.
     */
    public boolean containsBlobWrapperCells();

}
