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
 *   Oct 8, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.probability.nominal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.node.util.CheckUtils;

/**
 * Implementation of {@link NominalDistributionValue} that shares the value set between multiple instances defined over
 * the same set of values.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public final class NominalDistributionCell extends FileStoreCell implements NominalDistributionValue {

    static final DataType TYPE = DataType.getType(NominalDistributionCell.class);

    private static final long serialVersionUID = 1L;

    private NominalDistributionCellMetaData m_metaData;

    private final double[] m_probabilities;

    private final int m_maxIdx;

    NominalDistributionCell(final NominalDistributionCellMetaData metaData, final FileStore fileStore,
        final double[] probabilities) {
        super(fileStore);
        CheckUtils.checkArgument(metaData.size() == probabilities.length,
            "The number of elements in probabilities must match the number of values in metaData.");
        m_metaData = metaData;
        m_probabilities = probabilities;
        m_maxIdx = findMaxIdx(probabilities);
    }

    private static int findMaxIdx(final double[] probabilities) {
        int maxIdx = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < probabilities.length; i++) {
            final double p = probabilities[i];
            if (p > max) {
                max = p;
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    private NominalDistributionCell(final double[] probabilities) {
        m_probabilities = probabilities;
        m_maxIdx = findMaxIdx(probabilities);
    }

    @Override
    protected void flushToFileStore() throws IOException {
        m_metaData.write(getFileStores()[0]);
    }

    @Override
    public String toString() {
        // we don't have the spec so we potentially don't know all class names or how many classes we have
        return Arrays.toString(m_probabilities);
    }

    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        final NominalDistributionCell other = (NominalDistributionCell)dc;
        return m_metaData.equals(other.m_metaData) && Arrays.equals(m_probabilities, other.m_probabilities);
    }

    @Override
    public int hashCode() {
        return 31 + 31 * m_metaData.hashCode() + 31 * Arrays.hashCode(m_probabilities);
    }

    @Override
    protected void postConstruct() throws IOException {
        try {
            m_metaData = NominalDistributionCellMetaData.read(getFileStores()[0]);
        } catch (ExecutionException ex) {
            throw new IOException("The meta data cannot be read.", ex);
        }
    }

    @Override
    public double getProbability(final String value) {
        final int idx = m_metaData.getIndex(value);
        return idx == -1 ? 0.0 : m_probabilities[idx];
    }

    @Override
    public String getMostLikelyValue() {
        return m_metaData.getValueAtIndex(m_maxIdx);
    }

    @Override
    public double getMaximalProbability() {
        return m_probabilities[m_maxIdx];
    }

    @Override
    public boolean isKnown(final String value) {
        return m_metaData.getIndex(value) != -1;
    }

    @Override
    public Set<String> getKnownValues() {
        return m_metaData.getValues();
    }

    /**
     * Serializer for {@link NominalDistributionCell NominalDistributionCells}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class NominalDistributionSerializer implements DataCellSerializer<NominalDistributionCell> {

        @Override
        public void serialize(final NominalDistributionCell cell, final DataCellDataOutput output) throws IOException {
            final int length = cell.m_probabilities.length;
            output.writeInt(length);
            for (int i = 0; i < length; i++) {
                output.writeDouble(cell.m_probabilities[i]);
            }
        }

        @Override
        public NominalDistributionCell deserialize(final DataCellDataInput input) throws IOException {
            int length = input.readInt();
            final double[] probabilities = new double[length];
            for (int i = 0; i < length; i++) {
                probabilities[i] = input.readDouble();
            }
            return new NominalDistributionCell(probabilities);
        }

    }

}
