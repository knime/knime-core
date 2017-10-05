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
 *   15.01.2015 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.ExtensionDocument.Extension;
import org.dmg.pmml.PMMLDocument;
import org.knime.base.data.statistics.Statistic;
import org.knime.base.data.statistics.StatisticCalculator;
import org.knime.base.node.preproc.pmml.missingval.handlers.DoNothingMissingCellHandlerFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * DataTable implementation that provides an iterator that fills missing cells
 * on-the-fly using MissingCellHandlers as configured in a MVSettings object.
 * @author Alexander Fillbrunn
 */
public class MissingCellReplacingDataTable implements DataTable {

    private MissingCellHandler[] m_handlers;

    private BufferedDataTable m_table;

    private DataTableSpec m_outSpec;

    private StringBuffer m_warningMessage = new StringBuffer();

    private final int[] m_lookaheads;

    private final int[] m_lookbehinds;

    /**
     * Constructor for MissingCellReplacingDataTable that
     * loads the settings for the replacements from a NodeSettings object.
     * @param inTableSpec the input table spec for the table handled by this MissingCellReplacingDataTable.
     * @param settings the settings for the missing cell replacement
     * @throws InvalidSettingsException if the settings cannot be read
     */
    public MissingCellReplacingDataTable(final DataTableSpec inTableSpec, final MVSettings settings)
            throws InvalidSettingsException {
        m_handlers = prepareHandlers(inTableSpec, settings);
        m_outSpec = createSpec();

        // Create arrays with lookahead and lookbehind size for each column
        m_lookaheads = new int[m_handlers.length];
        m_lookbehinds = new int[m_handlers.length];

        for (int i = 0; i < m_handlers.length; i++) {
            m_lookaheads[i] = m_handlers[i].getNextCellsWindowSize();
            m_lookbehinds[i] = m_handlers[i].getPreviousCellsWindowSize();
        }
    }

    /**
     * Constructor for MissingCellReplacingDataTable.
     * @param inTableSpec the input table spec for the table handled by this MissingCellReplacingDataTable.
     * @param pmmlDoc a PMML document where the missing value replacements are declared in DerivedFields
     * @throws InvalidSettingsException if the settings cannot be read
     */
    public MissingCellReplacingDataTable(final DataTableSpec inTableSpec, final PMMLDocument pmmlDoc)
            throws InvalidSettingsException {
        m_handlers = prepareHandlers(inTableSpec, pmmlDoc);
        m_outSpec = createSpec();

        // Create arrays with lookahead and lookbehind size for each column
        m_lookaheads = new int[m_handlers.length];
        m_lookbehinds = new int[m_handlers.length];

        for (int i = 0; i < m_handlers.length; i++) {
            m_lookaheads[i] = m_handlers[i].getNextCellsWindowSize();
            m_lookbehinds[i] = m_handlers[i].getPreviousCellsWindowSize();
        }
    }

    /**
     * Initializes the statistics for the handlers. Only has to be called if actual replacement should take place.
     * @param inTable the actual DataTable which this table wraps.
     * @param exec the execution context for the iteration which calculates statistics
     * @throws InvalidSettingsException if the statistics from the
     *                                  missing cell handlers are conflicting with the table specs
     * @throws CanceledExecutionException when the user cancels the execution
     */
    public void init(final BufferedDataTable inTable, final ExecutionContext exec)
                        throws InvalidSettingsException, CanceledExecutionException {
        m_table = inTable;
        // Calculate necessary statistics
        ArrayList<Statistic> statistics = new ArrayList<Statistic>();
        for (int i = 0; i < m_table.getDataTableSpec().getNumColumns(); i++) {
            Statistic s = m_handlers[i].getStatistic();
            if (s != null) {
                statistics.add(s);
            }
        }

        // Fill the statistics retrieved from the handlers
        if (statistics.size() > 0) {
            StatisticCalculator calc = new StatisticCalculator(m_table.getDataTableSpec(),
                                                                statistics.toArray(new Statistic[0]));
            String res = calc.evaluate(m_table, exec);
            if (res != null) {
                addWarningMessage(res);
            }
        }
    }

    /**
     * Called when the pass over the data is finished to collect warning messages from the missing cell handlers.
     * @return the warning messages that occurred during initialization and execution.
     */
    public String finish() {
        for (MissingCellHandler h : m_handlers) {
            String wm = h.getWarningMessage();
            if (wm != null) {
                addWarningMessage(wm);
            }
        }
        return m_warningMessage.toString();
    }

