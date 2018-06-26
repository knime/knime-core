/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   04.10.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.swing.tree.DefaultMutableTreeNode;

import org.knime.base.node.preproc.stringmanipulation.manipulator.CapitalizeDelimManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.CapitalizeManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.CompareManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.CountCharsManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.CountCharsModifiersManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.CountManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.CountModifiersManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.IndexOfCharsManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.IndexOfCharsModifiersManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.IndexOfCharsOffsetManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.IndexOfCharsOffsetModifiersManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.IndexOfManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.IndexOfModifiersManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.IndexOfOffsetManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.IndexOfOffsetModifiersManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.JoinManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.JoinSepManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.LastIndexOfCharManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.LengthManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.LowerCaseManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.MD5ChecksumManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.PadLeftManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.PadLeftCharsManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.PadRightManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.PadRightCharsManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.RegexMatcherManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.RegexReplaceManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.RemoveCharsManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.RemoveDiacriticManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.RemoveSpaceCharDuplicatesManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.RemoveSpaceCharsManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ReplaceCharsManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ReplaceCharsModifiersManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ReplaceManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ReplaceModifiersManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ReplaceUmlautsManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ReverseManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.StringManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.StripEndManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.StripManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.StripStartManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.SubstringOffsetLengthManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.SubstringOffsetManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ToBooleanManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ToDoubleManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ToEmptyManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ToIntManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ToLongManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.ToNullManipulator;
import org.knime.base.node.preproc.stringmanipulation.manipulator.UpperCaseManipulator;
import org.knime.base.node.util.ManipulatorProvider;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.FileUtil;

/**
 * Provider for all string manipulation functions.
 *
 * @author Heiko Hofer
 */
public final class StringManipulatorProvider implements ManipulatorProvider {
    private Map<String, Collection<Manipulator>> m_manipulators;

    private File m_jarFile;

    private static final StringManipulatorProvider provider =
            new StringManipulatorProvider();

    /**
     * Get default shared instance.
     *
     * @return default StringManipulatorProvider
     */
    public static StringManipulatorProvider getDefault() {
        return provider;
    }

    /**
     * prevent instantiation.
     */
    private StringManipulatorProvider() {
        m_manipulators = new LinkedHashMap<String, Collection<Manipulator>>();

        Collection<Manipulator> manipulators =
                new TreeSet<Manipulator>(new Comparator<Manipulator>() {

                    @Override
                    public int compare(final Manipulator o1,
                            final Manipulator o2) {
                        return o1.getDisplayName().compareTo(
                                o2.getDisplayName());
                    }
                });

        manipulators.add(new CapitalizeDelimManipulator());
        manipulators.add(new CapitalizeManipulator());
        manipulators.add(new CompareManipulator());
        manipulators.add(new CountManipulator());
        manipulators.add(new CountModifiersManipulator());
        manipulators.add(new CountCharsManipulator());
        manipulators.add(new CountCharsModifiersManipulator());
        manipulators.add(new IndexOfCharsManipulator());
        manipulators.add(new IndexOfCharsModifiersManipulator());
        manipulators.add(new IndexOfCharsOffsetManipulator());
        manipulators.add(new IndexOfCharsOffsetModifiersManipulator());
        manipulators.add(new IndexOfManipulator());
        manipulators.add(new IndexOfOffsetManipulator());
        manipulators.add(new IndexOfOffsetModifiersManipulator());
        manipulators.add(new IndexOfModifiersManipulator());
        manipulators.add(new JoinManipulator());
        manipulators.add(new JoinSepManipulator());
        manipulators.add(new LengthManipulator());
        manipulators.add(new LowerCaseManipulator());
        manipulators.add(new RemoveSpaceCharsManipulator());
        manipulators.add(new RemoveSpaceCharDuplicatesManipulator());
        manipulators.add(new RemoveCharsManipulator());
        manipulators.add(new ReplaceCharsManipulator());
        manipulators.add(new ReplaceCharsModifiersManipulator());
        manipulators.add(new ReplaceManipulator());
        manipulators.add(new ReplaceModifiersManipulator());
        manipulators.add(new ReverseManipulator());
        manipulators.add(new StripEndManipulator());
        manipulators.add(new StripManipulator());
        manipulators.add(new StripStartManipulator());
        manipulators.add(new SubstringOffsetManipulator());
        manipulators.add(new SubstringOffsetLengthManipulator());
        manipulators.add(new ToBooleanManipulator());
        manipulators.add(new ToDoubleManipulator());
        manipulators.add(new ToEmptyManipulator());
        manipulators.add(new ToIntManipulator());
        manipulators.add(new ToLongManipulator());
        manipulators.add(new ToNullManipulator());
        manipulators.add(new StringManipulator());
        manipulators.add(new UpperCaseManipulator());
        manipulators.add(new ReplaceUmlautsManipulator());
        manipulators.add(new RemoveDiacriticManipulator());
        manipulators.add(new RegexMatcherManipulator());
        manipulators.add(new RegexReplaceManipulator());
        manipulators.add(new MD5ChecksumManipulator());
        manipulators.add(new LastIndexOfCharManipulator());
        manipulators.add(new PadLeftManipulator());
        manipulators.add(new PadRightManipulator());
        manipulators.add(new PadLeftCharsManipulator());
        manipulators.add(new PadRightCharsManipulator());

        Set<String> categories = new TreeSet<String>();
        for (Manipulator m : manipulators) {
            String category = m.getCategory();
            if (category.equals(ALL_CATEGORY)) {
                throw new IllegalStateException(
                        "The category \"All\" is not allowed.");
            }
            categories.add(category);
        }
        m_manipulators.put(ALL_CATEGORY, manipulators);
        for (String category : categories) {
            m_manipulators.put(category, new ArrayList<Manipulator>());
        }
        for (Manipulator m : manipulators) {
            Collection<Manipulator> list = m_manipulators.get(m.getCategory());
            list.add(m);
        }

    }

