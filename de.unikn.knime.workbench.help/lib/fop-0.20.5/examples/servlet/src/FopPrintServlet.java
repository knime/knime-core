/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */ 
import java.io.*;
import java.util.Vector ;

import java.awt.print.PrinterJob ;
import java.awt.print.PrinterException ;

import javax.servlet.*;
import javax.servlet.http.*;

import org.xml.sax.InputSource;

import org.apache.fop.apps.Driver;
import org.apache.fop.layout.Page;
import org.apache.fop.apps.XSLTInputHandler;
import org.apache.fop.messaging.MessageHandler;

import org.apache.fop.render.awt.AWTRenderer ;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;

/**
 * Example servlet to generate a fop printout from a servlet.
 * Printing goes to the default printer on host where the servlet executes.
 * Servlet param is:
 * <ul>
 *   <li>fo: the path to a formatting object file to render
 * </ul>
 *
 * Example URL: http://servername/fop/servlet/FopPrintServlet?fo=readme.fo
 * Example URL: http://servername/fop/servlet/FopPrintServlet?xml=data.xml&xsl=format.xsl
 * Compiling: you will need
 * - servlet_2_2.jar
 * - fop.jar
 * - sax api
 * - avalon-framework-x.jar (where x is the version found the FOP lib dir)
 *
 * Running: you will need in the WEB-INF/lib/ directory:
 * - fop.jar
 * - batik.jar
 * - avalon-framework-x.jar (where x is the version found the FOP lib dir)
 * - xalan-2.0.0.jar
 */

public class FopPrintServlet extends HttpServlet {
    public static final String FO_REQUEST_PARAM = "fo";
    public static final String XML_REQUEST_PARAM = "xml";
    public static final String XSL_REQUEST_PARAM = "xsl";
    Logger log = null;

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException {
        if (log == null) {
            log = new ConsoleLogger(ConsoleLogger.LEVEL_WARN);
            MessageHandler.setScreenLogger(log);
        }

        try {
            String foParam = request.getParameter(FO_REQUEST_PARAM);
            String xmlParam = request.getParameter(XML_REQUEST_PARAM);
            String xslParam = request.getParameter(XSL_REQUEST_PARAM);

            if (foParam != null) {
                FileInputStream file = new FileInputStream(foParam);
                renderFO(new InputSource(file), response);
            } else if ((xmlParam != null) && (xslParam != null)) {
                XSLTInputHandler input =
                  new XSLTInputHandler(new File(xmlParam),
                                       new File(xslParam));
                renderXML(input, response);
            } else {
                response.setContentType ("text/html");

                PrintWriter out = response.getWriter();
                out.println("<html><title>Error</title>\n"+ "<body><h1>FopServlet Error</h1><h3>No 'fo' or 'xml/xsl' "+
                            "request param given.</h3></body></html>");
            }
        } catch (ServletException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    /**
     * Renders an FO inputsource to the default printer.
     */
    public void renderFO(InputSource foFile,
                         HttpServletResponse response) throws ServletException {
        try {
            Driver driver = new Driver(foFile, null);
            PrinterJob pj = PrinterJob.getPrinterJob();
            PrintRenderer renderer = new PrintRenderer(pj);

            driver.setLogger (log);
            driver.setRenderer(renderer);
            driver.run();

            reportOK (response);
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    /**
     * Renders an FO generated using an XML and a stylesheet to the default printer.
     */
    public void renderXML(XSLTInputHandler input,
                          HttpServletResponse response) throws ServletException {
        try {
            Driver driver = new Driver();
            PrinterJob pj = PrinterJob.getPrinterJob();
            PrintRenderer renderer = new PrintRenderer(pj);

            pj.setCopies(1);

            driver.setLogger (log);
            driver.setRenderer (renderer);
            driver.render (input.getParser(), input.getInputSource());

            reportOK (response);
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    // private helper, tell (browser) user that file printed
    private void reportOK (HttpServletResponse response)
    throws ServletException {
        String sMsg =
          "<html><title>Success</title>\n" + "<body><h1>FopPrintServlet: </h1>" +
          "<h3>The requested data was printed</h3></body></html>" ;

        response.setContentType ("text/html");
        response.setContentLength (sMsg.length());

        try {
            PrintWriter out = response.getWriter();
            out.println (sMsg);
            out.flush();
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    // This is stolen from PrintStarter
    class PrintRenderer extends AWTRenderer {

        private static final int EVEN_AND_ALL = 0;
        private static final int EVEN = 1;
        private static final int ODD = 2;

        private int startNumber;
        private int endNumber;
        private int mode = EVEN_AND_ALL;
        private int copies = 1;
        private PrinterJob printerJob;

        PrintRenderer(PrinterJob printerJob) {
            super(null);

            this.printerJob = printerJob;
            startNumber = 0 ;
            endNumber = -1;

            printerJob.setPageable(this);

            mode = EVEN_AND_ALL;
            String str = System.getProperty("even");
            if (str != null) {
                try {
                    mode = Boolean.valueOf(str).booleanValue() ? EVEN : ODD;
                } catch (Exception e) {}

            }
        }

        public void stopRenderer(OutputStream outputStream)
        throws IOException {
            super.stopRenderer(outputStream);

            if (endNumber == -1)
                endNumber = getPageCount();

            Vector numbers = getInvalidPageNumbers();
            for (int i = numbers.size() - 1; i > -1; i--)
                removePage(
                  Integer.parseInt((String) numbers.elementAt(i)));

            try {
                printerJob.print();
            } catch (PrinterException e) {
                e.printStackTrace();
                throw new IOException("Unable to print: " +
                                      e.getClass().getName() + ": " + e.getMessage());
            }
        }

        public void renderPage(Page page) {
            pageWidth = (int)((float) page.getWidth() / 1000f);
            pageHeight = (int)((float) page.getHeight() / 1000f);
            super.renderPage(page);
        }


        private Vector getInvalidPageNumbers() {

            Vector vec = new Vector();
            int max = getPageCount();
            boolean isValid;
            for (int i = 0; i < max; i++) {
                isValid = true;
                if (i < startNumber || i > endNumber) {
                    isValid = false;
                } else if (mode != EVEN_AND_ALL) {
                    if (mode == EVEN && ((i + 1) % 2 != 0))
                        isValid = false;
                    else if (mode == ODD && ((i + 1) % 2 != 1))
                        isValid = false;
                }

                if (!isValid)
                    vec.add(i + "");
            }

            return vec;
        }
    } // class PrintRenderer

}

