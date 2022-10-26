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
 *   14 Nov 2022 (chaubold): created
 */
package org.knime.core.data.filestore;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

/**
 * Test the {@link FileStoreKey}
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public class FileStoreKeyTest {
    @Test
    public void testFSKeyFromToStringYieldsIdentity() {
        UUID uuid = UUID.randomUUID();

        testRoundtrip(new FileStoreKey(uuid, 0, null, -1, "test"));
        testRoundtrip(new FileStoreKey(uuid, 3, new int[] {}, 1, "test"));
        testRoundtrip(new FileStoreKey(uuid, 0, new int[] {1, 4, 19}, 0, "test"));
        testRoundtrip(new FileStoreKey(uuid, 0, new int[] {1, 4, 19}, 42, "test"));
        testRoundtrip(new FileStoreKey(uuid, 314, new int[] {1, 4, 19}, 42, "test"));
        testRoundtrip(new FileStoreKey(uuid, 314, new int[] {1, 4, 19}, 42, UUID.randomUUID().toString()));
    }

    @Test
    public void testLoadRealFSKey() {
        FileStoreKey.fromString("0_0_bd1ee3d4-3ea6-4eb8-8f3b-5c2caa4cbd06-(891613d3-c03d-41e8-b9d4-0476b3b8b1f8)");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadInvalidFSKey() {
        FileStoreKey.fromString("0_foo-(bar)");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadInvalidEmptyFSKey() {
        FileStoreKey.fromString("empty");
    }

    private static void testRoundtrip(final FileStoreKey fsKey) {
        assertEquals("ToString -> FromString failed for FileStoreKey: " + fsKey.toString(), fsKey, FileStoreKey.fromString(fsKey.toString()));
    }
}
