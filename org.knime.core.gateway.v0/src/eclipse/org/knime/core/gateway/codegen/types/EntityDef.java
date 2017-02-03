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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
@JsonPropertyOrder({"name", "description", "namespace", "parent", "fields"})
public class EntityDef extends AbstractDef {

    /**
     *
     */
    private static final String DEFAULT_IMPL_PACKAGE_PREFIX = "org.knime.core.gateway.serverproxy.service";

    private final String m_namespace;

    private final String m_name;

    private final String m_description;

    private final List<EntityField> m_fields;

    private String m_parentEntityName;

    private EntityDef m_parentEntity;

    /** Constructor used by Jackson.
     * @param namespace
     * @param name
     * @param description
     * @param parentEntityName
     * @param entityFields
     */
    @JsonCreator
    public EntityDef(
        @JsonProperty("namespace") final String namespace,
        @JsonProperty("name") final String name,
        @JsonProperty("description") final String description,
        @JsonProperty("parent") final String parentEntityName,
        @JsonProperty("fields") final EntityField[] entityFields) {
        m_namespace = ServiceDef.checkNamespace(namespace);
        m_name = name;
        m_description = description;
        m_fields = Arrays.asList(entityFields);
        m_parentEntityName = parentEntityName;
    }

    @JsonIgnore
    public void resolveParent(final Map<String, EntityDef> entityDef) {
        if (m_parentEntityName != null) {
            m_parentEntity = CheckUtils.checkArgumentNotNull(entityDef.get(m_parentEntityName),
                "No parent entity \"%s\" for child \"%s\"", m_parentEntityName, getName());
        }
    }

    /**
     * @return the description
     */
    @JsonProperty("description")
    public String getDescription() {
        return m_description;
    }

    /**
     * @return the namespace
     */
    @JsonProperty("namespace")
    public String getNamespace() {
        return m_namespace;
    }

    @JsonProperty("name")
    public String getName() {
        return m_name;
    }

    @JsonProperty("parent")
    public String getParent() {
        return m_parentEntityName;
    }

    @JsonProperty("fields")
    public List<EntityField> getFields() {
        return m_fields;
    }

    @JsonIgnore
    public String getNameWithNamespace() {
        return m_namespace + "." + m_name;
    }

    @JsonIgnore
    public Optional<String> getParentOptional() {
        return Optional.ofNullable(m_parentEntityName);
    }

    @JsonIgnore
    public Collection<String> getJavaImports() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        m_fields.stream().map(EntityField::getType).forEach(t -> result.addAll(t.getJavaImports()));
        return result;
    }

    @JsonIgnore
    public List<String> getImportsForDefaultClasses() {
        return getImportsPrivate(t -> getFullyQualifiedName(
            DEFAULT_IMPL_PACKAGE_PREFIX, "", "Default##name##", t.getNamespace(), t.getSimpleName()));
    }

    @JsonIgnore
    public List<String> getImportsForTestClasses(final String implPackagePrefix) {
        return getImportsPrivate(t -> getFullyQualifiedName(
            implPackagePrefix, "test", "##name##Test", t.getNamespace(), t.getSimpleName()));
    }

    @JsonIgnore
    public List<String> getImportsForAPIClasses(final String apiPackagePrefix) {
        return getImportsPrivate(t -> getFullyQualifiedName(
            apiPackagePrefix, "", "##name##", t.getNamespace(), t.getSimpleName()));
    }

    @JsonIgnore
    public List<String> getImportsForBuilderClasses(final String implPackagePrefix) {
        return getImportsPrivate(t -> getFullyQualifiedName(
            implPackagePrefix, "builder", "##name##Builder", t.getNamespace(), t.getSimpleName()));
    }

    @JsonIgnore
    private List<String> getImportsPrivate(final Function<Type, String> fullyQualifiedNameRetriever) {
        ArrayList<String> result = new ArrayList<String>();
        return m_fields.stream().map(EntityField::getType).flatMap(Type::getRequiredTypes)
        .filter(Type::isEntityType)
        .map(fullyQualifiedNameRetriever)
        .distinct().sorted()
        .collect(Collectors.toList());
    }

    @JsonIgnore
    static String getFullyQualifiedName(final String packagePrefix, final String packageSuffix,
        final String namePattern, final String namespace, final String typeName) {
        StringBuilder builder = new StringBuilder();
        builder.append(packagePrefix);
        if (namespace != null) {
            builder.append('.').append(namespace);
        }
        if (StringUtils.isNotEmpty(packageSuffix)) {
            builder.append('.').append(packageSuffix);
        }
        builder.append('.').append(namePattern.replace("##name##", typeName));
        return builder.toString();
    }

    @JsonIgnore
    public String getBuilderName() {
        return m_name.concat("Builder");
    }

    @JsonIgnore
    public String getTestName() {
        return m_name.concat("Test");
    }

    @JsonIgnore
    public String getDefaultFullyQualified() {
        return getFullyQualifiedName(DEFAULT_IMPL_PACKAGE_PREFIX, "", "Default##name##", getNamespace(), getName());
    }

    @JsonIgnore
    public String getTestFullyQualified(final String implPackagePrefix) {
        return getFullyQualifiedName(implPackagePrefix, "test", "##name##Test", getNamespace(), getName());
    }

    @JsonIgnore
    public String getBuilderNameFullyQualified(final String implPackagePrefix) {
        return getFullyQualifiedName(implPackagePrefix, "builder", "##name##Builder", getNamespace(), getName());
    }

    @JsonIgnore
    public String getAPINameFullyQualified(final String apiPackagePrefix) {
        return getFullyQualifiedName(apiPackagePrefix, "", "##name##", getNamespace(), getName());
    }

    @JsonIgnore
    public String getTestNameFullyQualified(final String implPackagePrefix) {
        return getFullyQualifiedName(implPackagePrefix, "", "##name##Test", getNamespace(), getName());
    }

    @Override
    public String toString() {
        return m_name + ":\n  " +
                m_fields.stream().map(e -> e.getTypeAsString() + " " + e.getName()).collect(Collectors.joining("\n  "));
    }

}
