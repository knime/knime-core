/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 */
package org.knime.core.data.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.data.v2.RowReadUtil.getValueReader;
import static org.knime.core.data.v2.RowReadUtil.readDoubleValue;
import static org.knime.core.data.v2.RowReadUtil.readIntValue;
import static org.knime.core.data.v2.RowReadUtil.readPrimitiveDoubleValue;
import static org.knime.core.data.v2.RowReadUtil.readPrimitiveIntValue;
import static org.knime.core.data.v2.RowReadUtil.readRowId;
import static org.knime.core.data.v2.RowReadUtil.readStringValue;
import static org.knime.testing.util.TableTestUtil.createTableFromColumns;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.testing.util.TableTestUtil;

public class RowReadUtilTest {

    private static RowRead createRowRead() {
        var stringColumn = new TableTestUtil.ObjectColumn("string", StringCell.TYPE, new String[]{"foo"});
        var doubleColumn = new TableTestUtil.ObjectColumn("double", DoubleCell.TYPE, new Double[]{1d});
        var intColumn = new TableTestUtil.ObjectColumn("int", IntCell.TYPE, new Integer[]{1});
        try (var cursor = createTableFromColumns(stringColumn, doubleColumn, intColumn).cursor()) {
            return cursor.forward();
        }
    }

    private static RowRead createRowReadMissing() {
        var stringColumn = new TableTestUtil.ObjectColumn("string", StringCell.TYPE, new String[]{null});
        var doubleColumn = new TableTestUtil.ObjectColumn("double", DoubleCell.TYPE, new Double[]{null});
        var intColumn = new TableTestUtil.ObjectColumn("int", IntCell.TYPE, new Integer[]{null});
        try (var cursor = createTableFromColumns(stringColumn, doubleColumn, intColumn).cursor()) {
            return cursor.forward();
        }
    }

    @Test
    void testReadValue() {
        assertThat(readRowId(createRowRead())).isEqualTo("rowkey 0");
        assertThat(readStringValue(createRowRead(), 0)).isEqualTo("foo");
        assertThat(readDoubleValue(createRowRead(), 1)).isEqualTo(1d);
        assertThat(readIntValue(createRowRead(), 2)).isEqualTo(1);
        assertThat(readPrimitiveIntValue(createRowRead(), 2)).isEqualTo(1);
    }

    @Test
    void testReadMissingValue() {
        assertThat(readStringValue(createRowReadMissing(), 0)).isNull();
        assertThat(readDoubleValue(createRowReadMissing(), 1)).isNull();
        assertThat(readIntValue(createRowReadMissing(), 2)).isNull();
        assertThrows(MissingValueException.class, () -> readPrimitiveDoubleValue(createRowReadMissing(), 1));
        assertThrows(MissingValueException.class, () -> readPrimitiveIntValue(createRowReadMissing(), 2));
    }

    @Test
    void testGetRowReader() {
        assertThat(getValueReader(StringCell.TYPE, 0).apply(createRowRead())).isEqualTo("foo");
        assertThat(getValueReader(IntCell.TYPE, 2).apply(createRowRead())).isEqualTo("1.0");
        assertThrows(NotImplementedException.class, () -> getValueReader(BinaryObjectDataCell.TYPE, 0));
    }

}
