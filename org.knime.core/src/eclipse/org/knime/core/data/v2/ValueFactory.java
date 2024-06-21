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
 *   Oct 1, 2020 (marcel): created
 */
package org.knime.core.data.v2;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.traits.DataTraitUtils;
import org.knime.core.table.schema.traits.DataTraits;

/**
 * Interface for implementations of {@link ValueFactory}s. Value factories can create logical {@link WriteValue
 * WriteValues} and {@link ReadValue ReadValues} which in turn allow access to an underlying data storage, which is
 * independent of the values themselves. The {@link DataSpec} defines the requirements to this data storage and also
 * provides additional meta data to create the storage.
 *
 * Implementations of {@link ValueFactory} must have an empty public constructor to create an instance of the value
 * factory from its class name.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 *
 * @param <R> type of read access
 * @param <W> type of write access
 * @since 4.3
 *
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface ValueFactory<R extends ReadAccess, W extends WriteAccess> {

    /**
     * Creates a new {@link ReadValue}.
     * <P>
     * The returned value should implement all {@link DataValue} interfaces that are associated with the
     * {@link DataType}. Note that these are the interfaces implemented by the corresponding {@link DataCell} if the
     * data type was created via {@link DataType#getType(Class)}.
     *
     * @param access the read access wrapped by the {@link ReadValue}
     * @return the {@link ReadValue}.
     */
    ReadValue createReadValue(R access);

    /**
     * Creates a new {@link WriteValue}.
     *
     * @param access the write access wrapped by the {@link WriteValue}
     * @return the {@link WriteValue}
     */
    WriteValue<?> createWriteValue(W access);

    /**
     * Provides the {@link DataSpec}.
     *
     * @return the {@link DataSpec} to create the actual data storage.
     */
    DataSpec getSpec();

    /**
     * Provides the {@link DataTraits}.
     *
     * @return the {@link DataTraits} describing additional info for the data storage
     * @since 4.5
     */
    default DataTraits getTraits() {
        return DataTraitUtils.emptyTraits(getSpec());
    }
}