    /**
     * @return a PMML translator that inserts DerivedFields
     *          documenting the missing value replacements into a PMML document.
     */
    public PMMLTranslator getPMMLTranslator() {
        return new PMMLMissingValueReplacementTranslator(m_handlers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_outSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowIterator iterator() {
        return new MissingValueReplacingIterator();
    }

    private void addWarningMessage(final String msg) {
        if (m_warningMessage.length() > 0) {
            m_warningMessage.append("\n");
        }
        m_warningMessage.append(msg);
    }

    private MissingCellHandler[] prepareHandlers(final DataTableSpec inSpec, final MVSettings settings)
                                                                        throws InvalidSettingsException {
        MissingCellHandler[] handlers = new MissingCellHandler[inSpec.getNumColumns()];
        boolean validPMML = true;
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            MVIndividualSettings s = settings.getSettingsForColumn(inSpec.getColumnSpec(i));
            MissingCellHandler handler = s.getFactory().createHandler(inSpec.getColumnSpec(i));
            if (!s.getFactory().producesPMML4_2()) {
                validPMML = false;
            }
            handler.loadSettingsFrom(s.getSettings());

            handlers[i] = handler;
        }
        if (!validPMML) {
            addWarningMessage("The current settings use missing value handling "
                + "methods that cannot be represented in PMML 4.2");
        }
        return handlers;
    }

    /**
     * @param inTableSpec
     * @param pmmlDoc
     * @return
     * @throws InvalidSettingsException
     */
    private MissingCellHandler[] prepareHandlers(final DataTableSpec inTableSpec, final PMMLDocument pmmlDoc)
                                                                                throws InvalidSettingsException {
        MissingCellHandler[] handlers = new MissingCellHandler[inTableSpec.getNumColumns()];
        if (pmmlDoc.getPMML().getTransformationDictionary() == null
            || pmmlDoc.getPMML().getTransformationDictionary().getDerivedFieldList().size() == 0) {
            for (int i = 0; i < inTableSpec.getNumColumns(); i++) {
                handlers[i] = DoNothingMissingCellHandlerFactory.getInstance()
                                .createHandler(inTableSpec.getColumnSpec(i));
            }
            return handlers;
        }

        DerivedFieldMapper mapper = new DerivedFieldMapper(pmmlDoc);
        Map<String, DerivedField> derivedFields = new HashMap<>();
        for (DerivedField df : pmmlDoc.getPMML().getTransformationDictionary().getDerivedFieldList()) {
            String name = mapper.getColumnName(df.getName());
            derivedFields.put(name, df);
        }

        for (int i = 0; i < inTableSpec.getNumColumns(); i++) {
            DataColumnSpec spec = inTableSpec.getColumnSpec(i);
            handlers[i] = createHandlerForColumn(spec, derivedFields.get(spec.getName()));
        }
        return handlers;
    }

    private MissingCellHandler createHandlerForColumn(final DataColumnSpec spec, final DerivedField df)
                                                                        throws InvalidSettingsException {
        if (df == null) {
            return DoNothingMissingCellHandlerFactory.getInstance().createHandler(spec);
        } else {
            for (Extension ext : df.getExtensionList()) {
                if (ext.getName().equals(MissingCellHandler.CUSTOM_HANDLER_EXTENSION_NAME)) {
                    MissingCellHandler handler;
                    try {
                        handler = MissingCellHandler.fromPMMLExtension(spec, ext);
                    } catch (InvalidSettingsException e) {
                        handler = DoNothingMissingCellHandlerFactory.getInstance().createHandler(spec);
                        addWarningMessage(e.getMessage() + " Falling back to \"do nothing\" handler.");
                    }
                    return handler;
                }
            }
            if (df.getApply() != null) {
                return new PMMLApplyMissingCellHandler(spec, df);
            }
            throw new InvalidSettingsException("No valid missing value replacement found in derived field for column "
                                               + spec.getName());
        }
    }

    private DataTableSpec createSpec() {
        DataTableSpecCreator specCreator = new DataTableSpecCreator();
        for (MissingCellHandler handler : m_handlers) {
            DataColumnSpecCreator c = new DataColumnSpecCreator(handler.getColumnSpec());
            if (handler.getOutputDataType() != null) {
                c.setType(handler.getOutputDataType());
            }
            specCreator.addColumns(c.createSpec());
        }
        return specCreator.createSpec();
    }

    /**
     * An iterator that replaces missing values as it passes over a data table.
     *
     * @author Alexander Fillbrunn
     */
    private final class MissingValueReplacingIterator extends RowIterator {

        private WindowedDataTableIterator m_iter;

        private DataTableSpec m_spec;

        private boolean[] m_generatedMissing;

        private MissingValueReplacingIterator() {
            m_spec = m_table.getDataTableSpec();
            m_generatedMissing = new boolean[m_spec.getNumColumns()];
            m_iter = new WindowedDataTableIterator(m_table, m_lookaheads, m_lookbehinds);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_iter.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            DataRow row = m_iter.next();
            DataCell[] cells = new DataCell[m_spec.getNumColumns()];
            boolean removeRow = false;
            for (int i = 0; i < m_spec.getNumColumns(); i++) {
                if (row.getCell(i).isMissing()) {
                    if (removeRow) {
                        m_handlers[i].rowRemoved(row.getKey());
                    } else {
                        cells[i] = m_handlers[i].getCell(row.getKey(), m_iter.getWindowForColumn(i));
                        if (cells[i] == null) {
                            removeRow = true;
                            // Other handlers might rely on the correct order and no skipped rows (eg NextValue),
                            // so we cannot break here. Instead from now on the rowRemoved method
                            // is called instead of getCell() to avoid unnecessary computations
                        } else if (cells[i].isMissing() && !m_generatedMissing[i]) {
                            addWarningMessage("Column \"" + m_spec.getColumnNames()[i] +
                                "\" still contains missing values.");
                            m_generatedMissing[i] = true;
                        }
                    }
                } else {
                    m_handlers[i].nonMissingValueSeen(row.getKey(), m_iter.getWindowForColumn(i));
                    cells[i] = row.getCell(i);
                }
            }
            if (removeRow) {
                return null;
            }
            return new DefaultRow(row.getKey(), cells);
        }

    }
}
