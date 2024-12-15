/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.workflow.NodeContainerMetadata.Link;

/**
 * AP-20810: Corrupt workflowset.meta Prevents Workflow Loading
 * https://knime-com.atlassian.net/browse/AP-20810
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public class BugAP20810_LineBreaksInWorkflowSetMetaLinkLines extends WorkflowTestCase {
    /** Loads workflow - expects it to be clean and the metadata to load properly. */
    @Test
    public void testLoadworkflow() throws Exception {
        loadAndSetWorkflow();
        final var workflowManager = getManager();
		final var metadata = workflowManager.getMetadata();
		assertFalse(workflowManager.isDirty());
		assertEquals(Optional.of("m_lauber"), metadata.getAuthor());
		assertEquals(NodeContainerMetadata.ContentType.PLAIN, metadata.getContentType());
		assertEquals(Optional.of(OffsetDateTime.parse("2019-10-11T12:00:01+02:00")),
				metadata.getCreated().map(ZonedDateTime::toOffsetDateTime));
		assertEquals(Optional.of("various methods to export data and graphics from KNIME with R\n\n"
				+ "You could toy around with R and KNIME exports either using the generic KNIME ports or just export "
				+ "from inside the R nodes into graphic files, CSV files, .RDS r files, or whole environments .rdata."),
				metadata.getDescription());
		assertEquals(List.of(
				new Link("https://forum.knime.com/t/feeding-knime-data-output-into-rstudio/15537/2?u=mlauber71",
						"forum entry"),
				new Link("https://www.andrewheiss.com/blog/2016/12/08/save-base-graphics-as-pseudo-objects-in-r/",
						"Save base graphics as pseudo-objects in R"),
				new Link("http://www.sthda.com/english/wiki/saving-data-into-r-data-format-rds-and-rdata",
						"save data as .RDS file (single file from R)")),
				metadata.getLinks());
		assertEquals(List.of("r", "KNIME", "export", "rds", "graphic"), metadata.getTags());
    }

    @AfterEach
    public void after() throws Exception {
    	closeWorkflow();
    }
}