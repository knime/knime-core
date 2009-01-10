/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class DateMetaGUIElement extends MetaGUIElement {
    
    private static final String SEPARATOR = "/";
    
    private static final String FORM_TYPE = "date";
    
    private int m_day;
    
    private int m_month;
    
    private int m_year;
    
    
    public DateMetaGUIElement(final String label, final String value) {
        super(label, value);
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
     * {@inheritDoc}
     */
    @Override
    public Control createGUIElement(FormToolkit toolkit, Composite parent) {
        Composite date = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(6, true);
        layout.horizontalSpacing = 10;
        date.setLayout(layout);        

        toolkit.createLabel(date, "Day:");
        final Combo day = new Combo(date, SWT.DROP_DOWN);
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
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
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
            public void modifyText(ModifyEvent e) {
                fireModifiedEvent(e);
            }
            
        });

        toolkit.createLabel(date, "Month:");
        final Combo month = new Combo(date, SWT.DROP_DOWN);
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
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = month.getSelectionIndex() ; 
                if (index < 0) {
                    showSelectPrompt("Month");
                } else {
                    m_month = index;
                }
            }
        });
        month.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fireModifiedEvent(e);
            }
            
        });
        
        toolkit.createLabel(date, "Year:");
        final Combo year = new Combo(date, SWT.DROP_DOWN);
        year.add("2008");
        year.add("2009");
        year.add("2010");
        year.add("2011");
        year.add("2012");
        year.add("2013");
        year.add("2014");
        year.add("2015");
        // current year
        year.select(year.indexOf("" + m_year));
        year.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIdx = year.getSelectionIndex();
                if (selectionIdx >= 0) {
                    String yearString = year.getItem(selectionIdx);
                    m_year = Integer.parseInt(yearString);
                    fireModifiedEvent(new ModifyEvent(new Event()));
                } else {
                    showSelectPrompt("Year");
                }
            }
            
        });
        year.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fireModifiedEvent(e);
            }
            
        });
        return date;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveTo(TransformerHandler parentElement) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA", 
                FORM_TYPE);
        atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA", getLabel());
        parentElement.startElement(null, null, MetaGUIElement.ELEMENT, atts);
        String dateString = m_day + SEPARATOR + m_month + SEPARATOR + m_year;
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

}
