/*
 * ------------------------------------------------------------------------
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
 * Created on 19.03.2013 by Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Vector;

import org.apache.commons.codec.binary.Base64;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.image.png.PNGImageValue;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.web.JSONDataTableSpec.JSTypes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;


/**
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 * @since 2.9
 */
@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class JSONDataTable {

    private static final String KNIME_DATA_TABLE_JSON = "knimeDataTableJSON";

    /* serialized members */
    private JSONDataTableSpec m_spec;
    private JSONDataTableRow[] m_rows;
    private Object[][] m_extensions;

    /** Empty serialization constructor. Don't use.*/
    public JSONDataTable() {
        // do nothing
    }

    /**
     * Creates a new data table which can be serialized into a JSON string from a given BufferedDataTable.
     * @param dTable the data table to read the rows from
     * @param firstRow the first row to store (must be greater than zero)
     * @param numOfRows the number of rows to store (must be zero or more)
     * @param execMon the object listening to our progress and providing cancel functionality.
     * @throws CanceledExecutionException If the execution of the node has been cancelled.
     */
    public JSONDataTable(final DataTable dTable, final int firstRow,
            final int numOfRows, final ExecutionMonitor execMon)
            throws CanceledExecutionException {

        if (dTable == null) {
            throw new NullPointerException("Must provide non-null data table"
                    + " for DataArray");
        }
        if (firstRow < 1) {
            throw new IllegalArgumentException("Starting row must be greater"
                    + " than zero");
        }
        if (numOfRows < 0) {
            throw new IllegalArgumentException("Number of rows to read must be"
                    + " greater than or equal zero");
        }

        DataTableSpec spec = dTable.getDataTableSpec();
        int numOfColumns = spec.getNumColumns();
        DataCell[] maxValues = new DataCell[numOfColumns];
        DataCell[] minValues = new DataCell[numOfColumns];
        Object[] minJSONValues = new Object[numOfColumns];
        Object[] maxJSONValues = new Object[numOfColumns];

        // create a new list for the values - but only for native string columns
        Vector<LinkedHashSet<Object>> possValues = new Vector<LinkedHashSet<Object>>();
        possValues.setSize(numOfColumns);
        for (int c = 0; c < numOfColumns; c++) {
            if (spec.getColumnSpec(c).getType()
                    .isCompatible(NominalValue.class)) {
                possValues.set(c, new LinkedHashSet<Object>());
            }
        }

        RowIterator rIter = dTable.iterator();
        int currentRowNumber = 0;
        int numRows = 0;

        ArrayList<String> rowColorList = new ArrayList<String>();
        ArrayList<JSONDataTableRow> rowList = new ArrayList<JSONDataTableRow>();

        while ((rIter.hasNext()) && (currentRowNumber + firstRow - 1 < numOfRows)) {
            // get the next row
            DataRow row = rIter.next();
            currentRowNumber++;

            if (currentRowNumber < firstRow) {
                // skip all rows until we see the specified first row
                continue;
            }

            rowColorList.add(cssHexStringFromColor(spec.getRowColor(row)));

            String rowKey = row.getKey().getString();
            rowList.add(new JSONDataTableRow(rowKey, numOfColumns));
            numRows++;

            // add cells, check min, max values and possible values for each column
            for (int c = 0; c < numOfColumns; c++) {
                DataCell cell = row.getCell(c);

                Object cellValue;
                if (!cell.isMissing()) {
                    cellValue = getJSONCellValue(cell);
                } else {
                    cellValue = null;
                }

                rowList.get(currentRowNumber - firstRow).getData()[c] = cellValue;
                if (cellValue == null) {
                    continue;
                }

                DataValueComparator comp =
                        spec.getColumnSpec(c).getType().getComparator();

                // test the min value
                if (minValues[c] == null) {
                    minValues[c] = cell;
                    minJSONValues[c] = getJSONCellValue(cell);
                } else {
                    if (comp.compare(minValues[c], cell) > 0) {
                        minValues[c] = cell;
                        minJSONValues[c] = getJSONCellValue(cell);
                    }
                }
                // test the max value
                if (maxValues[c] == null) {
                    maxValues[c] = cell;
                    maxJSONValues[c] = getJSONCellValue(cell);
                } else {
                    if (comp.compare(maxValues[c], cell) < 0) {
                        maxValues[c] = cell;
                        maxJSONValues[c] = getJSONCellValue(cell);
                    }
                }
                // add it to the possible values if we record them for this col
                LinkedHashSet<Object> possVals = possValues.get(c);
                if (possVals != null) {
                    // non-string cols have a null list and will be skipped here
                    possVals.add(getJSONCellValue(cell));
                }
            }
        }

        // TODO: Add extensions (color, shape, size, inclusion, selection, hiliting, ...)
        Object[][] extensionArray = null;

        JSONDataTableSpec jsonTableSpec = new JSONDataTableSpec(spec, numRows);
        jsonTableSpec.setMinValues(minJSONValues);
        jsonTableSpec.setMaxValues(maxJSONValues);
        jsonTableSpec.setPossibleValues(possValues);

        setSpec(jsonTableSpec);
        getSpec().setRowColorValues(rowColorList.toArray(new String[0]));
        setRows(rowList.toArray(new JSONDataTableRow[0]));
        setExtensions(extensionArray);

    }

    /**
     * @param rowColor
     * @return
     */
    private String cssHexStringFromColor(final ColorAttr colorAttr) {
        //get color value, omit alpha
        int colorValue = colorAttr.getColor().getRGB() & 0xFFFFFF;
        //convert to CSS hex color string
        return "#" + Integer.toHexString(colorValue).toUpperCase();
    }

    private Object getJSONCellValue(final DataCell cell) {
        JSTypes jsType = JSONDataTableSpec.getJSONType(cell.getType());
        switch (jsType) {
            case BOOLEAN:
                return ((BooleanValue)cell).getBooleanValue();
            case NUMBER:
                return ((DoubleValue)cell).getDoubleValue();
            case STRING:
                return ((StringValue)cell).getStringValue();
            case PNG:
                return new String(Base64.encodeBase64(((PNGImageValue)cell).getImageContent().getByteArray()));
            default:
                return null;
        }
    }

    private Object[][] getJSONDataArray(final ArrayList<Object[]> dataArray, final int numCols) {
        Object[][] jsonData = new Object[dataArray.size()][numCols];
        for (int i = 0; i < jsonData.length; i++) {
            jsonData[i] = dataArray.get(i);
        }
        return jsonData;
    }

    public JSONDataTableSpec getSpec() {
        return m_spec;
    }

    public void setSpec(final JSONDataTableSpec spec) {
        m_spec = spec;
    }

    /**
     * @since 2.10
     */
    public JSONDataTableRow[] getRows() {
        return m_rows;
    }

    /**
     * @since 2.10
     */
    public void setRows(final JSONDataTableRow[] rows) {
        m_rows = rows;
    }

    public Object[][] getExtensions() {
        return m_extensions;
    }

    public void setExtensions(final Object[][] extensions) {
        m_extensions = extensions;
    }

    /**
     *
     * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
     * @since 2.10
     */
    @JsonAutoDetect
    public static class JSONDataTableRow {

        private String m_rowKey;

        private Object[] m_data;

        /** Empty serialization constructor. Don't use.*/
        public JSONDataTableRow() { }

        public JSONDataTableRow(final String rowKey, final int numColumns) {
            m_rowKey = rowKey;
            m_data = new Object[numColumns];
        }

        public JSONDataTableRow(final String rowKey, final Object[] data) {
            m_rowKey = rowKey;
            m_data = data;
        }

        /**
         * @return the rowKey
         */
        public String getRowKey() {
            return m_rowKey;
        }

        /**
         * @param rowKey the rowKey to set
         */
        public void setRowKey(final String rowKey) {
            m_rowKey = rowKey;
        }

        /**
         * @return the data
         */
        public Object[] getData() {
            return m_data;
        }

        /**
         * @param data the data to set
         * @since 2.10
         */
        public void setData(final Object[] data) {
            m_data = data;
        }
    }

    /**
     * @param settings the settings to load from
     * @since 2.10
     */
    @JsonIgnore
    public void saveJSONToNodeSettings(final NodeSettingsWO settings) {
        ObjectMapper mapper = new ObjectMapper();
        String tableString = null;
        try {
            tableString = mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) { /*do nothing*/ }
        settings.addString(KNIME_DATA_TABLE_JSON, tableString);
    }

    /**
     * Loads a table from the settings given. If any errors occur null is returned.
     * @param settings the settings to load from
     * @return the table
     * @since 2.10
     */
    @JsonIgnore
    public static JSONDataTable loadFromNodeSettings(final NodeSettingsRO settings) {
        String tableString = settings.getString(KNIME_DATA_TABLE_JSON, null);
        if (tableString == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JSONDataTable table = new JSONDataTable();
        ObjectReader reader = mapper.readerForUpdating(table);
        try {
            reader.readValue(tableString);
            return table;
        } catch (IOException e) {
            return null;
        }
    }
}
