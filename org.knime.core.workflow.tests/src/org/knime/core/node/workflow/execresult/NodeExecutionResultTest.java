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
 *   Feb 2, 2017 (bjoern): created
 */
package org.knime.core.node.workflow.execresult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link NodeExecutionResultTest}.
 *
 * @author Bjoern Lohrmann, KNIME.com GmbH
 */
public class NodeExecutionResultTest {

    /**
     * Tests the JSON (de)serialization of NodeExecutionResult objects
     *
     * @throws IOException
     */
    @Test
    public void testJsonSerialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        NodeExecutionResult orig = ExecutionResultFixtures.createNodeExecutionResultFixture();

        String serialized = mapper.writeValueAsString(orig);

        NodeExecutionResult deserialized = mapper.readValue(serialized, NodeExecutionResult.class);
        assertEqualityAfterDeserialization(orig, deserialized);
    }

    /**
     *
     * @param orig
     * @param deserialized
     */
    public static void assertEqualityAfterDeserialization(final NodeExecutionResult orig,
        final NodeExecutionResult deserialized) {
        assertThat("Object is not the same", deserialized, not(sameInstance(orig)));
        assertThat("Warning message is equal", deserialized.getWarningMessage(), is(orig.getWarningMessage()));
        assertThat("Needs reset after load is equal", deserialized.needsResetAfterLoad(),
            is(orig.needsResetAfterLoad()));
        assertThat("Nr port objects is equal", deserialized.getNrOfPortObjects(), is(orig.getNrOfPortObjects()));
        for (int i = 0; i < orig.getNrOfPortObjects(); i++) {
        	assertNull(deserialized.getPortObject(i), "port object is null");
            
        }
        for (int i = 0; i < orig.getNrOfPortObjects(); i++) {
            assertNull(deserialized.getPortObjectSpec(i), "port object spec is null");
        }

        for (int i = 0; i < orig.getNrOfInternalHeldPortObjects(); i++) {
            assertNull(deserialized.getInternalHeldPortObjects()[i], "internal held port object is null");
        }

        assertThat("Nr internal port objects is equal", deserialized.getNrOfInternalHeldPortObjects(),
            is(orig.getNrOfInternalHeldPortObjects()));
    }
}
