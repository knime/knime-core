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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.expressions.node.formulas;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.expressions.node.MultiExpressionCellFactory;

/**
 * Model used to compute new columns derived by expressions.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class FormulasNodeModel extends NodeModel {

	private FormulasNodeConfiguration m_configuration;

	/**
	 * Empty constructor.
	 */
	public FormulasNodeModel() {
		super(1, 1);
		m_configuration = new FormulasNodeConfiguration();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (m_configuration.getExpressionTable() == null || m_configuration.getExpressionTable().length == 0) {
			return inSpecs;
		}

		ColumnRearranger rearranger = createColumnRearranger(inSpecs[0], 0, null);

		return new DataTableSpec[] { rearranger.createSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		if (m_configuration.getExpressionTable() == null || m_configuration.getExpressionTable().length == 0) {
			return inData;
		}

		ColumnRearranger rearranger = createColumnRearranger(inData[0].getSpec(), inData[0].size(), exec);

		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], rearranger, exec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// No internals to load
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// No internals to save
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_configuration.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		// Nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		// m_configuration = new FormulasNodeConfiguration();
		m_configuration.loadSettingsInModel(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Nothing to do
	}

	/**
	 * Creates a {@link ColumnRearranger} used to determine the resulting
	 * {@link DataColumnSpec} and providing code to fill it.
	 * 
	 * @param inSpec
	 *            original input {@link DataTableSpec}
	 * @param exec
	 *            the current {@link ExecutionContext}
	 * @return {@link ColumnRearranger} used to compute the output table.
	 */
	private ColumnRearranger createColumnRearranger(DataTableSpec inSpec, long rowCount, ExecutionContext exec) {
		ColumnRearranger rearranger = new ColumnRearranger(inSpec);

		String[][] expressions = m_configuration.getExpressionTable();
		DataType[] types = m_configuration.getDataTypes();

		/*
		 * Used to replace columns and adding new columns in such a way that the column
		 * names are still unique.
		 */
		UniqueNameGenerator replaceGenerator = new UniqueNameGenerator((Set<String>) null);
		UniqueNameGenerator appendGenerator = new UniqueNameGenerator(inSpec);

		HashMap<String, Integer> columnIndexMap = new HashMap<>(inSpec.getColumnNames().length);
		for (String columnName : inSpec.getColumnNames()) {
			columnIndexMap.put(columnName, inSpec.findColumnIndex(columnName));
		}

		/*
		 * Iterate over all column names that are generated/replaced in the node and
		 * append/replace them in the current spec.
		 */
		LinkedList<String> expressionList = new LinkedList<>();
		LinkedList<DataColumnSpec> specList = new LinkedList<>();
		LinkedList<DataType> typeList = new LinkedList<>();

		DataColumnSpec[] columns = new DataColumnSpec[inSpec.getNumColumns()];

		for (int i = 0; i < inSpec.getNumColumns(); i++) {
			columns[i] = inSpec.getColumnSpec(i);
		}

		for (int i = 0; i < expressions[0].length; i++) {
			if (columnIndexMap.containsKey(expressions[0][i])) {
				/* Single cell factory used as we simply replace a column. */
				int colIndex = columnIndexMap.get(expressions[0][i]);

				rearranger.remove(colIndex);
				rearranger.insertAt(colIndex,
						new MultiExpressionCellFactory(
								new DataColumnSpec[] { replaceGenerator.newColumn(expressions[0][i], types[i]) },
								columns, new String[] { expressions[1][i] }, new DataType[] { types[i] },
								getAvailableFlowVariables(), rowCount, getLogger()));
			} else {
				/* Multi cell factory used as we may append multiple columns. */
				specList.add(appendGenerator.newColumn(expressions[0][i], types[i]));
				expressionList.add(expressions[1][i]);
				typeList.add(types[i]);
			}
		}

		if (!expressionList.isEmpty()) {
			/*
			 * Create the multi cell factory if we don't simply replace already existing
			 * columns.
			 */
			String[] expressionArray = new String[expressionList.size()];
			DataColumnSpec[] specArray = new DataColumnSpec[specList.size()];
			DataType[] typeArray = new DataType[typeList.size()];

			expressionList.toArray(expressionArray);
			specList.toArray(specArray);
			typeList.toArray(typeArray);

			rearranger.append(new MultiExpressionCellFactory(specArray, columns, expressionArray, typeArray,
					getAvailableFlowVariables(), rowCount, getLogger()));
		}

		return rearranger;
	}

}
