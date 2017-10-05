/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   19.12.2014 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval.handlers;

import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.knime.base.data.statistics.Statistic;
import org.knime.base.node.preproc.pmml.missingval.DataColumnWindow;
import org.knime.base.node.preproc.pmml.missingval.MissingCellHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * A handler that replaces missing values in a cell with the average of cells before and after it.
 * @author Alexander Fillbrunn
 */
public class DoubleMovingAverageMissingCellHandler extends MissingCellHandler {

    private static final String LOOKBEHIND_SIZE_CFG = "lookbehindSize";

    private static final String LOOKAHEAD_SIZE_CFG = "lookaheadSize";

    /**
     * @return a SettingsModel for setting the lookbehind
     */
    public static SettingsModelIntegerBounded createLookbehindSettingsModel() {
        return new SettingsModelIntegerBounded(LOOKBEHIND_SIZE_CFG, 1, 1, Integer.MAX_VALUE);
    }

    /**
     * @return a SettingsModel for setting the lookahead
     */
    public static SettingsModelIntegerBounded createLookaheadSettingsModel() {
        return new SettingsModelIntegerBounded(LOOKAHEAD_SIZE_CFG, 1, 1, Integer.MAX_VALUE);
    }

    private SettingsModelIntegerBounded m_lookbehindSize = createLookbehindSettingsModel();
    private SettingsModelIntegerBounded m_lookaheadSize = createLookaheadSettingsModel();

    /**
     * @param col the column this handler is configured for
     */
    public DoubleMovingAverageMissingCellHandler(final DataColumnSpec col) {
        super(col);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statistic getStatistic() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final RowKey key, final DataColumnWindow window) {
        double sum = 0.0;
        int count = 0;
        for (int i = -m_lookbehindSize.getIntValue(); i < m_lookaheadSize.getIntValue() + 1; i++) {
            DataCell cell = window.getNthCell(i);
            if (cell != null && !cell.isMissing()) {
                sum += ((DoubleValue)cell).getDoubleValue();
                count++;
            }
        }
        if (count > 0) {
            return new DoubleCell(sum / count);
        } else {
            return new MissingCell("No cells for average calculation available");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getOutputDataType() {
        return DoubleCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DerivedField getPMMLDerivedField() {
        DerivedField f = createExtensionDerivedField(org.dmg.pmml.DATATYPE.DOUBLE,
                                    DoubleMovingAverageMissingCellHandlerFactory.ID);
        return f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPreviousCellsWindowSize() {
        return m_lookbehindSize.getIntValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNextCellsWindowSize() {
        return m_lookaheadSize.getIntValue();
    }

    /**
     * {@inheritDoc}
     * @throws InvalidSettingsException
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_lookaheadSize.loadSettingsFrom(settings);
        m_lookbehindSize.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_lookaheadSize.saveSettingsTo(settings);
        m_lookbehindSize.saveSettingsTo(settings);
    }
}
