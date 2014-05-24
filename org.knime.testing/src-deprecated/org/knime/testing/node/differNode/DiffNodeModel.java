/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   May 10, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.testing.internal.nodes.image.ImageDifferNodeFactory;
import org.knime.testing.node.differNode.DiffNodeDialog.Evaluators;

/**
 *
 * @author ritmeier, University of Konstanz
 * @deprecated use the new image comparator {@link ImageDifferNodeFactory} and the extension point for difference
 *             checker instead
 */
@Deprecated
public class DiffNodeModel extends NodeModel {

    /** Config key for the evaluator. */
    public static final String CFGKEY_EVALUATORKEY = "TESTEVALUATOR";

    /** Config key for the lower tolerance. */
    public static final String CFGKEY_LOWERTOLERANCEKEY = "LOWERTOLLERANCE";

    /** Config key for the upper tolerance. */
    public static final String CFGKEY_UPPERERTOLERANCEKEY = "UPPERTOLLERANCE";

    /** Config key for the epsilon for numeric values. */
    public static final String CFGKEY_EPSILON = "EPSILON";


    private Evaluators m_evaluator = Evaluators.TableDiffer;

    private int m_lowerTolerance;

    private int m_upperTolerance;

    private double m_epsilon;

    /**
     * Creates a model with two data inports. The first port for the new table,
     * the second forr the original ("golden") table.
     *
     */
    public DiffNodeModel() {
        super(2, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFGKEY_EVALUATORKEY, m_evaluator == null ? ""
                : m_evaluator.name());
        if (m_evaluator != null
                && m_evaluator.equals(Evaluators.LearnerScoreComperator)) {
            settings.addInt(DiffNodeModel.CFGKEY_LOWERTOLERANCEKEY,
                    m_lowerTolerance);
            settings.addInt(DiffNodeModel.CFGKEY_UPPERERTOLERANCEKEY,
                    m_upperTolerance);
        }
        settings.addDouble(CFGKEY_EPSILON, m_epsilon);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String evaluatorString = settings.getString(CFGKEY_EVALUATORKEY);
        Evaluators eval = null;
        try {
            eval = Evaluators.valueOf(evaluatorString);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException("no valid evaluator");
        }
        if (eval == null) {
            throw new InvalidSettingsException("no valid evaluator");
        }

        if (eval.equals(DiffNodeDialog.Evaluators.LearnerScoreComperator)) {
            settings.getInt(CFGKEY_LOWERTOLERANCEKEY);
            settings.getInt(CFGKEY_UPPERERTOLERANCEKEY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String evaluatorString = settings.getString(CFGKEY_EVALUATORKEY);
        m_evaluator = DiffNodeDialog.Evaluators.valueOf(evaluatorString);
        if (m_evaluator
                .equals(DiffNodeDialog.Evaluators.LearnerScoreComperator)) {
            m_lowerTolerance = settings.getInt(CFGKEY_LOWERTOLERANCEKEY);
            m_upperTolerance = settings.getInt(CFGKEY_UPPERERTOLERANCEKEY);
        }
        m_epsilon = settings.getDouble(CFGKEY_EPSILON, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        TestEvaluator eval = m_evaluator.getInstance();
        if (eval instanceof LearnerScoreComperator) {
            ((LearnerScoreComperator)eval).setTolerance(m_lowerTolerance,
                    m_upperTolerance);
        } else if (eval instanceof DataTableDiffer) {
            ((DataTableDiffer) eval).setEpsilon(m_epsilon);
        }
        eval.compare(inData[0], inData[1]);

        return new BufferedDataTable[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{};
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing todo
    }
}
