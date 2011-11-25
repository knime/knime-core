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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.06.2007 (thor): created
 */
package org.knime.base.node.preproc.stringreplacer;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model for the string replacer node that does the work.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class StringReplacerNodeModel extends NodeModel {
    private final StringReplacerSettings m_settings =
            new StringReplacerSettings();

    /**
     * Creates a new StringReplacerNodeModel.
     */
    public StringReplacerNodeModel() {
        super(1, 1);
    }

    /**
     * Creates the column rearranger that computes the new cells.
     *
     * @param spec the spec of the input table
     * @param p the pattern that should be used for finding matches
     * @return a column rearranger
     */
    private ColumnRearranger createRearranger(final DataTableSpec spec,
            final Pattern p) throws InvalidSettingsException {
        DataColumnSpec colSpec;
        if (m_settings.createNewColumn()) {
            colSpec = new DataColumnSpecCreator(m_settings.newColumnName(),
                            StringCell.TYPE).createSpec();
        } else {
            colSpec = new DataColumnSpecCreator(m_settings.columnName(),
                    StringCell.TYPE).createSpec();
        }

        final int index = spec.findColumnIndex(m_settings.columnName());
        SingleCellFactory cf = new SingleCellFactory(colSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(index);
                if (cell.isMissing()) {
                    return cell;
                }

                final String stringValue = ((StringValue)cell).getStringValue();
                Matcher m = p.matcher(stringValue);
                if (m_settings.replaceAllOccurrences()) {
                    return new StringCell(m
                            .replaceAll(m_settings.replacement()));
                } else if (m.matches()) {
                    return new StringCell(m_settings.replacement());
                } else {
                    return new StringCell(stringValue);
                }
            }
        };

        ColumnRearranger crea = new ColumnRearranger(spec);
        if (m_settings.createNewColumn()) {
            if (spec.containsName(m_settings.newColumnName())) {
                throw new InvalidSettingsException("Duplicate column name: "
                        + m_settings.newColumnName());
            }
            crea.append(cf);
        } else {
            crea.replace(cf, m_settings.columnName());
        }

        return crea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (inSpecs[0].findColumnIndex(m_settings.columnName()) == -1) {
            throw new InvalidSettingsException("Selected column '"
                    + m_settings.columnName()
                    + "' does not exist in input table");
        }

        ColumnRearranger crea = createRearranger(inSpecs[0], null);
        return new DataTableSpec[]{crea.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        exec.setMessage("Searching & Replacing");
        String regex = WildcardMatcher.wildcardToRegex(m_settings.pattern());
        // support for \n and international characters
        int flags = Pattern.DOTALL | Pattern.MULTILINE;
        if (!m_settings.caseSensitive()) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        Pattern p = Pattern.compile(regex, flags);

        ColumnRearranger crea =
                createRearranger(inData[0].getDataTableSpec(), p);

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], crea, exec)};
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        StringReplacerSettings s = new StringReplacerSettings();
        s.loadSettings(settings);

        if (s.createNewColumn() && (s.newColumnName() == null
                || s.newColumnName().trim().length() == 0)) {
            throw new InvalidSettingsException(
                    "No name for the new column given");
        }
        if (s.columnName() == null) {
            throw new InvalidSettingsException("No column selected");
        }
        if (s.pattern() == null || s.pattern().length() == 0) {
            throw new InvalidSettingsException("No pattern given");
        }
        if (s.replacement() == null) {
            throw new InvalidSettingsException("No replacement string given");
        }

        if (s.replaceAllOccurrences() && s.pattern().contains("*")) {
            throw new InvalidSettingsException(
                    "'*' is not allowed when all occurrences of the "
                            + "pattern should be replaced");
        }
    }
}
