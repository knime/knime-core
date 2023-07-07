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
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.schema.DocumentFactory;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata.ContentType.Enum;

/**
 * Represents general metadata associated with a node container (workflow or component).
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
public abstract class NodeContainerMetadata {

    private static final XmlOptions SAVE_OPTIONS = new XmlOptions();
    static {
        SAVE_OPTIONS.setCharacterEncoding(StandardCharsets.UTF_8.name());
        SAVE_OPTIONS.setUseDefaultNamespace();
        SAVE_OPTIONS.setSavePrettyPrint();
        SAVE_OPTIONS.setSavePrettyPrintIndent(4);
        SAVE_OPTIONS.setValidateStrict();
    }

    /** Content type of descriptions. */
    public enum ContentType {
        /** Plain text. */
        PLAIN(org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata.ContentType.TEXT_PLAIN),
        /** HTML formatted text. */
        HTML(org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata.ContentType.TEXT_HTML);

        final Enum m_xmlType;

        ContentType(final Enum xmlType) {
            m_xmlType = xmlType;
        }
    }


    /**
     * Link to external resources.
     *
     * @param url URL of the link
     * @param text text of the link
     */
    public record Link(String url, String text) {}

    final ContentType m_contentType;

    final ZonedDateTime m_created;

    final ZonedDateTime m_lastModified;

    final String m_author;

    final String m_description;

    final List<Link> m_links;

    final List<String> m_tags;

    private final DocumentFactory<? extends org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata>
            m_elementFactory;

    NodeContainerMetadata( // NOSONAR
            final DocumentFactory<? extends org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata> elementFactory, // NOSONAR
            final ContentType contentType, final ZonedDateTime lastModified, final ZonedDateTime created,
            final String author, final String description, final List<Link> links, final List<String> tags) {
        m_contentType = contentType;
        m_elementFactory = elementFactory;
        m_created = created;
        m_lastModified = lastModified;
        m_author = author;
        m_description = description;
        m_links = links;
        m_tags = tags;
    }

    /**
     * @return the author's name
     */
    public final Optional<String> getAuthor() {
        return Optional.ofNullable(m_author);
    }

    /**
     * @return creation time stamp
     */
    public final Optional<ZonedDateTime> getCreated() {
        return Optional.ofNullable(m_created);
    }

    /**
     * @return last-modified time stamp
     */
    public final ZonedDateTime getLastModified() {
        return m_lastModified;
    }

    /**
     * @return the description
     */
    public final Optional<String> getDescription() {
        return Optional.of(m_description);
    }

    /**
     * @return the content type of this metadata's rich-text fields
     */
    public ContentType getContentType() {
        return m_contentType;
    }

    /**
     * @return list of tags
     */
    public final List<String> getTags() {
        return Collections.unmodifiableList(m_tags);
    }

    /**
     * @return links represented as (URL, description) pairs
     */
    public final List<Link> getLinks() {
        return Collections.unmodifiableList(m_links);
    }

    @Override
    public abstract boolean equals(Object obj);

    boolean commonEquals(final NodeContainerMetadata other) {
        return Objects.equals(m_contentType, other.m_contentType) // NOSONAR
                && Objects.equals(m_created, other.m_created) //
                && Objects.equals(m_lastModified, other.m_lastModified) //
                && Objects.equals(m_author, other.m_author) //
                && Objects.equals(m_description, other.m_description) //
                && Objects.equals(m_links, other.m_links) //
                && Objects.equals(m_tags, other.m_tags);
    }

    @Override
    public abstract int hashCode();

    int commonHash() {
        return Objects.hash(m_contentType, m_created, m_lastModified, m_author, m_description, m_links, m_tags);
    }

    @Override
    public abstract String toString();

    void addCommonFields(final StringBuilder sb) {
        sb.append("author=").append(m_author).append(", description='").append(m_description) //
                .append("', links=").append(m_links).append(", tags=").append(m_tags) //
                .append(", created=").append(m_created).append(", lastModified=").append(m_lastModified);
    }

    /**
     * Writes these metadata to an XML file.
     *
     * @param xmlFile file location
     * @throws IOException if writing has failed
     * @since 5.1
     */
    public final void toXML(final Path xmlFile) throws IOException {
        getXMLDocument().save(xmlFile.toFile(), SAVE_OPTIONS);
    }

    /**
     * @return these metadata as an XML string
     * @since 5.1
     */
    public final String toXML() {
        try {
            final var writer = new StringWriter();
            getXMLDocument().save(writer, SAVE_OPTIONS);
            return writer.toString();
        } catch (IOException ex) {
            // cannot happen
            throw new IllegalStateException(ex);
        }
    }

    private XmlObject getXMLDocument() {
        final var metadataXML = m_elementFactory.newInstance();
        metadataXML.setContentType(m_contentType.m_xmlType);

        if (m_author != null) {
            metadataXML.setAuthor(m_author);
        }
        if (m_created != null) {
            metadataXML.setCreated(GregorianCalendar.from(m_created));
        }
        metadataXML.setLastModified(GregorianCalendar.from(m_lastModified));

        metadataXML.setDescription(m_description);

        if (!m_tags.isEmpty()) {
            final var tags = metadataXML.addNewTags();
            m_tags.forEach(tags::addTag);
        }

        if (!m_links.isEmpty()) {
            final var links = metadataXML.addNewLinks();
            for (final var link : m_links) {
                final var xmlLink = links.addNewLink();
                xmlLink.setHref(link.url);
                xmlLink.setStringValue(link.text);
            }
        }

        // save additional fields from sub-types and create a document
        return toXMLDocument(metadataXML);
    }

    abstract XmlObject toXMLDocument(org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata metadataXML);

    /**
     * Reads common fields from an XML file.
     *
     * @param <T> resulting metadata type
     *
     * @param metadataElement XML element
     * @param fluentBuilder fluent builder
     * @return built metadata object
     */
    static <T> T readCommonFields(final org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata metadataElement,
            final NeedsContentType<T> fluentBuilder) {
        final var description = metadataElement.getDescription();
        final var builder = fluentBuilder //
                .withContentType(getContentType(metadataElement.getContentType())) //
                .withLastModified(toZonedDateTime(metadataElement.getLastModified())) //
                .withDescription(description);

        final var created = metadataElement.getCreated();
        if (created != null) {
            builder.withCreated(toZonedDateTime(created));
        }

        final var author = metadataElement.getAuthor();
        if (author != null) {
            builder.withAuthor(author);
        }

        final var tags = metadataElement.getTags();
        if (tags != null) {
            for (final var tag : tags.getTagArray()) {
                builder.addTag(tag);
            }
        }

        final var links = metadataElement.getLinks();
        if (links != null) {
            for (final var link : links.getLinkArray()) {
                builder.addLink(link.getHref(), link.getStringValue());
            }
        }
        return builder.build();
    }

    static ContentType getContentType(final Enum contentTypeEnum) {
        if (contentTypeEnum == null) {
            return ContentType.PLAIN;
        }
        return switch (contentTypeEnum.intValue()) {
            case org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata.ContentType.INT_TEXT_PLAIN
                -> ContentType.PLAIN;
            case org.knime.core.node.workflow.metadata.v10.NodeContainerMetadata.ContentType.INT_TEXT_HTML
                -> ContentType.HTML;
            default -> throw new IllegalArgumentException("Unknown content type: " + contentTypeEnum);
        };
    }

    static ZonedDateTime toZonedDateTime(final Calendar calendar) {
        return calendar instanceof GregorianCalendar gregorian ? gregorian.toZonedDateTime()
            : ZonedDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
    }

    /**
     * Stage of a metadata builder.
     *
     * @param <T> result metadata type
     */
    public interface NeedsContentType<T> {

        /**
         * Sets the content type of the metadata.
         *
         * @param contentType content type
         * @return next stage of the builder
         */
        NeedsLastModified<T> withContentType(ContentType contentType);

        /**
         * Sets the content type of the metadata to {@code text/plain}.
         *
         * @return next stage of the builder
         */
        default NeedsLastModified<T> withPlainContent() {
            return withContentType(ContentType.PLAIN);
        }

        /**
         * Sets the content type of the metadata to {@code text/html}.
         *
         * @return next stage of the builder
         */
        default NeedsLastModified<T> withHtmlContent() {
            return withContentType(ContentType.HTML);
        }
    }

    /**
     * Stage of a metadata builder.
     *
     * @param <T> result metadata type
     */
    public interface NeedsLastModified<T> {

        /**
         * Sets the last-modified timestamp of the metadata.
         *
         * @param timestamp last time the metadata have been modified
         * @return next stage of the builder
         */
        NeedsDescription<T> withLastModified(ZonedDateTime timestamp);

        /**
         * Sets the last-modified timestamp of the metadata to the current moment.
         *
         * @return next stage of the builder
         */
        default NeedsDescription<T> withLastModifiedNow() {
            return withLastModified(ZonedDateTime.now());
        }
    }

    /**
     * Stage of a metadata builder.
     *
     * @param <T> result metadata type
     */
    public interface NeedsDescription<T> {

        /**
         * Sets the description text of the metadata. The description may contain HTML mark-up.
         *
         * @param description multi-line description
         * @return next stage of the builder
         */
        MetadataOptionals<T> withDescription(String description);
    }

    /**
     * Stage of a metadata builder.
     *
     * @param <T> result metadata type
     */
    public interface MetadataOptionals<T> {

        /**
         * Sets the author of the metadata.
         *
         * @param author author name
         * @return next stage of the builder
         */
        MetadataOptionals<T> withAuthor(String author);

        /**
         * Sets the created timestamp of the metadata.
         *
         * @param timestamp time at which the metadata were created
         * @return next stage of the builder
         */
        MetadataOptionals<T> withCreated(ZonedDateTime timestamp);

        /**
         * Adds a tag to the metadata.
         *
         * @param tag tag to be added
         * @return next stage of the builder
         */
        MetadataOptionals<T> addTag(String tag);

        /**
         * Adds a link to the metadata.
         *
         * @param url link URL
         * @param description link description
         * @return next stage of the builder
         */
        MetadataOptionals<T> addLink(String url, String description);

        /**
         * Creates the metadata.
         *
         * @return metadata object
         */
        T build();
    }

    abstract static class NodeContainerMetadataBuilder<T> implements NeedsContentType<T>, NeedsLastModified<T>,
            NeedsDescription<T>, MetadataOptionals<T> {

        ContentType m_contentType;

        String m_author;

        ZonedDateTime m_created;

        ZonedDateTime m_lastModified;

        String m_description;

        final List<String> m_tags = new ArrayList<>();

        final List<Link> m_links = new ArrayList<>();

        NodeContainerMetadataBuilder() {
        }

        @Override
        public NeedsLastModified<T> withContentType(final ContentType contentType) {
            m_contentType = contentType;
            return this;
        }

        @Override
        public MetadataOptionals<T> withAuthor(final String author) {
            m_author = author;
            return this;
        }

        @Override
        public MetadataOptionals<T> withCreated(final ZonedDateTime created) {
            m_created = created;
            return this;
        }

        @Override
        public NeedsDescription<T> withLastModified(final ZonedDateTime lastModified) {
            m_lastModified = lastModified;
            return this;
        }

        @Override
        public MetadataOptionals<T> withDescription(final String description) {
            m_description = StringUtils.defaultString(description);
            return this;
        }

        @Override
        public MetadataOptionals<T> addTag(final String tag) {
            m_tags.add(CheckUtils.checkNotNull(tag, "Tag cannot be `null`."));
            return this;
        }

        @Override
        public MetadataOptionals<T> addLink(final String url, final String description) {
            m_links.add(new Link(StringUtils.defaultString(url), StringUtils.defaultString(description)));
            return this;
        }
    }
}
