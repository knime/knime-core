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
 *   Jun 6, 2023 (leonard.woerteler): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.metadata.MetadataVersion;
import org.knime.core.node.workflow.metadata.v10.WorkflowMetadataDocument;
import org.knime.core.util.xml.NoExternalEntityResolver;

/**
 * Represents general metadata associated with a workflow.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
public final class WorkflowMetadata extends NodeContainerMetadata {

    private WorkflowMetadata(final ContentType contentType, final ZonedDateTime lastModified,
            final ZonedDateTime created, final String author, final String description, final List<Link> links,
            final List<String> tags) {
        super(org.knime.core.node.workflow.metadata.v10.WorkflowMetadata.Factory,
            contentType, lastModified, created, author, description, links, tags);
    }

    /**
     * Parses metadata from an XML file.
     *
     * @param xmlFile file location
     * @return read metadata
     * @param version version of the metadata XML to read
     * @throws IOException if reading has failed
     * @since 5.1
     */
    public static WorkflowMetadata fromXML(final Path xmlFile, final MetadataVersion version) throws IOException {
        CheckUtils.checkArgument(version == MetadataVersion.V1_0, "Expected metadata version 1.0, found '%s'.");
        try (final var bufferedReader = Files.newBufferedReader(xmlFile, StandardCharsets.UTF_8)) {
            final var xmlOptions = new XmlOptions();
            xmlOptions.setCharacterEncoding(StandardCharsets.UTF_8.name());
            xmlOptions.setValidateStrict();
            xmlOptions.disallowDocTypeDeclaration();
            xmlOptions.setEntityResolver(NoExternalEntityResolver.getInstance());

            final var metadataElement =
                    WorkflowMetadataDocument.Factory.parse(bufferedReader, xmlOptions).getWorkflowMetadata();
            return readCommonFields(metadataElement, fluentBuilder());
        } catch (XmlException e) {
            throw new IOException("Unable to load component metadata: " + e.getMessage(), e);
        }
    }

    @Override
    XmlObject toXMLDocument(final org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata metadataXML) {
        final var document = WorkflowMetadataDocument.Factory.newInstance();
        document.setWorkflowMetadata((org.knime.core.node.workflow.metadata.v10.WorkflowMetadata) metadataXML);
        return document;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof WorkflowMetadata other && commonEquals(other));
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder(getClass().getSimpleName()).append('[');
        addCommonFields(sb);
        return sb.append(']').toString();
    }

    @Override
    public int hashCode() {
        return commonHash();
    }

    /**
     * Builder for workflow metadata.
     *
     * @return builder fluent builder
     */
    public static NeedsContentType<WorkflowMetadata> fluentBuilder() {
        return new NodeContainerMetadataBuilder<WorkflowMetadata>() {
            @Override
            public WorkflowMetadata build() {
                return new WorkflowMetadata( //
                    CheckUtils.checkArgumentNotNull(m_contentType, "Workflow metadata need a content type"), //
                    CheckUtils.checkArgumentNotNull(m_lastModified, "Workflow metadata need a last-modified date"), //
                    m_created, //
                    m_author, //
                    CheckUtils.checkArgumentNotNull(m_description, "Workflow metadata need a description"), //
                    new ArrayList<>(m_links), //
                    new ArrayList<>(m_tags));
            }
        };
    }
}
