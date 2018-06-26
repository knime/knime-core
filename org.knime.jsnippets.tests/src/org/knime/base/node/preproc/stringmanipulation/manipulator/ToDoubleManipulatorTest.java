/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   20.10.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation.manipulator;

import org.junit.Assert;
import org.junit.Test;

/**
 * Basic test for the toDouble manipulator.
 *
 * @author Heiko Hofer
 */
public class ToDoubleManipulatorTest {

    /**
     * Test method for
     * {@link ToDoubleManipulator#toDouble(String)},
     * {@link ToDoubleManipulator#toDouble(Integer)}
     * {@link ToDoubleManipulator#toDouble(Long)}
     * {@link ToDoubleManipulator#toDouble(Double)}.
     */
    @Test
    public void testToStringAdditional() {
        Assert.assertEquals(null,
                ToDoubleManipulator.toDouble((String)null));
        Assert.assertEquals(null,
                ToDoubleManipulator.toDouble((Integer)null));
        Assert.assertEquals(null,
                ToDoubleManipulator.toDouble((Long)null));
        Assert.assertEquals(null,
                ToDoubleManipulator.toDouble((Double)null));
        Assert.assertEquals(new Double(100),
                ToDoubleManipulator.toDouble(100));
        Assert.assertEquals(new Double(100),
                ToDoubleManipulator.toDouble(100l));
        Assert.assertEquals(new Double(100),
                ToDoubleManipulator.toDouble(100.0));
        Assert.assertEquals(new Double(100),
                ToDoubleManipulator.toDouble("100"));
    }

    /**
     * Test method for the examples of the toDouble Manipulator
     */
    @Test
    public void testToStringExamples() {
        // Test the examples in the description of the toDouble function
        Assert.assertEquals(null,
                ToDoubleManipulator.toDouble((String)null));
        Assert.assertEquals(new Double(2),
                ToDoubleManipulator.toDouble(2.0));
        Assert.assertEquals(new Double(2),
                ToDoubleManipulator.toDouble("2"));
    }
}
