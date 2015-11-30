/*
 * ------------------------------------------------------------------------
 *
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
 *   Oct 12, 2015 (Lara): created
 */
package org.knime.base.node.io.database.binning;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.DiscretizeBinDocument.DiscretizeBin;
import org.dmg.pmml.IntervalDocument.Interval;
import org.dmg.pmml.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.base.node.io.database.binning.auto.DBAutoBinnerNodeModel;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretize;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretizeBin;
import org.knime.base.node.preproc.autobinner.pmml.PMMLInterval;
import org.knime.base.node.preproc.autobinner.pmml.PMMLInterval.Closure;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretize;
import org.knime.base.node.preproc.autobinner3.AutoBinner;
import org.knime.base.node.preproc.autobinner3.AutoBinnerLearnSettings;
import org.knime.base.node.preproc.pmml.binner.BinnerColumnFactory.Bin;
import org.knime.base.node.preproc.pmml.binner.NumericBin;
import org.knime.base.node.preproc.pmml.binner.PMMLBinningTranslator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.util.Pair;

/**
 * This class is an extension of class {@link AutoBinner}.
 *
 * @author Lara Gorini
 */
public class DBAutoBinner extends AutoBinner {

    /**
     * @param settings
     * @param spec
     * @throws InvalidSettingsException
     */
    public DBAutoBinner(final AutoBinnerLearnSettings settings, final DataTableSpec spec)
        throws InvalidSettingsException {
        super(settings, spec);
    }

    /**
     * This method creates a {@link PMMLPreprocDiscretize} object and is used in {@link DBAutoBinnerNodeModel}
     *
     * @param connection Connection to database
     * @param query Input database table
     * @param statementManipulator {@link StatementManipulator} to use
     * @param dataTableSpec DataTableSpec of incoming {@link BufferedDataTable}
     * @return a {@link PMMLPreprocDiscretize} object containing required parameters for binning operation
     * @throws SQLException
     */
    public PMMLPreprocDiscretize createPMMLPrepocDiscretize(final Connection connection, final String query,
        final StatementManipulator statementManipulator, final DataTableSpec dataTableSpec) throws SQLException {
        AutoBinnerLearnSettings settings = getSettings();
        String[] includeCols = settings.getFilterConfiguration().applyTo(dataTableSpec).getIncludes();

        if (includeCols.length == 0) {
            return createDisretizeOp(new LinkedHashMap<>());
        }

        double max = 0;
        double min = 0;
        StringBuilder minMaxQuery = new StringBuilder();
        minMaxQuery.append("SELECT");
        for (int i = 0; i < includeCols.length; i++) {
            minMaxQuery.append(" MAX(" + statementManipulator.quoteIdentifier(includeCols[i]) + ") "
                + statementManipulator.quoteIdentifier("max_" + includeCols[i]) + ",");
            minMaxQuery.append(" MIN(" + statementManipulator.quoteIdentifier(includeCols[i]) + ") "
                + statementManipulator.quoteIdentifier("min_" + includeCols[i]));
            if (i < includeCols.length - 1) {
                minMaxQuery.append(",");
            }
        }
        minMaxQuery.append(" FROM (" + query + ") T");
        HashMap<String, Pair<Double, Double>> maxAndMin = new LinkedHashMap<>();
        try (ResultSet valueSet = connection.createStatement().executeQuery(minMaxQuery.toString());) {
            while (valueSet.next()) {
                for (int i = 0; i < includeCols.length; i++) {
                    max = valueSet.getDouble("max_" + includeCols[i]);
                    min = valueSet.getDouble("min_" + includeCols[i]);
                    maxAndMin.put(includeCols[i], new Pair<Double, Double>(min, max));
                }
            }
        }
        int number = settings.getBinCount();
        Map<String, double[]> edgesMap = new LinkedHashMap<>();
        for (Entry<String, Pair<Double, Double>> entry : maxAndMin.entrySet()) {
            double[] edges =
                AutoBinner.calculateBounds(number, entry.getValue().getFirst(), entry.getValue().getSecond());
            if (settings.getIntegerBounds()) {
                edges = AutoBinner.toIntegerBoundaries(edges);
            }
            edgesMap.put(entry.getKey(), edges);
        }

        return createDisretizeOp(edgesMap);
    }

