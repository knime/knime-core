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
 *   29 Sept 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.internal.diagnostics;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

import org.knime.core.monitor.ApplicationHealth;
import org.knime.core.monitor.ExternalProcessType;
import org.knime.core.node.NodeLogger;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Collector for {@link ApplicationHealth} information.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class ApplicationHealthCollector implements Collector {

    private static final String FIELD_NAME = "applicationHealth";

    /** The singleton instance. */
    public static final ApplicationHealthCollector INSTANCE = new ApplicationHealthCollector();

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ApplicationHealthCollector.class);

    private ApplicationHealthCollector() {
        // singleton
    }

    @Override
    public boolean isEnabled(final DiagnosticInstructions instructions) {
        return instructions.applicationHealth();
    }

    @Override
    public String getJsonKey() {
        return FIELD_NAME;
    }

    @Override
    public void collect(final Instant timestamp, final DiagnosticInstructions instructions,
        final JsonGenerator generator, final Path outputDir) throws IOException {
        LOGGER.debug("Collecting application health info...");
        final var load = ApplicationHealth.getGlobalThreadPoolLoadAverages();
        generator.writeFieldName("threadPoolLoadAverages");
        generator.writeStartObject();
        generator.writeNumberField("1min", load.avg1Min());
        generator.writeNumberField("5min", load.avg5Min());
        generator.writeNumberField("15min", load.avg15Min());
        generator.writeEndObject();

        final var queue = ApplicationHealth.getGlobalThreadPoolQueuedAverages();
        generator.writeFieldName("threadPoolQueuedAverages");
        generator.writeStartObject();
        generator.writeNumberField("1min", queue.avg1Min());
        generator.writeNumberField("5min", queue.avg5Min());
        generator.writeNumberField("15min", queue.avg15Min());
        generator.writeEndObject();

        final var instances = ApplicationHealth.getInstanceCounters();
        generator.writeArrayFieldStart("instanceCounters");
        for (final var ins : instances) {
            generator.writeStartObject();
            generator.writeStringField("instance", ins.getName());
            generator.writeNumberField("count", ins.get());
            generator.writeEndObject();
        }
        generator.writeEndArray();

        generator.writeFieldName("nodeStates");
        generator.writeStartObject();
        generator.writeNumberField("executed", ApplicationHealth.getNodeStateExecutedCount());
        generator.writeNumberField("executing", ApplicationHealth.getNodeStateExecutingCount());
        generator.writeNumberField("other", ApplicationHealth.getNodeStateOtherCount());
        generator.writeEndObject();

        // RSS / PSS
        generator.writeObjectFieldStart("processBytes");
        generator.writeNumberField("knimeProcessRssBytes", ApplicationHealth.getKnimeProcessRssBytes());
        for (final var type : ExternalProcessType.values()) {
            generator.writeNumberField(type.name() + "PssBytes", ApplicationHealth.getExternalProcessesPssBytes(type));
        }
        generator.writeEndObject();
    }
}
