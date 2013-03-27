/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 4, 2011 (wiswedel): created
 */
package org.knime.core.data.uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JComponent;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 * Spec to {@link URIPortObject}. It represents a {@linkplain URIPortObjectSpec#getFileExtensions() list of extensions}.  
 * 
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class URIPortObjectSpec extends AbstractSimplePortObjectSpec {
    
    private final List<String> m_fileExtensions;

    /**
     * Framework constructor. <b>Do not use in client code.</b>. Use
     * <code>URIPortObjectSpec.INSTANCE</code> instead.
     */
    public URIPortObjectSpec() {
        m_fileExtensions = new ArrayList<String>();
    }
    
    /**
     * @param fileExtensions
     */
    public URIPortObjectSpec(Collection<String> fileExtensions) {
        // remove duplicates
        m_fileExtensions = new ArrayList<String>(new LinkedHashSet<String>(fileExtensions));
    }
    
    /**
     * @param fileExtensions
     */
    public URIPortObjectSpec(String... fileExtensions) {
        this(Arrays.asList(fileExtensions));
    }
    
    /** It contains a list of file extensions ("csv", "xml", ...). The corresponding
     * {@link URIPortObject} contains at least one for each of these extensions.
     * @return The list of extensions, not null.
     */
    public List<String> getFileExtensions() {
        return m_fileExtensions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof URIPortObjectSpec)) {
            return false;
        }
        URIPortObjectSpec other = (URIPortObjectSpec) o;
        return m_fileExtensions.equals(other.m_fileExtensions);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_fileExtensions.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_fileExtensions.addAll(Arrays.asList(model.getStringArray("fileExtensions")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        model.addStringArray("fileExtensions", m_fileExtensions.toArray(new String[m_fileExtensions.size()]));
    }
    
    /**
     * Creates new spec for list of {@link URIContent}. It scans through all the elements and determines the 
     * possible extensions.
     * @param uriContentIterable ...
     * @return ...
     */
    public static URIPortObjectSpec create(final Iterable<URIContent> uriContentIterable) {
        LinkedHashSet<String> fileExtensionsHash = new LinkedHashSet<String>();
        for (URIContent u : uriContentIterable) {
            fileExtensionsHash.add(u.getExtension());
        }
        return new URIPortObjectSpec(fileExtensionsHash);
    }
}
