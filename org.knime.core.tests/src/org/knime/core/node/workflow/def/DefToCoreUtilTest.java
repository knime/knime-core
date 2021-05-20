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
 *   May 19, 2021 (carlwitt): created
 */
package org.knime.core.node.workflow.def;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.workflow.def.ConfigDef;
import org.knime.core.workflow.def.ConfigMapDef;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class DefToCoreUtilTest {

    /**
     * Convert {@link NodeSettings} to {@link ConfigDef} and the {@link ConfigDef} back to {@link NodeSettings}.
     * Compare the original {@link NodeSettings} with the restored {@link NodeSettings}.
     */
    @Test
    public void testPersistRestore() {

        NodeSettings original = new NodeSettings("root");

        original.addStringArray("colors", "red", "green", "blue");
        original.addString("answerString", "42");

        original.addBoolean("A v ¬A", true);

        original.addInt("answerInt", Integer.MIN_VALUE);
        original.addFloat("answerFloat", Float.MIN_VALUE);
        original.addDouble("answerDouble", Double.MIN_VALUE);
        original.addShort("answerShort", Short.MIN_VALUE);
        original.addByte("answerByte", Byte.MIN_VALUE);
        original.addLong("answerLong", Long.MIN_VALUE);
        original.addPassword("answerPassword", "zebra", "secret");

        // tree structure
        NodeSettingsWO org = original.addNodeSettings("org");
        NodeSettingsWO knime = org.addNodeSettings("knime");
        knime.addLong("loc", Long.MAX_VALUE);
        NodeSettingsWO core = knime.addNodeSettings("core");
        // leafs that are not primitives (addBoolean, addString, ...)
        core.addNodeSettings("util");
        core.addNodeSettings("node");

        try {
            ConfigDef def = CoreToDefUtil.toConfigDef(original);
            NodeSettings restored = (NodeSettings)DefToCoreUtil.toNodeSettings((ConfigMapDef)def);

            // conversion process does not change information
            assertTrue(original.hasIdenticalValue(restored));

            // some additional tests on the correctness of hasIdenticalValue
            assertTrue("Configuration not identical to itself", restored.hasIdenticalValue(restored));
            assertTrue("Configuration not identical to itself", original.hasIdenticalValue(original));
            assertTrue("Configuration comparison is not commutative", restored.hasIdenticalValue(original));
        } catch (InvalidSettingsException e) {
            fail(e.getMessage());
        }
    }

}
