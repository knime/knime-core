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
 *   Jan 12, 2023 (wiswedel): created
 */
package org.knime.core.node.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.OPTIONAL;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObject;
import org.knime.testing.core.ExecutionContextExtension;

/**
 * Tests mostly preformatting in {@link RowIssue}.
 *
 * @author Bernd Wiswedel, KNIME GmbH
 */
final class RowIssueTest {

    @RegisterExtension
    static ExecutionContextExtension executionContextExtension = ExecutionContextExtension.create();

    static BufferedDataTable createTable(final int nrRows, final int nrCols) {
        var exec = executionContextExtension.getExecutionContext();
        var dtsCreator = new DataTableSpecCreator();
        var availableTypes = new DataType[] {StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE};
        @SuppressWarnings("unchecked")
        BiFunction<Integer, Integer, DataCell>[] generators = new BiFunction[] {
            (row, col) -> new StringCell(String.format("Cell in row %d and column %d", row, col)),
            (row, col) -> new IntCell((int)row * (int)col),
            (row, col) -> new DoubleCell((int)row / (double)(int)col)
        };
        for (var c = 0; c < nrCols; c++) {
            var nameBuilder = new StringBuilder();
            if (c == 0) { // first column with long name
                IntStream.range('A', 'Z').forEach(i -> nameBuilder.append((char)i));
            } else {
                nameBuilder.append((char)('A' + c));
            }
            var type = availableTypes[c % 3];
            dtsCreator.addColumns(new DataColumnSpecCreator(nameBuilder.toString(), type).createSpec());
        }
        var container = exec.createDataContainer(dtsCreator.createSpec());

        for (var r = 0; r < nrRows; r++) {
            var row = r; // quasi-final
            var cells = IntStream.range(0, nrCols) //
                    .mapToObj(col -> generators[col % 3].apply(row, col)) //
                    .toArray(DataCell[]::new);
            container.addRowToTable(new DefaultRow(RowKey.createRowKey((long)r), cells));
        }
        container.close();
        return container.getTable();
    }

    /** Single column table (with errors). */
    @SuppressWarnings("static-method")
    @Test
    void testPreformattingSingleColumn() {
        var table = createTable(10, 1);
        var messageBuilder = Message.builder() //
            .withSummary("ignored") //
            .addRowIssue(0, 0, 5, "some error");

        var message = messageBuilder.build().orElseThrow();
        assertThat(message.getIssue()).get().isInstanceOf(RowIssue.class);
        var message2 = message.renderIssueDetails(new PortObject[] {table});
        assertThat(message2.getIssue()).get().isInstanceOf(DefaultIssue.class);
        var defaultIssue = (DefaultIssue)message2.getIssue().orElseThrow();
        assertThat(defaultIssue).extracting(d -> d.toPreformatted()).isEqualTo(
                " RowID  | ABCDEFGHIJK...\n"
              + "--------+----------------\n"
              + " Row3   | Cell in row...\n"
              + " Row4   | Cell in row...\n"
              + " Row5   | Cell in row...\n"
              + "          ^^^^^^^^^^^^^^\n"
              + "some error\n");
    }

    /** Test invalid arguments of issue */
    @SuppressWarnings("static-method")
    @Test
    void testInvalidArguments() {
        var builder = Message.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.addRowIssue(-1, 0, 5, "some error"));
        assertThrows(IllegalArgumentException.class, () -> builder.addRowIssue(0, -1, 5, "some error"));
        assertThrows(IllegalArgumentException.class, () -> builder.addRowIssue(0, -1, -5, "some error"));

