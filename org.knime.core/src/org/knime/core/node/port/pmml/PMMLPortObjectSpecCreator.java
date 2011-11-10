/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.port.pmml;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Fabian Dill, University of Konstanz
 * @author Dominik Morent, KNIME.com AG, Zuerich, Switzerland
 */
public class PMMLPortObjectSpecCreator {

    private DataTableSpec m_dataTableSpec;

    private List<String> m_learningCols;

    private List<String> m_targetCols;

    private List<String> m_preprocCols;

    /**
     * @param tableSpec equivalent to the data dictionary
     */
    public PMMLPortObjectSpecCreator(final DataTableSpec tableSpec) {
       init(null, tableSpec);
    }

    /**
     * Creates a new {@link PMMLPortObjectSpecCreator} based on an existing
     * {@link PMMLPortObject}. If portObject is null it has the same effect as
     * calling {@link #PMMLPortObjectSpecCreator(DataTableSpec)}.
     *
     * @param portObject the {@link PMMLPortObject} to base this instance on
     * @param tableSpec the {@link DataTableSpec}
     */
    public PMMLPortObjectSpecCreator(final PMMLPortObject portObject,
            final DataTableSpec tableSpec) {
        if (portObject != null) {
            PMMLPortObjectSpec portSpec = portObject.getSpec();
            init(portSpec, tableSpec);
        } else {
            init(null, tableSpec);
        }
    }


    /**
     * Creates a new {@link PMMLPortObjectSpecCreator} based on an existing
     * {@link PMMLPortObjectSpec} and a {@link DataTableSpec}.
     *
     * @param portSpec the {@link PMMLPortObjectSpec} to base this instance on
     * @param tableSpec the {@link DataTableSpec}
     */
    public PMMLPortObjectSpecCreator(final PMMLPortObjectSpec portSpec,
            final DataTableSpec tableSpec) {
        init(portSpec, tableSpec);
    }

    /**
     * Creates a new {@link PMMLPortObjectSpecCreator} based on an existing
     * {@link PMMLPortObjectSpec}.
     *
     * @param portSpec the {@link PMMLPortObjectSpec} to base this instance on
     */
    public PMMLPortObjectSpecCreator(final PMMLPortObjectSpec portSpec) {
        init(portSpec, portSpec.getDataTableSpec());
    }

    private void init(final PMMLPortObjectSpec portSpec,
            final DataTableSpec newSpec) {
        m_dataTableSpec = newSpec;
        if (portSpec != null) {
            m_learningCols = new LinkedList<String>(
                    portSpec.getLearningFields());
            m_targetCols = new LinkedList<String>(portSpec.getTargetFields());
            m_preprocCols = new LinkedList<String>(
                    portSpec.getPreprocessingFields());
        } else {
            m_learningCols = new LinkedList<String>();
            m_targetCols = new LinkedList<String>();
            m_preprocCols = new LinkedList<String>();
        }
    }

    /**
     * Only double and nominal value compatible columns are yet supported for
     * PMML models!
     *
     * @param learningCols the learningCols to set
     */
    public void setLearningColsNames(final List<String> learningCols) {
        if (learningCols == null) {
            throw new IllegalArgumentException(
                    "Learning columns must not be null!");
        }
        validateColumnNames(learningCols);
        m_learningCols.clear();
        m_learningCols.addAll(learningCols);
    }

    /**
     * Only double and nominal value compatible columns are yet supported for
     * PMML models!
     * @param learningCols column used for training
     */
    public void setLearningCols(final List<DataColumnSpec> learningCols) {
        if (learningCols == null) {
            throw new IllegalArgumentException(
                    "Learning columns must not be null!");
        }
        validateColumns(learningCols);
        m_learningCols.clear();
        for (DataColumnSpec colSpec : learningCols) {
            m_learningCols.add(colSpec.getName());
        }
    }

    /**
     * Sets all columns of the data table spec as learning columns.
     * Only double and nominal value compatible columns are yet supported for
     * PMML models!
     * @param tableSpec the table spec containing the columns to be added
     * @throws InvalidSettingsException if a data column type is not compatible
     *          with double or nominal value (not supported by PMML)
     */
    public void setLearningCols(final DataTableSpec tableSpec)
            throws InvalidSettingsException {
        if (tableSpec == null) {
            throw new IllegalArgumentException(
                    "Table spec must not be null!");
        }
        // add all columns as learning columns
        List<String>colNames = new LinkedList<String>();
        for (DataColumnSpec colSpec : tableSpec) {
            colNames.add(colSpec.getName());
        }
        setLearningColsNames(colNames);
    }