    /**
     * This method translates a {@link PMMLPreprocDiscretize} object into {@link PMMLPortObject}.
     *
     * @param pmmlDiscretize {@link PMMLPreprocDiscretize} object
     * @param dataTableSpec {@link DataTableSpec} if incoming {@link BufferedDataTable}
     * @return a {@link PMMLPortObject} containing required parameters for binning operation
     */
    public static PMMLPortObject translate(final PMMLPreprocDiscretize pmmlDiscretize,
        final DataTableSpec dataTableSpec) {

        final Map<String, Bin[]> columnToBins = new HashMap<>();
        final Map<String, String> columnToAppend = new HashMap<>();

        List<String> replacedColumnNames = pmmlDiscretize.getConfiguration().getNames();
        for (String replacedColumnName : replacedColumnNames) {
            PMMLDiscretize discretize = pmmlDiscretize.getConfiguration().getDiscretize(replacedColumnName);
            List<PMMLDiscretizeBin> bins = discretize.getBins();
            String originalColumnName = discretize.getField();

            if (replacedColumnName.equals(originalColumnName)) { // wenn replaced, dann nicht anhängen
                columnToAppend.put(originalColumnName, null);
            } else { // nicht replaced -> anhängen
                columnToAppend.put(originalColumnName, replacedColumnName);
            }

            NumericBin[] numericBin = new NumericBin[bins.size()];
            int counter = 0;
            for (PMMLDiscretizeBin bin : bins) {
                String binName = bin.getBinValue();
                List<PMMLInterval> intervals = bin.getIntervals();
                boolean leftOpen = false;
                boolean rightOpen = false;
                double leftMargin = 0;
                double rightMargin = 0;
                //always returns only one interval
                for (PMMLInterval interval : intervals) {
                    Closure closure = interval.getClosure();
                    switch (closure) {
                        case openClosed:
                            leftOpen = true;
                            rightOpen = false;
                            break;
                        case openOpen:
                            leftOpen = true;
                            rightOpen = true;
                            break;
                        case closedOpen:
                            leftOpen = false;
                            rightOpen = true;
                        case closedClosed:
                            leftOpen = false;
                            rightOpen = false;
                            break;
                        default:
                            leftOpen = true;
                            rightOpen = false;
                            break;
                    }
                    leftMargin = interval.getLeftMargin();
                    rightMargin = interval.getRightMargin();
                }

                numericBin[counter] = new NumericBin(binName, leftOpen, leftMargin, rightOpen, rightMargin);
                counter++;
            }
            columnToBins.put(originalColumnName, numericBin);
        }

        //ColumnRearranger createColReg = createColReg(dataTableSpec, columnToBins, columnToAppended);
        DataTableSpec newDataTableSpec = createNewDataTableSpec(dataTableSpec, columnToAppend);
        PMMLPortObjectSpecCreator pmmlSpecCreator = new PMMLPortObjectSpecCreator(newDataTableSpec);
        PMMLPortObject pmmlPortObject = new PMMLPortObject(pmmlSpecCreator.createSpec(), null, newDataTableSpec);
        PMMLBinningTranslator trans =
            new PMMLBinningTranslator(columnToBins, columnToAppend, new DerivedFieldMapper(pmmlPortObject));
        TransformationDictionary exportToTransDict = trans.exportToTransDict();
        pmmlPortObject.addGlobalTransformations(exportToTransDict);
        return pmmlPortObject;

    }

    /**
     * This method creates a new {@link DataTableSpec} holding columns which were appended
     *
     * @param dataTableSpec Incoming {@link DataTableSpec}
     * @param columnToAppend Map of Strings containing name of target column as key and name of binned column which has
     *            to be appended as value. If target column is not be appended, value is null
     * @return {@link DataTableSpec} containing target and binned columns
     */
    public static DataTableSpec createNewDataTableSpec(final DataTableSpec dataTableSpec,
        final Map<String, String> columnToAppend) {
        List<DataColumnSpec> origDataColumnSpecs = new LinkedList<>();
        for (int i = 0; i < dataTableSpec.getNumColumns(); i++) {
            origDataColumnSpecs.add(dataTableSpec.getColumnSpec(i));
        }
        List<DataColumnSpec> addDataColumnSpecs = new LinkedList<>();
        for (String column : columnToAppend.keySet()) {
            String replacedColumnName = columnToAppend.get(column);
            if (replacedColumnName != null) {
                addDataColumnSpecs.add(new DataColumnSpecCreator(replacedColumnName, StringCell.TYPE).createSpec());
            }
        }
        origDataColumnSpecs.addAll(addDataColumnSpecs);
        DataTableSpec newDataTableSpec =
            new DataTableSpec(origDataColumnSpecs.toArray(new DataColumnSpec[origDataColumnSpecs.size()]));
        return newDataTableSpec;

    }

