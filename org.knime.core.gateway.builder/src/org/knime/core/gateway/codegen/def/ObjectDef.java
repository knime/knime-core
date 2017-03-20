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
 *   Jan 23, 2017 (wiswedel): created
 */
package org.knime.core.gateway.codegen.def;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Base class for services and entity definitions.
 * A definition consist of a name, namespace and a description.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = EntityDef.class, name = "entity"),
    @JsonSubTypes.Type(value = ServiceDef.class, name = "service"),})
public abstract class ObjectDef {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(Include.NON_NULL).registerModule(new Jdk8Module());

    private String m_namespace;

    private String m_name;

    private String m_description;

    /**
     * @param namespace
     * @param name
     * @param description
     */
    protected ObjectDef(final String namespace, final String name, final String description) {
        m_namespace = checkNamespace(namespace);
        m_name = CheckUtils.checkArgumentNotNull(name);
        m_description = description;
    }

    private static String checkNamespace(final String namespace) {
        CheckUtils.checkArgumentNotNull(namespace);
        CheckUtils.checkArgument(namespace.matches("[a-z0-9]+(?:\\.[a-z0-9]+)*"),
            "Package '%s' invalid: must be like 'abc.def.geh'", namespace);
        return namespace;
    }

    /**
     * @return the description
     */
    @JsonProperty("description")
    public String getDescription() {
        return m_description;
    }

    /**
     * @return the pkg
     */
    @JsonProperty("namespace")
    public String getNamespace() {
        return m_namespace;
    }

    /**
     * @return the object name
     */
    @JsonProperty("name")
    public String getName() {
        return m_name;
    }

    /**
     * @return <code>{namespace}.{name}</code>
     */
    public String getNameWithNamespace() {
        return getNamespace() + "." + getName();
    }

    /**
     * TODO
     *
     */
    public static <T extends ObjectDef> List<T> readAll(final Class<T> cl, Path apiPath) throws IOException {
        PathMatcher jsonMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.json");
        ArrayList<T> result = new ArrayList<>();
        try (Stream<Path> allFilesStream = Files.walk(apiPath)) {
            Iterator<Path> pathIterator = allFilesStream.filter(jsonMatcher::matches).iterator();
            while (pathIterator.hasNext()) {
                Path jsonFileFPath = pathIterator.next();
                ObjectDef struct;
                try {
                    struct = readFromJSON(jsonFileFPath);
                } catch (JsonProcessingException e) {
                    System.err.println("Error in " + jsonFileFPath);
                    throw e;
                }
                if (cl.isInstance(struct)) {
                    result.add(cl.cast(struct));
                }
            }
        }
        return result;
    }

    private static ObjectDef readFromJSON(final Path jsonFilePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            return OBJECT_MAPPER.readValue(reader, ObjectDef.class);
        }
    }
}
