/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Jun 16, 2007 (wiswedel): created
 */
package org.knime.base.node.preproc.stringreplacer.dict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CellFactory;
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
 * NodeModel for Search & Replace (Dictionary) node.
 * @author wiswedel, University of Konstanz
 */
final class SearchReplaceDictNodeModel extends NodeModel {
    
    /** Config key for dictionary location. */
    static final String CFG_DICT_LOCATION = "dictionary_location";
    /** Config key for target column selection. */
    static final String CFG_TARGET_COLUMN = "target_column";
    /** Config key for new appended column (if any). */
    static final String CFG_APPEND_COLUMN = "appended_column";
    /** Config key for delimiter in dictionary. */
    static final String CFG_DELIMITER_IN_DICT = "delimiter_in_dict";
    

    private String m_dictFileURLString;
    private String m_targetColumnName;
    private String m_newColumnName;
    private char m_delimInDictCharacter;

    /** temporarily used during execute. */
    private HashMap<String, String> m_replacementMap;

    /** One input, one output. */
    public SearchReplaceDictNodeModel() {
        super(1, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_dictFileURLString == null) {
            throw new InvalidSettingsException("No settings available");
        }
        File dictFile = new File(m_dictFileURLString);
        if (!dictFile.isFile()) {
            throw new InvalidSettingsException(
                    "No file at \"" + m_dictFileURLString + "\"");
        }
        if (!inSpecs[0].containsName(m_targetColumnName)) {
            throw new InvalidSettingsException("No such column \"" 
                    + m_targetColumnName + "\" in input table");
        }
        if (m_newColumnName != null 
                && inSpecs[0].containsName(m_newColumnName)) {
            throw new InvalidSettingsException("Column \"" + m_newColumnName 
                    + "\" already exists");
        }
        ColumnRearranger result = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{result.createSpec()};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        exec.setMessage("Reading dictionary");
        ExecutionMonitor subExec = exec.createSubProgress(0.2);
        m_replacementMap = readDictionary(subExec);
        exec.setMessage("Searching & Replacing");
        DataTableSpec spec = inData[0].getDataTableSpec();
        ColumnRearranger rearranger = createColumnRearranger(spec);
        BufferedDataTable result = exec.createColumnRearrangeTable(
                inData[0], rearranger, exec.createSubProgress(0.8));
        m_replacementMap = null;
        return new BufferedDataTable[]{result};
    }
    
    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) {
        ColumnRearranger result = new ColumnRearranger(spec);
        final int targetColIndex = spec.findColumnIndex(m_targetColumnName);
        DataColumnSpecCreator newColCreator;
        if (m_newColumnName == null) {
            DataColumnSpec old = spec.getColumnSpec(m_targetColumnName);
            newColCreator = new DataColumnSpecCreator(old);
            newColCreator.setType(StringCell.TYPE);
            newColCreator.setDomain(null);
        } else {
            newColCreator = new DataColumnSpecCreator(
                    m_newColumnName, StringCell.TYPE);
        }
        CellFactory amendedCol = new SingleCellFactory(
                newColCreator.createSpec()) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell c = row.getCell(targetColIndex);
                if (c.isMissing()) {
                    return c;
                }
                String cellContent = c.toString();
                String replacement = m_replacementMap.get(cellContent);
                if (replacement != null) {
                    return new StringCell(replacement);
                } else {
                    // do not return original cell (maybe not compatible to
                    // StringCell.TYPE!)
                    return new StringCell(cellContent);
                }
            }
        };
        if (m_newColumnName != null) {
            result.append(amendedCol);
        } else {
            result.replace(amendedCol, targetColIndex);
        }
        return result;
    }
    
    private HashMap<String, String> readDictionary(final ExecutionMonitor exec)
        throws IOException {
        File f = new File(m_dictFileURLString);
        BufferedReader reader = new BufferedReader(new FileReader(f));
        HashMap<String, String> result = new HashMap<String, String>();
        String line;
        final long size = f.length();
        long prog = 0;
        try {
            while ((line = reader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, 
                        Character.toString(m_delimInDictCharacter));
                if (!tokenizer.hasMoreTokens()) {
                    continue;
                }
                String value = trimIfNecessary(tokenizer.nextToken());
                while (tokenizer.hasMoreTokens()) {
                    String key = trimIfNecessary(tokenizer.nextToken());
                    result.put(key, value);
                }
                // ignores line breaks and such, hope it's ok
                prog += line.length(); 
                exec.setProgress(prog / size, 
                        "Read dictionary entry for value \"" + value + "\"");
            }
        } finally {
            reader.close();
        }
        return result;
    }
    
    /** Removes leading and trailing white spaces. If the argument consists
     * only of white spaces, it will be returned as-is. */
    private static String trimIfNecessary(final String arg) {
        String trimmed = arg.trim();
        if (trimmed.length() == 0) {
            return arg;
        }
        return trimmed;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_targetColumnName = settings.getString(CFG_TARGET_COLUMN);
        m_newColumnName = settings.getString(CFG_APPEND_COLUMN);
        m_dictFileURLString = settings.getString(CFG_DICT_LOCATION);
        m_delimInDictCharacter = settings.getChar(CFG_DELIMITER_IN_DICT);
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_replacementMap = null;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_targetColumnName != null) {
            settings.addString(CFG_TARGET_COLUMN, m_targetColumnName);
            settings.addString(CFG_APPEND_COLUMN, m_newColumnName);
            settings.addString(CFG_DICT_LOCATION, m_dictFileURLString);
            settings.addChar(CFG_DELIMITER_IN_DICT, m_delimInDictCharacter);
        }

    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(CFG_TARGET_COLUMN);
        String dictURL = settings.getString(CFG_DICT_LOCATION);
        if (dictURL == null) {
            throw new InvalidSettingsException("No dictionary file specified");
        }
        String newCol = settings.getString(CFG_APPEND_COLUMN);
        if (newCol != null && newCol.trim().length() == 0) {
            throw new InvalidSettingsException("New column name is empty");
        }
        char delim = settings.getChar(CFG_DELIMITER_IN_DICT);
        if (delim < ' ' && delim != '\t') {
            throw new InvalidSettingsException("Can't use '" + delim 
                    + "' (hex code " + Integer.toHexString(delim) 
                    + ") as delimiter");
        }
    }
    

}
