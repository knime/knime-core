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
 * Created on 24.10.2013 by hofer
 */
package org.knime.base.node.mine.regression.predict3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPPCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPredictor;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Abstraction for all predictro cell factories.
 * <p>Despite being public no official API.
 * @author Heiko Hofer
 */
public abstract class RegressionPredictorCellFactory extends AbstractCellFactory {

    /**
     * Creates the spec of the output if possible.
     *
     * @param portSpec the spec of the pmml input port
     * @param tableSpec the spec of the data input port
     * @param settings settings for the predictor node
     * @return The spec of the output or null
     * @throws InvalidSettingsException when tableSpec and portSpec do not match
     */
    public static DataColumnSpec[] createColumnSpec(
            final PMMLPortObjectSpec portSpec,
            final DataTableSpec tableSpec,
            final RegressionPredictorSettings settings) throws InvalidSettingsException {
        // Assertions
        if (portSpec.getTargetCols().isEmpty()) {
            throw new InvalidSettingsException("The general regression model"
                    + " does not specify a target column.");
        }

        for (DataColumnSpec learningColSpec : portSpec.getLearningCols()) {
            String learningCol = learningColSpec.getName();
            if (tableSpec.containsName(learningCol)) {
                DataColumnSpec colSpec = tableSpec.getColumnSpec(learningCol);
                if (learningColSpec.getType().isCompatible(NominalValue.class)) {
                    if (!colSpec.getType().isCompatible(BitVectorValue.class) && !colSpec.getType().isCompatible(ByteVectorValue.class) && !colSpec.getType().isCompatible(NominalValue.class)) {
                    throw new InvalidSettingsException("The column \""
                            + learningCol + "\" in the table of prediction "
                            + "is expected to be  compatible with "
                            + "\"NominalValue\".");
                    }
                } else if (learningColSpec.getType().isCompatible(
                        DoubleValue.class)
                        && !colSpec.getType().isCompatible(DoubleValue.class)) {
                    throw new InvalidSettingsException("The column \""
                            + learningCol + "\" in the table of prediction "
                            + "is expected to be numeric.");
                }
            } else {
                throw new InvalidSettingsException("The table for prediction "
                        + "does not contain the column \""
                        + learningCol + "\".");
            }
        }

        // The list of added columns
        List<DataColumnSpec> newColsSpec = new ArrayList<DataColumnSpec>();
        String targetCol = portSpec.getTargetFields().get(0);
        DataColumnSpec targetColSpec = portSpec.getDataTableSpec().getColumnSpec(targetCol);

        if (settings.getIncludeProbabilities() && targetColSpec.getType().isCompatible(NominalValue.class)) {
            if (!targetColSpec.getDomain().hasValues()) {
                return null;
            }
            List<DataCell> targetCategories = new ArrayList<DataCell>();
            targetCategories.addAll(targetColSpec.getDomain().getValues());

            for (DataCell value : targetCategories) {
                String name = "P (" + targetCol + "=" + value.toString() + ")" + settings.getPropColumnSuffix();
                String newColName = DataTableSpec.getUniqueColumnName(tableSpec, name);
                DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator(newColName, DoubleCell.TYPE);
                DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(new DoubleCell(0.0),
                                new DoubleCell(1.0));
                colSpecCreator.setDomain(domainCreator.createDomain());
                newColsSpec.add(colSpecCreator.createSpec());
            }
        }



        String targetColName = settings.getHasCustomPredictionName()
                ? settings.getCustomPredictionName() : "Prediction (" + targetCol + ")";
        String uniqueTargetColName = DataTableSpec.getUniqueColumnName(tableSpec, targetColName);

        DataType targetType = targetColSpec.getType().isCompatible(NominalValue.class)
            ? targetColSpec.getType() : DoubleCell.TYPE;
        DataColumnSpecCreator targetColSpecCreator = new DataColumnSpecCreator(uniqueTargetColName, targetType);
        if (targetColSpec.getType().isCompatible(NominalValue.class)) {
            DataColumnDomainCreator targetDomainCreator = new DataColumnDomainCreator(targetColSpec.getDomain());
            targetColSpecCreator.setDomain(targetDomainCreator.createDomain());
        }
        newColsSpec.add(targetColSpecCreator.createSpec());

        return newColsSpec.toArray(new DataColumnSpec[0]);
    }




    /**
     * @param trainingSpec the table spec of the training set
     * @param content the content
     * @return the factors name mapped to its values
     * @throws InvalidSettingsException If the PMML data dictionary contains more elements for a nominal column
     *                                  than represented in the data
     */
    protected static Map<String, List<DataCell>> determineFactorValues(final PMMLGeneralRegressionContent content,
        final DataTableSpec trainingSpec) throws InvalidSettingsException {
        HashMap<String, List<DataCell>> values = new HashMap<String, List<DataCell>>();

        for (PMMLPredictor factor : content.getFactorList()) {
            String factorName = factor.getName();
            Map<String, DataCell> domainValues = new HashMap<String, DataCell>();
            for (DataCell cell : trainingSpec.getColumnSpec(factorName).getDomain().getValues()) {
                domainValues.put(cell.toString(), cell);
            }

            Set<DataCell> factorValues = new LinkedHashSet<DataCell>();
            // add all values for all PMMLGeneralRegression model that do not specify all values in the PPMatrix
            factorValues.addAll(trainingSpec.getColumnSpec(factorName).getDomain().getValues());
            int count = 0;
            for (PMMLPPCell ppCell : content.getPPMatrix()) {
                if (ppCell.getPredictorName().equals(factorName)) {
                    DataCell cell = domainValues.get(ppCell.getValue());
                    // move cell to the end of the list, this gives in the end the same ordering
                    // as in the PPMatrix of the PMMLGeneralRegression model
                    factorValues.remove(cell);
                    factorValues.add(cell);
                    count++;
                }
            }
            // The base line category may not be in the PPMatrix of the PMMLGeneralRegression model
            // in this case count is lower than the number of domain values, but if count if even
            // less than that the base line category is ambiguous.
            final int valuesDataDictionary = trainingSpec.getColumnSpec(factorName).getDomain().getValues().size();
            if (count < valuesDataDictionary - 1) {
                throw new InvalidSettingsException("The data dictionary to column \"" + factorName
                    + "\" contains more elements than represented in the regression model "
                    + "(unable to decode dummy variables as reference is unknown: "
                    + valuesDataDictionary + " > " + count + " + 1)");
            }

            List<DataCell> vals = new ArrayList<DataCell>();
            vals.addAll(factorValues);
            values.put(factorName, vals);
        }
        return values;
    }

    /**
     * This constructor should be used during the configure phase of a node.
     * The created instance will give a valid spec of the output but cannot
     * be used to compute the cells.
     *
     * @param portSpec the spec of the pmml input port
     * @param tableSpec the spec of the data input port
     * @param settings settings for the predictor node
     * @throws InvalidSettingsException when tableSpec and portSpec do not match
     */
    public RegressionPredictorCellFactory(final PMMLPortObjectSpec portSpec,
            final DataTableSpec tableSpec,
            final RegressionPredictorSettings settings
            ) throws InvalidSettingsException {
        super(createColumnSpec(portSpec, tableSpec, settings));
    }


}
