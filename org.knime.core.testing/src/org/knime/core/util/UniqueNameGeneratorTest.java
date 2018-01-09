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
 *   Oct 8, 2011 (wiswedel): created
 */
package org.knime.core.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.StringCell;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class UniqueNameGeneratorTest {

    private static final HashSet<String> USED_NAMES = new HashSet<String>(
            Arrays.asList(
                    "Parenthesis", "Parenthesis (#1)", "Parenthesis (#3)", ""));

    /**
     * Test method for {@link org.knime.core.util.UniqueNameGenerator#
     * newName(java.lang.String)}.
     */
    @Test
    public void testNewNameDefaultBehaviour() {
        UniqueNameGenerator ng = new UniqueNameGenerator(USED_NAMES);
        assertEquals("NewName", ng.newName("NewName"));
        assertEquals("NewName (#1)", ng.newName("NewName"));
        assertEquals("NewName (#2)", ng.newName("NewName"));
        assertEquals("NewName (#3)", ng.newName("NewName"));
        assertEquals("NewName (#4)", ng.newName("NewName (#3)"));
        assertEquals("NewName (#40)", ng.newName("NewName (#40)"));
        assertEquals("NewName (#41)", ng.newName("NewName (#40)"));
        assertEquals("OtherName ()", ng.newName("OtherName ()"));
        assertEquals("OtherName (#NaN)", ng.newName("OtherName (#NaN)"));
        assertEquals("OtherName (#NaN) (#1)", ng.newName("OtherName (#NaN)"));
        assertEquals("OtherName (#NaN) (#2)", ng.newName("OtherName (#NaN)"));
        assertEquals("YetAnotherName (#1)", ng.newName("YetAnotherName (#1)"));
        assertEquals(" (#1)", ng.newName("")); // already present
        assertEquals("Parenthesis (#2)", ng.newName("Parenthesis"));
        assertEquals("Parenthesis (#4)", ng.newName("Parenthesis"));
    }
    
    @Test
    public void testNameUntrimmed() {
        UniqueNameGenerator ng = new UniqueNameGenerator(USED_NAMES);
        assertEquals("Parenthesis (#2)", ng.newName("Parenthesis"));
        assertEquals("Parenthesis (#4)", ng.newName("Parenthesis"));
        // see http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=4577#c1 
        assertEquals("Parenthesis (#5)", ng.newName(" Parenthesis"));
        assertEquals("Parenthesis (#6)", ng.newName(" Parenthesis "));
    }

    @Test
    public void testNewCreatorFromColumn() {
        UniqueNameGenerator ng = new UniqueNameGenerator(USED_NAMES);
        DataColumnSpecCreator c1 = ng.newCreator(new DataColumnSpecCreator("NewName", StringCell.TYPE).createSpec());
        assertEquals("NewName", c1.createSpec().getName());
        DataColumnSpecCreator c2 = ng.newCreator(new DataColumnSpecCreator("NewName", StringCell.TYPE).createSpec());
        assertEquals("NewName (#1)", c2.createSpec().getName());
        DataColumnSpecCreator c3 = ng.newCreator(new DataColumnSpecCreator("Parenthesis (#1)", StringCell.TYPE).createSpec());
        assertEquals("Parenthesis (#2)", c3.createSpec().getName());
    }

}
