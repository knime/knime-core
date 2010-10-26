/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 */
package org.knime.workbench.editor2.meta;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.editor2.meta.MetaNodePortType.DBMetaNodePortType;
import org.knime.workbench.editor2.meta.MetaNodePortType.DataMetaNodePortType;
import org.knime.workbench.editor2.meta.MetaNodePortType.FlowVarMetaNodePortType;
import org.knime.workbench.editor2.meta.MetaNodePortType.PMMLMetaNodePortType;

/**
 * Reads the contributions for the meta node port type extension point.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class AddMetaNodePortTypeCollector {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(AddMetaNodePortTypeCollector.class);

    // ID of "metanode_porttype" extension point
    private static final String ID_PORTTYPE =
        "org.knime.workbench.editor.metanode_porttype";

    private static AddMetaNodePortTypeCollector instance;

    /** Get instance to be used. 
     * @return The static instance.
     */
    public static AddMetaNodePortTypeCollector getInstance() {
        if (instance == null) {
            try {
                instance = initInstance();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                instance = new AddMetaNodePortTypeCollector();
            }
        }
        return instance;
    }

    private static AddMetaNodePortTypeCollector initInstance() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(ID_PORTTYPE);
        if (point == null) {
            throw new IllegalStateException("Invalid extension point : "
                    + ID_PORTTYPE);
        }

        Set<MetaNodePortType> typeSet = new HashSet<MetaNodePortType>();
        IExtension[] extensions = point.getExtensions();
        for (IExtension ext : extensions) {
            for (IConfigurationElement e : ext.getConfigurationElements()) {
                try {
                    MetaNodePortType portType =
                        (MetaNodePortType)e.createExecutableExtension(
                                "metaNodePortTypeClass");
                    if (!typeSet.add(portType)) {
                        LOGGER.warn("Duplicate meta node port type definition "
                                + portType.toString());
                    }
                } catch (Exception exc) {
                    LOGGER.error("Unable to load meta node port type from "
                            + e.getName(), exc);
                }
            }
        }
        MetaNodePortType[] ar = typeSet.toArray(
                new MetaNodePortType[typeSet.size()]);
        return new AddMetaNodePortTypeCollector(ar);
    }

    private final MetaNodePortType[] m_types;

    /** Inits default types in case the extension point has
     * errors or is unavailable. */
    private AddMetaNodePortTypeCollector() {
        this(new DataMetaNodePortType(),
                new DBMetaNodePortType(),
                new PMMLMetaNodePortType(),
                new FlowVarMetaNodePortType());
    }

    private AddMetaNodePortTypeCollector(final MetaNodePortType... types) {
        m_types = types;
    }

    /** @return the types */
    public MetaNodePortType[] getTypes() {
        return m_types;
    }

}
