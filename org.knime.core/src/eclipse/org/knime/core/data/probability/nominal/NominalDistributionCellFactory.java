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
import java.util.UUID;

import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.node.util.CheckUtils;

/**
 * Factory for {@link NominalDistributionCell NominalDistributionCells}. Cells created by such a factory share the same
 * meta data and {@link FileStore}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public final class NominalDistributionCellFactory {

    /**
     * The {@link DataType} of cells created by this factory.
     */
    public static final DataType TYPE = NominalDistributionCell.TYPE;

    private final NominalDistributionCellMetaData m_metaData;

    private final FileStore m_fileStore;

    /**
     * Constructs a {@link NominalDistributionCellFactory} for the creation of {@link NominalDistributionCell
     * NominalDistributionCells}.
     *
     * @param fileStoreFactory used to create the {@link FileStore} shared by the cells created by this factory
     * @param values the values the distribution is defined over
     */
    public NominalDistributionCellFactory(final FileStoreFactory fileStoreFactory, final String[] values) {
        CheckUtils.checkNotNull(fileStoreFactory, "The file store factory must not be null.");
        final Set<String> trimmedValues = NominalDistributionUtil.toTrimmedSet(values);
        try {
            m_fileStore = fileStoreFactory.createFileStore(UUID.randomUUID().toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Can't create file store for nominal distribution cells.", ex);
        }
        m_metaData = new NominalDistributionCellMetaData(FileStoreUtil.getFileStoreKey(m_fileStore), trimmedValues);
    }

    /**
     * @param probabilities the probabilities for the different values
     * @param epsilon the imprecision that the sum of probabilities is allowed to have
     * @return a {@link NominalDistributionCell} containing <b>probabilities</b>
     * @throws NullPointerException if <b>probabilities</b> is null
     * @throws IllegalArgumentException if <b>probabilities</b> does not have the same number of elements as there are
     *             values in the meta data
     */
    public NominalDistributionCell createCell(final double[] probabilities, final double epsilon) {
        // Check null
        CheckUtils.checkNotNull(probabilities, "The list of probabilities must not be null.");
        // Check that probabilities are compatible with meta data
        CheckUtils.checkArgument(probabilities.length == m_metaData.size(),
            "The number of elements in probabilities (%s) must match the number of values in the meta data (%s).",
            probabilities.length, m_metaData.size());
        // check that precision is positive
        CheckUtils.checkArgument(epsilon >= 0, "The epsilon must not be negative");
        // check that no probability is negative
        CheckUtils.checkArgument(Arrays.stream(probabilities).noneMatch(e -> e < 0d),
            "Probability must not be negative.");
        // check that probabilities sum up to 1
        CheckUtils.checkArgument(sumUpToOne(Arrays.stream(probabilities).sum(), epsilon),
            "The probabilities do not sum up to 1. Consider setting a proper epsilon.");
        return new NominalDistributionCell(m_metaData, m_fileStore, probabilities.clone());
    }

    private static boolean sumUpToOne(final double a, final double epsilon) {
        return Math.abs(a - 1.0d) < epsilon;
    }

    /**
     * Creates a probability cell in which <b>value</b> has probability 1 and all other values have probability 0.
     *
     * @param value the value with probability 1 (must be one of the values provided in the constructor of this
     *            instance)
     * @return a NominalDistributionCell where <b>value</b> has probability 1 and all other values have probability 0
     * @throws IllegalArgumentException if <b>value</b> is unknown to the factory
     */
    public NominalDistributionCell createCell(final String value) {
        CheckUtils.checkNotNull(value, "The provided value must not be null.");
        final int idx = m_metaData.getIndex(value);
        CheckUtils.checkArgument(idx != -1, "Unknown value '%s'.", value);
        final double[] probs = new double[m_metaData.size()];
        probs[idx] = 1.0;
        return new NominalDistributionCell(m_metaData, m_fileStore, probs);
    }

}
