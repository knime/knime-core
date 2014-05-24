/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.ui.metainfo.model;

import java.util.Calendar;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Fabian Dill, KNIME.com AG
 */
public class DateMetaGUIElement extends MetaGUIElement {

    private static final String SEPARATOR = "/";

    private static final String FORM_TYPE = "date";

    private int m_day;

    private int m_month;

    private int m_year;


    public DateMetaGUIElement(final String label, final String value,
            final boolean isReadOnly) {
        super(label, value, isReadOnly);
        // if value == null -> current date
        if (value == null || value.trim().isEmpty()) {
            Calendar c = Calendar.getInstance();
            m_day = c.get(Calendar.DAY_OF_MONTH);
            m_month = c.get(Calendar.MONTH);
            m_year = c.get(Calendar.YEAR);
        } else {
            // else -> load it
            String[] fields = value.split(SEPARATOR);
            m_day = Integer.parseInt(fields[0].trim());
            m_month = Integer.parseInt(fields[1].trim());
            m_year = Integer.parseInt(fields[2].trim());
        }
    }

    /**
     * Sets the represented date to the current time.
     */
    public void updateDate() {
        Calendar date = Calendar.getInstance();
        m_day = date.get(Calendar.DAY_OF_MONTH);
        m_month = date.get(Calendar.MONTH);
        m_year = date.get(Calendar.YEAR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Control createGUIElement(final FormToolkit toolkit,
            final Composite parent) {
        Composite date = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(6, false);
        layout.horizontalSpacing = 10;
        date.setLayout(layout);

        toolkit.createLabel(date, "Day:");
        createDayCombo(date);

        toolkit.createLabel(date, "Month:");
        createMonthCombo(date);

        toolkit.createLabel(date, "Year:");
        createYearCombo(date);
        return date;
    }




    private void createYearCombo(final Composite parent) {
        // must not be rw (resize problem on linux:
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=218224)
        final Combo year = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        int endYear = Calendar.getInstance().get(Calendar.YEAR) + 10;
        for (int i = 2008; i <= endYear; i++) {
            year.add(Integer.toString(i));
        }
        // current year
        year.select(year.indexOf(Integer.toString(m_year)));
        year.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                int selectionIdx = year.getSelectionIndex();
                if (selectionIdx >= 0) {
                    String yearString = year.getItem(selectionIdx);
                    m_year = Integer.parseInt(yearString);
                } else {
                    showSelectPrompt("Year");
                }
            }

        });
        year.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                fireModifiedEvent(e);
            }

        });
        year.setEnabled(!isReadOnly());
    }




    private void createMonthCombo(final Composite parent) {
        // must not be rw (resize problem on linux:
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=218224)
        final Combo month = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        month.add("01");
        month.add("02");
        month.add("03");
        month.add("04");
        month.add("05");
        month.add("06");
        month.add("07");
        month.add("08");
        month.add("09");
        month.add("10");
        month.add("11");
        month.add("12");
        // current month
        month.select(m_month);
        month.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                int index = month.getSelectionIndex();
                if (index < 0) {
                    showSelectPrompt("Month");
                } else {
                    m_month = index;
                }
            }
        });
        month.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                fireModifiedEvent(e);
            }

        });
        month.setEnabled(!isReadOnly());
    }




    private void createDayCombo(final Composite parent) {
        // must not be rw (resize problem on linux:
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=218224)
        final Combo day = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        day.add("01");
        day.add("02");
        day.add("03");
        day.add("04");
        day.add("05");
        day.add("06");
        day.add("07");
        day.add("08");
        day.add("09");
        day.add("10");
        day.add("11");
        day.add("12");
        day.add("13");
        day.add("14");
        day.add("15");
        day.add("16");
        day.add("17");
        day.add("18");
        day.add("19");
        day.add("20");
        day.add("21");
        day.add("22");
        day.add("23");
        day.add("24");
        day.add("25");
        day.add("26");
        day.add("27");
        day.add("28");
        day.add("29");
        day.add("30");
        day.add("31");
        // current day
        day.select(m_day - 1);
        day.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                int index = day.getSelectionIndex();
                if (index < 0) {
                    showSelectPrompt("Day");
                } else {
                    m_day = index + 1;
                }
            }
        });
        day.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                fireModifiedEvent(e);
            }

        });
        day.setEnabled(!isReadOnly());
    }




    /**
     * {@inheritDoc}
     */
    @Override
    public void saveTo(final TransformerHandler parentElement)
        throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA",
                FORM_TYPE);
        atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA", getLabel());
        atts.addAttribute(null, null, MetaGUIElement.READ_ONLY, "CDATA",
                "" + isReadOnly());
        parentElement.startElement(null, null, MetaGUIElement.ELEMENT, atts);
        String dateString = createStorageString(m_day, m_month, m_year);
        char[] date = dateString.toCharArray();
        parentElement.characters(date, 0, date.length);
        parentElement.endElement(null, null, MetaGUIElement.ELEMENT);
    }


    private void showSelectPrompt(final String missingField) {
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                MessageDialog.openWarning(Display.getDefault().getActiveShell(),
                        "Please select...",
                        missingField + " is empty. Please select");
            }

        });
    }

    /**
     * Return the string representation that is used to store the date within
     * XML.
     * @param day 1-31
     * @param month 0-11
     * @param year 2008 - 2015 (for now)
     * @return string representation
     */
    public static String createStorageString(final int day, final int month,
            final int year) {
        return day + SEPARATOR + month + SEPARATOR + year;
    }

}
