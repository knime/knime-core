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
 *   Jun 20, 2020 (carlwitt): created
 */
package org.knime.core.data.join.results;

import static org.junit.Assert.assertEquals;
import static org.knime.core.data.join.JoinTestInput.cell;
import static org.knime.core.data.join.JoinTestInput.defaultRow;

import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinTableSettings;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.data.join.implementation.JoinerFactory.JoinAlgorithm;
import org.knime.core.data.join.JoinTestInput;
import org.knime.core.data.join.results.JoinResult.OutputCombined;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class JoinContainerTest {

    private static final int LEFT = 0, RIGHT = 1;

    private final DataTableSpec[] m_specs = new DataTableSpec[2];

    private final JoinTableSettings[] m_settings = new JoinTableSettings[2];

    private final DataRow[] m_rows;

    /**
     * @throws InvalidSettingsException
     */
    public JoinContainerTest() throws InvalidSettingsException {
        m_rows = new DataRow[]{defaultRow("left,a,b,c,d,e"), defaultRow("right,u,v,w,x,y,z")};

        BufferedDataTable left = JoinTestInput.table("A,B,C,D,E", m_rows[0]);
        BufferedDataTable right = JoinTestInput.table("U,V,W,A,Y,Z", m_rows[1]);
        m_specs[LEFT] = left.getDataTableSpec();
        m_specs[RIGHT] = right.getDataTableSpec();
        m_settings[LEFT] = new JoinTableSettings(true, JoinColumn.array("A", "B", "C", "D"),
            new String[]{"A", "C", "E"}, InputTable.LEFT, left);
        m_settings[RIGHT] = new JoinTableSettings(true, JoinColumn.array("A", "Y", "Z", "A"),
            new String[]{"U", "V", "A"}, InputTable.RIGHT, right);

    }

    /**
     * Padding makes sense only when providing output in a single table. In this case, the included columns from the
     * other table are filled with missing values. When merge join columns is enabled, the added missing values must
     * conform to the different output table columns.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void toSingleTableFormatMerged() throws InvalidSettingsException {

        // right outer has values u,v,w,x,y,z so will be mapped to
        // joined row has columns "A", "C=Z", "D=A", "E", "U", "V"
        //                         x     z      x     ?    u    v
        // left outer has values a,b,c,d,e so will be mapped to
        // joined row has columns "A", "C=Z", "D=A", "E", "U", "V"
        //                         a     c      d     e    ?    ?

        JoinSpecification jspec =
            new JoinSpecification.Builder(m_settings[LEFT], m_settings[RIGHT]).mergeJoinColumns(true).build();
        JoinContainer<OutputCombined> container = (JoinContainer<OutputCombined>)Unsorted
                .createCombined(JoinAlgorithm.AUTO.getFactory().create(jspec, JoinTestInput.EXEC), false);

        { // pad left
            DataRow padded = container.rightToSingleTableFormat(m_rows[RIGHT]);

            assertEquals(cell("x"), padded.getCell(0)); // A=A
            assertEquals(cell("z"), padded.getCell(1)); // C=Z
            assertEquals(cell("x"), padded.getCell(2)); // D=A
            assertEquals(DataType.getMissingCell(), padded.getCell(3)); // E
            assertEquals(cell("u"), padded.getCell(4)); // U
            assertEquals(cell("v"), padded.getCell(5)); // V
        }

        { // pad right
            DataRow padded = container.leftToSingleTableFormat(m_rows[LEFT]);

            assertEquals(cell("a"), padded.getCell(0));
            assertEquals(cell("c"), padded.getCell(1));
            assertEquals(cell("d"), padded.getCell(2));
            assertEquals(cell("e"), padded.getCell(3));
            assertEquals(DataType.getMissingCell(), padded.getCell(4)); // U
            assertEquals(DataType.getMissingCell(), padded.getCell(5)); // V
        }
    }

    /**
     * Padding makes sense only when providing output in a single table. In this case, the included columns from the
     * other table are filled with missing values.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void toSingleTableFormat() throws InvalidSettingsException {
        // left table A B C D E
        // left join columns  A B C D, include A C E
        // right table U V W A Y Z
        // right join columns A Y Z A, include U V A
        // joined row has columns "A", "C", "E", "U", "V", "A (#1)"

        JoinSpecification jspec =
            new JoinSpecification.Builder(m_settings[LEFT], m_settings[RIGHT]).mergeJoinColumns(false).build();
        JoinContainer<OutputCombined> container = (JoinContainer<OutputCombined>)Unsorted
            .createCombined(JoinAlgorithm.AUTO.getFactory().create(jspec, JoinTestInput.EXEC), false);

        { // pad left
            DataRow padded = container.rightToSingleTableFormat(m_rows[RIGHT]);

            assertEquals(DataType.getMissingCell(), padded.getCell(0)); // A
            assertEquals(DataType.getMissingCell(), padded.getCell(1)); // C
            assertEquals(DataType.getMissingCell(), padded.getCell(2)); // E
            assertEquals(cell("u"), padded.getCell(3));
            assertEquals(cell("v"), padded.getCell(4));
            assertEquals(cell("x"), padded.getCell(5));
        }

        { // pad right
            DataRow padded = container.leftToSingleTableFormat(m_rows[LEFT]);

            assertEquals(cell("a"), padded.getCell(0));
            assertEquals(cell("c"), padded.getCell(1));
            assertEquals(cell("e"), padded.getCell(2));
            assertEquals(DataType.getMissingCell(), padded.getCell(3)); // U
            assertEquals(DataType.getMissingCell(), padded.getCell(4)); // V
            assertEquals(DataType.getMissingCell(), padded.getCell(5)); // A (#1)
        }
    }

}
