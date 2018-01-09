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
 */
package org.knime.core.data;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeSettings;

/**
 * Tests {@link DataTableSpec} and {@link DataTableSpecCreator}.
 * @author wiswedel
 *
 */
public final class DataTableSpecCreatorTest {

    private static DataColumnSpec[] createColumnSpecs(final int count, final String prefixName) {
        DataColumnSpec[] result = new DataColumnSpec[count];
        for (int i = 0; i < count; i++) {
            DataType type;
            switch (i % 3) {
                case 0: type = StringCell.TYPE; break;
                case 1: type = DoubleCell.TYPE; break;
                default: type = IntCell.TYPE; break;
            }
            String name = prefixName + i;
            result[i] = new DataColumnSpecCreator(name, type).createSpec();
        }
        return result;
    }

    @Test
    public void testCreatorSimple() {
        DataColumnSpec[] cols = createColumnSpecs(5, "ColName");
        DataTableSpec reference = new DataTableSpec(cols);
        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(cols);
        DataTableSpec toTest = creator.createSpec();
        Assert.assertEquals(reference, toTest);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCreatorSimpleConflictNames() {
        DataColumnSpec[] cols = createColumnSpecs(5, "ColName");
        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(cols);
        creator.addColumns(cols[4]); // throws exception
    }

    @Test
    public void testCreatorMultipleTableSpecs() {
        DataColumnSpec[] cols1 = createColumnSpecs(5, "ColName");
        DataColumnSpec[] cols2 = createColumnSpecs(5, "OtherColName");
        DataColumnSpec[] cols3 = createColumnSpecs(5, "YetOtherColName");
        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(new DataTableSpec(cols1)).addColumns(
                new DataTableSpec(cols2)).addColumns(new DataTableSpec(cols3));
        DataTableSpec outputSpec = creator.createSpec();
        DataColumnSpec[] allCols = new DataColumnSpec[cols1.length + cols2.length + cols3.length];
        System.arraycopy(cols1, 0, allCols, 0, cols1.length);
        System.arraycopy(cols2, 0, allCols, cols1.length, cols2.length);
        System.arraycopy(cols3, 0, allCols, cols1.length + cols2.length, cols3.length);
        Assert.assertEquals(outputSpec, new DataTableSpec(allCols));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCreatorMultipleTableSpecsConflictNames() {
        DataColumnSpec[] cols1 = createColumnSpecs(5, "ColName");
        DataColumnSpec[] cols2 = createColumnSpecs(5, "OtherColName");
        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(new DataTableSpec(cols1));
        creator.addColumns(new DataTableSpec(cols2));
        creator.addColumns(new DataTableSpec(cols2)); // throws exception
    }

    @Test
    public void testSetName() {
        DataColumnSpec[] cols1 = createColumnSpecs(5, "ColName");
        DataTableSpecCreator creator = new DataTableSpecCreator();
        String name = "expected name 'ae-";
        creator.addColumns(cols1).setName(name);
        Assert.assertEquals(creator.createSpec().getName(), name);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testEmptyProperty() {
        Map<String, String> props = new DataTableSpec().getProperties();
        Assert.assertTrue(props.isEmpty());
        props.put("key", "value"); // read only, throws exception
    }

    @Test
    public void testReplace() {
        DataColumnSpec[] cols1 = createColumnSpecs(5, "ColName");
        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(cols1);
        DataColumnSpec replaceCol = new DataColumnSpecCreator("ColName2", BooleanCell.TYPE).createSpec();
        creator.replaceColumn(2, replaceCol);

        replaceCol = new DataColumnSpecCreator("ColName-somethingdifferent", BooleanCell.TYPE).createSpec();
        creator.replaceColumn(0, replaceCol);

        DataTableSpec spec = creator.createSpec();

        final DataColumnSpec colSpec0 = spec.getColumnSpec(0);
        assertEquals("Column names don't match", "ColName-somethingdifferent", colSpec0.getName());
        assertEquals("Column types don't match", BooleanCell.TYPE, colSpec0.getType());

        final DataColumnSpec colSpec2 = spec.getColumnSpec(2);
        assertEquals("Column names don't match", "ColName2", colSpec2.getName());
        assertEquals("Column types don't match", BooleanCell.TYPE, colSpec2.getType());

        creator.addColumns(new DataColumnSpecCreator("ColName0", BooleanCell.TYPE).createSpec());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReplaceWithDuplicate() {
        DataColumnSpec[] cols1 = createColumnSpecs(5, "ColName");
        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(cols1);
        DataColumnSpec replaceCol = new DataColumnSpecCreator("ColName3", BooleanCell.TYPE).createSpec();
        creator.replaceColumn(2, replaceCol);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testReplaceWithInvalidIndex() {
        DataColumnSpec[] cols1 = createColumnSpecs(5, "ColName");
        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(cols1);
        DataColumnSpec replaceCol = new DataColumnSpecCreator("NewName", BooleanCell.TYPE).createSpec();
        creator.replaceColumn(6, replaceCol);
    }

    @Test
    public void testAllPropertiesAndSaveLoad() throws Exception {
        DataColumnSpec[] cols1 = createColumnSpecs(5, "ColName");
        DataTableSpecCreator creator = new DataTableSpecCreator();
        creator.addColumns(cols1);
        String name = "expected name 'ae-";
        creator.setName(name);
        creator.putProperties(Collections.<String, String>emptyMap());
        creator.putProperties(Collections.singletonMap("key1", "value1"));
        creator.putProperty("key2", "value2");
        DataTableSpec spec = creator.createSpec();
        Map<String, String> properties = spec.getProperties();
        Assert.assertEquals(properties.get("key1"), "value1");
        Assert.assertEquals(properties.get("key2"), "value2");
        NodeSettings config = new NodeSettings("dummy");
        spec.save(config);
        DataTableSpec load = DataTableSpec.load(config);
        Map<String, String> loadProperties = load.getProperties();
        Assert.assertEquals(loadProperties.get("key1"), "value1");
        Assert.assertEquals(loadProperties.get("key2"), "value2");
        Assert.assertEquals(spec, load);
    }

}
