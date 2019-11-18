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
 *   Oct 10, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.meta;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableDomainCreator;

/**
 * Allows to create {@link DataColumnMetaData} from actual data e.g. in the {@link DataTableDomainCreator}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type of {@link DataColumnMetaData} this {@link DataColumnMetaDataCreator} creates
 * @noextend This interface is not intended to be extended by clients. Pending API.
 * @noimplement This interface is not intended to be implemented by clients. Pending API.
 * @noreference This interface is not intended to be referenced by clients. Pending API.
 * @since 4.1
 */
public interface DataColumnMetaDataCreator<T extends DataColumnMetaData> extends DataColumnMetaDataFramework<T> {

    /**
     * Updates the meta data according to the contents of cell.</br>
     * Missing values and incompatible cell types (e.g. in a transposed table) must not cause an exception and should
     * simply be ignored.
     *
     * @param cell whose information should (if possible) be included in the meta data
     */
    void update(final DataCell cell);

    /**
     * Creates the {@link DataColumnMetaData} corresponding to the current state of this creator.</br>
     * It must be possible to call this method multiple times even if the creator is still updated and
     * {@link DataColumnMetaData metaData objects} created in different calls must be independent from each other i.e.
     * they may not share any mutable objects.
     *
     * @return the {@link DataColumnMetaData} containing the meta data at the current state of this creator
     */
    T create();

    /**
     * Creates a deep copy of this creator i.e. calling update on the copied creator has no effect on this creator.
     *
     * @return a deep copy of this creator
     */
    DataColumnMetaDataCreator<T> copy();

    /**
     * Merges two {@link DataColumnMetaDataCreator creators} by including the data from <b>other</b> into this creator.
     *
     * @param other the creator to merge into this creator
     * @return this creator for method chaining
     */
    default DataColumnMetaDataCreator<T> merge(final DataColumnMetaDataCreator<T> other) {
        return merge(other.create());
    }

    /**
     * Merges the information from {@link DataColumnMetaData other} into this creator.
     *
     * @param other the {@link DataColumnMetaData} to merge into this creator
     * @return this creator for method chaining
     */
    DataColumnMetaDataCreator<T> merge(final T other);
}
