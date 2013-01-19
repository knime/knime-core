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
 * -------------------------------------------------------------------
 *
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.viz.crosstable;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.util.DoubleFormat;
import org.knime.base.node.viz.crosstable.CrosstabNodeModel.CrosstabTotals;
import org.knime.base.node.viz.crosstable.CrosstabStatisticsCalculator.CrosstabStatistics;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeView;

/**
 * View for the cross tabulation node.
 *
 * @author Heiko Hofer
 */
public class CrosstabNodeView extends NodeView<CrosstabNodeModel> {

    private static final int DEFAULT_MAX_ROWS = 10;
    private static final int DEFAULT_MAX_COLS = 10;
    /** The text pane that holds the information. */
    private JEditorPane m_headerPane;
    private JEditorPane m_tablePane;
    private JEditorPane m_statPane;
    private CrosstabTable m_crosstab;
    private JPanel m_propsPanel;
    private final List<JCheckBox> m_propBoxes;
    private SpinnerNumberModel m_maxRows;
    private SpinnerNumberModel m_maxCols;


    /**
     * New instance.
     *
     * @param model the model to look at
     */
    public CrosstabNodeView(final CrosstabNodeModel model) {
        super(model);
        // define default visibility of crosstab properties
        m_propBoxes = new ArrayList<JCheckBox>();
        CrosstabProperties naming = CrosstabProperties.create(
                model.getSettings().getNamingVersion());
        for (String col : model.getSettings().getProperties()) {
            JCheckBox checkBox = new JCheckBox(col);

            if (col.equals(naming.getFrequencyName())
                    || col.equals(naming.getPercentName())
                    || col.equals(naming.getRowPercentName())
                    || col.equals(naming.getColPercentName())) {
                checkBox.setSelected(true);
            }
            m_propBoxes.add(checkBox);
        }
        // create content
        JPanel p = createMainPanel();
        p.setBackground(Color.white);
        JScrollPane scroller = new JScrollPane(p);
        scroller.setBackground(Color.white);
        setComponent(scroller);
    }

    /** The content of the view. */
    private JPanel createMainPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = createGridBagConstraints();
        c.gridwidth = 2;
        m_headerPane = new JEditorPane("text/html", "");
        m_headerPane.setEditable(false);
        p.add(m_headerPane, c);
        c.gridwidth = 1;

        c.gridy++;
        c.weightx = 1;
        m_tablePane = new JEditorPane("text/html", "");
        m_tablePane.setEditable(false);
        p.add(m_tablePane, c);

        c.weightx = 0;
        c.gridx++;
        p.add(createControlsPanel(), c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        m_statPane = new JEditorPane("text/html", "");
        m_statPane.setEditable(false);
        p.add(m_statPane, c);

        c.gridy++;
        c.weighty = 1;
        JPanel foo = new JPanel();
        foo.setBackground(Color.white);
        p.add(foo, c);

        return p;
    }

