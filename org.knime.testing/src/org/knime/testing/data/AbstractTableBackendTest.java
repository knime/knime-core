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
 *   Mar 9, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.testing.data;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.data.TableBackend;
import org.knime.core.data.TableBackend.AppendConfig;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InternalTableAPI;
import org.knime.testing.core.ExecutionContextExtension;

/**
 * Extend this class to test the table API of a {@link TableBackend} that is all the table related operations (append,
 * concatenate, ColumnRearranger and so on).
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractTableBackendTest {

    /**
     * You can set up the {@link ExecutionContext} via the {@link ExecutionContextExtension}.
     *
     * @return an {@link ExecutionContext} that uses the {@link TableBackend} that is being tested.
     */
    protected abstract ExecutionContext getExecutionContext();

    private ColumnRearrangerAPITester m_rearrangerTester;

    private AppendAPITester m_appendTester;

    private SpecReplacerAPITester m_specReplacerTester;

    private ConcatenationAPITester m_concatenationTester;

    private ContainerAPITester m_containerTester;


    @BeforeEach
    void before() {
        m_rearrangerTester = new ColumnRearrangerAPITester(getExecutionContext());
        m_appendTester = new AppendAPITester(getExecutionContext());
        m_specReplacerTester = new SpecReplacerAPITester(getExecutionContext());
        m_concatenationTester = new ConcatenationAPITester(getExecutionContext());
        m_containerTester = new ContainerAPITester(getExecutionContext());
    }

    //////////////////////////////// ColumnRearranger ////////////////////////////////

    @Test
    void testReorderingOnlyExisting() throws Exception {
        m_rearrangerTester.testReorderingOnlyExisting();
    }

    @Test
    void testFilteringOnlyExisting() throws Exception {
        m_rearrangerTester.testFilteringOnlyExisting();
    }

    @Test
    void testAppendSingleFactorySingleColumn() throws Exception {
        m_rearrangerTester.testAppendSingleFactorySingleColumn();
    }

    @Test
    void testAppendSingleFactoryMultipleColumns() throws Exception {
        m_rearrangerTester.testAppendSingleFactoryMultipleColumns();
    }

    @Test
    void testAppendMultipleFactoriesOneColumnEach() throws Exception {
        m_rearrangerTester.testAppendMultipleFactoriesOneColumnEach();
    }

    @Test
    void testReplaceSingleFactorySingleColumn() throws Exception {
        m_rearrangerTester.testReplaceSingleFactorySingleColumn();
    }

    @Test
    void testReplaceSingleFactoryMultipleColumns() throws Exception {
        m_rearrangerTester.testReplaceSingleFactoryMultipleColumns();
    }

    @Test
    void testReplaceMultipleFactoriesOneColumnEach() throws Exception {
        m_rearrangerTester.testReplaceMultipleFactoriesOneColumnEach();
    }

    @Test
    void testReplaceMultipleCellFactoriesMultipleColumnsEach() throws Exception {
        m_rearrangerTester.testReplaceMultipleCellFactoriesMultipleColumnsEach();
    }

    @Test
    void testInputRowMethods() throws Exception {
        m_rearrangerTester.testInputRowMethods();
    }

    @Test
    void testSingleTypeConverter() throws Exception {
        m_rearrangerTester.testSingleTypeConverter();
    }

    @Test
    void testMultipleTypeConverters() throws Exception {
        m_rearrangerTester.testMultipleTypeConverters();
    }

    @Test
    void testMultipleConvertersPerColumn() throws Exception {
        m_rearrangerTester.testMultipleConvertersPerColumn();
    }

    @Test
    void testAppendConvertedColumn() throws Exception {
        m_rearrangerTester.testAppendConvertedColumn();
    }

    // replacing a converted column is not possible because the CellFactory and the DataCellTypeConverter replace each
    // other. The same happens if multiple cell factories replace the same column.

    @Test
    void testFilterThenReorder() throws Exception {
        m_rearrangerTester.testFilterThenReorder();
    }

    @Test
    void testReorderThenFilter() throws Exception {
        m_rearrangerTester.testReorderThenFilter();
    }

    @Test
    void testReorderThenFilterThenAppend() throws Exception {
        m_rearrangerTester.testReorderThenFilterThenAppend();
    }

    @Test
    void testAppendThenFilterOutColumnUsedByAppend() throws Exception {
        m_rearrangerTester.testAppendThenFilterOutColumnUsedByAppend();
    }

    ////////////////////// Append //////////////////////////////

    /**
     * Tests {@link InternalTableAPI#append(ExecutionContext, AppendConfig, BufferedDataTable, BufferedDataTable)}.
     *
     * @throws Exception if the tests fail
     */
    @Test
    void testAppendRightIDs() throws Exception {
        m_appendTester.testAppendRightIDs();
    }

    @Test
    void testAppendLeftIDs() throws Exception {
        m_appendTester.testAppendLeftIDs();
    }

    @Test
    void testAppendMatchingIDsWithMatchingIDs() throws Exception {
        m_appendTester.testAppendMatchingIDsWithMatchingIDs();
    }

    @Test
    void testAppendMatchingIDsWithNonMatchingIDs() throws Exception {
        m_appendTester.testAppendMatchingIDsWithNonMatchingIDs();
    }

    ////////////////////// Spec Replacement //////////////////////////

    @Test
    void testReplaceNames() throws Exception {
        m_specReplacerTester.testReplaceNames();
    }

    @Test
    void testUpcast() throws Exception {
        m_specReplacerTester.testUpcast();
    }

    @Test
    void testDowncastAfterUpcastToDataValue() throws Exception {
        m_specReplacerTester.testDowncastAfterUpcastToDataValue();
    }

    //////////////////////// Concatenation //////////////////////////

    @Test
    void testConcatenateWithDifferingSpecs() throws Exception {
        m_concatenationTester.testConcatenateWithDifferingSpecs();
    }

    @Test
    void testFailOnDuplicateRowKeys() throws Exception {
        m_concatenationTester.testFailOnDuplicateRowIDs();
    }

    @Test
    void testDeduplicateRowIDsWithSuffix() throws Exception {
        m_concatenationTester.testDeduplicateRowIDsWithSuffix();
    }

    @Test
    void testMissingColumnInOneTable() throws Exception {
        m_concatenationTester.testMissingColumnInFirstTable();
        m_concatenationTester.testMissingColumnInSecondTable();
    }

    ////////////////////// Container Tests  //////////////////////////

    @Test
    void testDuplicateIDRowContainer() {
        m_containerTester.testDuplicateCheckingOnRowContainerAdd();
    }

    @Test
    void testDuplicateIDDataContainer() {
        m_containerTester.testDuplicateCheckingOnDataContainerAdd();
    }

    @Test
    void testDomainUpdateRowContainerAPI() throws IOException {
        m_containerTester.testDomainUpdateRowContainerAPI();
    }

    @Test
    void testDomainUpdateDataContainerAPI() throws IOException {
        m_containerTester.testDomainUpdateDataContainerAPI();
    }
}