    /**
     * This method translates a {@link PMMLPortObject} into a {@link DBBinnerMaps} object which holds several Maps
     * needed to create a binner statement in {@link StatementManipulator}
     *
     * @param pmmlPortObject A {@link PMMLPortObject} containing all necessary information about binning operation
     * @param dataTableSpec Incoming {@link DataTableSpec}
     * @return a {@link DBBinnerMaps} object containing required parameters for {@link StatementManipulator}
     */
    public static DBBinnerMaps intoBinnerMaps(final PMMLPortObject pmmlPortObject, final DataTableSpec dataTableSpec) {

        Map<String, List<Pair<Double, Double>>> boundariesMap = new LinkedHashMap<>();
        Map<String, List<Pair<Boolean, Boolean>>> boundariesOpenMap = new LinkedHashMap<>();
        Map<String, List<String>> namingMap = new LinkedHashMap<>();
        Map<String, String> appendMap = new LinkedHashMap<>();

        DerivedField[] derivedFields = pmmlPortObject.getDerivedFields();
        for (int i = 0; i < derivedFields.length; i++) { // each column has its own derived fields

            List<Pair<Double, Double>> boundaries = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<Pair<Boolean, Boolean>> boundariesOpen = new ArrayList<>();

            List<DiscretizeBin> discretizeBinList = derivedFields[i].getDiscretize().getDiscretizeBinList();
            String replacedColumnName = DataTableSpec.getUniqueColumnName(dataTableSpec, derivedFields[i].getName());
            String originalColumnName = derivedFields[i].getDiscretize().getField();
            for (DiscretizeBin discBin : discretizeBinList) {
                Interval interval = discBin.getInterval();
                double left = interval.isSetLeftMargin() ? interval.getLeftMargin() : Double.NEGATIVE_INFINITY;
                double right = interval.isSetRightMargin() ? interval.getRightMargin() : Double.POSITIVE_INFINITY;
                boundaries.add(new Pair<>(left, right));
                names.add(discBin.getBinValue());
                boolean leftOpen;
                boolean rightOpen;
                int closure = discBin.getInterval().xgetClosure().enumValue().intValue();
                /*
                 *static final int INT_OPEN_CLOSED = 1;
                 *static final int INT_OPEN_OPEN = 2;
                 *static final int INT_CLOSED_OPEN = 3;
                 *static final int INT_CLOSED_CLOSED = 4;
                 */
                switch (closure) {
                    case 1:
                        leftOpen = true;
                        rightOpen = false;
                        break;
                    case 2:
                        leftOpen = true;
                        rightOpen = true;
                        break;
                    case 3:
                        leftOpen = false;
                        rightOpen = true;
                        break;
                    case 4:
                        leftOpen = false;
                        rightOpen = false;
                        break;
                    default:
                        leftOpen = true;
                        rightOpen = false;
                        break;
                }
                boundariesOpen.add(new Pair<>(leftOpen, rightOpen));
            }

            boundariesMap.put(originalColumnName, boundaries);
            namingMap.put(originalColumnName, names);
            boundariesOpenMap.put(originalColumnName, boundariesOpen);
            if (replacedColumnName.matches("(.*)" + originalColumnName + "\\*" + "(.*)")) {
                appendMap.put(originalColumnName, null);
            } else {
                appendMap.put(originalColumnName, replacedColumnName);
            }
        }
        DBBinnerMaps maps = new DBBinnerMaps(boundariesMap, boundariesOpenMap, namingMap, appendMap);
        return maps;
    }

    /**
     * Method filters columns.
     *
     * @param includeCols Columns that are included
     * @param allColumns All columns
     * @return Columns that are included in allColumns but not in includeCols
     */
    public static String[] filter(final String[] includeCols, final String[] allColumns) {
        final Set<String> allCols = new LinkedHashSet<>(Arrays.asList(allColumns));
        allCols.removeAll(Arrays.asList(includeCols));
        return allCols.toArray(new String[0]);
    }

}
