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
 *   Feb 3, 2017 (hornm): created
 */
package org.knime.core.gateway.codegen.types;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * Definition of an entity specification, e.g. entity builders, entities for testing, default entity implementation.
 *
 * @author Martin Horn, University of Konstanz
 */
public class EntitySpec {

    /**
     * Specification of the entity api interface.
     */
    public static final EntitySpec Api = new EntitySpec("api", "##name##", "org.knime.core.gateway.v0", "");

    /**
     * Specification of the entity api builder interface.
     */
    public static final EntitySpec Builder = new EntitySpec("builder", "##name##Builder", "org.knime.core.gateway.v0", "builder");

    /**
     * Specification of the default implementation of the entity api interface.
     */
    public static final EntitySpec Impl = new EntitySpec("impl", "Default##name##", "org.knime.core.gateway.v0", "impl");

    /**
     * Specification of the juni-tests for the entity api interface.
     */
    public static final EntitySpec Test = new EntitySpec("test", "##name##Test", "org.knime.core.gateway.v0.test", "test");

    /**
     * List of all available 'core' specifications.
     */
    public static final EntitySpec[] DefaultSpecs = new EntitySpec[]{Api, Builder, Impl, Test};

    private String m_pattern;

    private String m_packageSuffix;

    private String m_packagePrefix;

    private String m_id;

    /**
     * A new entity spec. The fully qualified name of an entity is build like that:
     * {packagePrefix}.{entity-namespace}.{packageSuffix}.{pattern-with-##name##-replaced-by-entity-name}
     *
     *
     * @param id a unique identifier for the entity specification
     * @param packagePrefix a package prefix
     * @param packageSuffix a package suffix
     * @param pattern essentially the name of the source file, i.e. the entity class or interface. The placeholder
     *            "##name##" will be replaced with the actual entity name.
     */
    public EntitySpec(final String id, final String pattern, final String packagePrefix, final String packageSuffix) {
        m_id = id;
        m_pattern = pattern;
        m_packagePrefix = packagePrefix;
        m_packageSuffix = packageSuffix;
    }

    /**
     * @return the naming pattern that contains a ##name##-placeholder
     */
    public String getPattern() {
        return m_pattern;
    }

    /**
     * @return the package prefix
     */
    public String getPackagePrefix() {
        return m_packagePrefix;
    }

    /**
     * @return the package suffix
     */
    public String getPackageSuffix() {
        return m_packageSuffix;
    }

    /**
     * @param entityDef the entity def to create the imports-list for
     * @return a list of imports of all entities that are used within the given entity, adopted according to this entity
     *         specification (i.e. with name, package suffix and prefix replaced/added)
     */
    public List<String> getImports(final EntityDef entityDef) {
        return getImportsPrivate(t -> getFullyQualifiedName(this.getPackagePrefix(), this.getPackageSuffix(),
            this.getPattern(), t.getNamespace(), t.getSimpleName()), entityDef.getFields().stream());
    }

    private List<String> getImportsPrivate(final Function<Type, String> fullyQualifiedNameRetriever,
        final Stream<EntityField> fields) {
        return fields.map(EntityField::getType).flatMap(Type::getRequiredTypes).filter(Type::isEntity)
            .map(fullyQualifiedNameRetriever).distinct().sorted().collect(Collectors.toList());
    }

    /**
     * Composes the fully qualified name with respect to this entity specification. It is build in the following manner:
     * {packagePrefix}.{namespace}.{packageSuffix}.{pattern-with-##name##-replaced-by-entityName}
     *
     * @param namespace the entity's namespace
     * @param entityName the entity's name
     * @return the fully qualified name with respect to this entity specification
     */
    public String getFullyQualifiedName(final String namespace, final String entityName) {
        return getFullyQualifiedName(this.getPackagePrefix(), this.getPackageSuffix(), this.getPattern(), namespace,
            entityName);
    }

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

    /**
     * Gets the entity spec for the given entity spec id
     *
     * @param entitySpecId
     * @return the entity spec for the given id or an {@link IllegalArgumentException}
     */
    public static EntitySpec fromId(final String entitySpecId) {
        for(EntitySpec es : DefaultSpecs) {
            if(entitySpecId.toLowerCase().equals(es.m_id)) {
                return es;
            }
        }
        throw new IllegalArgumentException("No entity specification for id " + entitySpecId);
    }

}
