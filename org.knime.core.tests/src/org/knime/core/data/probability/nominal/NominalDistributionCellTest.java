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
 *   Nov 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.probability.nominal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.WriteFileStoreHandler;

/**
 * Unit tests for {@link NominalDistributionCell}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class NominalDistributionCellTest {

    private static final double[] PROBABILITIES = new double[]{0.1, 0.2, 0.7};

    private static final String[] VALUES = new String[]{"A", "B", "C"};


    private NominalDistributionCell m_testInstance;

    private static NominalDistributionCell createCell(final double ... probs) {
        final UUID storeId = UUID.randomUUID();
        final WriteFileStoreHandler handler = new WriteFileStoreHandler("testHandler", storeId);
        final FileStoreKey key = new FileStoreKey(storeId, 0, new int[]{0}, 0, "testFileStore");
        final FileStore fileStore = FileStoreUtil.createFileStore(handler, key);
        final NominalDistributionCellMetaData metaData = new NominalDistributionCellMetaData(key,
            Arrays.stream(VALUES).collect(Collectors.toCollection(LinkedHashSet::new)));
        return new NominalDistributionCell(metaData, fileStore, probs);
    }

    /**
     * Initializes the members before each unit test.
     */
    @Before
    public void init() {
        m_testInstance = createCell(PROBABILITIES);
    }

    /**
     * Tests the {@link NominalDistributionCell#getMostLikelyValue()} method.
     */
    @Test
    public void testGetMostLikelyValue() {
        assertEquals("C", m_testInstance.getMostLikelyValue());

        NominalDistributionCell equallyLikelyValues = createCell(0.4, 0.4, 0.2);
        assertEquals("A", equallyLikelyValues.getMostLikelyValue());
    }

    /**
     * Tests the {@link NominalDistributionCell#getMaximalProbability()} method, including the verification that
     * {@link NominalDistributionCell#getProbability(String)} called with the return value of
     * {@link NominalDistributionCell#getMostLikelyValue()} equals the output of getMaximalProbability.
     */
    @Test
    public void testGetMaximalProbability() {
        assertEquals(0.7, m_testInstance.getMaximalProbability(), 1e-6);
        assertEquals(m_testInstance.getProbability(m_testInstance.getMostLikelyValue()),
            m_testInstance.getMaximalProbability(), 1e-6);
    }

    /**
     * Tests {@link NominalDistributionCell#isKnown(String)}.
     */
    @Test
    public void testIsKnown() {
        for (String value : VALUES) {
            assertTrue(m_testInstance.isKnown(value));
        }
        assertFalse(m_testInstance.isKnown("Z"));
    }

    /**
     * Tests {@link NominalDistributionCell#getKnownValues()}.
     */
    @Test
    public void testGetKnownValues() {
        final List<String> expected = Arrays.stream(VALUES).collect(Collectors.toCollection(ArrayList::new));
        assertEquals(expected, new ArrayList<>(m_testInstance.getKnownValues()));
    }

    /**
     * Tests {@link NominalDistributionCell#getProbability(String)}.
     */
    @Test
    public void testGetProbability() {
        for (int i = 0; i < PROBABILITIES.length; i++) {
            assertEquals(PROBABILITIES[i], m_testInstance.getProbability(VALUES[i]), 1e-6);
        }
        assertEquals(0, m_testInstance.getProbability("Z"), 1e-6);
    }

}
