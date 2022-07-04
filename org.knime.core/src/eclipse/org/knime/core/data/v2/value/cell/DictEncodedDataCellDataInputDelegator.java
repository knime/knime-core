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
 */
package org.knime.core.data.v2.value.cell;

import java.io.DataInput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.collection.CellCollection;
import org.knime.core.table.io.ReadableDataInput;

/**
 * Dictionary encoded {@link DataCellDataInput} implementation that needs to know the list of class names that will be
 * deserialized in subsequent calls to {@link DataCellDataInput#readDataCell()}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 */
public final class DictEncodedDataCellDataInputDelegator extends AbstractDataInputDelegator {

    private static final DataTypeRegistry REGISTRY = DataTypeRegistry.getInstance();

    private List<String> m_classNames;

    /**
     * Construct a {@link DictEncodedDataCellDataInputDelegator} based on a {@link DataInput}.
     *
     * @param dataRepository The data repository
     * @param input The delegate {@link DataInput}
     * @param classNames Semicolon-separated list of class names of {@link DataCell}s that are contained in the
     *            {@link DataInput}. Can be generated using
     *            {@link DictEncodedDataCellDataInputDelegator#getSerializedCellNames(DataCell)}.
     */
    DictEncodedDataCellDataInputDelegator(final IDataRepository dataRepository, final ReadableDataInput input,
        final String classNames) {
        super(dataRepository, input);
        m_classNames = Arrays.stream(classNames.split(";")).collect(Collectors.toList());
    }

    @Override
    protected DataCell readDataCellImpl() throws IOException {
        Optional<DataCellSerializer<DataCell>> serializer = Optional.empty();

        if (m_classNames.isEmpty()) {
            throw new IllegalStateException("No more DataCell type information available for this stream");
        }

        final var className = m_classNames.remove(0);
        if (className.isEmpty()) {
            throw new IllegalStateException("Cannot read DataCell with empty type information");
        }

        final var cellClass = REGISTRY.getCellClass(className);
        if (!cellClass.isEmpty()) {
            serializer = REGISTRY.getSerializer(cellClass.get());
        }

        if (serializer.isEmpty()) {
            // fall back to java serialization
            try {
                final ObjectInputStream ois = new ObjectInputStream(this);
                return (DataCell)ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }

        return serializer.get().deserialize(this);
    }

    /**
     * @param cell The data cell to serialize
     * @return A string describing the data cell and all the cells that it possibly contains (if it is a
     *         {@link CellCollection}), as needed to construct a {@link DictEncodedDataCellDataInputDelegator}.
     */
    public static String getSerializedCellNames(final DataCell cell) {
        final var cellNamesBuilder = new StringBuilder(cell.getClass().getName());
        if (cell instanceof CellCollection) {
            ((CellCollection)cell).forEach(c -> cellNamesBuilder.append(";" + c.getClass().getName()));
        }

        return cellNamesBuilder.toString();
    }
}
