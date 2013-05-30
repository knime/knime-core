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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on 23.05.2013 by thor
 */
package org.knime.workbench.ui.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.renderer.DataValueRendererFactory;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author thor
 */
public class PreferredRendererPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    private static final ScopedPreferenceStore CORE_STORE = new ScopedPreferenceStore(InstanceScope.INSTANCE,
        FrameworkUtil.getBundle(DataValueRendererFactory.class).getSymbolicName());

    private static final Comparator<ExtensibleUtilityFactory> utilityFactoryComparator =
        new Comparator<ExtensibleUtilityFactory>() {
            @Override
            public int compare(final ExtensibleUtilityFactory o1, final ExtensibleUtilityFactory o2) {
                int rendererCount1 = o1.getAvailableRenderers().size();
                int rendererCount2 = o2.getAvailableRenderers().size();

                if ((rendererCount1 < 2) && (rendererCount2 < 2)) {
                    return o1.getName().compareTo(o2.getName());
                } else if (rendererCount1 < 2) {
                    return 1;
                } else if (rendererCount2 < 2) {
                    return -1;
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            }
        };

    private static final Comparator<DataValueRendererFactory> rendererFactoryComparator =
        new Comparator<DataValueRendererFactory>() {
            @Override
            public int compare(final DataValueRendererFactory o1, final DataValueRendererFactory o2) {
                return o1.getDescription().compareTo(o2.getDescription());
            }
        };

    /**
     *
     */
    public PreferredRendererPreferencePage() {
        super(GRID);
        setDescription("Select the preferred renderer that should be used in table views or reports for any available"
            + " data type. The selection for data types that have only one renderer is disabled.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        Map<String, List<ExtensibleUtilityFactory>> groupedUtilityFactories =
            new HashMap<String, List<ExtensibleUtilityFactory>>();

        // TODO retrieve the utility factories from the data type extension point once we have it
        for (ExtensibleUtilityFactory fac : ExtensibleUtilityFactory.getAllFactories()) {
            List<ExtensibleUtilityFactory> groupList = groupedUtilityFactories.get(fac.getGroupName());
            if (groupList == null) {
                groupList = new ArrayList<ExtensibleUtilityFactory>(16);
                groupedUtilityFactories.put(fac.getGroupName(), groupList);
            }
            groupList.add(fac);
        }

        List<String> groupNames = new ArrayList<String>(groupedUtilityFactories.keySet());
        Collections.sort(groupNames);

        for (String group : groupNames) {
            LabelField separator = new LabelField(getFieldEditorParent(), group);
            addField(separator);

            List<ExtensibleUtilityFactory> utilityFactories = groupedUtilityFactories.get(group);
            Collections.sort(utilityFactories, utilityFactoryComparator);
            for (ExtensibleUtilityFactory utilFac : utilityFactories) {
                List<DataValueRendererFactory> rendererFactories =
                    new ArrayList<DataValueRendererFactory>(utilFac.getAvailableRenderers());
                Collections.sort(rendererFactories, rendererFactoryComparator);

                String[][] comboEntries = new String[utilFac.getAvailableRenderers().size()][2];
                int i = 0;
                for (DataValueRendererFactory rendFac : rendererFactories) {
                    comboEntries[i++] = new String[]{rendFac.getDescription(), rendFac.getId()};
                }

                Composite parent = getFieldEditorParent();
                ComboFieldEditor c =
                    new ComboFieldEditor(utilFac.getPreferenceKey(), utilFac.getName(), comboEntries, parent);
                c.setEnabled(comboEntries.length > 1, parent);
                addField(c);
            }
        }
    }

//    private Composite createSection(final String title) {
//        ExpandableComposite section =
//            new ExpandableComposite(getFieldEditorParent(), ExpandableComposite.TWISTIE
//                | ExpandableComposite.CLIENT_INDENT);
//        section.setBackground(null);
//        section.setText(title);
//        Composite composite = new Composite(section, SWT.NONE);
//        composite.setLayout(new GridLayout(2, false));
//        GridData data = new GridData();
//        data.horizontalAlignment = SWT.FILL;
//        data.grabExcessHorizontalSpace = true;
//        composite.setLayoutData(data);
//        section.setClient(composite);
//        return composite;
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(CORE_STORE);
    }
}