    /**
     * Get all categories.
     *
     * @return the categories
     * @since 2.6
     */
    @Override
    public Collection<String> getCategories() {
        return m_manipulators.keySet();
    }

    /**
     * Get the {@link Manipulator}s in the given category.
     *
     * @param category a category as given by getCategories()
     * @return the {@link Manipulator}s in the given category
     */
    @Override
    public Collection<Manipulator> getManipulators(final String category) {
        return m_manipulators.get(category);
    }

    /**
     * Give jar file with all *.class files returned by
     * getManipulators(ALL_CATEGORY).
     *
     * @return file object of a jar file with all compiled manipulators
     * @throws IOException if jar file cannot be created
     */
    public synchronized File getJarFile() throws IOException {
        if (m_jarFile == null || !m_jarFile.exists()) {
            // Temp file must not be associated with a node therefore we need to specify the base directory
            // explicitly.
            m_jarFile =
                FileUtil.createTempFile("jsnippet-manipulators", ".jar", new File(KNIMEConstants.getKNIMETempDir()),
                    true);
            Collection<Object> classes = new ArrayList<Object>();
            classes.add(Manipulator.class);
            classes.addAll(m_manipulators.get(ALL_CATEGORY));
            // create tree structure for classes
            DefaultMutableTreeNode root = createTree(classes);
            try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(m_jarFile))) {
                createJar(root, jar, null);
            }
        }
        return m_jarFile;
    }

    private DefaultMutableTreeNode createTree(
            final Collection<? extends Object> classes) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("build");
        for (Object o : classes) {
            Class<?> cl = o instanceof Class ? (Class<?>)o : o.getClass();
            Package pack = cl.getPackage();
            DefaultMutableTreeNode curr = root;
            for (String p : pack.getName().split("\\.")) {
                DefaultMutableTreeNode child = getChild(curr, p);
                if (null == child) {
                    DefaultMutableTreeNode h = new DefaultMutableTreeNode(p);
                    curr.add(h);
                    curr = h;
                } else {
                    curr = child;
                }
            }
            curr.add(new DefaultMutableTreeNode(cl));
        }

        return root;
    }

    private DefaultMutableTreeNode getChild(final DefaultMutableTreeNode curr,
            final String p) {
        for (int i = 0; i < curr.getChildCount(); i++) {
            DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode)curr.getChildAt(i);
            if (child.getUserObject().toString().equals(p)) {
                return child;
            }
        }
        return null;
    }

    private void createJar(final DefaultMutableTreeNode node,
            final JarOutputStream jar, final String path) throws IOException {
        Object o = node.getUserObject();
        if (o instanceof String) {
            // folders must end with a "/"
            String subPath = null == path ? "" : (path + (String)o + "/");
            if (path != null) {
                JarEntry je = new JarEntry(subPath);
                jar.putNextEntry(je);
                jar.flush();
                jar.closeEntry();
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child =
                        (DefaultMutableTreeNode)node.getChildAt(i);
                createJar(child, jar, subPath);
            }
        } else {
            Class<?> cl = (Class<?>)o;
            String className = cl.getSimpleName();
            className = className.concat(".class");
            JarEntry entry = new JarEntry(path + className);
            jar.putNextEntry(entry);

            ClassLoader loader = cl.getClassLoader();
            InputStream inStream =
                    loader.getResourceAsStream(cl.getName().replace('.', '/')
                            + ".class");

            FileUtil.copy(inStream, jar);
            inStream.close();
            jar.flush();
            jar.closeEntry();

        }
    }

}
