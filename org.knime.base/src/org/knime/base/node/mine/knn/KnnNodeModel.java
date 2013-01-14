/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.knn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.util.kdtree.KDTree;
import org.knime.base.util.kdtree.KDTreeBuilder;
import org.knime.base.util.kdtree.NearestNeighbour;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.MutableDouble;
import org.knime.core.util.MutableInteger;

/**
 * This is the model for the k Nearest Neighbor node. In contrast to most
 * learner/predictor combinations this is "all in one" since the model here
 * really stores all of the training data.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class KnnNodeModel extends NodeModel {
    private KnnSettings m_settings = new KnnSettings();

    private final Map<DataCell, MutableInteger> m_classDistribution =
            new HashMap<DataCell, MutableInteger>();

    /**
     * Creates a new model for the kNN node.
     */
    public KnnNodeModel() {
        super(2, 1);
    }

    /**
     * Checks if the two input tables are correct and fills the last two
     * arguments with sensible values.
     *
     * @param inSpecs the input tables' specs
     * @param featureColumns a list that gets filled with the feature columns'
     *            indices; all columns with {@link DoubleValue}s are used as
     *            features
     * @param firstToSecond a map that afterwards maps the indices of the
     *            feature columns in the first table to the corresponding
     *            columns from the second table
     * @throws InvalidSettingsException if the two tables are not compatible
     */
    private void checkInputTables(final DataTableSpec[] inSpecs,
            final List<Integer> featureColumns,
            final Map<Integer, Integer> firstToSecond)
            throws InvalidSettingsException {
        if (!inSpecs[0].containsCompatibleType(DoubleValue.class)) {
            throw new InvalidSettingsException(
                    "First input table does not contain a numeric column.");
        }
        if (!inSpecs[0].containsCompatibleType(StringValue.class)) {
            throw new InvalidSettingsException(
                    "First input table does not contain a class column of type "
                            + "string.");
        }

        int i = 0;
        for (DataColumnSpec cs : inSpecs[0]) {
            if (cs.getType().isCompatible(DoubleValue.class)) {
                featureColumns.add(i);
            } else if (!cs.getName().equals(m_settings.classColumn())) {
                setWarningMessage("Input table contains more than one "
                        + "non-numeric column; they will be ignored.");
            }
            i++;
        }

        for (int k : featureColumns) {
            DataColumnSpec cs = inSpecs[0].getColumnSpec(k);
            int secondColIndex = inSpecs[1].findColumnIndex(cs.getName());
            if (secondColIndex == -1) {
                throw new InvalidSettingsException(
                        "Second input table does not"
                                + " contain a column named '" + cs.getName()
                                + "'");
            }

            if (inSpecs[1].getColumnSpec(secondColIndex).equalStructure(cs)) {
                firstToSecond.put(k, secondColIndex);
            } else {
                throw new InvalidSettingsException("Column '" + cs.getName()
                        + "' from the second table is not compatible with the "
                        + "corresponding column from the first table.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        int classColIndex =
                inSpecs[0].findColumnIndex(m_settings.classColumn());
        if (classColIndex == -1) {
            DataColumnSpec colSpec = null;
            for (DataColumnSpec cs : inSpecs[0]) {
                if (cs.getType().isCompatible(NominalValue.class)) {
                    if (colSpec != null) {
                        throw new InvalidSettingsException(
                                "Please choose a valid class column");
                    }
                    colSpec = cs;
                }
            }

            if (colSpec == null) {
                throw new InvalidSettingsException(
                        "Please choose a valid class column.");
            }
            m_settings.classColumn(colSpec.getName());
            setWarningMessage("Auto-selected column '" + colSpec.getName()
                    + "' as class column.");
            classColIndex =
                    inSpecs[0].findColumnIndex(m_settings.classColumn());
        }

        List<Integer> featureColumns = new ArrayList<Integer>();
        Map<Integer, Integer> secondIndex = new HashMap<Integer, Integer>();
        checkInputTables(inSpecs, featureColumns, secondIndex);

        if (m_settings.outputClassProbabilities()
                && (inSpecs[0].getColumnSpec(m_settings.classColumn())
                        .getDomain().getValues() == null)) {
            return new DataTableSpec[1];
        }

        DataColumnSpec classColSpec = inSpecs[0].getColumnSpec(classColIndex);
        if (m_settings.outputClassProbabilities()
                && (classColSpec.getDomain().getValues() == null)) {
            return new DataTableSpec[1];
        }

        ColumnRearranger crea =
                createRearranger(inSpecs[1], classColSpec, null, null, null, -1);

        return new DataTableSpec[]{crea.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        int classColIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_settings.classColumn());
        if (classColIndex == -1) {
            throw new InvalidSettingsException("Invalid class column chosen.");
        }

        List<Integer> featureColumns = new ArrayList<Integer>();
        Map<Integer, Integer> firstToSecond = new HashMap<Integer, Integer>();
        checkInputTables(new DataTableSpec[]{inData[0].getDataTableSpec(),
                inData[1].getDataTableSpec()}, featureColumns, firstToSecond);

        KDTreeBuilder<DataCell> treeBuilder =
                new KDTreeBuilder<DataCell>(featureColumns.size());
        int count = 0;
        for (DataRow currentRow : inData[0]) {
            exec.checkCanceled();
            exec.setProgress(0.1 * count * inData[0].getRowCount(),
                    "Reading row " + currentRow.getKey());

            double[] features = createFeatureVector(currentRow, featureColumns);
            if (features == null) {
                setWarningMessage("Input table contains missing values, the "
                        + "affected rows are ignored.");
            } else {
                DataCell thisClassCell = currentRow.getCell(classColIndex);
                // and finally add data
                treeBuilder.addPattern(features, thisClassCell);

                // compute the majority class for breaking possible ties later
                MutableInteger t = m_classDistribution.get(thisClassCell);
                if (t == null) {
                    m_classDistribution.put(thisClassCell,
                            new MutableInteger(1));
                } else {
                    t.inc();
                }
            }
        }

        // and now use it to classify the test data...
        DataTableSpec inSpec = inData[1].getDataTableSpec();
        DataColumnSpec classColumnSpec =
                inData[0].getDataTableSpec().getColumnSpec(classColIndex);

        exec.setMessage("Building kd-tree");
        KDTree<DataCell> tree =
                treeBuilder.buildTree(exec.createSubProgress(0.3));

        if (tree.size() < m_settings.k()) {
            setWarningMessage("There are only " + tree.size()
                    + " patterns in the input table, but " + m_settings.k()
                    + " nearest neighbours were requested for classification."
                    + " The prediction will be the majority class for all"
                    + " input patterns.");
        }

        exec.setMessage("Classifying");
        ColumnRearranger c =
                createRearranger(inSpec, classColumnSpec, featureColumns,
                        firstToSecond, tree, inData[1].getRowCount());
        BufferedDataTable out =
                exec.createColumnRearrangeTable(inData[1], c,
                        exec.createSubProgress(0.6));
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_classDistribution.clear();
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        (new KnnSettings()).loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    private ColumnRearranger createRearranger(final DataTableSpec in,
            final DataColumnSpec classColumnSpec,
            final List<Integer> featureColumns,
            final Map<Integer, Integer> firstToSecond,
            final KDTree<DataCell> tree, final double maxRows) {
        ColumnRearranger c = new ColumnRearranger(in);
        String newName = "Class [kNN]";
        while (in.containsName(newName)) {
            newName += "_dup";
        }

        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
        DataColumnSpecCreator crea = new DataColumnSpecCreator(classColumnSpec);
        crea.setName(newName);
        colSpecs.add(crea.createSpec());

        final DataCell[] possibleValues;
        if (m_settings.outputClassProbabilities()) {
            possibleValues =
                    classColumnSpec.getDomain().getValues()
                            .toArray(new DataCell[0]);
            Arrays.sort(possibleValues, new Comparator<DataCell>() {
                @Override
                public int compare(final DataCell o1, final DataCell o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });

            for (DataCell posVal : possibleValues) {
                newName = posVal.toString();
                while (in.containsName(newName)) {
                    newName += "_dup";
                }
                crea = new DataColumnSpecCreator(newName, DoubleCell.TYPE);
                colSpecs.add(crea.createSpec());
            }
        } else {
            possibleValues = new DataCell[0];
        }

        final DataColumnSpec[] colSpecArray =
                colSpecs.toArray(new DataColumnSpec[colSpecs.size()]);
        c.append(new CellFactory() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor exec) {
                exec.setProgress(curRowNr / maxRows, "Classifying row "
                        + lastKey);
            }

            @Override
            public DataCell[] getCells(final DataRow row) {
                List<DataCell> output =
                        classify(row, tree, featureColumns, firstToSecond,
                                possibleValues);
                return output.toArray(new DataCell[output.size()]);
            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                return colSpecArray;
            }
        });
        return c;
    }

    // returns a list where the first value if the winner class, and the
    // following values are the class probabilities (if enabled)
    private List<DataCell> classify(final DataRow row,
            final KDTree<DataCell> tree, final List<Integer> featureColumns,
            final Map<Integer, Integer> firstToSecond,
            final DataCell[] allClassValues) {
        double[] features =
                createQueryVector(row, featureColumns, firstToSecond);
        List<DataCell> output = new ArrayList<DataCell>();
        if (features == null) {
            for (int i = 0; i < 1 + allClassValues.length; i++) {
                output.add(DataType.getMissingCell());
            }
            return output;
        }

        HashMap<DataCell, MutableDouble> classWeights =
                new HashMap<DataCell, MutableDouble>();
        List<NearestNeighbour<DataCell>> nearestN =
                tree.getKNearestNeighbours(features,
                        Math.min(m_settings.k(), tree.size()));

        for (NearestNeighbour<DataCell> n : nearestN) {
            MutableDouble count = classWeights.get(n.getData());
            if (count == null) {
                count = new MutableDouble(0);
                classWeights.put(n.getData(), count);
            }
            if (m_settings.weightByDistance()) {
                count.add(1 / n.getDistance());
            } else {
                count.inc();
            }
        }

        double winnerWeight = 0;
        double weightSum = 0;
        DataCell winnerCell = DataType.getMissingCell();
        for (Map.Entry<DataCell, MutableDouble> e : classWeights.entrySet()) {
            double weight = e.getValue().doubleValue();
            if (weight > winnerWeight) {
                winnerWeight = weight;
                winnerCell = e.getKey();
            }
            weightSum += weight;
        }

        // check if there are other classes with the same weight
        for (Map.Entry<DataCell, MutableDouble> e : classWeights.entrySet()) {
            double weight = e.getValue().doubleValue();
            if (weight == winnerWeight) {
                if (m_classDistribution.get(winnerCell).intValue() < m_classDistribution
                        .get(e.getKey()).intValue()) {
                    winnerCell = e.getKey();
                }
            }
        }

        output.add(winnerCell);

        if (m_settings.outputClassProbabilities()) {
            for (DataCell classVal : allClassValues) {
                MutableDouble v = classWeights.get(classVal);
                if (v == null) {
                    output.add(new DoubleCell(0));
                } else {
                    output.add(new DoubleCell(v.doubleValue() / weightSum));
                }
            }
        }

        return output;
    }

    /**
     * Creates a double array with the features of one data row.
     *
     * @param row the row
     * @param featureColumns the indices of the column with the features to use
     * @return a double array with the features' values
     */
    private double[] createFeatureVector(final DataRow row,
            final List<Integer> featureColumns) {
        double[] features = new double[featureColumns.size()];
        int currentIndex = 0;
        for (int i : featureColumns) {
            DataCell thisCell = row.getCell(i);

            if (!thisCell.isMissing()) {
                features[currentIndex] =
                        ((DoubleValue)thisCell).getDoubleValue();
            } else {
                return null;
            }
            currentIndex++;
        }
        return features;
    }

    /**
     * Creates a double array with the features of one data row used for
     * querying the tree.
     *
     * @param row the row
     * @param featureColumns the indices of the column with the features to use
     * @param firstToSecond a map that maps the indices of the feature columns
     *            from the first table to the columns in the second table
     * @return a double array with the features' values
     */
    private double[] createQueryVector(final DataRow row,
            final List<Integer> featureColumns,
            final Map<Integer, Integer> firstToSecond) {
        double[] features = new double[featureColumns.size()];
        int currentIndex = 0;
        for (Integer i : featureColumns) {
            DataCell thisCell = row.getCell(firstToSecond.get(i));

            if (!thisCell.isMissing()) {
                features[currentIndex] =
                        ((DoubleValue)thisCell).getDoubleValue();
            } else {
                return null;
            }
            currentIndex++;
        }
        return features;
    }
}
