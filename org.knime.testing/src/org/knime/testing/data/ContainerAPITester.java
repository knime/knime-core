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
 *   June 02, 2024 (Bernd Wiswedel, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.testing.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.testing.data.TableBackendTestUtils.doubleFactory;
import static org.knime.testing.data.TableBackendTestUtils.intFactory;
import static org.knime.testing.data.TableBackendTestUtils.stringFactory;

import java.io.IOException;
import java.util.function.LongFunction;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.DuplicateKeyException;
import org.knime.testing.data.TableBackendTestUtils.Column;

/**
 * Tests the container API of TableBackends.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
final class ContainerAPITester extends AbstractTableBackendAPITester {

    ContainerAPITester(final ExecutionContext exec) {
        super(exec);
    }

    void testDuplicateCheckingOnRowContainerAdd() {
        testDuplicateChecking(TableBackendTestUtils::createTableViaRowContainerAPI);
    }

    void testDuplicateCheckingOnDataContainerAdd() {
        testDuplicateChecking(TableBackendTestUtils::createTableViaDataContainerAPI);
    }

    private void testDuplicateChecking(final TableCreateMethod createMethod) {
        var fooInt = new Column("fooI", intFactory(1, 2, 3));
        var fooDouble = new Column("fooD", doubleFactory(1.5, 2.5, 3.5));
        var tableSpec = TableBackendTestUtils.createSpec(fooInt, fooDouble);
        assertDoesNotThrow(() -> createMethod.create(getExec(), tableSpec, DataContainerSettings.getDefault(),
            "Row%d"::formatted, fooInt, fooDouble));

        assertThrows(DuplicateKeyException.class, () -> createMethod.create(getExec(), tableSpec,
            DataContainerSettings.getDefault(), l -> "Row%d".formatted(l % 2), fooInt, fooDouble));

        assertDoesNotThrow(() -> createMethod.create(getExec(), tableSpec,
            DataContainerSettings.builder().withCheckDuplicateRowKeys(false).build(), l -> "Row%d".formatted(l % 2),
            fooInt, fooDouble));
    }

    void testDomainUpdateRowContainerAPI() throws IOException {
        testDomainUpdate(TableBackendTestUtils::createTableViaRowContainerAPI);
    }

    void testDomainUpdateDataContainerAPI() throws IOException {
        testDomainUpdate(TableBackendTestUtils::createTableViaDataContainerAPI);
    }

    private void testDomainUpdate(final TableCreateMethod createMethod) throws IOException {
        var fooInt = new Column("fooI", intFactory(1, 2, 3));
        var fooDouble = new Column("fooD", doubleFactory(1.5, 2.5, 3.5));
        var fooString = new Column("fooS", stringFactory("one", "two", "three"));
        var allColumns = new Column[] {fooInt, fooDouble, fooString};

        final var fooIntSpecCreator = new DataColumnSpecCreator(fooInt.getSpec());
        final var fooIntDomain = new DataColumnDomainCreator(new IntCell(0), new IntCell(5)).createDomain();
        fooIntSpecCreator.setDomain(fooIntDomain);
        final var fooIntSpec = fooIntSpecCreator.createSpec();

        final var fooDoubleSpecCreator = new DataColumnSpecCreator(fooDouble.getSpec());
        fooDoubleSpecCreator.setDomain(null);
        final var fooDoubleSpec = fooDoubleSpecCreator.createSpec();
        final var fooDoubleDomain = fooDoubleSpec.getDomain();

        final var fooStringSpecCreator = new DataColumnSpecCreator(fooString.getSpec());
        final var differentPossibleValues =
            new DataCell[]{new StringCell("one"), new StringCell("two"), new StringCell("four")};
        final var fooStringDomain = new DataColumnDomainCreator(differentPossibleValues).createDomain();
        fooStringSpecCreator.setDomain(fooStringDomain);
        final var fooStringSpec = fooStringSpecCreator.createSpec();

        final var tableSpec = new DataTableSpec(fooIntSpec, fooDoubleSpec, fooStringSpec);

        final var correctedTable = createMethod.create(getExec(), tableSpec, DataContainerSettings.getDefault(),
            "Row%d"::formatted, allColumns);
        final var correctTableSpec = correctedTable.getSpec();

        // domain was wider, should be unmodified after table is created (not updated)
        assertEquals(fooIntDomain, correctTableSpec.getColumnSpec("fooI").getDomain());
        assertNotEquals(fooDoubleDomain, correctTableSpec.getColumnSpec("fooD").getDomain());

        // domain was missing, during construction, now present after table creation
        assertEquals(fooDouble.getSpec().getDomain(), correctTableSpec.getColumnSpec("fooD").getDomain());

        // domain had too few data elements before construction, has corrected after construction
        assertArrayEquals(new String[]{"one", "two", "four", "three"}, correctTableSpec.getColumnSpec("fooS")
            .getDomain().getValues().stream().map(DataCell::toString).toArray(String[]::new));



        final var freshDomainTable = createMethod.create(getExec(), tableSpec,
            DataContainerSettings.builder().withInitializedDomain(false).build(), "Row%d"::formatted, allColumns);
        final var freshDomainTableSpec = freshDomainTable.getSpec();

        // domain is minimal (again)
        assertEquals(fooInt.getSpec().getDomain(), freshDomainTableSpec.getColumnSpec("fooI").getDomain());

        // domain was missing, during construction, now present after table creation
        assertEquals(fooDouble.getSpec().getDomain(), freshDomainTableSpec.getColumnSpec("fooD").getDomain());

        assertEquals(fooString.getSpec().getDomain(), freshDomainTableSpec.getColumnSpec("fooS").getDomain());


        final var unchangedDomainTable = createMethod.create(getExec(), tableSpec,
            DataContainerSettings.builder().withDomainUpdate(false).build(), "Row%d"::formatted, allColumns);
        final var unchangedDomainTableSpec = unchangedDomainTable.getSpec();

        // domain is minimal (again)
        assertEquals(fooIntSpec.getDomain(), unchangedDomainTableSpec.getColumnSpec("fooI").getDomain());

        // domain was missing, during construction, now present after table creation
        assertEquals(fooDoubleSpec.getDomain(), unchangedDomainTableSpec.getColumnSpec("fooD").getDomain());

        // this domain is technically wrong (more values in data than in spec) - but that's a node bug, not framework
        assertEquals(fooStringSpec.getDomain(), unchangedDomainTableSpec.getColumnSpec("fooS").getDomain());
    }

    @FunctionalInterface
    interface TableCreateMethod {

        BufferedDataTable create(final ExecutionContext exec, final DataTableSpec tableSpec,
            final DataContainerSettings containerSettings, final LongFunction<String> rowIDFactory,
            final Column... columns) throws IOException;

    }

}
