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
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.knime.core.node.workflow.EditorUIInformation;

/**
 * Test cases for the {@link EditorUIInformationTest} class.
 *
 * @author Martin Horn, KNIME.com
 */
public class EditorUIInformationTest {

    @Test
    public void testBuilderAndGetters() {
        EditorUIInformation eui = EditorUIInformation.builder()
                .setConnectionLineWidth(1)
                .setGridX(10)
                .setGridY(11)
                .setHasCurvedConnections(true)
                .setShowGrid(false)
                .setSnapToGrid(true)
                .setZoomLevel(.1).build();

        assertEquals(eui.getConnectionLineWidth(), 1);
        assertEquals(eui.getGridX(), 10);
        assertEquals(eui.getGridY(), 11);
        assertEquals(eui.getHasCurvedConnections(), true);
        assertEquals(eui.getShowGrid(), false);
        assertEquals(eui.getSnapToGrid(), true);
        assertEquals(eui.getZoomLevel(), .1, 0);
    }

    @Test
    public void testEqualsAndHashCode() {
        EditorUIInformation eui = EditorUIInformation.builder()
                .setConnectionLineWidth(1)
                .setGridX(10)
                .setGridY(11)
                .setHasCurvedConnections(true)
                .setShowGrid(false)
                .setSnapToGrid(true)
                .setZoomLevel(.1).build();

        EditorUIInformation eui2 = EditorUIInformation.builder()
                .setConnectionLineWidth(1)
                .setGridX(10)
                .setGridY(11)
                .setHasCurvedConnections(true)
                .setShowGrid(false)
                .setSnapToGrid(true)
                .setZoomLevel(.1).build();

        assertTrue(eui.equals(eui2));
        assertEquals(eui.hashCode(), eui2.hashCode());
    }

    @Test
    public void testCopyFrom() {
        EditorUIInformation eui = EditorUIInformation.builder()
                .setConnectionLineWidth(1)
                .setGridX(10)
                .setGridY(11)
                .setHasCurvedConnections(true)
                .setShowGrid(false)
                .setSnapToGrid(true)
                .setZoomLevel(.1).build();

        EditorUIInformation eui2 = EditorUIInformation.builder().copyFrom(eui).build();

        assertTrue(eui.equals(eui2));
        assertEquals(eui.hashCode(), eui2.hashCode());
    }

}
