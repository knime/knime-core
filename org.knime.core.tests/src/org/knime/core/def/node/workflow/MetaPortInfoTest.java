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
 *   Sep 23, 2016 (hornm): created
 */
package org.knime.core.def.node.workflow;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.PortType;

/**
 * Tests for the {@link MetaPortInfo} class.
 *
 * @author Martin Horn, KNIME.com
 */
public class MetaPortInfoTest {

    @Test
    public void testBuilderAndGetters() {
        PortType portType = BufferedDataTable.TYPE;
        MetaPortInfo mpi = MetaPortInfo.builder()
                .setIsConnected(false)
                .setMessage("message")
                .setNewIndex(1)
                .setOldIndex(2)
                .setPortType(portType).build();
        assertEquals(mpi.isConnected(), false);
        assertEquals(mpi.getMessage(), "message");
        assertEquals(mpi.getNewIndex(), 1);
        assertEquals(mpi.getOldIndex(), 2);
        assertEquals(mpi.getType(), portType);
    }

    @Test
    public void testCopyBuilder() {
        PortType portType = BufferedDataTable.TYPE;
        MetaPortInfo mpi = MetaPortInfo.builder()
                .setIsConnected(false)
                .setMessage("message")
                .setNewIndex(1)
                .setOldIndex(2)
                .setPortType(portType).build();

        MetaPortInfo mpi2 = MetaPortInfo.builder(mpi).build();
        assertEquals(mpi2.isConnected(), false);
        assertEquals(mpi2.getMessage(), "message");
        assertEquals(mpi2.getNewIndex(), 1);
        assertEquals(mpi2.getOldIndex(), 2);
        assertEquals(mpi2.getType(), portType);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testUIDNotSet() throws IllegalArgumentException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No port type set");
        MetaPortInfo mpi = MetaPortInfo.builder().build();
    }


}
