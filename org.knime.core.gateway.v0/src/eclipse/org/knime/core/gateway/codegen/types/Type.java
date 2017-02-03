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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class Type {

    private Random m_rand = new Random();


    public static enum GenericType {
        NONE,
        LIST,
        MAP;
    }

    public static final Type VOID = new Type("void");

    private final String m_namespace;
    private final String m_simpleName;

    private GenericType m_genericType;

    private List<Type> m_typeParameters;

    /**
     * @param name <code>null</code> if void
     * @param genericType
     *
     */
    private Type(final GenericType genericType, final String... typeParameters) {
        m_genericType = genericType;
        m_typeParameters = Arrays.stream(typeParameters).map(t -> Type.parse(t)).collect(Collectors.toList());
        switch (genericType) {
            case LIST:
                m_namespace = "java.util";
                m_simpleName = "List";
                break;
            case MAP:
                m_namespace = "java.util";
                m_simpleName = "Map";
                break;
            default:
                throw new IllegalStateException("Constructor only meant for collections");
        }
    }

    private Type(final String name) {
        m_genericType = GenericType.NONE;
        if (StringUtils.contains(name, ".")) {
            m_simpleName = StringUtils.substringAfterLast(name, ".");
            m_namespace = StringUtils.substringBeforeLast(name, ".");
        } else {
            m_simpleName = name;
            m_namespace = null;
        }
        m_typeParameters = Collections.emptyList();
    }

    public String toString(final String append, final String prepend) {
        return toString(append, prepend, true);
    }

    public String toString(final String append, final String prepend, final boolean useSimpleClassName) {
        if (isVoid()) {
            return "void";
        } else if (m_typeParameters.size() == 0 && isPrimitive()) {
            return m_simpleName;
        } else if (m_typeParameters.size() == 0) {
            String className;
            if (useSimpleClassName) {
                className = m_simpleName;
            } else {
                className = m_namespace != null ? m_namespace + "." + m_simpleName : m_simpleName;
            }
            return append + className + prepend;
        } else {
            String[] params = new String[m_typeParameters.size()];
            for (int i = 0; i < params.length; i++) {
                params[i] = m_typeParameters.get(i).toString(append, prepend, useSimpleClassName);
            }
            return m_simpleName + "<" + String.join(", ", params) + ">";
        }

    }

    @Override
    public String toString() {
        return toString("", "");
    }

    /**
     * @return the namespace
     */
    public String getNamespace() {
        return m_namespace;
    }

    /**
     * @return the simpleName
     */
    public String getSimpleName() {
        return m_simpleName;
    }

    public Type getTypeParameter(final int index) {
        return m_typeParameters.get(index);
    }

    public boolean isList() {
        return m_genericType == GenericType.LIST;
    }

    public boolean isMap() {
        return m_genericType == GenericType.MAP;
    }

    public boolean isVoid() {
        return m_simpleName.equals("void");
    }

    public boolean isPrimitive() {
        if(m_typeParameters.size() == 0) {
            return isNamePrimitive();
        } else {
            return m_typeParameters.stream().allMatch(t -> t.isPrimitive());
        }
    }

    public boolean isEntityType() {
        return !isVoid() && !isPrimitive() && !isList() && !isMap();
    }

    /**
     * @return
     */
    private boolean isNamePrimitive() {
        return m_simpleName.equals("String") ||
                m_simpleName.equals("int") || m_simpleName.equals("Integer") ||
                m_simpleName.toLowerCase().equals("byte") ||
                m_simpleName.toLowerCase().equals("short") ||
                m_simpleName.toLowerCase().equals("long") ||
                m_simpleName.toLowerCase().equals("float") ||
                m_simpleName.toLowerCase().equals("double") ||
                m_simpleName.toLowerCase().equals("boolean");
    }

    public Collection<String> getJavaImports() {
        Set<String> result = new LinkedHashSet<>();
        switch (m_genericType) {
            case LIST:
                result.add(List.class.getName());
                break;
            case MAP:
                result.add(Map.class.getName());
                break;
            default:
        }
        m_typeParameters.stream().forEach(t -> result.addAll(t.getJavaImports()));
        return result;
    }

    public Stream<Type> getRequiredTypes() {
        return Stream.concat(Stream.of(this), m_typeParameters.stream());
    }

    public Object createRandomPrimitive() {
        if(isPrimitive()) {
            if(isList()) {
                //TODO
                return null;
            } else if( isMap()) {
                //TODO
                return null;
            } else {
                if(m_simpleName.equals("boolean")) {
                    return m_rand.nextBoolean();
                } else if(m_simpleName.equals("int")) {
                    return m_rand.nextInt();
                } else if(m_simpleName.equals("float")) {
                    return m_rand.nextFloat();
                } else if(m_simpleName.equals("double")) {
                    return m_rand.nextDouble();
                } else if(m_simpleName.equals("String")) {
                    return "\"" + RandomStringUtils.randomAlphanumeric(5) + "\"";
                }
                return null;
            }
        } else {
            return null;
        }
    }


    /**
     * Parses the type from a string, e.g. Map<Integer, String>
     * @param s
     * @return the newly created type
     */
    public static Type parse(final String s) {
        if (s.startsWith("List<")) {
            return new Type(GenericType.LIST, parseTypeParameters(s));
        } else if (s.startsWith("Map<")) {
            return new Type(GenericType.MAP, parseTypeParameters(s));
        } else {
            return new Type(s);
        }
    }

    private static String[] parseTypeParameters(final String s) {
        String[] split = s.substring(0, s.length() - 1).split("<")[1].split(",");
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].trim();
        }
        return split;
    }

    public static void main(final String[] args) {
        System.out.println(Type.parse("Map<String, Integer>").toString("", ""));
        System.out.println(Type.parse("List<Test>").toString("", ""));
        System.out.println(Type.parse("List<Test>").toString("T", ""));
        System.out.println(Type.parse("Map<String, Integer>").toString("T", ""));
        System.out.println(Type.parse("Map<Test, Test2>").toString("T", ""));
        System.out.println(Type.parse("Map<Blub, Integer>").toString("", "ToThrift"));
    }


}
