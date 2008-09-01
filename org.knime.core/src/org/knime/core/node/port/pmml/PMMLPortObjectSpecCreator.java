/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 */
package org.knime.core.node.port.pmml;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObjectSpecCreator {

    private final DataTableSpec m_dataTableSpec;

    private final Set<String>m_learningCols;

    private final Set<String> m_ignoredCols;

    private final Set<String> m_targetCols;

    /**
     * Adds all columns in the table spec as learning columns.
     * When the target or ignore columns are set, they are removed from the
     * learning columns.
     *
     * @param tableSpec equivalent to the data dictionary
     */
    public PMMLPortObjectSpecCreator(final DataTableSpec tableSpec) {
        m_dataTableSpec = tableSpec;
        m_learningCols = new LinkedHashSet<String>();
        m_ignoredCols = new LinkedHashSet<String>();
        m_targetCols = new LinkedHashSet<String>();
        // add all columns as learning columns
        Set<String>colNames = new LinkedHashSet<String>();
        for (DataColumnSpec colSpec : tableSpec) {
            colNames.add(colSpec.getName());
        }
        setLearningColsNames(colNames);
    }

    /**
     * @param learningCols the learningCols to set
     */
    public void setLearningColsNames(final Set<String> learningCols) {
        if (learningCols == null) {
            throw new IllegalArgumentException(
                    "Learning columns must not be null!");
        }
        if (validColumnSpecNames(learningCols)) {
            m_learningCols.clear();
            m_learningCols.addAll(learningCols);
        }
    }

    /**
     *
     * @param learningCols column used for training
     */
    public void setLearningCols(final Set<DataColumnSpec> learningCols) {
        // TODO: sanity checks . != null, etc.
        if (learningCols == null) {
           throw new IllegalArgumentException(
                   "Learning columns must not be null!");
        }
        if (validColumnSpec(learningCols)) {
            m_learningCols.clear();
            for (DataColumnSpec colSpec : learningCols) {
                m_learningCols.add(colSpec.getName());
            }
        }
    }


    /**
     * @param ignoredCols the ignoredCols to set
     */
    public void setIgnoredColsNames(final Set<String> ignoredCols) {
        if (ignoredCols == null) {
            throw new IllegalArgumentException(
                    "Ignored columns must not be null!");
        }
        if (validColumnSpecNames(ignoredCols)) {
            m_ignoredCols.clear();
            m_ignoredCols.addAll(ignoredCols);
        }
        m_learningCols.removeAll(m_ignoredCols);
        m_targetCols.removeAll(m_ignoredCols);
    }


    /**
     *
     * @param ignoredCols columns ignored during learning
     */
    public void setIgnoredCols(final Set<DataColumnSpec>ignoredCols) {
        if (ignoredCols == null) {
            throw new IllegalArgumentException(
                    "Ignored columns must not be null!");
        }
        if (validColumnSpec(ignoredCols)) {
            m_ignoredCols.clear();
            for (DataColumnSpec colSpec : ignoredCols) {
                m_ignoredCols.add(colSpec.getName());
            }
        }
        m_learningCols.removeAll(m_ignoredCols);
        m_targetCols.removeAll(m_ignoredCols);
    }

    /**
     * @param targetCol the target column to set
     */
    public void setTargetColName(final String targetCol) {
        Set<String> set = new HashSet<String>();
        set.add(targetCol);
        setTargetColsNames(set);
    }

    /**
     * @param targetCol the target column to set
     */
    public void setTargetCol(final DataColumnSpec targetCol) {
        Set<DataColumnSpec> set = new HashSet<DataColumnSpec>();
        set.add(targetCol);
        setTargetCols(set);
    }

    /**
     * @param targetCols the targetCols to set
     */
    public void setTargetColsNames(final Set<String> targetCols) {
        if (targetCols == null) {
            throw new IllegalArgumentException(
                    "Target columns must not be null!");
        }
        if (validColumnSpecNames(targetCols)) {
            m_targetCols.clear();
            m_targetCols.addAll(targetCols);
        }
        m_learningCols.removeAll(m_targetCols);
    }

    /**
     *
     * @param targetCols predicted columns
     */
    public void setTargetCols(final Set<DataColumnSpec>targetCols) {
        // TODO: sanity checks != null, etc.
        if (targetCols == null) {
            throw new IllegalArgumentException(
                    "Target columns must not be null!");
        }
        if (validColumnSpec(targetCols)) {
            m_targetCols.clear();
            for (DataColumnSpec colSpec : targetCols) {
                m_targetCols.add(colSpec.getName());
            }
        }
        m_learningCols.removeAll(m_targetCols);
    }

    private boolean validColumnSpec(final Set<DataColumnSpec> colSpecs) {
        for (DataColumnSpec colSpec : colSpecs) {
            if (m_dataTableSpec.getColumnSpec(colSpec.getName()) == null) {
                throw new IllegalArgumentException("Column with name "
                        + colSpec.getName()
                        + " is not in underlying DataTableSpec!");
            }
        }
        return true;
    }

    private boolean validColumnSpecNames(final Set<String> colSpecs) {
        for (String colSpec : colSpecs) {
            if (m_dataTableSpec.getColumnSpec(colSpec) == null) {
                throw new IllegalArgumentException("Column with name "
                        + colSpec
                        + " is not in underlying DataTableSpec!");
            }
        }
        return true;
    }

    /**
     * Creates a new {@link PMMLPortObjectSpec} based on the internal attributes
     * of this creator.
     *
     * @return created spec based upon the set attributes
     */
    public PMMLPortObjectSpec createSpec() {
        return new PMMLPortObjectSpec(
                m_dataTableSpec,
                m_learningCols,
                m_ignoredCols,
                m_targetCols);
    }


}
