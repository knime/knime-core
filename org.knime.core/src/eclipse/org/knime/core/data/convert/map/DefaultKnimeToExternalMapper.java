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

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.map.Destination.ConsumerParameters;
import org.knime.core.util.Pair;

/**
 * Default {@link KnimeToExternalMapper} implementation.
 *
 * @param <D> the type of the mapping and consumption processes' destination.
 * @param <P> the type of the consumption processes' parameters.
 * @author Noemi Balassa
 */
class DefaultKnimeToExternalMapper<D extends Destination<?>, P extends ConsumerParameters<D>>
    implements KnimeToExternalMapper<D, P> {
    private final int m_cellCount;

    private final List<Pair<DataCellToJavaConverter<?, ?>, CellValueConsumer<D, Object, P>>> m_converterConsumerPairs;

    /**
     * Constructs a {@link DefaultKnimeToExternalMapper} object.
     *
     * @param paths the consumption paths for each cell (column).
     * @throws NullPointerException if {@code paths} or any of its elements is {@code null}.
     */
    DefaultKnimeToExternalMapper(final ConsumptionPath... paths) {
        final int cellCount = requireNonNull(paths, "paths").length;
        final List<Pair<DataCellToJavaConverter<?, ?>, CellValueConsumer<D, Object, P>>> pairs =
            new ArrayList<>(cellCount);
        int i = 0;
        for (final ConsumptionPath path : paths) {
            if (path == null) {
                throw new NullPointerException("paths[" + i + ']');
            }
            @SuppressWarnings("unchecked")
            final CellValueConsumer<D, Object, P> consumer =
                (CellValueConsumer<D, Object, P>)path.getConsumerFactory().create();
            pairs.add(new Pair<>(path.getConverterFactory().create(), consumer));
            ++i;
        }
        m_cellCount = cellCount;
        m_converterConsumerPairs = unmodifiableList(pairs);
    }

    @Override
    public void map(final DataRow row, final D destination, @SuppressWarnings("unchecked") final P... parameters)
        throws MappingException {
        requireNonNull(row, "row");
        requireNonNull(destination, "destination");
        if (requireNonNull(parameters, "parameters").length != m_cellCount) {
            throw new IllegalArgumentException("The number of consumer parameters is not equal to the number of cells ("
                + m_cellCount + "): " + parameters.length);
        }
        try {
            final ListIterator<Pair<DataCellToJavaConverter<?, ?>, // Forced line break.
                    CellValueConsumer<D, Object, P>>> converterConsumerPairIterator =
                        m_converterConsumerPairs.listIterator();
            for (final DataCell cell : row) {
                final Pair<DataCellToJavaConverter<?, ?>, CellValueConsumer<D, Object, P>> converterConsumerPair =
                    converterConsumerPairIterator.next();
                converterConsumerPair.getSecond().consumeCellValue(destination,
                    cell.isMissing() ? null : converterConsumerPair.getFirst().convertUnsafe(cell),
                    parameters[converterConsumerPairIterator.previousIndex()]);
            }
        } catch (final Exception exception) {
            //always unwrap the exception and return a new MappingException with the cause message
            final Throwable cause = exception.getCause();
            final String message;
            if (cause != null) {
                message = cause.getMessage();
            } else {
                message = "Data type mapping exception";
            }
            throw new MappingException(message, cause);
        }
    }
}
