/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.node;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.ComplexNumberCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.FuzzyIntervalCell;
import org.knime.core.data.def.FuzzyNumberCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.config.base.JSONConfig.WriterConfig;

/**
 * Test the <code>Config</code> class.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class NodeSettingsTest {

    private NodeSettings m_settings;

    @Before
    public void setUp() throws Exception {
        m_settings = new NodeSettings("test-settings");
    }

    @After
    public void tearDown() throws Exception {
        StringBuffer buf = new StringBuffer();
        m_settings.toString(buf);
        NodeLogger.getLogger(getClass()).debug(buf.toString());
        testFile();
        testXML();
        testJSON();
    }

//    /**
//     * Tests special chars.
//     * @throws Exception
//     */
//    public void testSpecialChars() throws Exception {
//        NodeSettings s = new NodeSettings("Special Chars");
//        char[] cs = new char[2 * 127];
//        for (int i = 0; i < cs.length; i += 2) {
//            cs[i] = (char)(i / 2);
//            cs[i + 1] = ' ';
//        }
//        String csString = new String(cs);
//        System.out.println(csString);
//        csString = new String(new BASE64Encoder().encodeBuffer(csString.getBytes()));
//        s.addString("key", csString);
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        s.saveToXML(out);
//        out.close();
//        byte[] outBytes = out.toByteArray();
//        ByteArrayInputStream in = new ByteArrayInputStream(outBytes);
//        NodeSettingsRO r = NodeSettings.loadFromXML(in);
//        csString = new String(new BASE64Decoder().decodeBuffer(r.getString("key")));
//        for (int i = 0; i < csString.length(); i += 2) {
//            char c = csString.charAt(i);
//            char space = csString.charAt(i + 1);
//            assert c == (char)(i / 2);
//            assert space == ' ';
//            System.out.print(c);
//            System.out.print(space);
//        }
//        System.out.println();
//    }

    /**
     * Test write/read of ints.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testInt() throws Exception {
        try {
            m_settings.addInt(null, 11);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        String key = "kint";
        m_settings.addInt(key, 5);
        assertTrue(m_settings.containsKey(key));
        assertTrue(5 == m_settings.getInt(key));
        assertTrue(5 == m_settings.getInt(key, -1));
        key += "array";
        m_settings.addIntArray(key, new int[]{42, 13});
        assertTrue(m_settings.containsKey(key));
        int[] a = m_settings.getIntArray(key);
        assertTrue(a[0] == 42 && a[1] == 13);
        a = m_settings.getIntArray(key, new int[0]);
        assertTrue(a[0] == 42 && a[1] == 13);
        key = "kint_array_0";
        m_settings.addIntArray(key, new int[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getIntArray(key).length == 0);
        assertTrue(m_settings.getIntArray(key, new int[1]).length == 0);
        key = "kint-";
        m_settings.addIntArray(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getIntArray(key) == null);
    }

    @Test
    public void testBasicJSONIO() throws IOException {
        m_settings.addBoolean("key-bool", true);
        m_settings.addBooleanArray("key-bool-arr", new boolean[] {true, false});
        m_settings.addString("key-string", "Sequence of Characters");
        m_settings.addStringArray("key-string-arr", new String[] {"Sequence", "of", "Characters"});
        m_settings.addDouble("key-double", 27.6);
        m_settings.addDoubleArray("key-double-arr", new double[] {12.2, 15.0, -2.7});
        m_settings.addInt("key-int", 12);
        m_settings.addIntArray("key-int-arr", new int[] {1, 2, 3});
        m_settings.addPassword("key-int", "secret", "the-real-password");
        m_settings.addTransientString("key-transient", "transient-string-value");
        Config subtree = m_settings.addConfig("key-subtree");
        subtree.addString("key-subtree-string", "value-subtree-string");
        subtree.addInt("key-subtree-int", 391);
        subtree.addDouble("key-subtree-double", -13.2);
        String jsonString = JSONConfig.toJSONString(m_settings, WriterConfig.PRETTY);
        System.out.println(jsonString);
        NodeSettings newCopy = JSONConfig.readJSON(new NodeSettings("test-settings"), new StringReader(jsonString));
        // transient strings are ... transient so not saved and we restore the original value.
        newCopy.addTransientString("key-transient", "transient-string-value");
        Assert.assertEquals(m_settings, newCopy);

        // there is more testing done in 'teardown', which includes save/load
        m_settings.addString("key-transient", "overwritten-for-persistable-string");
    }

    /**
     * Test write/read of doubles.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testDouble() throws Exception {
        try {
            m_settings.addDouble(null, 11.11);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "kdouble";
        m_settings.addDouble(key, 5.5);
        assertTrue(m_settings.containsKey(key));
        assertTrue(5.5 == m_settings.getDouble(key));
        assertTrue(5.5 == m_settings.getDouble(key, -1.0));
        key += "array";
        m_settings.addDoubleArray(key, new double[]{42.42, 13.13});
        assertTrue(m_settings.containsKey(key));
        double[] a = m_settings.getDoubleArray(key);
        assertTrue(a[0] == 42.42 && a[1] == 13.13);
        a = m_settings.getDoubleArray(key, new double[0]);
        assertTrue(a[0] == 42.42 && a[1] == 13.13);
        key = "kdouble_array_0";
        m_settings.addDoubleArray(key, new double[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDoubleArray(key).length == 0);
        assertTrue(m_settings.getDoubleArray(key, new double[1]).length == 0);
        key = "kdouble-";
        m_settings.addDoubleArray(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDoubleArray(key) == null);
    }

    /**
     * Test write/read of floats.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testFloat() throws Exception {
        try {
            m_settings.addFloat(null, 11.11f);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "kfloat";
        m_settings.addFloat(key, 5.5f);
        assertTrue(m_settings.containsKey(key));
        assertTrue(5.5 == m_settings.getFloat(key));
        assertTrue(5.5 == m_settings.getFloat(key, -1.0f));
        key += "array";
        m_settings.addFloatArray(key, new float[]{42.42f, 13.13f});
        assertTrue(m_settings.containsKey(key));
        float[] a = m_settings.getFloatArray(key);
        assertTrue(a[0] == 42.42f && a[1] == 13.13f);
        a = m_settings.getFloatArray(key, new float[0]);
        assertTrue(a[0] == 42.42f && a[1] == 13.13f);
        key = "kfloat_array_0";
        m_settings.addFloatArray(key, new float[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getFloatArray(key).length == 0);
        assertTrue(m_settings.getFloatArray(key, new float[1]).length == 0);
        key = "kfloat-";
        m_settings.addFloatArray(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getFloatArray(key) == null);
    }

    /**
     * Test write/read of ints.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testBoolean() throws Exception {
        try {
            m_settings.addBoolean(null, true);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "kboolean";
        m_settings.addBoolean(key, true);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getBoolean(key));
        assertTrue(m_settings.getBoolean(key, false));
        key += "array";
        m_settings.addBooleanArray(key, new boolean[]{false, true});
        assertTrue(m_settings.containsKey(key));
        boolean[] a = m_settings.getBooleanArray(key);
        assertTrue(!a[0] && a[1]);
        a = m_settings.getBooleanArray(key, new boolean[0]);
        assertTrue(!a[0] && a[1]);
        key = "kboolean_array_0";
        m_settings.addBooleanArray(key, new boolean[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getBooleanArray(key).length == 0);
        assertTrue(m_settings.getBooleanArray(key, new boolean[1]).length == 0);
        key = "kboolean-";
        m_settings.addBooleanArray(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getBooleanArray(key) == null);
    }

    /**
     * Test write/read of chars.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testChar() throws Exception {
        try {
            m_settings.addChar(null, '5');
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "kchar";
        m_settings.addChar(key, '5');
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getChar(key) == '5');
        assertTrue(m_settings.getChar(key, 'n') == '5');
        key += "array";
        m_settings.addCharArray(key, new char[]{'4', '2'});
        assertTrue(m_settings.containsKey(key));
        char[] a = m_settings.getCharArray(key);
        assertTrue(a[0] == '4' && a[1] == '2');
        a = m_settings.getCharArray(key, new char[0]);
        assertTrue(a[0] == '4' && a[1] == '2');
        key = "kchar_array_0";
        m_settings.addCharArray(key, new char[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getCharArray(key).length == 0);
        assertTrue(m_settings.getCharArray(key, new char[1]).length == 0);
        key = "kchar-";
        m_settings.addCharArray(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getCharArray(key) == null);
    }

    /**
     * Test write/read of shorts.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testShort() throws Exception {
        try {
            m_settings.addShort(null, (short)'5');
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "kshort";
        m_settings.addShort(key, (short)'5');
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getShort(key) == '5');
        assertTrue(m_settings.getShort(key, (short)'n') == (short)'5');
        key += "array";
        m_settings.addShortArray(key, new short[]{'4', '2'});
        assertTrue(m_settings.containsKey(key));
        short[] a = m_settings.getShortArray(key);
        assertTrue(a[0] == '4' && a[1] == '2');
        a = m_settings.getShortArray(key, new short[0]);
        assertTrue(a[0] == '4' && a[1] == '2');
        key = "kshort_array_0";
        m_settings.addShortArray(key, new short[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getShortArray(key).length == 0);
        assertTrue(m_settings.getShortArray(key, new short[1]).length == 0);
        key = "kshort-";
        m_settings.addShortArray(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getShortArray(key) == null);
    }

    /**
     * Test write/read of longs.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testLong() throws Exception {
        try {
            m_settings.addLong(null, 42L);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "klong";
        m_settings.addLong(key, 42L);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getLong(key) == 42L);
        assertTrue(m_settings.getLong(key, 5L) == 42L);
        key += "array";
        m_settings.addLongArray(key, 11L, 66L);
        assertTrue(m_settings.containsKey(key));
        long[] a = m_settings.getLongArray(key);
        assertTrue(a[0] == 11L && a[1] == 66L);
        a = m_settings.getLongArray(key, new long[0]);
        assertTrue(a[0] == 11L && a[1] == 66L);
        key = "klong_array_0";
        m_settings.addLongArray(key, new long[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getLongArray(key).length == 0);
        assertTrue(m_settings.getLongArray(key, new long[1]).length == 0);
        key = "klong-";
        m_settings.addLongArray(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getLongArray(key) == null);
    }

    /**
     * Test write/read of bytes.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testByte() throws Exception {
        try {
            m_settings.addByte(null, (byte)'n');
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "kbyte";
        m_settings.addByte(key, (byte)'b');
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getByte(key) == (byte)'b');
        assertTrue(m_settings.getByte(key, (byte)'n') == (byte)'b');
        key += "array";
        m_settings.addByteArray(key, new byte[]{'4', '2'});
        assertTrue(m_settings.containsKey(key));
        byte[] a = m_settings.getByteArray(key);
        assertTrue(a[0] == '4' && a[1] == '2');
        a = m_settings.getByteArray(key, new byte[0]);
        assertTrue(a[0] == '4' && a[1] == '2');
        key = "kbyte_array_0";
        m_settings.addByteArray(key, new byte[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getByteArray(key).length == 0);
        assertTrue(m_settings.getByteArray(key, new byte[1]).length == 0);
        key = "kbyte-";
        m_settings.addByteArray(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getByteArray(key) == null);
    }

    /**
     * Test write/read of StringCells.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testStringDataCell() throws Exception {
        try {
            m_settings.addDataCell(null, new StringCell("null"));
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "nullDataCell";
        m_settings.addDataCell(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDataCell(key) == null);
        DataCell nullCell = new StringCell("null");
        assertTrue(m_settings.getDataCell(key, nullCell) == null);
        key = "kDataCell";
        m_settings.addDataCell(key, new StringCell("B"));
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDataCell(key).equals(new StringCell("B")));
        assertTrue(m_settings.getDataCell(key, null).equals(
                new StringCell("B")));
        key += "array";
        m_settings.addDataCellArray(key, new DataCell[]{new StringCell("T"),
                new StringCell("P"), new StringCell("M")});
        assertTrue(m_settings.containsKey(key));
        DataCell[] a = m_settings.getDataCellArray(key);
        assertTrue(a[0].equals(new StringCell("T")));
        assertTrue(a[1].equals(new StringCell("P")));
        assertTrue(a[2].equals(new StringCell("M")));
        a = m_settings.getDataCellArray(key, new DataCell[0]);
        assertTrue(a[0].equals(new StringCell("T")));
        assertTrue(a[1].equals(new StringCell("P")));
        assertTrue(a[2].equals(new StringCell("M")));
        key = "kDataCell_array_0";
        m_settings.addDataCellArray(key, new DataCell[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDataCellArray(key).length == 0);
        assertTrue(m_settings.getDataCellArray(key, new DataCell[1]).length == 0);
        key = "kDataCell-";
        m_settings.addDataCellArray(key, (DataCell[]) null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDataCellArray(key) == null);
        key = "unknownDataCell";
        DataCell unknownCell = new FuzzyNumberCell(0.0, 1.0, 2.0);
        m_settings.addDataCell(key, unknownCell);
        assertTrue(m_settings.containsKey(key));
        assertTrue(unknownCell.equals(m_settings.getDataCell(key)));
    }

    /**
     * Test write/read of DataCells.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testDataCell() throws Exception {
        StringCell s = new StringCell("stringi");
        m_settings.addDataCell("string", s);
        assertTrue(m_settings.containsKey("string"));
        assertTrue(m_settings.getDataCell("string").equals(s));
        DoubleCell d = new DoubleCell(45.42);
        m_settings.addDataCell("double", d);
        assertTrue(m_settings.containsKey("double"));
        assertTrue(m_settings.getDataCell("double").equals(d));
        IntCell i = new IntCell(11);
        m_settings.addDataCell("int", i);
        assertTrue(m_settings.containsKey("int"));
        assertTrue(m_settings.getDataCell("int").equals(i));
        DataCell m = DataType.getMissingCell();
        m_settings.addDataCell("missing", m);
        assertTrue(m_settings.containsKey("missing"));
        assertTrue(m_settings.getDataCell("missing").equals(m));
        ComplexNumberCell c = new ComplexNumberCell(5.4, 4.5);
        m_settings.addDataCell("complex", c);
        assertTrue(m_settings.containsKey("complex"));
        assertTrue(m_settings.getDataCell("complex").equals(c));
        FuzzyNumberCell n = new FuzzyNumberCell(1, 2, 4);
        m_settings.addDataCell("fnumber", n);
        assertTrue(m_settings.containsKey("fnumber"));
        assertTrue(m_settings.getDataCell("fnumber").equals(n));
        FuzzyIntervalCell f = new FuzzyIntervalCell(1, 2, 3, 4);
        m_settings.addDataCell("finterval", f);
        assertTrue(m_settings.containsKey("finterval"));
        assertTrue(m_settings.getDataCell("finterval").equals(f));
        DataCell unknownCell = new UnknownCell();
        m_settings.addDataCell("unknownCell", unknownCell);
        assertTrue(m_settings.containsKey("unknownCell"));
        assertTrue(m_settings.getDataCell("unknownCell").equals(unknownCell));
    }

    private static class UnknownCell extends DataCell {
        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            return dc instanceof UnknownCell;
        }
        @Override
        public int hashCode() {
            return toString().hashCode();
        }
        @Override
        public String toString() {
            return "unknown";
        }
    }


    /**
     * Test write/read of DataType elements.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testDataType() throws Exception {
        try {
            m_settings.addDataType(null, StringCell.TYPE);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "nullDataType";
        m_settings.addDataType(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDataType(key) == null);
        assertTrue(m_settings.getDataType(key, StringCell.TYPE) == null);
        key = "kDataType";
        m_settings.addDataType(key, DoubleCell.TYPE);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDataType(key).equals(DoubleCell.TYPE));
        assertTrue(m_settings.getDataType(key, null).equals(DoubleCell.TYPE));
        key += "array";
        m_settings.addDataTypeArray(key, new DataType[]{DoubleCell.TYPE,
                StringCell.TYPE, IntCell.TYPE});
        assertTrue(m_settings.containsKey(key));
        DataType[] a = m_settings.getDataTypeArray(key);
        assertTrue(a[0].equals(DoubleCell.TYPE));
        assertTrue(a[1].equals(StringCell.TYPE));
        assertTrue(a[2].equals(IntCell.TYPE));
        a = m_settings.getDataTypeArray(key, new DataType[0]);
        assertTrue(a[0].equals(DoubleCell.TYPE));
        assertTrue(a[1].equals(StringCell.TYPE));
        assertTrue(a[2].equals(IntCell.TYPE));
        key = "kDataType_array_0";
        m_settings.addDataTypeArray(key, new DataType[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDataTypeArray(key).length == 0);
        assertTrue(m_settings.getDataTypeArray(key, new DataType[1]).length == 0);
        key = "kDataType-";
        m_settings.addDataTypeArray(key, (DataType[]) null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getDataTypeArray(key) == null);
        key = "unknownDataType";
        m_settings.addDataType(key, FuzzyIntervalCell.TYPE);
        assertTrue(m_settings.containsKey(key));
        DataType unknownType = m_settings.getDataType(key);
        assertTrue(FuzzyIntervalCell.TYPE.equals(unknownType));
    }

    /**
     * Test write/read of Strings.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testString() throws Exception {
        try {
            m_settings.addString(null, "null");
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "nullString";
        m_settings.addString(key, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getString(key) == null);
        assertTrue(m_settings.getString(key, "null") == null);
        key = "kString";
        m_settings.addString(key, "B");
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getString(key).equals("B"));
        assertTrue(m_settings.getString(key, null).equals("B"));
        key += "array";
        m_settings.addStringArray(key, new String[]{"T", "P", "M"});
        assertTrue(m_settings.containsKey(key));
        String[] a = m_settings.getStringArray(key);
        assertTrue(a[0].equals("T"));
        assertTrue(a[1].equals("P"));
        assertTrue(a[2].equals("M"));
        a = m_settings.getStringArray(key, new String[0]);
        assertTrue(a[0].equals("T"));
        assertTrue(a[1].equals("P"));
        assertTrue(a[2].equals("M"));
        key = "kString_array_0";
        m_settings.addStringArray(key, new String[0]);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getStringArray(key).length == 0);
        assertTrue(m_settings.getStringArray(key, new String[1]).length == 0);
        key = "kString-";
        m_settings.addStringArray(key, (String[]) null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getStringArray(key) == null);
    }

    /**
     * Test write/read of NodeSettings.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testConfig() throws Exception {
        try {
            m_settings.addNodeSettings((String)null);
            fail();
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
        String key = "kConfig";
        NodeSettings c = (NodeSettings) m_settings.addNodeSettings(key);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getNodeSettings(key) == c);
        c.addString("kString_plus", "6");
        c.containsKey("kString_plus");
        assertTrue(c.getString("kString_plus", "-1").equals("6"));
    }

    /**
     * Tests <code>getKeySet()</code> and <code>getKeySet(String)</code>.
     */
    @Test
    public void testKeySets() {
        NodeSettings key = (NodeSettings) m_settings.addNodeSettings("test_key_set");
        key.addNodeSettings("ConfigA");
        key.addNodeSettings("ConfigB");
        key.addNodeSettings("ConfigC");
        key.addInt("intA", 0);
        key.addInt("intB", 1);
        assertTrue(key.containsKey("ConfigA"));
        assertTrue(key.containsKey("ConfigB"));
        assertTrue(key.containsKey("ConfigC"));
        assertTrue(key.containsKey("intA"));
        assertTrue(key.containsKey("intB"));
        Set<String> keys = key.keySet();
        assertFalse(keys == null);
        assertTrue("" + keys.size(), keys.size() == 5);
        String[] k = keys.toArray(new String[0]);
        assertTrue(k[0].equals("ConfigA"));
        assertTrue(k[1].equals("ConfigB"));
        assertTrue(k[2].equals("ConfigC"));
        assertTrue(k[3].equals("intA"));
        assertTrue(k[4].equals("intB"));
    }

    /**
     * Test \n, \r, and \t.
     *
     * @throws Exception Should not happen.
     */
    @Test
    public void testSpecialStrings() throws Exception {
        NodeSettings key = (NodeSettings) m_settings.addNodeSettings(
                "special_strings");
        key.addString("N", "\n");
        key.addString("R", "\r");
        key.addString("T", "\t");
        key.addString("GT", ">");
        key.addString("EMPTY", "");
        key.addString("LENGTH1", " ");
        key.addString("null", null);
        key.addString("NULL", "null");
        assertTrue(key.containsKey("N"));
        assertTrue(key.containsKey("R"));
        assertTrue(key.containsKey("T"));
        assertTrue(key.containsKey("GT"));
        assertTrue(key.containsKey("EMPTY"));
        assertTrue(key.containsKey("null"));
        assertTrue(key.containsKey("NULL"));
        assertTrue(key.containsKey("LENGTH1"));
        assertTrue(key.getString("N").equals("\n"));
        assertTrue(key.getString("R").equals("\r"));
        assertTrue(key.getString("T").equals("\t"));
        assertTrue(key.getString("GT").equals(">"));
        assertTrue(key.getString("EMPTY").equals(""));
        assertTrue(key.getString("null"), key.getString("null") == null);
        assertTrue(key.getString("NULL").equals("null"));
        assertTrue(key.getString("LENGTH1").equals(" "));
    }

    /**
     * Test a 3x2x3 int array.
     * @throws InvalidSettingsException If a value could not be read.
     */
    @Test
    public void testInt3DMatrix() throws InvalidSettingsException {
        NodeSettings config = (NodeSettings) m_settings.addNodeSettings("matrix");
        // write int matrix
        int[][][] array = new int[][][]{{{1, 2, 4}, {5, 2, 6}, {7, 1, 9}},
                {{7, 6, 4}, {8, 2, 9}, {0, 1, 2}}};
        for (int r = 0; r < array.length; r++) {
            for (int i = 0; i < array[r].length; i++) {
                String key = "int_array_" + r + "_" + i;
                config.addIntArray(key, array[r][i]);
                assertTrue(config.containsKey(key));
                assertTrue(Arrays.equals(config.getIntArray(key), array[r][i]));
            }
        }
        // read and test int matrix
        for (int r = 0; r < array.length; r++) {
            for (int i = 0; i < array[r].length; i++) {
                String key = "int_array_" + r + "_" + i;
                assertTrue(config.containsKey(key));
                assertTrue(Arrays.equals(config.getIntArray(key), array[r][i]));
            }
        }
    }

    /**
     * Tests multiple keys for different types.
     */
    @Test
    public void testKeys() {
        String key = "key-test";
        m_settings.addNodeSettings(key);
        m_settings.addString(key, "string");
        m_settings.addBoolean(key, true);
        m_settings.addInt(key, -42);
        m_settings.addDouble(key, 5.0);
        m_settings.addShort(key, (short)'s');
        m_settings.addChar(key, 'c');
        m_settings.addByte(key, (byte)'b');
        m_settings.addDataCell(key, null);
        m_settings.addDataType(key, null);
    }

    /**
     * Test serialize/deserialize.
     * @throws IOException
     */
    public void testFile() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        // write this NodeSettings
        m_settings.writeToFile(oos);
        // and read the NodeSettings again
        byte[] bytes = os.toByteArray();
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(is);
        NodeSettings settings = NodeSettings.readFromFile(ois);
        assertTrue(settings.isIdentical(m_settings));
    }

    /**
     * Test XML read/write.
     * @throws IOException
     */
    public void testXML() throws IOException {
        // store to XML
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        m_settings.saveToXML(os);
        // read from XML
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        NodeSettingsRO settings = NodeSettings.loadFromXML(is);
        assertTrue(settings.equals(m_settings));
    }

    public void testJSON() throws Exception {
        StringWriter writer = new StringWriter();
        JSONConfig.writeJSON(m_settings, writer, WriterConfig.PRETTY);
        String s = writer.toString();
        NodeSettings copySettings = JSONConfig.readJSON(new NodeSettings("test-settings"), new StringReader(s));
        assertTrue(copySettings.equals(m_settings));
    }

    /**
     * Checks whether the add/getPassword methods work as expected.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testPassword() throws Exception {
        final String encKey = "LaLeLu";

        try {
            m_settings.addPassword(null, encKey, "null");
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
        String key = "nullString";
        m_settings.addPassword(key, encKey, null);
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getPassword(key, encKey) == null);
        assertTrue(m_settings.getPassword(key, encKey, "null") == null);

        key = "simplePassword";
        m_settings.addPassword(key, encKey, "B");
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getPassword(key, encKey).equals("B"));
        assertTrue(m_settings.getPassword(key, encKey, null).equals("B"));

        key = "emptyPassword";
        m_settings.addPassword(key, encKey, "");
        assertTrue(m_settings.containsKey(key));
        assertTrue(m_settings.getPassword(key, encKey).equals(""));
        assertTrue(m_settings.getPassword(key, encKey, null).equals(""));

        assertTrue(m_settings.getPassword("nonExistingPassword", encKey, "none").equals("none"));
    }

    @Test
    public void testJapanese() throws Exception {
        m_settings.addString("japanese", "あ");
        Assert.assertEquals("あ",   m_settings.getString("japanese"));
    }

} // NodeSettingsTest