        assertDoesNotThrow(() -> builder.addRowIssue(0, 0, 0, null));
    }

    /** Table with multiple columns, last column has an error. */
    @SuppressWarnings("static-method")
    @Test
    void testPreformattingInLastColumn() {
        var table = createTable(10, 5);
        var messageBuilder = Message.builder().withSummary("ignored") //
            .addRowIssue(0, 4, 5, "some error");
        var message = messageBuilder.build().orElseThrow();
        assertThat(message.getIssue()).isPresent();
        var rowIssue = (RowIssue)message.getIssue().orElseThrow();
        var defaultIssue = rowIssue.toDefaultIssue(new PortObject[] {table});
        assertThat(defaultIssue).extracting(d -> d.toPreformatted()).isEqualTo(
                " RowID  |   ..   |       D        |   E   \n"
              + "--------+--------+----------------+--------\n"
              + " Row3   | ..     | Cell in row... | 12    \n"
              + " Row4   | ..     | Cell in row... | 16    \n"
              + " Row5   | ..     | Cell in row... | 20    \n"
              + "                                    ^^\n"
              + "some error\n");
    }

    /** Table with multiple columns, error somewhere in between. */
    @SuppressWarnings("static-method")
    @Test
    void testPreformattingMiddleColumn() {
        var table = createTable(10, 5);
        var messageBuilder = Message.builder().withSummary("ignored") //
            .addRowIssue(0, 2, 5, "some error");
        var message = messageBuilder.build().orElseThrow();
        assertThat(message.getIssue()).isPresent();
        var rowIssue = (RowIssue)message.getIssue().orElseThrow();
        var defaultIssue = rowIssue.toDefaultIssue(new PortObject[] {table});
        assertThat(defaultIssue).extracting(d -> d.toPreformatted()).isEqualTo(
                " RowID  |   ..   |   B    |   C    |       D       \n"
              + "--------+--------+--------+--------+----------------\n"
              + " Row3   | ..     | 3      | 1.5    | Cell in row...\n"
              + " Row4   | ..     | 4      | 2.0    | Cell in row...\n"
              + " Row5   | ..     | 5      | 2.5    | Cell in row...\n"
              + "                            ^^^\n"
              + "some error\n");
    }

    /** Table with a single string column, of which the highlighted cell contains an empty string. */
    @SuppressWarnings("static-method")
    @Test
    void testEmptyString() {
        final var exec = executionContextExtension.getExecutionContext();
        final var dataContainer = exec.createDataContainer(new DataTableSpecCreator() //
                .addColumns(new DataColumnSpecCreator("Empty", StringCell.TYPE).createSpec()) //
                .createSpec());
        dataContainer.addRowToTable(new DefaultRow("Row0", new StringCell("Longer cell content")));
        dataContainer.addRowToTable(new DefaultRow("Row1", new StringCell("")));
        dataContainer.close();

        final var issue = Message.builder().withSummary("ignored") //
            .addRowIssue(0, 0, 1, "some error") //
            .build().orElseThrow().getIssue();
        final var rowIssue = (RowIssue)issue.orElseThrow();
        final var defaultIssue = rowIssue.toDefaultIssue(new PortObject[] { dataContainer.getTable() });
        assertThat(defaultIssue).extracting(d -> d.toPreformatted()).isEqualTo(
                  " RowID  |     Empty     \n"
                + "--------+----------------\n"
                + " Row0   | Longer cell...\n"
                + " Row1   |               \n"
                + "          ^\n"
                + "some error\n");
    }

    /** Table with multiple columns, many rows (details will be skipped until default backend is changed). */
    @SuppressWarnings("static-method")
    @Test
    void testPreformattingMiddleColumnLargeTable() {
        var table = createTable(2000, 5);
        var messageBuilder = Message.builder().withSummary("ignored") //
            .addRowIssue(0, 2, 1500, "some error");
        var message = messageBuilder.build().orElseThrow();
        assertThat(message.getIssue()).isPresent();
        var rowIssue = (RowIssue)message.getIssue().orElseThrow();
        var defaultIssue = rowIssue.toDefaultIssue(new PortObject[] {table});
        assertThat(defaultIssue).extracting(d -> d.toPreformatted()).isEqualTo("some error");
    }

    /** Checking {@link Message#fromRowIssue(String, int, long, int, String)} */
    @SuppressWarnings("static-method")
    @Test
    void testMessageFactoryMethod() {
        var table = createTable(10, 5);
        assertThrowsExactly(IllegalArgumentException.class, () -> Message.fromRowIssue(null, 0, 0, 0, "non-null"));
        assertDoesNotThrow(() -> Message.fromRowIssue("non-null", 0, 0, 0, null));
        var message = Message.fromRowIssue("Some Summary", 1, 4, 1, "unknown message");
        message = message.renderIssueDetails(new PortObject[] {null, table});
        assertThat(message).extracting(m -> m.getIssue()) //
            .asInstanceOf(OPTIONAL).get() //
            .asInstanceOf(type(DefaultIssue.class)) //
            .extracting(DefaultIssue::toPreformatted) //
            .isEqualTo(
                " RowID  | ABCDEFGHIJK... |   B    |   C   \n"
              + "--------+----------------+--------+--------\n"
              + " Row2   | Cell in row... | 2      | 1.0   \n"
              + " Row3   | Cell in row... | 3      | 1.5   \n"
              + " Row4   | Cell in row... | 4      | 2.0   \n"
              + "                           ^\n"
              + "unknown message\n");
    }

}
