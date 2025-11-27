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
 *   Sep 29, 2025 (manuelhotz): created
 */
package org.knime.core.internal.diagnostics.collectors;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.knime.core.internal.diagnostics.Collector;
import org.knime.core.internal.diagnostics.DiagnosticInstructions;
import org.knime.core.node.KNIMEConstants;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * KNIME-specific information diagnostic collector.
 *
 * @since 5.8
 */
public final class KNIMEInfoCollector implements Collector {

    /** The singleton instance. */
    public static final KNIMEInfoCollector INSTANCE = new KNIMEInfoCollector();

    private KNIMEInfoCollector() {
        // Singleton
    }

    @Override
    public boolean isEnabled(final DiagnosticInstructions instructions) {
        return instructions.knimeInfo();
    }

    @Override
    public String getJsonKey() {
        return "knimeInfo";
    }

    @Override
    public void collect(final Instant timestamp, final DiagnosticInstructions instructions,
        final JsonGenerator generator, final Path outputDir) throws IOException {
        generator.writeStringField("coreVersion", KNIMEConstants.VERSION);
        generator.writeStringField("buildInfo", "%s%s %s".formatted(KNIMEConstants.BUILD,
            (KNIMEConstants.isNightlyBuild() ? " (nightly)" : ""), KNIMEConstants.BUILD_DATE));
        generator.writeStringField("installationPath", System.getProperty("osgi.install.area", ""));
        generator.writeStringField("knimeHomeDir", KNIMEConstants.getKNIMEHomeDir());

        final var knimeTempPath = KNIMEConstants.getKNIMETempDir();
        generator.writeStringField("knimeTempDir", knimeTempPath);

        final var tempPath = Paths.get(knimeTempPath).toFile();
        long freeSpace = tempPath.getFreeSpace();
        long totalSpace = tempPath.getTotalSpace();
        long usableSpace = tempPath.getUsableSpace();

        generator.writeObjectFieldStart("knimeTempDiskSpace");
        generator.writeNumberField("freeBytes", freeSpace);
        generator.writeNumberField("totalBytes", totalSpace);
        generator.writeNumberField("usableBytes", usableSpace);

        generator.writeEndObject();
    }
}
