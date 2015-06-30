/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.timeseries.node.timemissvaluehandler;

import static org.knime.core.node.util.DataColumnSpecListCellRenderer.createInvalidSpec;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestEvent;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestListener;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfiguredColumnDeterminer;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ListModifier;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.SearchedItemsSelectionMode;
import org.knime.timeseries.node.timemissvaluehandler.TimeMissingValueHandlingColSetting.ConfigType;

/**
 * Dialog to the timeseries missing value handling node.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Marcel Hanser, University of Konstanz
 * @deprecated See new missing node that incorporates time series handling in package
 * org.knime.base.node.preproc.pmml.missingval
 */
@Deprecated
public class TimeMissingValueHandlingNodeDialogPane extends NodeDialogPane {
    /**
     *
     */
    private static final String INCOMPATIBLE_COLUMN = "!---INCOMPATIBLE_COLUMN---!";

    private static final TimeMissingValueHandlingPanel DUMMY_PANEL = new TimeMissingValueHandlingPanel(
        new TimeMissingValueHandlingColSetting(ConfigType.NON_NUMERICAL));

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TimeMissingValueHandlingNodeDialogPane.class);

    private final JPanel m_defaultsPanel;

    private final JPanel m_individualsPanel;

    private final JButton m_addButton;

    private final ColumnSelectionSearchableListPanel m_searchableListPanel;

    private ListModifier m_searchableListModifier;

    /**
     * Constructs new dialog with an appropriate dialog title.
     */
    public TimeMissingValueHandlingNodeDialogPane() {
        super();
        // Default handling, first tab
        m_defaultsPanel = new JPanel(new GridLayout(0, 1));
        addTab("Default", new JScrollPane(m_defaultsPanel));

        // Individual Handling, second tab
        m_searchableListPanel =
            new ColumnSelectionSearchableListPanel(SearchedItemsSelectionMode.SELECT_ALL,
                new ConfiguredColumnDeterminer() {

                    @Override
                    public boolean isConfiguredColumn(final DataColumnSpec cspec) {

                        final Component[] c = m_individualsPanel.getComponents();
                        for (int j = 0; j < c.length; j++) {
                            final TimeMissingValueHandlingPanel p = (TimeMissingValueHandlingPanel)c[j];
                            if (p.getSettings().isMetaConfig()) {
                                continue;
                            }
                            final List<String> names = Arrays.asList(p.getSettings().getNames());

                            if (names.contains(cspec.getName())) {
                                return true;
                            }
                        }
                        return false;
                    }
                });

        m_searchableListPanel.addConfigurationRequestListener(new ConfigurationRequestListener() {
            @Override
            public void configurationRequested(final ConfigurationRequestEvent event) {
                checkButtonStatus();
                if (event.getType().equals(ConfigurationRequestEvent.Type.CREATION)) {
                    if (!m_addButton.isEnabled()) {
                        return;
                    }
                    onAdd(m_searchableListPanel.getSelectedColumns());
                }
            }
        });

        final JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.add(m_searchableListPanel, BorderLayout.CENTER);

        m_individualsPanel = new IndividualsPanel();
        JScrollPane scroller = new JScrollPane(m_individualsPanel);
        tabPanel.add(scroller, BorderLayout.EAST);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_addButton = new JButton("Add");
        m_addButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                onAdd(m_searchableListPanel.getSelectedColumns());
            }
        });
        buttonPanel.add(m_addButton);
        tabPanel.add(buttonPanel, BorderLayout.SOUTH);
        addTab("Individual", tabPanel);
    }

    /** Enables/disables the button according to list selection. */
    private void checkButtonStatus() {
        List<DataColumnSpec> selectedColumns = m_searchableListPanel.getSelectedColumns();
        if (selectedColumns.isEmpty()) {
            m_addButton.setEnabled(false);
        } else {
            // at least one item is selected, get its type and compare it
            // with all the other selected elements
            final ConfigType type = TimeMissValueNodeModel.initType(selectedColumns.get(0));
            for (DataColumnSpec dcs : selectedColumns) {
                ConfigType type2 = TimeMissValueNodeModel.initType(dcs);
                if (!type.equals(type2)) {
                    m_addButton.setEnabled(false);
                    return;
                }
            }
            final Component[] c = m_individualsPanel.getComponents();
            for (int i = 0; i < c.length; i++) {
                TimeMissingValueHandlingPanel p = (TimeMissingValueHandlingPanel)c[i];
                if (p.getSettings().isMetaConfig()) {
                    continue;
                }
                final List<String> names = Arrays.asList(p.getSettings().getNames());
                for (DataColumnSpec dcs : selectedColumns) {
                    if (names.contains(dcs.getName())) {
                        m_addButton.setEnabled(false);
                        return;
                    }
                }
            }
            m_addButton.setEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        m_searchableListModifier = m_searchableListPanel.update(specs[0]);

        TimeMissingValueHandlingColSetting[] defaults =
            TimeMissingValueHandlingColSetting.loadMetaColSettings(settings, specs[0]);
        TimeMissingValueHandlingColSetting[] individuals =
            TimeMissingValueHandlingColSetting.loadIndividualColSettings(settings, specs[0]);

        m_defaultsPanel.removeAll();
        for (int i = 0; i < defaults.length; i++) {
            final TimeMissingValueHandlingPanel p =
                new TimeMissingValueHandlingPanel(defaults[i], (DataColumnSpec)null);
            m_defaultsPanel.add(p);
        }
        m_individualsPanel.removeAll();
        Set<String> invalidColumns = new LinkedHashSet<String>();
        for (int i = 0; i < individuals.length; i++) {
            String[] names = individuals[i].getNames();
            ArrayList<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
            for (int j = 0; j < names.length; j++) {
                final DataColumnSpec cspec = specs[0].getColumnSpec(names[j]);
                if (cspec == null) {
                    LOGGER.debug("No such column in spec: " + names[j]);
                    DataColumnSpec createUnkownSpec = createUnkownSpec(names[j], individuals[i]);
                    colSpecs.add(createUnkownSpec);
                    invalidColumns.add(names[j]);
                } else {
                    colSpecs.add(cspec);
                }
            }
            if (!colSpecs.isEmpty()) {
                names = new String[colSpecs.size()];
                for (int j = 0; j < names.length; j++) {
                    names[j] = colSpecs.get(j).getName();
                }
                individuals[i].setNames(names);

                markIncompatibleTypedColumns(individuals[i].getType(), colSpecs);

                final TimeMissingValueHandlingPanel p =
                    new TimeMissingValueHandlingPanel(individuals[i], colSpecs.toArray(new DataColumnSpec[0]));

                p.registerMouseListener(new MouseAdapter() {
                    /** {@inheritDoc} */
                    @Override
                    public void mouseClicked(final MouseEvent me) {
                        selectColumns(p.getSettings());
                    }
                });
                addToIndividualPanel(p);
            }
        }
        m_searchableListModifier.addInvalidColumns(invalidColumns.toArray(new String[invalidColumns.size()]));
        m_individualsPanel.setPreferredSize(m_defaultsPanel.getPreferredSize());
        checkButtonStatus();
    }

    private static void markIncompatibleTypedColumns(final ConfigType type, final List<DataColumnSpec> colSpecs) {
        ListIterator<DataColumnSpec> iterator = colSpecs.listIterator();
        while (iterator.hasNext()) {
            DataColumnSpec dataColumnSpec = iterator.next();
            if (isIncompatible(type, dataColumnSpec)) {
                iterator.remove();
                iterator.add(createAsIncompatibleMarkedColumnSpec(dataColumnSpec));
            }
        }
    }

    private static DataColumnSpec createAsIncompatibleMarkedColumnSpec(final DataColumnSpec originalSpec) {
        DataColumnSpecCreator creator = new DataColumnSpecCreator(originalSpec);
        final DataColumnProperties origProps = originalSpec.getProperties();
        final Map<String, String> map = createIncompatiblePropertiesMap();
        final DataColumnProperties props;
        if (origProps != null) {
            props = origProps.cloneAndOverwrite(map);
        } else {
            props = new DataColumnProperties(map);
        }
        creator.setProperties(props);
        final DataColumnSpec invalidSpec = creator.createSpec();
        return invalidSpec;
    }

    private static Map<String, String> createIncompatiblePropertiesMap() {
        Map<String, String> toReturn = new HashMap<String, String>();
        toReturn.put(INCOMPATIBLE_COLUMN, INCOMPATIBLE_COLUMN);
        return toReturn;
    }

    /**
     * @param spec the spec to check
     * @return <code>true</code> if the given spec is marked as incompatible.
     */
    static boolean isIncompatible(final DataColumnSpec spec) {
        return spec.getProperties().containsProperty(INCOMPATIBLE_COLUMN);
    }

    /**
     * @param type the expected type
     * @param dataColumnSpec the spec to check
     * @return <code>false</code> if the actual type of the dataColumnSpec is not compatible to the expected one
     */
    static boolean isIncompatible(final ConfigType type, final DataColumnSpec dataColumnSpec) {
        ConfigType colType = TimeMissValueNodeModel.initType(dataColumnSpec);
        return !type.equals(colType);
    }

    private DataColumnSpec createUnkownSpec(final String string, final TimeMissingValueHandlingColSetting individuals) {
        DataType type = null;
        switch (individuals.getType()) {
            case NON_NUMERICAL:
                type = StringCell.TYPE;
                break;
            case NUMERICAL:
                type = DoubleCell.TYPE;
                break;
            default:
                type = DataType.getType(MissingCell.class);
                break;
        }
        return createInvalidSpec(string, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        Component[] cs = m_defaultsPanel.getComponents();
        TimeMissingValueHandlingColSetting[] defaults = new TimeMissingValueHandlingColSetting[cs.length];
        for (int i = 0; i < defaults.length; i++) {
            defaults[i] = ((TimeMissingValueHandlingPanel)cs[i]).getSettings();
        }
        cs = m_individualsPanel.getComponents();
        TimeMissingValueHandlingColSetting[] individuals = new TimeMissingValueHandlingColSetting[cs.length];
        for (int i = 0; i < individuals.length; i++) {
            individuals[i] = ((TimeMissingValueHandlingPanel)cs[i]).getSettings();
        }
        TimeMissingValueHandlingColSetting.saveMetaColSettings(defaults, settings);
        TimeMissingValueHandlingColSetting.saveIndividualsColSettings(individuals, settings);
    }

    private void onAdd(final List<DataColumnSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        final TimeMissingValueHandlingPanel p = new TimeMissingValueHandlingPanel(specs);
        p.registerMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent me) {
                selectColumns(p.getSettings());
            }
        });
        addToIndividualPanel(p);
        checkButtonStatus();
    }

    private void removeFromIndividualPanel(final TimeMissingValueHandlingPanel panel) {
        for (String name : panel.getSettings().getNames()) {
            if (m_searchableListPanel.isAdditionalColumn(name)) {
                m_searchableListModifier.removeAdditionalColumn(name);
            }
        }
        m_individualsPanel.remove(panel);
        m_individualsPanel.revalidate();
        m_individualsPanel.repaint();
        m_searchableListPanel.revalidate();
        m_searchableListPanel.repaint();
        checkButtonStatus();
    }

    private void addToIndividualPanel(final TimeMissingValueHandlingPanel panel) {
        panel.addPropertyChangeListener(TimeMissingValueHandlingPanel.REMOVE_ACTION, new PropertyChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                removeFromIndividualPanel((TimeMissingValueHandlingPanel)evt.getSource());
            }
        });
        panel.addPropertyChangeListener(TimeMissingValueHandlingPanel.REMOVED_INVALID_COLUMNS,
            new PropertyChangeListener() {
                /** {@inheritDoc} */
                @Override
                public void propertyChange(final PropertyChangeEvent evt) {
                    DataColumnSpec[] removedSpecs = (DataColumnSpec[])evt.getNewValue();
                    if (removedSpecs != null) {
                        for (DataColumnSpec spec : removedSpecs) {
                            if (m_searchableListPanel.isAdditionalColumn(spec.getName())) {
                                m_searchableListModifier.removeAdditionalColumn(spec.getName());
                            }
                        }
                    }
                    checkButtonStatus();
                    m_searchableListPanel.repaint();
                }
            });
        m_individualsPanel.add(panel);
        m_individualsPanel.revalidate();
        //        force the outer parent to render out first individual if there does none exist previously
        //        getPanel().repaint();
    }

    private void selectColumns(final TimeMissingValueHandlingColSetting setting) {
        if (setting.isMetaConfig()) {
            return;
        }
        m_searchableListPanel.setSelectedColumns(setting.getNames());
        m_searchableListPanel.ensureSelectedColumnsVisible();
    }

    /**
     * Panel hosting the individual panels. It implements {@link Scrollable} to allow for correct jumping to the next
     * enclosed panel. It allows overwrites getPreferredSize() to return the sum of all individual heights.
     */
    @SuppressWarnings("serial")
    private static class IndividualsPanel extends JPanel implements Scrollable {

        /** Set box layout. */
        public IndividualsPanel() {
            BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
            setLayout(layout);
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        /** {@inheritDoc} */
        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, //
            final int direction) {
            int rh = getComponentCount() > 0 ? getComponent(0).getHeight() : 0;
            return (rh > 0) ? Math.max(rh, (visibleRect.height / rh) * rh) : visibleRect.height;
        }

        /** {@inheritDoc} */
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return getComponentCount() > 0 ? getComponent(0).getHeight() : 100;
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getPreferredSize() {
            if (getComponentCount() < 1) {
                return DUMMY_PANEL.getPreferredSize();
            }
            int height = 0;
            int width = 0;
            for (Component c : getComponents()) {
                Dimension h = c.getPreferredSize();
                height += h.height;
                width = Math.max(width, h.width);
            }
            return new Dimension(width, height);
        }
    }
}
