/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   15.10.2014 (tibuch): created
 */
package org.knime.base.node.io.fixedwidthfr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Tim-Oliver Buchholz
 */
public class FixedWidthFRSettings {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FixedWidthFRSettings.class);

    private static final String CFGKEY_COLPROPS = "ColumnProperties";

    private static final String CFGKEY_HASCOLHEADER = "HasColumnHeader";

    private static final String CFGKEY_HASROWHEADER = "HasRowHeader";

    private static final String CFGKEY_NUMBEROFCOLUMNS = "NumberOfColumns";

    /**
     * configuration key for file location URL.
     */
    static final String CFGKEY_URL = "FileLocationURL";

    private URL m_fileLocation;

    private List<FixedWidthColProperty> m_colProperties;

    private int m_numberOfColumns;

    private boolean m_hasRowHeader;

    private boolean m_hasColHeader;

    /**
     * A new default fixed width file reader settings object.
     */
    FixedWidthFRSettings() {

        m_fileLocation = null;

        m_colProperties = new ArrayList<FixedWidthColProperty>();

        m_colProperties.add(new FixedWidthColProperty("remaining characters", StringCell.TYPE, Integer.MAX_VALUE, true,
            null, null));

        m_numberOfColumns = m_colProperties.size();

        m_hasRowHeader = false;
    }

    /**
     * @param clonee the fixed width file reader settings object of which we create a copy
     */
    public FixedWidthFRSettings(final FixedWidthFRSettings clonee) {
        m_fileLocation = clonee.m_fileLocation;

        m_colProperties = new ArrayList<FixedWidthColProperty>();
        for (FixedWidthColProperty f : clonee.m_colProperties) {
            m_colProperties.add(new FixedWidthColProperty(f));
        }

        m_numberOfColumns = clonee.m_numberOfColumns;

        m_hasRowHeader = clonee.getHasRowHeader();

        m_hasColHeader = clonee.getHasColHeaders();
    }

    /**
     * create a fixed width file reader settings object from a NodeSettingsRO object.
     *
     * @param cfg the node settings object with all stored settings
     * @throws InvalidSettingsException the exception thrown if the settings aren't valid
     */
    public FixedWidthFRSettings(final NodeSettingsRO cfg) throws InvalidSettingsException {
        if (cfg != null) {
            try {
                String u = cfg.getString(CFGKEY_URL);
                URL fileLocation;
                if (u != null) {
                    fileLocation = new URL(u);
                } else {
                    fileLocation = null;
                }
                setFileLocation(fileLocation);
            } catch (MalformedURLException mfue) {
                throw new IllegalArgumentException("Cannot create URL of data file from '" + cfg.getString(CFGKEY_URL)
                    + "'in filereader config", mfue);
            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException("Illegal config object for " + "file reader settings! Key '"
                    + CFGKEY_URL + "' missing!", ice);
            }

            try {
                m_numberOfColumns = cfg.getInt(CFGKEY_NUMBEROFCOLUMNS);
            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException("Illegal config object for " + "file reader settings! Key '"
                    + CFGKEY_NUMBEROFCOLUMNS + "' missing!", ice);
            }

            try {
                m_hasColHeader = cfg.getBoolean(CFGKEY_HASCOLHEADER);
            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException("Illegal config object for " + "file reader settings! Key '"
                    + CFGKEY_HASCOLHEADER + "' missing!", ice);
            }

            try {
                m_hasRowHeader = cfg.getBoolean(CFGKEY_HASROWHEADER);
            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException("Illegal config object for " + "file reader settings! Key '"
                    + CFGKEY_HASROWHEADER + "' missing!", ice);
            }

            readColumnPropsFromConfig(cfg.getNodeSettings(CFGKEY_COLPROPS));

        }

    }

    /**
     * @param cfg the node settings object to save the config to
     */
    public void saveToConfiguration(final NodeSettingsWO cfg) {

        if (cfg == null) {
            throw new NullPointerException("Can't save fixed width file reader settings into null config.");
        }

        if (m_fileLocation != null) {
            cfg.addString(CFGKEY_URL, m_fileLocation.toString());
        } else {
            cfg.addString(CFGKEY_URL, null);
        }
        cfg.addInt(CFGKEY_NUMBEROFCOLUMNS, m_numberOfColumns);
        cfg.addBoolean(CFGKEY_HASROWHEADER, m_hasRowHeader);
        cfg.addBoolean(CFGKEY_HASCOLHEADER, m_hasColHeader);

        saveColumnPropsToConfig(cfg.addNodeSettings(CFGKEY_COLPROPS));

    }

    // save the colProperties array to the nodeSettingsWO object
    private void saveColumnPropsToConfig(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't store column properties in null config.");
        }

        for (int i = 0; i < m_colProperties.size(); i++) {
            FixedWidthColProperty cProp = m_colProperties.get(i);
            if (cProp != null) {
                cProp.saveToConfiguration(cfg.addNodeSettings("" + i));
            }
        }
    }

    // rebuild the colProperties array from a NodeSettingsRO object
    private void readColumnPropsFromConfig(final NodeSettingsRO cfg) throws InvalidSettingsException {

        if (cfg == null) {
            throw new NullPointerException("Can't read column props from null config.");
        }

        int len = cfg.keySet().size();
        m_colProperties = new ArrayList<FixedWidthColProperty>(len);
        for (int i = 0; i < len; i++) {
            m_colProperties.add(null);
        }

        for (String key : cfg.keySet()) {
            int pos = -1;
            try {
                pos = Integer.parseInt(key);
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Keyword for column property must be a integer number (got '"
                    + key + "').");
            }
            if ((pos < 0) || (pos >= len)) {
                throw new InvalidSettingsException("Expecting column property key between 0 and " + (len - 1)
                    + " (got " + pos + ").");
            }
            if (m_colProperties.get(pos) != null) {
                throw new InvalidSettingsException("Properties for column number " + pos
                    + " are specified twice in the conf object.");
            }

            m_colProperties.set(pos, new FixedWidthColProperty(cfg.getNodeSettings(key)));
        }
    }

    /**
     * @param location the URL of the file
     */
    public void setFileLocation(final URL location) {
        m_fileLocation = location;
    }

    /**
     *
     */
    public void reset() {
        m_colProperties = new Vector<FixedWidthColProperty>();

        m_colProperties.add(new FixedWidthColProperty("remaining characters", StringCell.TYPE, Integer.MAX_VALUE, true,
            null, null));

        m_numberOfColumns = m_colProperties.size();

        m_hasRowHeader = false;

        m_hasColHeader = false;
    }

    /**
     * checks settings.
     *
     * @throws InvalidSettingsException exception thrown if settings are invalid
     */
    public void checkSettings() throws InvalidSettingsException {
        CheckUtils.checkSetting(m_fileLocation != null, "No location set");

        if (m_fileLocation != null) {
            try {
                BufferedFileReader f = createNewInputReader();
                f.close();
            } catch (IOException ioe) {
                throw new InvalidSettingsException("Can't read from file '" + m_fileLocation + "'.");
            } catch (NullPointerException npe) {
                LOGGER.error("Filelocation is null.", npe);
            }
        }
        if (getNumberOfIncludedColumns() > getNumberOfColumns()) {
            throw new InvalidSettingsException(
                "Looks like an internal error. More columns included than actual columns available.");
        }

        if (getNumberOfColumns() < 1) {
            throw new InvalidSettingsException("No columns configured.");
        }
    }

    /**
     * @return the location of the file (URL)
     */
    public URL getFileLocation() {
        return m_fileLocation;
    }

    /**
     * @return a new BufferedFileReader with the m_fileLocation as source
     * @throws IOException thrown by the BufferedFileReader
     */
    public BufferedFileReader createNewInputReader() throws IOException {
        return BufferedFileReader.createNewReader(m_fileLocation);
    }

    /**
     * @return number of included columns (included in the preview and output table)
     */
    public int getNumberOfIncludedColumns() {
        int r = 0;
        for (int i = 0; i < m_colProperties.size(); i++) {
            if (m_colProperties.get(i).getInclude()) {
                r++;
            }
        }
        return r;
    }

    /**
     * @return total number of columns (included ones and not included ones)
     */
    public int getNumberOfColumns() {
        return m_colProperties.size();
    }

    /**
     * @return all column properties
     */
    public List<FixedWidthColProperty> getColProperties() {
        return m_colProperties;
    }

    /**
     * @param index position in the column properties vector
     * @return the column property at this position
     */
    public FixedWidthColProperty getColPropertyAt(final int index) {
        return m_colProperties.get(index);
    }

    /**
     * @return an array with the missing value patterns from all included columns. Note: The length of this array is
     *         equal to getNumberOfIncludedColumns
     */
    public String[] getMissingValuePatterns() {
        String[] result = new String[getNumberOfIncludedColumns()];
        int r = 0;
        for (int i = 0; i < m_colProperties.size(); i++) {
            if (m_colProperties.get(i).getInclude()) {
                result[r++] = m_colProperties.get(i).getMissingValuePattern();
            }
        }
        return result;
    }

    /**
     * @return an array with the format parameters from all included columns. Note: The length of this array is
     *         equal to getNumberOfIncludedColumns
     */
    public String[] getFormatParameters() {
        String[] result = new String[getNumberOfIncludedColumns()];
        int r = 0;
        for (int i = 0; i < m_colProperties.size(); i++) {
            if (m_colProperties.get(i).getInclude()) {
                result[r++] = m_colProperties.get(i).getFormatParameter().orElse(null);
            }
        }
        return result;
    }


    /**
     * @return an array with all column widths also not included columns
     */
    public int[] getColWidths() {
        int[] result = new int[getNumberOfColumns()];

        for (int j = 0; j < getNumberOfColumns(); j++) {
            result[j] = getColPropertyAt(j).getColWidth();
        }
        return result;
    }

    /**
     * @return a tableSpec of all included columns
     */
    public DataTableSpec createDataTableSpec() {
        DataColumnSpec[] colSpecs = new DataColumnSpec[getNumberOfIncludedColumns()];
        int j = 0;

        for (int i = 0; i < getNumberOfColumns(); i++) {
            if (m_colProperties.get(i).getInclude()) {
                colSpecs[j++] = m_colProperties.get(i).getColSpec();
            }
        }
        return new DataTableSpec(colSpecs);
    }

    /**
     * @param newColumn the column to add
     * @param i the position to add the column
     */
    public void insertNewColAt(final FixedWidthColProperty newColumn, final int i) {
        m_colProperties.add(i, newColumn);
        m_numberOfColumns++;
    }

    /**
     * @param i the index of the column to remove
     */
    public void removeColAt(final int i) {
        m_colProperties.remove(i);
        m_numberOfColumns--;

    }

    /**
     * @return an array of all columns with the include parameter
     */
    public boolean[] getIncludes() {
        boolean[] result = new boolean[getNumberOfColumns()];
        for (int i = 0; i < result.length; i++) {
            result[i] = getColPropertyAt(i).getInclude();
        }
        return result;
    }

    /**
     * @return the hasRowHeader
     */
    public boolean getHasRowHeader() {
        return m_hasRowHeader;
    }

    /**
     * @param hasRowHeader the hasRowHeader to set
     */
    public void setHasRowHeader(final boolean hasRowHeader) {

        m_hasRowHeader = hasRowHeader;
    }

    /**
     * @param selected the boolean to set
     */
    public void setHasColHeader(final boolean selected) {
        m_hasColHeader = selected;

    }

    /**
     * @return file contains column headers
     */
    public boolean getHasColHeaders() {
        return m_hasColHeader;
    }

    /**
     * @param idx the index to map from all columns (included and excluded) to only included
     * @return the index for the included array
     */
    public int getColIdxIncluded(final int idx) {
        int index = idx;
        int i = 0;
        while (index >= 0) {
            if (m_colProperties.get(i++).getInclude()) {
                index--;
            }
        }
        i--;

        return i;
    }
}
