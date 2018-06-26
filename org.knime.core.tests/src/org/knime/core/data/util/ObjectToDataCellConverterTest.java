/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.data.util;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;

import junit.framework.TestCase;

/**
 * Unit test for <code>ObjectToDataCellConverter</code>.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ObjectToDataCellConverterTest extends TestCase {
    
    /** Instance to test on. */
    @SuppressWarnings("deprecation")
    private final ObjectToDataCellConverter m_converter = 
        new ObjectToDataCellConverter();
    
    /** Main class to start unit test from commandline.
     * @param args ignored
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(ObjectToDataCellConverterTest.class);
    }

    /**
     * Class under test for DataCell createDataCell(Object).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellObject() {
        int i = 5;
        DataCell c = m_converter.createDataCell(new Integer(i));
        assert ((IntValue)c).getIntValue() == i;
        c = m_converter.createDataCell(new Byte((byte)i));
        assert ((IntValue)c).getIntValue() == i;
        float d = 8.0f;
        c = m_converter.createDataCell(new Double(d));
        assert ((DoubleValue)c).getDoubleValue() == d;
        c = m_converter.createDataCell(new Float(d));
        assert ((DoubleValue)c).getDoubleValue() == d;
        String s = "Test String";
        c = m_converter.createDataCell(s);
        assert ((StringValue)c).getStringValue().equals(s);
        c = m_converter.createDataCell(null);
        assert c.isMissing();
        try {
            c = m_converter.createDataCell(new Object());
            assert false;
        } catch (IllegalArgumentException iae) {
            assert true; // avoid complaints by checkstyle!
        }
    } // testCreateDataCellObject()

    /**
     * Class under test for DataCell createDataCell(double).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCelldouble() {
        double d = 8.0;
        DataCell c = m_converter.createDataCell(d);
        assert ((DoubleValue)c).getDoubleValue() == d;
    } // testCreateDataCelldouble()

    /**
     * Class under test for DataCell createDataCell(float).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellfloat() {
        float f = 8.0f;
        DataCell c = m_converter.createDataCell(f);
        assert ((DoubleValue)c).getDoubleValue() == f;
    } // testCreateDataCellfloat()

    /**
     * Class under test for DataCell createDataCell(int).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellint() {
        int i = 12;
        DataCell c = m_converter.createDataCell(i);
        assert ((IntValue)c).getIntValue() == i;
    } // testCreateDataCellint()

    /**
     * Class under test for DataCell createDataCell(byte).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellbyte() {
        byte b = 13;
        DataCell c = m_converter.createDataCell(b);
        assert ((IntValue)c).getIntValue() == b;
    } // testCreateDataCellbyte()

    /**
     * Class under test for DataCell createDataCell(boolean).
     */
    @SuppressWarnings("deprecation")
    public final void testCreateDataCellboolean() {
        boolean b = true;
        DataCell c = m_converter.createDataCell(b);
        assert ((IntValue)c).getIntValue() == 1;
        b = false;
        c = m_converter.createDataCell(b);
        assert ((IntValue)c).getIntValue() == 0;
    } // testCreateDataCellboolean()

}
