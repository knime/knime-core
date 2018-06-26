/*
 * ------------------------------------------------------------------ *
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
 *   Mar 28, 2007 (wiswedel): created
 */
package org.knime.core.util;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.knime.core.node.NodeLogger;

import junit.framework.Assert;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class DuplicateCheckerTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testNoDuplicateManyRows() throws IOException {
        long t = System.currentTimeMillis();
        DuplicateChecker dc = new DuplicateChecker();

        try {
            int[] indices = new int[10000000];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }
            Random r = new Random();
            for (int i = indices.length - 1; i >= 0; i--) {
                int swpIndex = r.nextInt(indices.length);
                int swp = indices[i];
                indices[i] = indices[swpIndex];
                indices[swpIndex] = swp;
            }
            for (int i : indices) {
                dc.addKey("Row " + i);
            }
            dc.checkForDuplicates();
        } catch (DuplicateKeyException ex) {
            NodeLogger.getLogger(getClass()).error("No duplicates inserted but exception was thrown", ex);
            Assert.fail("No duplicates inserted but exception was thrown");
        } finally {
            dc.clear();
        }
        NodeLogger.getLogger(getClass()).info((System.currentTimeMillis() - t) + "ms");
    }

    @Test
    public void testNoStringsAtAll() throws DuplicateKeyException, IOException {
        DuplicateChecker dc = new DuplicateChecker();
        dc.checkForDuplicates();
    }

    @Test
    public void testArbitraryStringsNoDuplicates() throws IOException {
        long seed = System.currentTimeMillis();
        NodeLogger.getLogger(getClass()).info("Using seed " + seed);
        internalTestArbitraryStrings(false, seed);
    }

    @Test
    public void testArbitraryStringsDuplicates() throws IOException {
        long seed = System.currentTimeMillis();
        NodeLogger.getLogger(getClass()).info("Using seed " + seed);
        internalTestArbitraryStrings(true, seed);

        // this one generates invalid UTF-16 strings
        seed = 1343253055319L;
        NodeLogger.getLogger(getClass()).info("Using seed " + seed);
        internalTestArbitraryStrings(true, seed);
    }

    /**
     * Simple test for duplicates in the first chunk.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testEarlyDuplicate() throws Exception {
        DuplicateChecker checker = new DuplicateChecker();
        checker.addKey("A");
        expectedException.expect(DuplicateKeyException.class);
        checker.addKey("A");
    }

    private void internalTestArbitraryStrings(final boolean isAddDuplicates, final long seed) throws IOException {
        LinkedHashSet<String> hash = new LinkedHashSet<String>();
        Random r = new Random(seed);
        while (hash.size() < 300000) {
            int length = 5 + r.nextInt(30);
            char[] c = new char[length];
            for (int i = 0; i < length; i++) {
                double prob = r.nextDouble();
                if (prob < 0.05) {
                    c[i] = getRandomSpecialChar(r);
                } else if (prob < 0.1) {
                    c[i] = getRandomArbitraryChar(r);
                } else {
                    c[i] = getRandomASCIIChar(r);
                }
            }
            hash.add(new String(c));
        }
        DuplicateChecker dc = new DuplicateChecker(1000, 50);
        int duplIndex = r.nextInt(hash.size());
        int indexToInsert = r.nextInt(hash.size());
        while (indexToInsert == duplIndex) {
            indexToInsert = r.nextInt(hash.size());
        }
        if (duplIndex > indexToInsert) {
            int swap = duplIndex;
            duplIndex = indexToInsert;
            indexToInsert = swap;
        }
        String dupl = null;
        try {
            int index = 0;
            for (String c : hash) {
                if (index == duplIndex) {
                    dupl = c;
                } else if (isAddDuplicates && index == indexToInsert) {
                    assert dupl != null;
                    dc.addKey(dupl);
                }
                dc.addKey(c);
                index++;
            }
            dc.checkForDuplicates();
        } catch (DuplicateKeyException e) {
            dc.clear();
            if (isAddDuplicates) {
                Assert.assertEquals(dupl, e.getKey());
                return;
            } else {
                NodeLogger.getLogger(getClass()).error("No duplicates inserted but exception was thrown", e);
                Assert.fail("Duplicate detected even though no duplicates are present");
            }
        }

        dc.clear();
        if (isAddDuplicates) {
            Assert.fail("No duplicate detected even though at least one is present");
        }
    }

    private static char getRandomSpecialChar(final Random rand) {
        switch (rand.nextInt(3)) {
        case 0 : return '%';
        case 1 : return '\n';
        case 2 : return '\r';
        default: throw new RuntimeException("Invalid random number");
        }
    }

    private static char getRandomArbitraryChar(final Random rand) {
        return (char)rand.nextInt(Character.MAX_VALUE + 1);
    }

    private static char getRandomASCIIChar(final Random rand) {
        return (char)(' ' + rand.nextInt(Byte.MAX_VALUE - ' '));
    }
}
