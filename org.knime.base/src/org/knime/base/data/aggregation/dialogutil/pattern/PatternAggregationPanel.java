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
 *   18.07.2014 (koetter): created
 */
package org.knime.base.data.aggregation.dialogutil.pattern;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.dialogutil.AbstractAggregationPanel;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionAndRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer.ValueRenderer;
import org.knime.base.data.aggregation.dialogutil.BooleanCellRenderer;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public class PatternAggregationPanel
extends AbstractAggregationPanel<PatternAggregationTableModel, PatternAggregator, Object> {
    /**The default title of the panel to display in a dialog.*/
    public static final String DEFAULT_TITLE = "Pattern Based Aggregation";

    private static final int REGEX_SIZE = 45;

    private String m_key;

    /**
     * Constructor.
     * @param key the unique settings key
     */
    public PatternAggregationPanel(final String key) {
        super("Aggregation Settings", "Data types", new DefaultListCellRenderer(), "Aggregation methods",
            new PatternAggregationTableModel());
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty");
        }
        m_key = key;
        getTableModel().setRootPanel(getComponentPanel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Component createListComponent(final String title) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Component createButtonComponent() {
        final JButton addRegexButton = new JButton("Add");
        addRegexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final List<PatternAggregator> methods = new LinkedList<>();
                methods.add(new PatternAggregator(".*", true, AggregationMethods.getDefaultNotNumericalMethod()));
                addRows(methods);
            }
        });
        final JButton removeSelectedButton = new JButton("Remove");
        removeSelectedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onRemIt();
            }
        });
        final JButton removeAllButton = new JButton("Remove all");
        removeAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onRemAll();
            }
        });
        final int width = Math.max(Math.max(addRegexButton.getPreferredSize().width,
            removeSelectedButton.getPreferredSize().width), removeAllButton.getPreferredSize().width);
        final Dimension dimension = new Dimension(width, 150);
        final JPanel panel = new JPanel(new GridBagLayout());
//        panel.setBorder(BorderFactory.createBevelBorder(1));
        panel.setPreferredSize(dimension);
        panel.setMaximumSize(dimension);
        panel.setMinimumSize(dimension);
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.PAGE_START;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(addRegexButton, c);
        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(new JLabel(), c);
        c.weighty = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.gridy++;
        panel.add(removeSelectedButton, c);
        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(new JLabel(), c);
        c.weighty = 0;
        c.anchor = GridBagConstraints.PAGE_END;
        c.gridy++;
        panel.add(removeAllButton, c);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(
            new AggregationFunctionRowTableCellRenderer<>(new ValueRenderer<PatternAggregator>() {
                @Override
                public void renderComponent(final DefaultTableCellRenderer c, final PatternAggregator row) {
                    final String regex = row.getInputPattern();
                    c.setText(regex);
                }
            }, true, "Double click to change search pattern. Right mouse click for context menu."));
        columnModel.getColumn(0).setCellEditor(new PatternTableCellEditor());
        columnModel.getColumn(1).setCellRenderer(new BooleanCellRenderer(
            "Tick if the pattern is a regular expression"));
        columnModel.getColumn(1).setMinWidth(REGEX_SIZE);
        columnModel.getColumn(1).setMaxWidth(REGEX_SIZE);
        columnModel.getColumn(2).setCellEditor(new PatternAggregatorTableCellEditor());
        columnModel.getColumn(2).setCellRenderer(new AggregationFunctionAndRowTableCellRenderer());
        columnModel.getColumn(0).setPreferredWidth(250);
        columnModel.getColumn(1).setPreferredWidth(150);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPopupMenu createTablePopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem invalidRowsMenu = createInvalidRowsSelectionMenu();
        if (invalidRowsMenu != null) {
            menu.add(invalidRowsMenu);
            menu.addSeparator();
        }
        if (getNoOfSelectedRows() < getNoOfTableRows()) {
            //add this option only if at least one row is not selected
            final JMenuItem item =
                new JMenuItem("Select all");
            item.addActionListener(new ActionListener() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    selectAllRows();
                }
            });
            menu.add(item);
        }
        appendMissingValuesEntry(menu);
        return menu;
    }

    /**
     * @param settings {@link NodeSettingsRO}
     * @param spec {@link DataTableSpec}
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
    throws InvalidSettingsException {
        final List<PatternAggregator> aggregators = PatternAggregator.loadAggregators(settings, m_key, spec);
        initialize(Collections.EMPTY_LIST, aggregators, spec);
    }

    /**
     * @param settings {@link NodeSettingsWO}
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        PatternAggregator.saveAggregators(settings, m_key, getTableModel().getRows());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PatternAggregator createRow(final Object selectedListElement) {
        return new PatternAggregator(".*", true, AggregationMethods.getDefaultNotNumericalMethod());
    }
}
