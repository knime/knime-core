/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   Sep 1, 2008 (wiswedel): created
 */
package org.knime.base.node.preproc.regexsplit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
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
 * 
 * @author wiswedel, University of Konstanz
 */
final class RegexSplitNodeModel extends NodeModel {

    private RegexSplitSettings m_settings;
    
    /** One input, one output. */
    public RegexSplitNodeModel() {
        super(1, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger rearranger = 
            createRearranger(inSpecs[0], new AtomicInteger());
        return new DataTableSpec[]{rearranger.createSpec()};
    }
    
    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        AtomicInteger i = new AtomicInteger();
        ColumnRearranger rearranger = createRearranger(inData[0].getSpec(), i);
        BufferedDataTable t = exec.createColumnRearrangeTable(
                inData[0], rearranger, exec);
        if (i.get() > 0) {
            setWarningMessage(i.get() + " input string(s) did not match the " 
                    + "pattern or contained more groups than expected");
        }
        return new BufferedDataTable[]{t};
    }
    
    private ColumnRearranger createRearranger(final DataTableSpec spec,
            final AtomicInteger errorCounter) 
        throws InvalidSettingsException {
        if (m_settings == null) {
            throw new InvalidSettingsException("Not configuration available.");
        }
        final int colIndex = spec.findColumnIndex(m_settings.getColumn());
        if (colIndex < 0) {
            throw new InvalidSettingsException("No such column in input table: "
                    + m_settings.getColumn());
        }
        DataColumnSpec colSpec = spec.getColumnSpec(colIndex);
        if (!colSpec.getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("Selected column does not " 
                    + "contain strings");
        }
        final Pattern p = m_settings.compile();
        int count = 0;
        String patternS = p.pattern();
        boolean isNextSpecial = false;
        boolean isPreviousAParenthesis = false;
        // count openening parentheses to get group count, ignore
        // escaped parentheses "\(" or non-capturing groups "(?"
        for (int i = 0; i < patternS.length(); i++) {
            switch (patternS.charAt(i)) {
            case '\\': 
                isNextSpecial = !isNextSpecial;
                isPreviousAParenthesis = false;
                break;
            case '(' :
                count += isNextSpecial ? 0 : 1;
                isPreviousAParenthesis = !isNextSpecial;
                isNextSpecial = false;
                break;
            case '?':
                if (isPreviousAParenthesis) {
                    count -= 1;
                }
                // no break;
            default : 
                isNextSpecial = false;
                isPreviousAParenthesis = false;
            }
        }
        final int newColCount = count;
        final DataColumnSpec[] newColSpecs = new DataColumnSpec[count];
        for (int i = 0; i < newColCount; i++) {
            String name = DataTableSpec.getUniqueColumnName(spec, "split_" + i);
            newColSpecs[i] = new DataColumnSpecCreator(
                    name, StringCell.TYPE).createSpec();
        }
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        rearranger.append(new AbstractCellFactory(newColSpecs) {
            /** {@inheritDoc} */
            @Override
            public DataCell[] getCells(final DataRow row) {
                DataCell[] result = new DataCell[newColCount];
                Arrays.fill(result, DataType.getMissingCell());
                DataCell c = row.getCell(colIndex);
                if (c.isMissing()) {
                    return result;
                }
                String s = ((StringValue)c).getStringValue();
                Matcher m = p.matcher(s);
                if (m.matches()) {
                    int max = m.groupCount();
                    if (m.groupCount() > newColCount) {
                        errorCounter.incrementAndGet();
                        max = newColCount;
                    }
                    for (int i = 0; i < max; i++) {
                        // group(0) will return the entire string and is not
                        // included in groupCount, see Matcher API for details
                        String str = m.group(i + 1);
                        if (str != null) { // null for optional groups "(...)?"
                            result[i] = new StringCell(str); 
                        }
                    }
                    return result;
                } else {
                    errorCounter.incrementAndGet();
                    return result;
                }
            }
        });
        return rearranger;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        RegexSplitSettings s = new RegexSplitSettings();
        s.loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings = new RegexSplitSettings();
        m_settings.loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_settings != null) {
            m_settings.saveSettingsTo(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}
