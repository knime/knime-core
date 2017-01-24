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
 *   Dec 5, 2016 (hornm): created
 */
package org.knime.core.gateway.codegen.types;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
@JsonPropertyOrder({"name", "description", "package", "parent", "fields"})
public class EntityDef extends AbstractDef {

    private final String m_package;

    private final String m_name;

    private final String m_description;

    private final List<EntityField> m_fields;

    private String m_parent;

    /**
     * @param pkg TODO
     * @param description TODO
     *
     */
    @JsonIgnore
    public EntityDef(final String pkg, final String name, final String description, final EntityField... entityFields) {
        m_package = ServiceDef.checkPackage(pkg);
        m_name = name;
        m_description = description;
        m_fields = Arrays.asList(entityFields);
    }

    @JsonIgnore
    public EntityDef setParent(final String parent) {
        m_parent = parent;
        return this;
    }

    /** Constructor used by Jackson.
     * @param name
     * @param entityFields
     * @param commonEntities
     * @param imports
     * @return new object
     */
    @JsonCreator
    public static EntityDef restoreFromJSON(
        @JsonProperty("package") final String pkg,
        @JsonProperty("name") final String name,
        @JsonProperty("description") final String description,
        @JsonProperty("parent") final String parent,
        @JsonProperty("fields") final EntityField[] entityFields) {
        EntityDef result = new EntityDef(pkg, name, description, entityFields);
        result.setParent(parent);
        return result;
    }

    /**
     * @return the description
     */
    @JsonProperty("description")
    public String getDescription() {
        return m_description;
    }

    /**
     * @return the package
     */
    @JsonProperty("package")
    public String getPackage() {
        return m_package;
    }

    @JsonProperty("name")
    public String getName() {
        return m_name;
    }

    @JsonProperty("parent")
    public String getParent() {
        return m_parent;
    }

    @JsonProperty("fields")
    public List<EntityField> getFields() {
        return m_fields;
    }

    @JsonIgnore
    public String getFullyQualifiedName() {
        return m_package + "." + m_name;
    }

    @JsonIgnore
    public Optional<String> getParentOptional() {
        return Optional.ofNullable(m_parent);
    }

    @JsonIgnore
    public List<String> getImports() {
        return m_fields.stream()
                .map(EntityField::getType)
                .flatMap(t -> t.getImports())
                .sorted().distinct()
                .collect(Collectors.toList());
    }

}
