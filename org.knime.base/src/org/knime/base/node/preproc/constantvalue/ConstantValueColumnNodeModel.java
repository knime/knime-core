/*
 * ------------------------------------------------------------------------
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
 *
 */
package org.knime.base.node.preproc.constantvalue;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;

/**
 * Model for the Constant Value Column.
 *
 * @author Marcel Hanser
 */
final class ConstantValueColumnNodeModel extends SimpleStreamableFunctionNodeModel {

    private ConstantValueColumnConfig m_config = new ConstantValueColumnConfig();

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        DataTableSpec out = rearranger.createSpec();
        return new DataTableSpec[]{out};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        DataTableSpec spec = inData[0].getDataTableSpec();
        ColumnRearranger rearranger = createColumnRearranger(spec);
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /** {@inheritDoc} */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec in) throws InvalidSettingsException {
        final String value = m_config.getValue();
        checkSetting(value != null, "Configuration missing.");

        checkSetting(!(m_config.getReplacedColumn() == null && m_config.getNewColumnName() == null),
            "Either a replacing column or a new column name must be specified");

        String colName = m_config.getReplacedColumn();
        final int replacedColumn = in.findColumnIndex(m_config.getReplacedColumn());

        checkSetting(!(colName != null && replacedColumn < 0), "Column to replace: '%s' does not exist in input table",
            colName);

        String newName =
            replacedColumn >= 0 ? colName : DataTableSpec.getUniqueColumnName(in, m_config.getNewColumnName());

        DataColumnSpec outColumnSpec =
            new DataColumnSpecCreator(newName, m_config.getCellFactory().getDataType()).createSpec();

        final DataCell constantCell = m_config.getCellFactory().createCell(value, m_config.getDateFormat());

        ColumnRearranger rearranger = new ColumnRearranger(in);
        CellFactory fac = new SingleCellFactory(outColumnSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                return constantCell;
            }
        };

        if (replacedColumn >= 0) {
            rearranger.replace(fac, replacedColumn);
        } else {
            rearranger.append(fac);
        }
        return rearranger;
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ConstantValueColumnConfig().loadInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        ConstantValueColumnConfig cfg = new ConstantValueColumnConfig();
        cfg.loadInModel(settings);
        m_config = cfg;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config.getValue() != null) {
            m_config.save(settings);
        }
    }

    /**
     * Throws an {@link InvalidSettingsException} with the given string template, if the given predicate is
     * <code>false</code>.
     *
     * @param predicate the predicate
     * @param template the template
     * @throws InvalidSettingsException
     */
    private static void checkSetting(final boolean predicate, final String template, final Object... args)
        throws InvalidSettingsException {
        if (!predicate) {
            throw new InvalidSettingsException(String.format(template, args));
        }
    }

}
