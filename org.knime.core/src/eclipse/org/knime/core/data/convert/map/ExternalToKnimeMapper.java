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
 */
package org.knime.core.data.convert.map;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.convert.map.Source.ProducerParameters;

/**
 * An executor of production and external to KNIME type mapping operations.
 * <p>
 * Compared to the corresponding static {@link MappingFramework} {@code map} methods, depending on the implementation,
 * the methods of this interface may use prepared resources and be more efficient when used repeatedly.
 * </p>
 *
 * @param <S> the type of the mapping and production processes' source.
 * @param <P> the type of the production processes' parameters.
 * @author Noemi Balassa
 * @since 4.1
 */
public interface ExternalToKnimeMapper<S extends Source<?>, P extends ProducerParameters<S>> {
    /**
     * Reads and maps data from an external source to a {@link DataRow}.
     *
     * @param key the key for the row to be created.
     * @param source the source to read from.
     * @param parameters the production parameters for each cell (column) of the row.
     * @return a {@link DataRow} object that contains the data read and mapped from the source.
     * @throws MappingException if an error occurs during the mapping or production operation.
     */
    DataRow map(RowKey key, S source, @SuppressWarnings("unchecked") P... parameters) throws MappingException;
}
