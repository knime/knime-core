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
import java.util.function.IntFunction;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.map.Source.ProducerParameters;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.util.Pair;

/**
 * Default {@link ExternalToKnimeMapper} implementation.
 *
 * @param <S> the type of the mapping and production processes' source.
 * @param <P> the type of the production processes' parameters.
 * @author Noemi Balassa
 */
class DefaultExternalToKnimeMapper<S extends Source<?>, P extends ProducerParameters<S>>
    implements ExternalToKnimeMapper<S, P> {
    private final int m_cellCount;

    private final List<Pair<CellValueProducer<S, ?, P>, JavaToDataCellConverter<?>>> m_producerConverterPairs;

    /**
     * Constructs a {@link DefaultExternalToKnimeMapper} object.
     *
     * @param fileStoreFactoryFunction the function that accepts a cell (column) index and produces a file store factory
     *            for the cell factory that requires it.
     * @param paths the production paths for each cell (column).
     * @throws NullPointerException if {@code fileStoreFactoryFunction}, {@code paths}, or any of {@code paths}'
     *             elements is {@code null}.
     */
    DefaultExternalToKnimeMapper(final IntFunction<FileStoreFactory> fileStoreFactoryFunction,
        final ProductionPath... paths) {
        requireNonNull(fileStoreFactoryFunction, "fileStoreFactoryFunction");
        final int cellCount = requireNonNull(paths, "paths").length;
        final List<Pair<CellValueProducer<S, ?, P>, JavaToDataCellConverter<?>>> pairs = new ArrayList<>(cellCount);
        int i = 0;
        for (final ProductionPath path : paths) {
            if (path == null) {
                throw new NullPointerException("paths[" + i + ']');
            }
            @SuppressWarnings("unchecked")
            final CellValueProducer<S, ?, P> producer = (CellValueProducer<S, ?, P>)path.getProducerFactory().create();
            pairs.add(new Pair<>(producer, path.getConverterFactory().create(fileStoreFactoryFunction.apply(i++))));
        }
        m_cellCount = cellCount;
        m_producerConverterPairs = unmodifiableList(pairs);
    }

    @Override
    public DataRow map(final RowKey key, final S source, @SuppressWarnings("unchecked") final P... parameters)
        throws MappingException {
        requireNonNull(key, "key");
        requireNonNull(source, "source");
        if (requireNonNull(parameters, "parameters").length != m_cellCount) {
            throw new IllegalArgumentException("The number of producer parameters is not equal to the number of cells ("
                + m_cellCount + "): " + parameters.length);
        }
        try {
            final DataCell[] cells = new DataCell[m_producerConverterPairs.size()];
            for (final ListIterator<Pair<CellValueProducer<S, ?, P>, JavaToDataCellConverter<?>>> // Forced line break.
            producerConverterPairIterator = m_producerConverterPairs.listIterator(); producerConverterPairIterator
                .hasNext();) {
                final Pair<CellValueProducer<S, ?, P>, JavaToDataCellConverter<?>> producerConverterPair =
                    producerConverterPairIterator.next();
                final int index = producerConverterPairIterator.previousIndex();
                cells[index] = producerConverterPair.getSecond()
                    .convertUnsafe(producerConverterPair.getFirst().produceCellValue(source, parameters[index]));
            }
            return new DefaultRow(key, cells);
        } catch (final MappingException mappingException) {
            throw mappingException;
        } catch (final Exception exception) {
            throw new MappingException(exception.getMessage(), exception);
        }
    }
}