    private JPanel createControlsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.white);
        GridBagConstraints c = createGridBagConstraints();



        c.insets = new Insets(8, 3, 3, 3);
        m_propsPanel = new JPanel(new GridBagLayout());
        m_propsPanel.setBackground(Color.white);
        p.add(m_propsPanel, c);

        c.insets = new Insets(3, 3, 3, 3);
        c.gridy++;
        p.add(new JLabel("Max rows:"), c);
        c.gridy++;
        m_maxRows = new SpinnerNumberModel(DEFAULT_MAX_ROWS, 1,
                10000, 1);
        m_maxRows.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                reCreateCrosstabulation();
            }
        });
        p.add(new JSpinner(m_maxRows), c);

        c.insets = new Insets(8, 3, 3, 3);
        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Max columns:"), c);
        c.gridy++;
        c.insets = new Insets(3, 3, 3, 3);
        m_maxCols = new SpinnerNumberModel(DEFAULT_MAX_COLS, 1,
                10000, 1);
        m_maxCols.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                reCreateCrosstabulation();
            }
        });
        p.add(new JSpinner(m_maxCols), c);


        c.gridy++;
        c.weighty = 1;
        JPanel foo = new JPanel();
        foo.setBackground(Color.white);
        p.add(foo, c);
        return p;
    }

    /** Convenient method to create GridBagConstraints. */
    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 3, 3, 3);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        return c;
    }

    /**
     * Update the checkboxes that allow to edit the displayed properties
     * in the cross tabulation.
     */
    private void updatePropsPanel() {
        // remember selection
        Set<String> notSelected = new HashSet<String>();
        for (JCheckBox checkBox : m_propBoxes) {
            if (!checkBox.isSelected()) {
                notSelected.add(checkBox.getText());
            }
        }
        // remove props
        for (JCheckBox prop : m_propBoxes) {
            m_propsPanel.remove(prop);
        }
        m_propBoxes.clear();
        if (null != m_crosstab) {
            // create new
            GridBagConstraints c = createGridBagConstraints();
            c.insets = new Insets(3, 3, 3, 3);
            for (String prop : m_crosstab.getProperties()) {
                JCheckBox checkBox = new JCheckBox(prop);
                checkBox.setBackground(Color.white);
                checkBox.setSelected(!notSelected.contains(prop));
                checkBox.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        updateCrosstabulation();
                    }
                });
                m_propBoxes.add(checkBox);
                m_propsPanel.add(checkBox, c);
                c.gridy++;
            }
            c.weighty = 1;
            JPanel foo = new JPanel();
            foo.setBackground(Color.white);
            m_propsPanel.add(foo, c);
        }
        m_propsPanel.validate();
    }


    /** Update the display of the header. */
    private void updateHeader() {
        m_headerPane.setText(renderHeader());
        m_headerPane.revalidate();
    }

    /** Update the display of the cross tabulation. */
    private void updateCrosstabulation() {
        List<String> props = new ArrayList<String>();
        for (JCheckBox checkBox : m_propBoxes) {
            if (checkBox.isSelected()) {
                props.add(checkBox.getText());
            }
        }
        m_tablePane.setText(renderTable(props));
        m_tablePane.revalidate();
    }

    /** Update the display of the statistics. */
    private void updateStatistics() {
        m_statPane.setText(renderStatistics());
        m_statPane.revalidate();
    }

    private void reCreateCrosstabulation() {
        CrosstabNodeModel model = getNodeModel();
        if (model.isDataAvailable()) {
            int maxToDisplayRows = m_maxRows.getNumber().intValue();
            int maxToDisplayCols = m_maxCols.getNumber().intValue();
            if (null != m_crosstab) {
                // check if cross tabulation must be recreated
                int numDisplayedRows = m_crosstab.getRowVars().size();
                int numDisplayedCols = m_crosstab.getColVars().size();
                int numRows = m_crosstab.m_numSkippedRows + numDisplayedRows;
                int numCols = m_crosstab.m_numSkippedCols + numDisplayedCols;
                if (maxToDisplayRows > numRows
                        && numRows == numDisplayedRows
                        && maxToDisplayCols > numCols
                        && numCols == numDisplayedCols) {
                    // complete table already displayed
                    return;
                }
            }
            m_crosstab = new CrosstabTable(model, maxToDisplayRows,
                    maxToDisplayCols);
            updateCrosstabulation();
        } else {
            m_crosstab = null;
            updateCrosstabulation();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        reCreateCrosstabulation();
        if (null != m_crosstab) {
            updatePropsPanel();
            updateHeader();
            updateStatistics();
        }
    }

    /** Convenient method to create HTML Header. */
    private StringBuilder createHtmlHeader() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<html>\n");
        buffer.append("<head>\n");
        buffer.append("<style type=\"text/css\">\n");
        buffer.append("body {color:#333333;}");
        buffer.append("table {width: 100%;margin: 7px 0 7px 0;}");
        buffer.append("th {font-weight: bold;background-color: #aaccff;"
                + "vertical-align: bottom;}");
        buffer.append("td {padding: 4px 10px 4px 10px;}");
        buffer.append("th {padding: 4px 10px 4px 10px;}");
        buffer.append(".left {text-align: left}");
        buffer.append(".right {text-align: right}");
        buffer.append(".numeric {text-align: right}");
        buffer.append(".odd {background-color:#ddeeff;}");
        buffer.append(".even {background-color:#ffffff;}");
        buffer.append("</style>\n");
        buffer.append("</head>\n");
        return buffer;
    }

    /** Create HTML of the header. */
    private String renderHeader() {
        StringBuilder buffer = createHtmlHeader();
        buffer.append("<body>\n");
        buffer.append("<h2>Cross Tabulation of ");
        buffer.append(escapeHtml(m_crosstab.getExplanatoryVariable()));
        buffer.append(" by ");
        buffer.append(escapeHtml(m_crosstab.getResponseVariable()));
        buffer.append("</h2>");
        buffer.append("</body>\n");
        buffer.append("</html>\n");
        return buffer.toString();
    }

    /** Escape special html characters. */
    private String escapeHtml(final String str) {
        // escape the quote character
        String s = str.replace("&", "&amp;");
        // escape lower than
        s = s.replace("<", "&lt;");
        // escape greater than
        s = s.replace(">", "&gt;");
        // escape quote character
        s = s.replace("\"", "&quot;");
        return s;
    }


    /** Create HTML of the cross tabulation. */
    private String renderTable(final List<String> props) {
        StringBuilder buffer = createHtmlHeader();
        buffer.append("<body>\n");

        if (null != m_crosstab) {
            buffer.append("<table>\n");
            buffer.append("<tr>");
            buffer.append("<th class=\"left\">");
            for (Iterator<String> iter = props.iterator();
                    iter.hasNext();) {
                String prop = iter.next();
                buffer.append(prop);
                if (iter.hasNext()) {
                    buffer.append("<br/>");
                }
            }
            buffer.append("</th>");
            for (DataCell colCell : m_crosstab.getColVars()) {
                buffer.append("<th>");
                buffer.append(escapeHtml(colCell.toString()));
                buffer.append("</th>");
            }
            if (m_crosstab.getNumSkippedCols() > 0) {
                buffer.append("<th>");
                buffer.append("... (");
                buffer.append(m_crosstab.getNumSkippedCols());
                buffer.append(")");
                buffer.append("</th>");
            }
            buffer.append("<th>");
            buffer.append("Total");
            buffer.append("</th>");
            buffer.append("</tr>");


            int i = 0;
            for (CrosstabRow row : m_crosstab) {
                String cssClass = i % 2 == 0 ? "even" : "odd";
                renderRow(row, props, buffer, cssClass);
                i++;
            }
            if (m_crosstab.getNumSkippedRows() > 0) {
                String cssClass = i % 2 == 0 ? "even" : "odd";
                buffer.append("<tr class=\"" + cssClass + "\">\n");
                buffer.append("<td>");
                buffer.append("... (");
                buffer.append(m_crosstab.getNumSkippedRows());
                buffer.append(")");
                buffer.append("</td>");
                buffer.append("</tr>");
                i++;
            }
            CrosstabRow row = m_crosstab.getTotalRow();
            if (null != row) {
                String cssClass = i % 2 == 0 ? "even" : "odd";
                renderRow(row, props, buffer, cssClass);
            }
            buffer.append("</table>\n");

        } else {
            buffer.append("No data available.\n");
        }

        buffer.append("</body>\n");
        buffer.append("</html>\n");
        return buffer.toString();
    }

    /** Create HTML for the given row. */
    private void renderRow(final CrosstabRow row,
            final List<String> props,
            final StringBuilder buffer,
            final String cssClass) {
        boolean first = true;
        List<String> rowProps = new ArrayList<String>();
        rowProps.addAll(props);
        rowProps.retainAll(row.getProperties());
        for (String prop : rowProps) {
            if (first) {
                first = false;
                buffer.append("<tr class=\"" + cssClass + "\">\n");
                buffer.append("<td>");
                buffer.append(escapeHtml(row.getLabel()));
            } else {
                buffer.append("<tr class=\"" + cssClass + "\">\n");
                buffer.append("<td>");
            }
            for (CrosstabCell cell : row) {
                buffer.append("</td>\n<td class=\"numeric\">");
                buffer.append(cell.getFormatted(prop));
            }
            CrosstabCell cell = row.getTotalCell();
            if (null != cell) {
                buffer.append("</td>\n<td class=\"numeric\">");
                buffer.append(cell.getFormatted(prop));
                buffer.append("</td>\n");
                buffer.append("</tr>\n");
            }
        }
    }

    /** Create HTML for the statistics. */
    private String renderStatistics() {
        StringBuilder buffer = createHtmlHeader();
        buffer.append("<body>\n");
        if (null != m_crosstab) {
            buffer.append("<h2>Statistics for Table of ");
            buffer.append(m_crosstab.getExplanatoryVariable());
            buffer.append(" by ");
            buffer.append(m_crosstab.getResponseVariable());
            buffer.append("</h2>");
            buffer.append("<table>\n");
            buffer.append("<tr>");
            buffer.append("<th class=\"left\">Statistic</th>");
            buffer.append("<th>DF</th>");
            buffer.append("<th>Value</th>");
            buffer.append("<th>Prob</th>");
            buffer.append("</tr>");
            buffer.append("<tr>");
            CrosstabStatistics stats = getNodeModel().getStatistics();
            if (null != stats) {
                buffer.append("<td class=\"left\">Chi-Square</td>");
                buffer.append("<td class=\"numeric\">");
                buffer.append(stats.getChiSquaredDegreesOfFreedom());
                buffer.append("</td>");
                buffer.append("<td class=\"numeric\">");
                buffer.append(DoubleFormat.formatDouble(
                        stats.getChiSquaredStatistic()));
                buffer.append("</td>");
                buffer.append("<td class=\"numeric\">");
                buffer.append(DoubleFormat.formatDouble(
                        stats.getChiSquaredPValue()));
                buffer.append("</td>");
                buffer.append("</tr>");
                if (!Double.isNaN(stats.getFisherExactPValue())) {
                    buffer.append(
                            "<td class=\"left\">Fisher's Exact Test (2-tail)"
                            + "</td>");
                    buffer.append("<td class=\"numeric\">");
                    buffer.append("</td>");
                    buffer.append("<td class=\"numeric\">");
                    buffer.append("</td>");
                    buffer.append("<td class=\"numeric\">");
                    buffer.append(DoubleFormat.formatDouble(
                            stats.getFisherExactPValue()));
                    buffer.append("</td>");
                    buffer.append("</tr>");
                }
            }
            buffer.append("</table>\n");
            buffer.append("<p>Total sample size: ");
            buffer.append(getNodeModel().getTotals().getTotal());
            buffer.append("</p>");
        }
        buffer.append("</body>\n");
        buffer.append("</html>\n");
        return buffer.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // do nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // do nothing.
    }

    /**
     * Reads the data from the model and build the cross tabulation with
     * restrict number of rows and columns.
     *
     * @author Heiko Hofer
     */
    private static class CrosstabTable implements Iterable<CrosstabRow> {
        private final List<CrosstabRow> m_rows;
        private final CrosstabRow m_totalRow;
        private final List<DataCell> m_rowVars;
        private final List<DataCell> m_colVars;
        private final Collection<String> m_props;
        private final String m_responseVariable;
        private final String m_explanatoryVariable;
        private final int m_numSkippedCols;
        private final int m_numSkippedRows;

        /**
         * @param model the model
         * @param maxRows the maximum number of rows to display
         * @param maxCols the maximum number of columns to display
         */
        public CrosstabTable(final CrosstabNodeModel model,
                final int maxRows, final int maxCols) {
            int c = 0;
            m_rowVars = new ArrayList<DataCell>();
            for (DataCell cell : model.getTotals().getRowTotal().keySet()) {
                if (c < maxRows) {
                    m_rowVars.add(cell);
                } else {
                    break;
                }
                c++;
            }
            m_numSkippedRows = model.getTotals().getRowTotal().size()
                - m_rowVars.size();
            c = 0;
            m_colVars = new ArrayList<DataCell>();
            for (DataCell cell : model.getTotals().getColTotal().keySet()) {
                if (c < maxCols) {
                    m_colVars.add(cell);
                } else {
                    break;
                }
                c++;
            }
            m_numSkippedCols = model.getTotals().getColTotal().size()
                - m_colVars.size();


            CrosstabNodeSettings settings = model.getSettings();
            CrosstabProperties naming = CrosstabProperties.create(
                    settings.getNamingVersion());
            Map<String, Integer> propIndices = getPropIndices(model);
            // remove total props e.g. row total since they display
            // separately
            for (String key : getTotalPropIndices(
                    naming, propIndices).keySet()) {
                propIndices.remove(key);
            }
            m_props = propIndices.keySet();
            m_rows = createRows(model, m_rowVars, m_colVars,
                    m_numSkippedCols > 0);
            m_totalRow = createTotalRow(model, m_colVars,
                    m_numSkippedCols > 0);
            m_responseVariable = settings.getColVarColumn();
            m_explanatoryVariable = settings.getRowVarColumn();
        }


        /**
         * @return the responseVariable
         */
        String getResponseVariable() {
            return m_responseVariable;
        }


        /**
         * @return the explanatoryVariable
         */
        String getExplanatoryVariable() {
            return m_explanatoryVariable;
        }

        /**
         * Get the number of columns that are not displayed.
         * @return the number of skipped columns
         */
        int getNumSkippedCols() {
            return m_numSkippedCols;
        }

        /**
         * Get the number of rows that are not displayed.
         * @return the number of skipped rows
         */
        int getNumSkippedRows() {
            return m_numSkippedRows;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<CrosstabRow> iterator() {
            return m_rows.iterator();
        }

        /**
         * Get the row with the totals.
         *
         * @return row holding the totals
         */
        public CrosstabRow getTotalRow() {
            return m_totalRow;
        }

        /**
         * Get the displayed categories of the explanatory variable.
         * @return the displayed categories of the explanatory variable
         */
        public List<DataCell> getRowVars() {
            return m_rowVars;
        }

        /**
         * Get the displayed categories of the response variable.
         * @return the displayed categories of the response variable
         */
        public List<DataCell> getColVars() {
            return m_colVars;
        }

        /**
         * Get the crosstab properties e.g. Frequency, Percent, Row Percent...
         * @return the crosstab properties
         */
        public Collection<String> getProperties() {
            return m_props;
        }

        /**
         * @param model the model
         * @param rowVars the displayed categories of the explanatory variable
         * @param colVars the displayed categories of the response variable
         * @return the rows of the cross tabulation except the header and the
         * row with the totals.
         */
        private List<CrosstabRow> createRows(final CrosstabNodeModel model,
                final List<DataCell> rowVars, final List<DataCell> colVars,
                final boolean colsSkipped) {
            BufferedDataTable out = model.getOutTable();
            CrosstabNodeSettings settings = model.getSettings();
            CrosstabProperties naming = CrosstabProperties.create(
                    settings.getNamingVersion());
            CrosstabTotals totals = model.getTotals();
            Map<String, Integer> propIndices = getPropIndices(model);
            // remove total props e.g. row total since they display
            // separately
            for (String key : getTotalPropIndices(
                    naming, propIndices).keySet()) {
                propIndices.remove(key);
            }
            Iterator<DataRow> iter = out.iterator();
            DataRow row = iter.next();
            List<CrosstabRow> rows = new ArrayList<CrosstabRow>();

            for (DataCell rowVar : rowVars) {
                List<CrosstabCell> cells = new ArrayList<CrosstabCell>();
                for (DataCell colVar : colVars) {
                    if (null != row && rowVar.equals(row.getCell(0))
                            && colVar.equals(row.getCell(1))) {
                        Map<String, Double> props =
                            new LinkedHashMap<String, Double>();
                        for (String key : propIndices.keySet()) {
                            DataCell cell = row.getCell(propIndices.get(key));
                            if (!cell.isMissing()) {
                                Double v = ((DoubleValue)cell).getDoubleValue();
                                props.put(key, v);
                            } else {
                                props.put(key, null);
                            }
                        }
                        cells.add(new CrosstabCell(props));
                        // one step further in the output table of the
                        // cross tab node
                        row = iter.hasNext() ? iter.next() : null;
                    } else {
                        // create empty cell
                        cells.add(new CrosstabCell());
                    }
                }
                if (colsSkipped) {
                    // create empty cell
                    cells.add(new CrosstabCell());
                }
                // Create the total cell
                CrosstabCell totalCell = null;
                Map<String, Double> props =
                    new LinkedHashMap<String, Double>();
                if (propIndices.containsKey(naming.getFrequencyName())) {
                    // the frequency
                    props.put(naming.getFrequencyName(),
                            totals.getRowTotal().get(rowVar));
                }
                if (propIndices.containsKey(naming.getPercentName())) {
                    // the percent
                    props.put(naming.getPercentName(),
                            totals.getRowTotal().get(rowVar)
                            / totals.getTotal() * 100);
                }
                if (!props.isEmpty()) {
                    totalCell = new CrosstabCell(props);
                }
                CrosstabRow crosstabRow = new CrosstabRow(rowVar.toString(),
                        cells, totalCell, propIndices.keySet());
                rows.add(crosstabRow);
            }

            return rows;
        }

        /**
         * @param model the model
         * @param rowVars the displayed categories of the explanatory variable
         * @return the row with the totals of the cross tabulation
         */
        private CrosstabRow createTotalRow(final CrosstabNodeModel model,
                final List<DataCell> colVars,
                final boolean colsSkipped) {
            CrosstabNodeSettings settings = model.getSettings();
            CrosstabProperties naming = CrosstabProperties.create(
                    settings.getNamingVersion());
            CrosstabTotals totals = model.getTotals();
            Map<String, Integer> allProps = getPropIndices(model);
            Map<String, Integer> propIndices =
                new LinkedHashMap<String, Integer>();
            if (allProps.containsKey(naming.getFrequencyName())) {
                propIndices.put(naming.getFrequencyName(),
                        allProps.get(naming.getFrequencyName()));
            }
            if (allProps.containsKey(naming.getPercentName())) {
                propIndices.put(naming.getPercentName(),
                        allProps.get(naming.getPercentName()));
            }
            if (propIndices.isEmpty()) {
                return null;
            }

            List<CrosstabCell> cells = new ArrayList<CrosstabCell>();
            for (DataCell colVar : colVars) {
                // Create the total cell
                CrosstabCell totalCell = null;
                Map<String, Double> props =
                    new LinkedHashMap<String, Double>();
                if (propIndices.containsKey(naming.getFrequencyName())) {
                    // the frequency
                    props.put(naming.getFrequencyName(),
                            totals.getColTotal().get(colVar));
                }
                if (propIndices.containsKey(naming.getPercentName())) {
                    // the percent
                    props.put(naming.getPercentName(),
                            totals.getColTotal().get(colVar)
                            / totals.getTotal() * 100);
                }
                if (!props.isEmpty()) {
                    totalCell = new CrosstabCell(props);
                }
                cells.add(totalCell);
            }
            if (colsSkipped) {
                // create empty cell
                cells.add(new CrosstabCell());
            }
            // Create the total cell
            CrosstabCell totalCell = null;
            Map<String, Double> props =
                new LinkedHashMap<String, Double>();
            if (propIndices.containsKey(naming.getFrequencyName())) {
                // the frequency
                props.put(naming.getFrequencyName(), totals.getTotal());
            }
            if (propIndices.containsKey(naming.getPercentName())) {
                // the percent
                props.put(naming.getPercentName(), 100.0);
            }
            if (!props.isEmpty()) {
                totalCell = new CrosstabCell(props);
            }
            CrosstabRow crosstabRow = new CrosstabRow("Total",
                    cells, totalCell, propIndices.keySet());
            return crosstabRow;

        }

        private Map<String, Integer> getPropIndices(
                final CrosstabNodeModel model) {
            BufferedDataTable out = model.getOutTable();
            CrosstabNodeSettings settings = model.getSettings();
            CrosstabProperties naming = CrosstabProperties.create(
                    settings.getNamingVersion());

            DataTableSpec spec = out.getDataTableSpec();
            List<String> supportedProps = naming.getProperties();
            Map<String, Integer> props = new LinkedHashMap<String, Integer>();
            for (String prop : supportedProps) {
                conditionallyAddProp(props, spec, prop);
            }
            return props;
        }

        /** Convenient method to add a prop when it is found in the spec. */
        private void conditionallyAddProp(final Map<String, Integer> props,
                final DataTableSpec spec, final String colName) {
            int index = spec.findColumnIndex(colName);
            if (index >= 0) {
                props.put(colName, index);
            }
        }

        /** Convenient method to get the properties of the total row and their
         * column index in the output table.
         */
        private Map<String, Integer> getTotalPropIndices(
                final CrosstabProperties naming,
                final Map<String, Integer> propIndices) {
            Map<String, Integer> totalPropIndices =
                new LinkedHashMap<String, Integer>();
            if (propIndices.containsKey(naming.getTotalRowCountName())) {
                // the total row count
                totalPropIndices.put(naming.getTotalRowCountName(),
                        propIndices.get(naming.getTotalRowCountName()));
            }
            if (propIndices.containsKey(naming.getTotalColCountName())) {
                // the total column count
                totalPropIndices.put(naming.getTotalColCountName(),
                        propIndices.get(naming.getTotalColCountName()));
            }
            if (propIndices.containsKey(naming.getTotalCountName())) {
                // the total count
                totalPropIndices.put(naming.getTotalCountName(),
                        propIndices.get(naming.getTotalCountName()));
            }
            return totalPropIndices;
        }

    }

    /**
     * A row in the {@link CrosstabTable}.
     *
     * @author Heiko Hofer
     */
    private static class CrosstabRow implements Iterable<CrosstabCell> {
        private final String m_label;
        private final List<CrosstabCell> m_cells;
        private final CrosstabCell m_totalCell;
        private final Collection<String> m_props;

        /**
         * @param label the label of the row
         * @param cells the normal cells
         * @param totalCell the extra cell for the row total
         * @param props the properties provided by the cells of this row
         */
        public CrosstabRow(final String label, final List<CrosstabCell> cells,
                final CrosstabCell totalCell, final Collection<String> props) {
            super();
            m_label = label;
            m_cells = cells;
            m_props = props;
            m_totalCell = totalCell;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<CrosstabCell> iterator() {
            return m_cells.iterator();
        }

        /**
         * @return all available properties
         */
        public Collection<String> getProperties() {
            return m_props;
        }

        /**
         * @return the cell containing the totals
         */
        public CrosstabCell getTotalCell() {
            return m_totalCell;
        }

        /**
         * @return the label of this row
         */
        public String getLabel() {
            return m_label;
        }
    }

    /**
     * A cell in the {@linke CrosstabTable}.
     *
     * @author Heiko Hofer
     */
    private static class CrosstabCell {
        private final Map<String, Double> m_props;

        public CrosstabCell() {
            this(new HashMap<String, Double>());
        }
        /**
         * @param props the properties that are provided by this cell.
         */
        public CrosstabCell(final Map<String, Double> props) {
            super();
            m_props = props;
        }

        /**
         * @param prop the property e.g. Frequency, Percent, Row Percent
         * @return the formatted value of the property
         */
        public String getFormatted(final String prop) {
            if (m_props.containsKey(prop)) {
                Double value = m_props.get(prop);
                if (null != value) {
                    String formatted = DoubleFormat.formatDouble(value);
                    return formatted;
                } else {
                    return "?";
                }
            } else {
                return "";
            }
        }
    }

}
