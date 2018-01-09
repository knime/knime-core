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
 *   27.05.2016 (Jonathan Hale): created
 */
package org.knime.core.data.convert;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.knime.core.data.convert.util.ClassUtil;
import org.knime.core.data.convert.util.SerializeUtil;

/**
 * Tests for {@link SerializeUtil}.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 */
public class ClassUtilTest {

    private interface TestInterface {
    }

    private interface TestSuperclassInterface {
    }

    private class TestSuperclass implements TestSuperclassInterface {
    }

    private class TestClass extends TestSuperclass implements TestInterface {
    }

    @Test
    public void testClassHierarchyStream() throws Exception {
        final List<Class<?>> hierarchy =
            ClassUtil.streamForClassHierarchy(TestClass.class).collect(Collectors.toList());

        assertEquals(TestClass.class, hierarchy.get(0));
        assertEquals(TestSuperclass.class, hierarchy.get(1));
        assertEquals(Object.class, hierarchy.get(2));
        assertEquals(TestSuperclassInterface.class, hierarchy.get(3));
        assertEquals(TestInterface.class, hierarchy.get(4));
    }

    @Test
    public void testClassHierarchyMap() throws Exception {
        final List<Class<?>> hierarchy = new ArrayList<>();
        ClassUtil.recursiveMapToClassHierarchy(TestClass.class, c -> hierarchy.add(c));

        assertEquals(TestClass.class, hierarchy.get(0));
        assertEquals(TestInterface.class, hierarchy.get(1));
        assertEquals(TestSuperclass.class, hierarchy.get(2));
        assertEquals(TestSuperclassInterface.class, hierarchy.get(3));
        assertEquals(Object.class, hierarchy.get(4));
    }

    @Test
    public void testEnsureObjectType() {
        Class[] primitives = new Class[]{boolean.class, byte.class, char.class, int.class, short.class, long.class,
            float.class, double.class, void.class};
        Class[] boxingTypes = new Class[]{Boolean.class, Byte.class, Character.class, Integer.class, Short.class,
            Long.class, Float.class, Double.class, Void.class};

        assertEquals("Test is invalid. Arrays are expected to be the same size.", primitives.length, boxingTypes.length);

        for(int i = 0; i < primitives.length; ++i) {
            assertEquals(boxingTypes[i], ClassUtil.ensureObjectType(primitives[i]));
            assertEquals(boxingTypes[i], ClassUtil.ensureObjectType(boxingTypes[i]));
        }
    }

    @Test
    public void testGetArrayType() {
        String[] strArray = {};
        assertEquals(strArray.getClass(), ClassUtil.getArrayType(String.class));
    }
}