    /**
     * Puts argument into set and call {@link #setTargetColsNames(List)}.
     *
     * @param targetCol the target column to set
     */
    public void setTargetColName(final String targetCol) {
        setTargetColsNames(Arrays.asList(targetCol));
    }

    /**
     * Puts argument into set and call {@link #setTargetCols(List)}.
     *
     * @param targetCol the target column to set
     */
    public void setTargetCol(final DataColumnSpec targetCol) {
        setTargetCols(Arrays.asList(targetCol));
    }

    /**
     * Only double and nominal value compatible columns are yet supported for
     * PMML models!
     *
     * @param targetCols the targetCols to set
     */
    public void setTargetColsNames(final List<String> targetCols) {
        if (targetCols == null) {
            throw new IllegalArgumentException(
                    "Target columns must not be null!");
        }
        validateColumnNames(targetCols);
        m_targetCols.clear();
        m_targetCols.addAll(targetCols);
        m_learningCols.removeAll(m_targetCols);
    }

    /**
     * Only double and nominal value compatible columns are yet supported for
     * PMML models!
     *
     * @param targetCols predicted columns
     */
    public void setTargetCols(final List<DataColumnSpec> targetCols) {
        if (targetCols == null) {
            throw new IllegalArgumentException(
                    "Target columns must not be null!");
        }
        validateColumns(targetCols);
        m_targetCols.clear();
        for (DataColumnSpec colSpec : targetCols) {
            m_targetCols.add(colSpec.getName());
        }
        m_learningCols.removeAll(m_targetCols);
    }

    /**
     * Adds the specified columns as preprocessing column. Nothing happens if
     * they are already set as preprocessing column.
     *
     * @param activeCols active columns
     */
    public void addPreprocCols(final List<DataColumnSpec> activeCols) {
        if (activeCols == null) {
            throw new IllegalArgumentException(
                    "Target columns must not be null!");
        }
        validateColumns(activeCols);
        for (DataColumnSpec colSpec : activeCols) {
            m_preprocCols.add(colSpec.getName());
        }
    }

    /**
     * Adds the specified column names as preprocessing column. Nothing happens
     * if they are already set as preprocessing column.
     *
     * @param colNames the names of the columns to be marked as active
     */
    public void addPreprocColNames(final List<String> colNames) {
        validateColumnNames(colNames);
        for (String columnName : colNames) {
            if (!m_preprocCols.contains(columnName)) {
                m_preprocCols.add(columnName);
            }
        }
    }

    /**
     * Creates a new {@link PMMLPortObjectSpec} based on the internal attributes
     * of this creator.
     *
     * @return created spec based upon the set attributes
     */
    public PMMLPortObjectSpec createSpec() {
        return new PMMLPortObjectSpec(m_dataTableSpec, m_preprocCols,
                m_learningCols, m_targetCols);
    }

    /**
     * Checks if the provided column names are valid.
     *
     * @param columnNames a list of column names
     * @throws IllegalArgumentException if the column names are not valid and
     *      cannot be set as learning, target or preprocessing column
     */
    private void validateColumnNames(final List<String> columnNames)
            throws IllegalArgumentException {
        for (String colSpec : columnNames) {
            DataColumnSpec columnSpec = m_dataTableSpec.getColumnSpec(colSpec);
            if (m_dataTableSpec.getColumnSpec(colSpec) == null) {
                throw new IllegalArgumentException("Column with name "
                        + colSpec + " is not in underlying DataTableSpec!");
            }
            if (!isValidColumnSpec(columnSpec)) {
                throw new IllegalArgumentException(
                        "Only double, nominal and string value compatible "
                        + "columns are yet supported for PMML models!");
            }
        }
    }

    private static void validateColumns(final List<DataColumnSpec> colSpecs) {
        for (DataColumnSpec colSpec : colSpecs) {
            if (!isValidColumnSpec(colSpec)) {
                throw new IllegalArgumentException(
                        "Only double, nominal and string value compatible "
                        + "columns are yet supported for PMML models!");
            }
        }
    }

    private static boolean isValidColumnSpec(final DataColumnSpec columnSpec) {
        return columnSpec.getType().isCompatible(DoubleValue.class)
                || columnSpec.getType().isCompatible(NominalValue.class)
                || columnSpec.getType().isCompatible(StringValue.class);
    }

}
