/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 22, 2026 (chaubold): created
 */
package org.knime.core.node.agentic.tool;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * NodeModel for MCP Tool Caller node.
 * 
 * Executes MCP (Model Context Protocol) tools by calling remote MCP servers via JSON-RPC 2.0.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
public final class MCPToolCallerNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MCPToolCallerNodeModel.class);

    static final String CFG_ROW_ID = "row_id";
    static final String CFG_PARAMETERS = "parameters";

    private final SettingsModelInteger m_rowId = createRowIdModel();
    private final SettingsModelString m_parameters = createParametersModel();

    private final HttpClient m_httpClient;

    /**
     * Constructor for MCPToolCallerNodeModel.
     */
    protected MCPToolCallerNodeModel() {
        super(1, 1); // 1 input port (tool table), 1 output port (result table)
        m_httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }

    static SettingsModelInteger createRowIdModel() {
        return new SettingsModelInteger(CFG_ROW_ID, 0);
    }

    static SettingsModelString createParametersModel() {
        return new SettingsModelString(CFG_PARAMETERS, "{}");
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // Validate that input table has tool column
        var inSpec = inSpecs[0];
        if (inSpec == null) {
            throw new InvalidSettingsException("Input table specification is not available.");
        }

        // Check if there's at least one ToolCell column
        boolean hasToolColumn = false;
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            var colSpec = inSpec.getColumnSpec(i);
            if (colSpec.getType().isCompatible(ToolValue.class)) {
                hasToolColumn = true;
                break;
            }
        }

        if (!hasToolColumn) {
            throw new InvalidSettingsException("Input table must contain at least one Tool column.");
        }

        // Validate parameters JSON format
        var parametersStr = m_parameters.getStringValue();
        try {
            parseParametersJson(parametersStr);
        } catch (Exception e) {
            throw new InvalidSettingsException("Parameters must be valid JSON: " + e.getMessage(), e);
        }

        return new DataTableSpec[]{createOutputSpec()};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        var toolTable = inData[0];
        var rowId = m_rowId.getIntValue();

        // Get the selected tool
        ToolCell toolCell = extractToolCell(toolTable, rowId);

        // Validate it's an MCP tool
        if (toolCell.getToolType() != ToolType.MCP) {
            throw new InvalidSettingsException(
                "Selected tool is not an MCP tool. Tool type: " + toolCell.getToolType());
        }

        // Parse parameters
        var parameters = parseParametersJson(m_parameters.getStringValue());

        // Execute the MCP tool
        LOGGER.info("Calling MCP tool '" + toolCell.getName() + "' at server: " + toolCell.getServerUri());
        var result = executeMCPTool(toolCell, parameters, exec);

        // Create output table
        var container = exec.createDataContainer(createOutputSpec());
        container.addRowToTable(new DefaultRow(new RowKey("Result"), new StringCell(result)));
        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    private ToolCell extractToolCell(final BufferedDataTable toolTable, final int rowIndex)
        throws InvalidSettingsException {

        // Find first tool column
        int toolColumnIndex = -1;
        var spec = toolTable.getDataTableSpec();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (spec.getColumnSpec(i).getType().isCompatible(ToolValue.class)) {
                toolColumnIndex = i;
                break;
            }
        }

        if (toolColumnIndex < 0) {
            throw new InvalidSettingsException("No tool column found in input table.");
        }

        // Get the row at the specified index
        int currentIndex = 0;
        for (DataRow row : toolTable) {
            if (currentIndex == rowIndex) {
                DataCell cell = row.getCell(toolColumnIndex);
                if (cell.isMissing()) {
                    throw new InvalidSettingsException("Selected row contains a missing tool value.");
                }
                if (!(cell instanceof ToolCell)) {
                    throw new InvalidSettingsException(
                        "Selected cell is not a ToolCell. Type: " + cell.getClass().getName());
                }
                return (ToolCell)cell;
            }
            currentIndex++;
        }

        throw new InvalidSettingsException(
            "Row index " + rowIndex + " not found in table with " + currentIndex + " rows.");
    }

    private JsonObject parseParametersJson(final String parametersStr) throws InvalidSettingsException {
        try (JsonReader reader = Json.createReader(new java.io.StringReader(parametersStr))) {
            JsonValue value = reader.readValue();
            if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new InvalidSettingsException("Parameters must be a JSON object.");
            }
            return value.asJsonObject();
        } catch (Exception e) {
            throw new InvalidSettingsException("Failed to parse parameters JSON: " + e.getMessage(), e);
        }
    }

    private String executeMCPTool(final ToolCell toolCell, final JsonObject parameters, final ExecutionContext exec)
        throws Exception {

        // Create JSON-RPC 2.0 request
        var requestId = UUID.randomUUID().toString();
        JsonObjectBuilder requestBuilder = Json.createObjectBuilder()
            .add("jsonrpc", "2.0")
            .add("id", requestId)
            .add("method", "tools/call")
            .add("params", Json.createObjectBuilder()
                .add("name", toolCell.getToolName())
                .add("arguments", parameters));

        var requestJson = requestBuilder.build().toString();

        LOGGER.debug("MCP JSON-RPC request: " + requestJson);

        // Create HTTP request
        var httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(toolCell.getServerUri()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
            .build();

        // Send request
        exec.setMessage("Calling MCP server...");
        HttpResponse<String> httpResponse = m_httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Check HTTP status
        if (httpResponse.statusCode() != 200) {
            throw new IOException("MCP server returned HTTP " + httpResponse.statusCode() + ": "
                + httpResponse.body());
        }

        // Parse JSON-RPC response
        try (JsonReader reader = Json.createReader(new java.io.StringReader(httpResponse.body()))) {
            JsonObject responseJson = reader.readObject();

            // Check for JSON-RPC error
            if (responseJson.containsKey("error")) {
                var error = responseJson.getJsonObject("error");
                throw new IOException("MCP tool execution failed: " + error.toString());
            }

            // Extract result
            if (!responseJson.containsKey("result")) {
                throw new IOException("MCP response missing 'result' field");
            }

            var result = responseJson.get("result");
            return result.toString();

        } catch (Exception e) {
            throw new IOException("Failed to parse MCP response: " + e.getMessage(), e);
        }
    }

    private static DataTableSpec createOutputSpec() {
        return new DataTableSpec(new DataColumnSpecCreator("Result", StringCell.TYPE).createSpec());
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No internals to load
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No internals to save
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rowId.saveSettingsTo(settings);
        m_parameters.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowId.loadSettingsFrom(settings);
        m_parameters.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowId.validateSettings(settings);
        m_parameters.validateSettings(settings);
    }

    @Override
    protected void reset() {
        // Nothing to reset
    }
}
