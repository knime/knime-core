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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.IDataRepository;

/**
 * {@link DataCellDataInput} implementation on {@link ByteArrayInputStream}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
final class DictEncodedDataCellDataInputDelegator extends AbstractDataInputDelegator {

    private static final DataTypeRegistry REGISTRY = DataTypeRegistry.getInstance();

    private String m_nextDataCellClassName;

    DictEncodedDataCellDataInputDelegator(final IDataRepository dataRepository, //
        final DataInput input) {
        super(dataRepository, input);
    }

    /**
     * The {@link DictEncodedDataCellDataInputDelegator} can only read a {@link DataCell} if it knows the class name,
     * which needs to be stored separately. Thus, the {@link DictEncodedDataCellDataInputDelegator#readDataCell(String)}
     * overload should be used.
     */
    @SuppressWarnings("javadoc")
    @Override
    public DataCell readDataCell() throws IOException {
        throw new UnsupportedOperationException(
            "Reading the next data cell without specifying its class name is unsupported");
    }

    public DataCell readDataCell(final String dataCellClassName) throws IOException {
        m_nextDataCellClassName = dataCellClassName;
        return super.readDataCell();
    }

    @Override
    protected DataCell readDataCellImpl() throws IOException {
        Optional<DataCellSerializer<DataCell>> serializer = Optional.empty();
        final var className = m_nextDataCellClassName;
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
}
