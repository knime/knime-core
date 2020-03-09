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
 *   May 26, 2019 (loki): created
 */
package org.knime.core.xml;

/**
 * This is a class of static constants used in our XML storage of workflow metadata. This was created for the 3.8
 * release which was originally thought to feature a file with more XML elements; unfortunately we didn't have
 * enough time to change other areas which would be impacted by the format additions (namely on the server and
 * hub sides.) So many of these elements exist for future versions of the metadata file.
 *
 * TODO no one seems able to locate a DTD for this file - one should be made just to cross the t's and dot the i's.
 *
 * @author loki der quaeler
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class MetadataXML {
    /** This is the label which has been historically used to denote the author **/
    public static final String AUTHOR_LABEL = "Author";
    /** This is the label which has been historically used to denote the description **/
    public static final String DESCRIPTION_LABEL = "Comments";
    /** This is the label which has been historically used to denote the creation dat **/
    public static final String CREATION_DATE_LABEL = "Creation Date";

    /** the namespace prefix we use **/
    public static final String NAMESPACE_PREFIX = "met";
    /** the URI we attach to our namespace **/
    public static final String NAMESPACE_URI = "http://www.knime.org/2.9/metainfo";


    /** The root element name for describing metadata.  */
    public static final String METADATA_ELEMENT = "KNIMEMetaInfo";
    /** The root element name for describing metadata for writing.  */
    public static final String METADATA_WRITE_ELEMENT = NAMESPACE_PREFIX + ":" + METADATA_ELEMENT;

    /**
     * Attribute name for the root element tag which lists the version of the metadata; does not exist in pre-3.8 KAP
     * metadata.
     */
    public static final String METADATA_VERSION = "version";



    /** The sub-element name which describes a single atom of metadata.  */
    public static final String ATOM_ELEMENT = "element";
    /** The sub-element name which describes a single atom of metadata.  */
    public static final String ATOM_WRITE_ELEMENT = NAMESPACE_PREFIX + ":" + ATOM_ELEMENT;

    /**
     * Attribute name for the UI element descriptor; this is a holdover from pre-3.8.0 metadata storage and is basically
     * useless now as we craft the UI on what metadata type we are displaying. That said, since we still write
     * "old style" metadata in the 3.8 release, this attribute is used in the writes.
     */
    public static final String FORM = "form";

    /** Attribute name for the display label of this element */
    public static final String NAME = "name";

    /** Attribute name for the metadata type of this element */
    public static final String TYPE = "type";

    /** Attribute name for the "read-only" attribute. */
    public static final String READ_ONLY = "read-only";

    /** Valid 'form' attribute value for pulldowns - NextGen XML format */
    public static final String COMBOBOX = "pulldown";

    /** Valid 'form' attribute value for dates */
    public static final String DATE = "date";

    /** Valid 'form' attribute value for text areas */
    public static final String MULTILINE = "multiline";

    /** Valid 'form' attribute value for text fields */
    public static final String TEXT = "text";

    /** Valid 'form' attribute value for url links - NextGen XML format */
    public static final String URL = "url-link";

    /** This must be present when form == URL - NextGen XML format */
    public static final String URL_TYPE_ATTRIBUTE = "url-type";
    /** This must be present when form == URL - NextGen XML format */
    public static final String URL_URL_ATTRIBUTE = "url-url";


    private MetadataXML() { }
}
