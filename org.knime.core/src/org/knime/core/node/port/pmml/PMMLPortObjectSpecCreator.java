/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObjectSpecCreator {

    private final DataTableSpec m_dataTableSpec;

    private final List<String> m_learningCols;

    private final List<String> m_ignoredCols;

    private final List<String> m_targetCols;

    /**
     * @param tableSpec equivalent to the data dictionary
     */
    public PMMLPortObjectSpecCreator(final DataTableSpec tableSpec) {
        m_dataTableSpec = tableSpec;
        m_learningCols = new LinkedList<String>();
        m_ignoredCols = new LinkedList<String>();
        m_targetCols = new LinkedList<String>();
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
        if (isValidColumnSpecNames(learningCols)) {
            m_learningCols.clear();
            m_learningCols.addAll(learningCols);
        }
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
        if (isValidColumnSpec(learningCols)) {
            m_learningCols.clear();
            for (DataColumnSpec colSpec : learningCols) {
                m_learningCols.add(colSpec.getName());
            }
        }
    }

    /**
     * This method will be removed in a version 2.2 . Ignored columns/fields are
     * by definition not necessary for the model and can cause problems when the
     * types are not supported.
     *
     * @param ignoredCols the ignoredCols to set
     */
    @Deprecated
    public void setIgnoredColsNames(final List<String> ignoredCols) {
        if (ignoredCols == null) {
            throw new IllegalArgumentException(
                    "Ignored columns must not be null!");
        }
        if (isValidColumnSpecNames(ignoredCols)) {
            m_ignoredCols.clear();
            m_ignoredCols.addAll(ignoredCols);
        }
        m_learningCols.removeAll(m_ignoredCols);
        m_targetCols.removeAll(m_ignoredCols);
    }

    /**
     * This method will be removed in a version 2.2 . Ignored columns/fields are
     * by definition not necessary for the model and can cause problems when the
     * types are not supported.
     *
     * @param ignoredCols columns ignored during learning
     */
    @Deprecated
    public void setIgnoredCols(final List<DataColumnSpec> ignoredCols) {
        if (ignoredCols == null) {
            throw new IllegalArgumentException(
                    "Ignored columns must not be null!");
        }
        if (isValidColumnSpec(ignoredCols)) {
            m_ignoredCols.clear();
            for (DataColumnSpec colSpec : ignoredCols) {
                m_ignoredCols.add(colSpec.getName());
            }
        }
        m_learningCols.removeAll(m_ignoredCols);
        m_targetCols.removeAll(m_ignoredCols);
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
        if (isValidColumnSpecNames(targetCols)) {
            m_targetCols.clear();
            m_targetCols.addAll(targetCols);
        }
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
        if (isValidColumnSpec(targetCols)) {
            m_targetCols.clear();
            for (DataColumnSpec colSpec : targetCols) {
                m_targetCols.add(colSpec.getName());
            }
        }
        m_learningCols.removeAll(m_targetCols);
    }

    /**
     * Creates a new {@link PMMLPortObjectSpec} based on the internal attributes
     * of this creator.
     *
     * @return created spec based upon the set attributes
     */
    public PMMLPortObjectSpec createSpec() {
        if (m_learningCols.isEmpty()) {
            /*
             * Add all compatible columns as learning columns if no learning
             * column has been set explicitly.
             */
            List<String> colNames = new LinkedList<String>();
            for (DataColumnSpec colSpec : m_dataTableSpec) {
                if (isValidColumnSpec(colSpec)) {
                    colNames.add(colSpec.getName());
                    m_ignoredCols.remove(colSpec);
                    m_targetCols.remove(colSpec);
                }
            }
            setLearningColsNames(colNames);
        }
        return new PMMLPortObjectSpec(m_dataTableSpec, m_learningCols,
                m_ignoredCols, m_targetCols);
    }

    private boolean isValidColumnSpec(final List<DataColumnSpec> colSpecs) {
        for (DataColumnSpec colSpec : colSpecs) {
            if (m_dataTableSpec.getColumnSpec(colSpec.getName()) == null) {
                throw new IllegalArgumentException("Column with name "
                        + colSpec.getName()
                        + " is not in underlying DataTableSpec!");
            }
            if (!isValidColumnSpec(colSpec)) {
                throw new IllegalArgumentException(
                        "Only double and nominal value compatible columns "
                                + "are yet supported for PMML models!");
            }
        }
        return true;
    }

    private boolean isValidColumnSpecNames(final List<String> colSpecs) {
        for (String colSpec : colSpecs) {
            DataColumnSpec columnSpec = m_dataTableSpec.getColumnSpec(colSpec);
            if (m_dataTableSpec.getColumnSpec(colSpec) == null) {
                throw new IllegalArgumentException("Column with name "
                        + colSpec + " is not in underlying DataTableSpec!");
            }
            if (!isValidColumnSpec(columnSpec)) {
                throw new IllegalArgumentException(
                        "Only double and nominal value compatible columns "
                                + "are yet supported for PMML models!");
            }
        }
        return true;
    }

    private boolean isValidColumnSpec(final DataColumnSpec columnSpec) {
        return columnSpec.getType().isCompatible(DoubleValue.class)
                || columnSpec.getType().isCompatible(NominalValue.class);
    }

}
