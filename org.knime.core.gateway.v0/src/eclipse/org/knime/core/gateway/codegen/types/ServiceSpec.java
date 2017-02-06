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
package org.knime.core.gateway.codegen.types;

/**
 * Definition of service specifications, e.g. services for testing, default implementations etc.
 *
 * @author Martin Horn, University of Konstanz
 */
public class ServiceSpec {

    /**
     * Specification of the service api interface.
     */
    public static final ServiceSpec Api = new ServiceSpec("api", "##name##", "org.knime.core.gateway.v0", "");

    /**
     * Specification of the service test class.
     */
    public static final ServiceSpec Test = new ServiceSpec("test", "##name##Test", "org.knime.core.gateway.v0", "test");

    /**
     * List of all available 'core' specifications.
     */
    public static final ServiceSpec[] DefaultSpecs = new ServiceSpec[]{
        Api,
        Test
    };

    private String m_pattern;

    private String m_packagePrefix;

    private String m_packageSuffix;

    private String m_id;

    /**
     * A new service spec. The fully qualified name of a service is build like that:
     * {packagePrefix}.{service-namespace}.{packageSuffix}.{pattern-with-##name##-replaced-by-service-name}
     *
     * @param id a unique identifier for the service specification
     * @param pattern essentially the name of the source file, i.e. the service class or interface. The placeholder
     *            "##name##" will be replaced by the actual service name.
     * @param packagePrefix
     * @param packageSuffix
     */
    public ServiceSpec(final String id, final String pattern, final String packagePrefix, final String packageSuffix) {
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
     * Composes the fully qualified name with respect to this service specification. It is build in the following
     * manner: {packagePrefix}.{namespace}.{packageSuffix}.{pattern-with-##name##-replaced-by-serviceName}
     *
     * @param namespace the service's namespace
     * @param serviceName the service's name
     * @return the fully qualified name with respect to this service specification
     */
    public String getFullyQualifiedName(final String namespace, final String serviceName) {
        return EntitySpec.getFullyQualifiedName(getPackagePrefix(), getPackageSuffix(), getPattern(), namespace,
            serviceName);
    }

    /**
     * Gets the service spec for the given entity spec id
     *
     * @param serviceSpecId
     * @return the service spec for the given id or an {@link IllegalArgumentException}
     */
    public static ServiceSpec fromId(final String serviceSpecId) {
        for(ServiceSpec ss : DefaultSpecs) {
            if(serviceSpecId.toLowerCase().equals(ss.m_id)) {
                return ss;
            }
        }
        throw new IllegalArgumentException("No service specification for id " + serviceSpecId);
    }

}
