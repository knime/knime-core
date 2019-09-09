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
 *   Aug 28, 2019 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.probability;

import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;

/**
 * Default implementation of a {@link ProbabilityDistributionValue}, whereby the underlying data structure is a double
 * array.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public final class ProbabilityDistributionCell extends DataCell implements ProbabilityDistributionValue {

    /** */
    private static final long serialVersionUID = 1L;

    /** {@link DataType} of this cell. */
    static final DataType TYPE = DataType.getType(ProbabilityDistributionCell.class);

    private final double[] m_probabilities;

    ProbabilityDistributionCell(final double[] probabilities) {
        m_probabilities = probabilities;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getProbability(final int index) {
        return m_probabilities[index];
    }

    @Override
    public int size() {
        return m_probabilities.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Arrays.toString(m_probabilities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return Arrays.equals(m_probabilities, ((ProbabilityDistributionCell)dc).m_probabilities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(m_probabilities);
    }

    /**
     * Serializer for {@link ProbabilityDistributionCell}s.
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class ProbabilityDistributionSerializer
        implements DataCellSerializer<ProbabilityDistributionCell> {
        /** {@inheritDoc} */
        @Override
        public ProbabilityDistributionCell deserialize(final DataCellDataInput input) throws IOException {
            int length = input.readInt();
            final double[] probabilities = new double[length];
            for (int i = 0; i < length; i++) {
                probabilities[i] = input.readDouble();
            }
            return new ProbabilityDistributionCell(probabilities);
        }

        /** {@inheritDoc} */
        @Override
        public void serialize(final ProbabilityDistributionCell cell, final DataCellDataOutput output)
            throws IOException {
            final int length = cell.m_probabilities.length;
            output.writeInt(length);
            for (int i = 0; i < length; i++) {
                output.writeDouble(cell.m_probabilities[i]);
            }
        }
    }
}
