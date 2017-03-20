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
 */
package org.knime.core.jaxrs.serverproxy;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class to essentially deal with the names and namespaces of object definitions and the specifications.
 *
 * @author Martin Horn, University of Konstanz
 */
public final class ObjectSpecUtil {

    private ObjectSpecUtil() {
        //utility class
    }

    /**
     * Composes the fully qualified name with respect to this object specification. It is build in the following manner:
     * <code>{packagePrefix}.{namespace}.{packageSuffix}.{pattern-with-     *
     * @param namespace
     * @param name
     * @param specId
     *
     * @return the fully qualified name with respect to this object specification
     */
    public static String getFullyQualifiedName(final String namespace, final String name, final String specId) {
        StringBuilder builder = new StringBuilder();
        builder.append(getPackagePrefix(specId));
        if (namespace != null) {
            builder.append('.').append(namespace);
        }
        if (StringUtils.isNotEmpty(getPackageSuffix(specId))) {
            builder.append('.').append(getPackageSuffix(specId));
        }
        builder.append('.').append(getPattern(specId).replace(getNamePlaceholder(), name));
        return builder.toString();
    }

    /**
     * @param namespace
     * @param name
     * @param specId
     * @return the actual class for the fully qualified name
     * @throws ClassNotFoundException
     */
    public static Class<?> getClassForFullyQualifiedName(final String namespace, final String name, final String specId)
        throws ClassNotFoundException {
        return Class.forName(getFullyQualifiedName(namespace, name, specId));
    }

    /**
     * Little helper to extract the object's name of a fully qualified name, given this spec.
     *
     * @param clazz
     * @param specId
     * @return the object's name
     */
    public static String extractNameFromClass(final Class<?> clazz, final String specId) {
        String n = clazz.getCanonicalName();
        //get rid of all inner classes, proxy classes etc.
        if (n.indexOf("$") > 0) {
            n = n.substring(0, n.indexOf("$"));
        }
        n = n.substring(n.lastIndexOf(".") + 1);
        int patternOffset = getPattern(specId).indexOf(getNamePlaceholder());
        return n.substring(patternOffset,
            n.length() - (getPattern(specId).length() - patternOffset - getNamePlaceholder().length()));
    }

    /**
     * Little helper to extract the object's namespace given this spec.
     *
     * @param clazz
     * @param specId
     * @return the object's namespace
     */
    public static String extractNamespaceFromClass(final Class<?> clazz, final String specId) {
        String n = clazz.getCanonicalName();
        if (getPackagePrefix(specId).length() > 0) {
            n = n.substring(getPackagePrefix(specId).length() + 1);
        }
        if (getPackageSuffix(specId).length() > 0) {
            n = n.substring(0, n.lastIndexOf(".") - getPackageSuffix(specId).length() - 1);
        } else {
            n = n.substring(0, n.lastIndexOf("."));
        }
        return n;
    }

    private static String getPattern(final String specId) {
		if(specId.equals("restwrapper")) {
			return "RSWrapper##name##";
		}				
		return null;
    }

    private static String getPackagePrefix(final String specId) {
		if(specId.equals("restwrapper")) {
			return "org.knime.core.jaxrs";
		}				
		return null;
    }

    private static String getPackageSuffix(final String specId) {
		if(specId.equals("restwrapper")) {
			return "";
		}				
		return null;
    }

    private static String getNamePlaceholder() {
    	return "##name##";
    }

}
