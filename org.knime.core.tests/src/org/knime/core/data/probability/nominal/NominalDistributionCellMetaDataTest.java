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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.filestore.FileStoreKey;

/**
 * Unit tests for NominalDistributionCellMetaData.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class NominalDistributionCellMetaDataTest {

    private static final String[] VALUES = new String[]{"A", "B", "C"};

    private NominalDistributionCellMetaData m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        final UUID storeId = UUID.randomUUID();
        final FileStoreKey key = new FileStoreKey(storeId, 0, new int[]{0}, 0, "testFileStore");
        m_testInstance = new NominalDistributionCellMetaData(key,
            Arrays.stream(VALUES).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * Tests the get index method.
     */
    @Test
    public void testGetIndex() {
        for (int i = 0; i < VALUES.length; i++) {
            assertEquals(i, m_testInstance.getIndex(VALUES[i]));
        }
        assertEquals(-1, m_testInstance.getIndex("Z"));
    }

    /**
     * Tests the getValueAtIndex method.
     */
    @Test
    public void testGetValueAtIndex() {
        for (int i = 0; i < VALUES.length; i++) {
            assertEquals(VALUES[i], m_testInstance.getValueAtIndex(i));
        }
    }

    /**
     * Tests the size method.
     */
    @Test
    public void testSize() {
        assertEquals(VALUES.length, m_testInstance.size());
    }

    /**
     * Tests the getValues method.
     */
    @Test
    public void testGetValues() {
        List<String> expected = Arrays.stream(VALUES).collect(Collectors.toCollection(ArrayList::new));
        assertEquals(expected, new ArrayList<>(m_testInstance.getValues()));
    }

}
