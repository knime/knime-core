/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   12.08.2013 (thor): created
 */
package org.knime.testing.internal.nodes.differ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceChecker.Result;
import org.knime.testing.core.DifferenceCheckerFactory;
import org.knime.testing.internal.diffcheckers.EqualityChecker;
import org.knime.testing.internal.diffcheckers.IgnoreChecker;

/**
 * Model for the difference checker node.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class DifferenceCheckerNodeModel extends NodeModel {
    private final DifferenceCheckerSettings m_settings = new DifferenceCheckerSettings();

    private final Map<DataColumnSpec, DifferenceChecker<? extends DataValue>> m_checkers =
            new HashMap<DataColumnSpec, DifferenceChecker<? extends DataValue>>();

    DifferenceCheckerNodeModel() {
        super(2, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[1] == null) {
            throw new InvalidSettingsException("No reference table available");
        }

        if (m_settings.configuredColumns().size() == 0) {
            // auto-configure
            for (DataColumnSpec dcs : inSpecs[1]) {
                DifferenceCheckerFactory<? extends DataValue> fac = new EqualityChecker.Factory();
                m_settings.checkerFactory(dcs.getName(), fac);
                fac.newChecker().saveSettings(m_settings.internalsForColumn(dcs.getName()));
            }
        } else {
            for (DataColumnSpec dcs : inSpecs[1]) {
                DifferenceCheckerFactory<? extends DataValue> fac = m_settings.checkerFactory(dcs.getName());
                if (fac == null) {
                    throw new InvalidSettingsException("No checker configured for column '" + dcs.getName() + "'");
                }

                DataType type =
                        dcs.getType().isCollectionType() ? dcs.getType().getCollectionElementType() : dcs.getType();
                if (!type.isCompatible(fac.getType())) {
                    throw new InvalidSettingsException("Difference checker '" + fac.getDescription()
                            + "' is not compatible with data type " + dcs.getType());
                }
            }
        }

        return new DataTableSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        BufferedDataTable testTable = inData[0];
        BufferedDataTable refTable = inData[1];
        for (DataColumnSpec colSpec : refTable.getDataTableSpec()) {
            m_checkers.put(colSpec, m_settings.createCheckerForColumn(colSpec.getName()));
        }

        exec.setMessage("Comparing table specs");
        checkTableSpecs(testTable.getDataTableSpec(), refTable.getDataTableSpec());

        if (testTable.getRowCount() != refTable.getRowCount()) {
            throw new IllegalStateException("Wrong number of rows: expected " + refTable.getRowCount() + ", got "
                    + testTable.getRowCount());
        }


        exec.setMessage("Comparing table contents");
        final double max = refTable.getRowCount();
        int i = 0;
        RowIterator testIt = testTable.iterator();
        for (DataRow refRow : refTable) {
            exec.setProgress(i / max, "Comparing row " + i);
            exec.checkCanceled();

            DataRow testRow = testIt.next();
            compareRow(refTable.getDataTableSpec(), testRow, refRow, i);
            i++;
        }

        return new BufferedDataTable[0];
    }

    private void compareRow(final DataTableSpec spec, final DataRow testRow, final DataRow refRow, final int rowIndex) {
        if (!m_settings.ignoreRowIds() && !refRow.getKey().equals(testRow.getKey())) {
            throw new IllegalStateException("Wrong row key in row " + rowIndex + ": expected '" + refRow.getKey()
                    + "', got '" + testRow.getKey() + "'");
        }

        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            DifferenceChecker<DataValue> checker = (DifferenceChecker<DataValue>)m_checkers.get(colSpec);
            if (checker instanceof IgnoreChecker) {
                continue;
            }

            DataCell testCell = testRow.getCell(i);
            DataCell refCell = refRow.getCell(i);

            if (refCell.isMissing() && !testCell.isMissing()) {
                throw new IllegalStateException("Expected missing cell in row '" + refRow.getKey() + "' and column '"
                        + colSpec.getName() + "' but got '" + testCell + "'");
            } else if (!refCell.isMissing() && testCell.isMissing()) {
                throw new IllegalStateException("Unexpected missing cell in row '" + refRow.getKey() + "' and column '"
                        + colSpec.getName() + "'");
            } else if (!refCell.isMissing() && !testCell.isMissing()) {
                if (colSpec.getType().isCollectionType() && !(checker instanceof EqualityChecker)) {
                    compareCollection(colSpec, checker, testCell, refCell);
                } else {
                    Result res = checker.check(testCell, refCell);
                    if (!res.ok()) {
                        throw new IllegalStateException("Wrong value in row '" + refRow.getKey() + "' and column '"
                                + colSpec.getName() + "': " + res.getMessage() + " (using checker '"
                                + checker.getDescription() + "')");
                    }
                }
            } else {
                // both cells are missing => OK
            }
        }
    }

    /**
     * @param colSpec
     * @param checker
     * @param testCell
     * @param refCell
     */
    private void compareCollection(final DataColumnSpec colSpec, final DifferenceChecker<DataValue> checker,
                                   final DataCell testCell, final DataCell refCell) {
        CollectionDataValue testCollection = (CollectionDataValue)testCell;
        CollectionDataValue refCollection = (CollectionDataValue)refCell;

        if (refCollection.size() != testCollection.size()) {
            throw new IllegalStateException("Wrong number of elements in collection of column '" + colSpec.getName()
                    + "': expected " + refCollection.size() + ", got " + testCollection.size());
        }

        int index = 0;
        Iterator<DataCell> testCollIt = testCollection.iterator();
        for (DataCell refCollCell : refCollection) {
            DataCell testCollCell = testCollIt.next();

            Result res = checker.check(testCollCell, refCollCell);
            if (!res.ok()) {
                throw new IllegalStateException("Wrong value at position " + index + " in collection of column '"
                        + colSpec.getName() + "': " + res.getMessage() + " (using checker '" + checker.getDescription()
                        + "')");
            }
            index++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        DifferenceCheckerSettings s = new DifferenceCheckerSettings();
        s.loadSettings(settings);

        for (String colName : s.configuredColumns()) {
            DifferenceCheckerFactory<? extends DataValue> fac = s.checkerFactory(colName);
            if (fac == null) {
                throw new InvalidSettingsException("Unknown difference checker factory for column '" + colName + "': "
                        + s.checkerFactoryClassName(colName));
            }
            DifferenceChecker<? extends DataValue> checker = fac.newChecker();
            checker.validateSettings(s.internalsForColumn(colName));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_checkers.clear();
    }

    private void checkTableSpecs(final DataTableSpec testTable, final DataTableSpec referenceTable)
        throws InvalidSettingsException {
        Set<String> columnNames = new HashSet<String>();

        for (int i = 0; i < referenceTable.getNumColumns(); i++) {
            DataColumnSpec colSpec = referenceTable.getColumnSpec(i);
            DifferenceChecker<DataValue> checker = (DifferenceChecker<DataValue>)m_checkers.get(colSpec);

            DataColumnSpec refColSpec = referenceTable.getColumnSpec(i);
            if (testTable.getNumColumns() <= i) {
                throw new IllegalStateException("Column '" + refColSpec + "' is missing in test table");
            }
            DataColumnSpec testColSpec = testTable.getColumnSpec(i);
            if (!refColSpec.getName().equals(testColSpec.getName())) {
                throw new IllegalStateException("Expected column named '" + refColSpec.getName() + "' at position " + i
                        + " in test table");
            }
            if (!refColSpec.getType().equals(testColSpec.getType())) {
                throw new IllegalStateException("Expected type '" + refColSpec.getType() + "' for column "
                        + refColSpec.getName() + " in test table");
            }

            checkDomain(testColSpec, refColSpec);

            if (!ConvenienceMethods.areEqual(refColSpec.getColorHandler(), testColSpec.getColorHandler())) {
                throw new IllegalStateException("Unexpected color handler in column '" + refColSpec.getName() + "'");
            }
            if (!ConvenienceMethods.areEqual(refColSpec.getShapeHandler(), testColSpec.getShapeHandler())) {
                throw new IllegalStateException("Unexpected shape handler in column '" + refColSpec.getName() + "'");
            }
            if (!ConvenienceMethods.areEqual(refColSpec.getSizeHandler(), testColSpec.getSizeHandler())) {
                throw new IllegalStateException("Unexpected size handler in column '" + refColSpec.getName() + "'");
            }

            if (!refColSpec.getElementNames().equals(testColSpec.getElementNames())) {
                throw new IllegalStateException("Wrong elements names in column '" + refColSpec.getName()
                        + "': expected '" + refColSpec.getElementNames() + "', got '" + testColSpec.getElementNames()
                        + "'");
            }
            if (!checker.ignoreColumnProperties() && !refColSpec.getProperties().equals(testColSpec.getProperties())) {
                throw new IllegalStateException("Wrong properties in column '" + refColSpec.getName() + "': expected '"
                        + refColSpec.getProperties() + "', got '" + testColSpec.getProperties() + "'");
            }

            columnNames.add(refColSpec.getName());
        }

        for (DataColumnSpec dcs : testTable) {
            if (!columnNames.contains(dcs.getName())) {
                throw new IllegalStateException("Unexpected column in test table: " + dcs.getName());
            }
        }
    }

    private void checkDomain(final DataColumnSpec testColSpec, final DataColumnSpec refColSpec)
            throws InvalidSettingsException {
        DifferenceChecker<DataValue> checker =
                (DifferenceChecker<DataValue>)m_settings.createCheckerForColumn(refColSpec.getName());
        if (!checker.ignoreDomain()) {
            DataColumnDomain testDom = testColSpec.getDomain();
            DataColumnDomain refDom = refColSpec.getDomain();
            if (!refDom.equals(testDom)) {
                checkPossibleValues(testColSpec, refColSpec, checker);
                checkBounds(testColSpec, refColSpec, checker);
            }
        }
    }

    private void checkBounds(final DataColumnSpec testColSpec, final DataColumnSpec refColSpec,
                             final DifferenceChecker<DataValue> checker) {
        DataColumnDomain testDom = testColSpec.getDomain();
        DataColumnDomain refDom = refColSpec.getDomain();

        if (refDom.hasLowerBound() && !testDom.hasLowerBound()) {
            throw new IllegalStateException("Missing lower bound in column '" + refColSpec.getName() + "'");
        } else if (!refDom.hasLowerBound() && testDom.hasLowerBound()) {
            throw new IllegalStateException("New lower bound in column '" + refColSpec.getName() + "'");
        } else if (refDom.hasLowerBound()) {
            DataCell refBound = refDom.getLowerBound();
            DataCell testBound = testDom.getLowerBound();

            Result res = checker.check(refBound, testBound);

            if (!res.ok()) {
                throw new IllegalStateException("Wrong lower bound in column '" + refColSpec.getName() + "': "
                        + res.getMessage());
            }
        }

        if (refDom.hasUpperBound() && !testDom.hasUpperBound()) {
            throw new IllegalStateException("Missing upper bound in column '" + refColSpec.getName() + "'");
        } else if (!refDom.hasUpperBound() && testDom.hasUpperBound()) {
            throw new IllegalStateException("New upper bound in column '" + refColSpec.getName() + "'");
        } else if (refDom.hasUpperBound()) {
            DataCell refBound = refDom.getUpperBound();
            DataCell testBound = testDom.getUpperBound();

            Result res = checker.check(refBound, testBound);

            if (!res.ok()) {
                throw new IllegalStateException("Wrong upper bound in column '" + refColSpec.getName() + "': "
                        + res.getMessage());
            }
        }
    }

    private void checkPossibleValues(final DataColumnSpec testColSpec, final DataColumnSpec refColSpec,
                                     final DifferenceChecker<DataValue> checker) {
        DataColumnDomain testDom = testColSpec.getDomain();
        DataColumnDomain refDom = refColSpec.getDomain();

        if (refDom.hasValues() && !testDom.hasValues()) {
            getLogger().debug("Expected possible values: " + refDom.getValues());
            throw new IllegalStateException("Possible values in domain of column '" + testColSpec.getName()
                    + "' expected");
        } else if (!refDom.hasValues() && testDom.hasValues()) {
            getLogger().debug("Actual possible values: " + testDom.getValues());
            throw new IllegalStateException("No possible values in domain of column '" + testColSpec.getName()
                    + "' expected");
        } else if (refDom.hasValues() && testDom.hasValues()) {
            if (refDom.getValues().size() != testDom.getValues().size()) {
                getLogger().debug(
                    "Expected possible values: " + refDom.getValues() + "; actual possible values: "
                        + testDom.getValues());
                throw new IllegalStateException("Unequal number of possible values in column '" + refColSpec.getName()
                        + "': expected " + refDom.getValues().size() + ", got " + testDom.getValues().size());
            }

            List<DataCell> refValues = new ArrayList<DataCell>(refDom.getValues());
            Collections.sort(refValues, refColSpec.getType().getComparator());

            List<DataCell> testValues = new ArrayList<DataCell>(testDom.getValues());
            Collections.sort(testValues, testColSpec.getType().getComparator());

            for (int i = 0; i < testValues.size(); i++) {
                DataCell refCell = refValues.get(i);
                DataCell testCell = testValues.get(i);

                Result res = checker.check(refCell, testCell);
                if (!res.ok()) {
                    throw new IllegalStateException("Wrong possible value in column '" + refColSpec.getName() + "': "
                            + res.getMessage());
                }
            }
        } else {
            // no possible values in both domains => OK
        }
    }
}
