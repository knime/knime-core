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
 *   Feb 6, 2017 (hornm): created
 */
package org.knime.core.gateway.codegen.spec;

import org.apache.commons.lang3.StringUtils;

/**
 * Object specifications, such as package prefix and suffix, name pattern etc. In combination with a namespace and an object name
 * it mainly helps to build the fully qualified name of an interface or class (see
 * {@link #getFullyQualifiedName(String, String)}).
 *
 * @author Martin Horn, University of Konstanz
 */
public class ObjectSpec {

    private static final String NAME_PLACEHOLDER = "##name##";

    private String m_pattern;

    private String m_packagePrefix;

    private String m_packageSuffix;

    private String m_id;

    /**
     * A new object spec. The fully qualified name of an object is build like that:
     * {packagePrefix}.{object-namespace}.{packageSuffix}.{pattern-with-##name##-replaced-by-object-name}
     *
     * @param id a unique identifier for the object specification
     * @param pattern essentially the name of the source file, i.e. the object class or interface. The placeholder
     *            "##name##" will be replaced by the actual object name.
     * @param packagePrefix
     * @param packageSuffix
     */
    public ObjectSpec(final String id, final String pattern, final String packagePrefix,
        final String packageSuffix) {
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

    String getId() {
        return m_id;
    }

    /**
     * Composes the fully qualified name with respect to this object specification. It is build in the following manner:
     * <code>{packagePrefix}.{namespace}.{packageSuffix}.{pattern-with-##name##-replaced-by-objectName}</code>.
     *
     * @param namespace
     * @param name
     *
     * @return the fully qualified name with respect to this object specification
     */
    public String getFullyQualifiedName(final String namespace, final String name) {
        StringBuilder builder = new StringBuilder();
        builder.append(getPackagePrefix());
        if (namespace != null) {
            builder.append('.').append(namespace);
        }
        if (StringUtils.isNotEmpty(getPackageSuffix())) {
            builder.append('.').append(getPackageSuffix());
        }
        builder.append('.').append(getPattern().replace(NAME_PLACEHOLDER, name));
        return builder.toString();
    }

    /**
     * @param namespace
     * @param name
     * @return the actual class for the fully qualified name
     * @throws ClassNotFoundException
     */
    public Class<?> getClassForFullyQualifiedName(final String namespace, final String name) throws ClassNotFoundException {
        return Class.forName(getFullyQualifiedName(namespace, name));
    }

    /**
     * Little helper to extract the object's name of a fully qualified name, given this spec.
     *
     * @param clazz
     * @return the object's name
     */
    public String extractNameFromClass(final Class<?> clazz) {
        String n = clazz.getCanonicalName();
        //get rid of all inner classes, proxy classes etc.
        if (n.indexOf("$") > 0) {
            n = n.substring(0, n.indexOf("$"));
        }
        n = n.substring(n.lastIndexOf(".") + 1);
        int patternOffset = getPattern().indexOf(NAME_PLACEHOLDER);
        return n.substring(patternOffset,
            n.length() - (getPattern().length() - patternOffset - NAME_PLACEHOLDER.length()));
    }

    /**
     * Little helper to extract the object's namespace given this spec.
     *
     * @param clazz
     * @return the object's namespace
     */
    public String extractNamespaceFromClass(final Class<?> clazz) {
        String n = clazz.getCanonicalName();
        if (getPackagePrefix().length() > 0) {
            n = n.substring(getPackagePrefix().length() + 1);
        }
        if (getPackageSuffix().length() > 0) {
            n = n.substring(0, n.lastIndexOf(".") - getPackageSuffix().length() - 1);
        } else {
            n = n.substring(0, n.lastIndexOf("."));
        }
        return n;
    }
}
