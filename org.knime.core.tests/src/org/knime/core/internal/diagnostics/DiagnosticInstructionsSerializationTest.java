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
 *   Dec 13, 2025 (github-copilot): created
 */
package org.knime.core.internal.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.nio.file.Path;

import org.junit.Test;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for serialization of {@link DiagnosticInstructions}.
 */
public class DiagnosticInstructionsSerializationTest {

    /**
     * Tests serialization of DiagnosticInstructions from JSON.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testSerialization() throws Exception {
        final var objectMapper = new ObjectMapper();
        final var defaultsAsString =
            objectMapper.writer(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter().withLinefeed("\n")))
                .writeValueAsString(DiagnosticInstructions.createDefaults(Path.of("diagnostic-output")));

        final var json = """
            {
              "outputDirectory" : "diagnostic-output",
              "heapDump" : null,
              "systemInfo" : true,
              "jvmInfo" : true,
              "gcInfo" : true,
              "applicationHealth" : true,
              "knimeInfo" : true,
              "workflowManagers" : true,
              "threadDump" : true
            }
            """;

        assertEquals("Serialized JSON should match expected", defaultsAsString.trim(), json.trim());
    }

    /**
     * Tests deserialization of DiagnosticInstructions from JSON.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testDeserialization() throws Exception {
        final var objectMapper = new ObjectMapper();

        final var json = """
            {
              "outputDirectory" : "diagnostic-output",
              "heapDump" : null,
              "systemInfo" : false,
              "jvmInfo" : false,
              "gcInfo" : false,
              "applicationHealth" : false,
              "knimeInfo" : false,
              "workflowManagers" : false,
              "threadDump" : false
            }
            """;

        final var instructions = objectMapper.readValue(json, DiagnosticInstructions.class);

        assertEquals("Output directory should match", Path.of("diagnostic-output"), instructions.outputDirectory());
        assertNull("Heap dump path should be null", instructions.heapDumpPath());
        assertFalse("System info should be false", instructions.systemInfo());
        assertFalse("JVM info should be false", instructions.jvmInfo());
        assertFalse("GC info should be false", instructions.gcInfo());
        assertFalse("Application health should be false", instructions.applicationHealth());
        assertFalse("KNIME info should be false", instructions.knimeInfo());
        assertFalse("Workflow managers should be false", instructions.workflowManagers());
        assertFalse("Thread dump should be false", instructions.threadDump());
    }

}
