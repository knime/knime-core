/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 * History
 *    11.01.2007 (Tobias Koetter): created
 */

package org.knime.ext.ainet.unittests.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.ext.ainet.core.netimpl.hibernateresources.AiNetHibernateUtil;
import org.knime.ext.ainet.data.textmining.DocumentAuthorFactory;
import org.knime.ext.ainet.data.textmining.DocumentFactory;
import org.knime.ext.ainet.data.textmining.ProcessingInfoFactory;
import org.knime.ext.ainet.data.textmining.TermDocumentFrequenceSetFactory;
import org.knime.ext.ainet.data.textmining.TermFactory;
import org.knime.ext.textmining.data.Document;
import org.knime.ext.textmining.data.DocumentAuthor;
import org.knime.ext.textmining.data.DocumentCategory;
import org.knime.ext.textmining.data.DocumentSource;
import org.knime.ext.textmining.data.DocumentType;
import org.knime.ext.textmining.data.PartOfSpeechTag;
import org.knime.ext.textmining.data.ProcessingInfo;
import org.knime.ext.textmining.data.PublicationDate;
import org.knime.ext.textmining.data.Term;
import org.knime.ext.textmining.data.TermDocumentFrequencySet;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class TextMiningObjectsTest extends TestCase {
    
    private static final String CHARACTER_TESTFILE_URL = 
        "org/knime/ext/ainet/unittests/data/moreThan4000CharacterTestFile.txt";
    
    private final Set<DocumentCategory> m_testCategories = 
        new HashSet<DocumentCategory>();

    private final Set<DocumentSource> m_testSources = 
        new HashSet<DocumentSource>();
    
    /**Constructor for class DocumentAuthorFactoryTest.
     * 
     */
    public TextMiningObjectsTest() {
        m_testCategories.add(new DocumentCategory("junitTestCategory12"));
        m_testSources.add(new DocumentSource("junitTestSource1"));
    }

    /**
     * Test method for Document creation
     * {@link org.knime.ext.ainet.data.textmining.DocumentFactory#create(
     * org.knime.ext.textmining.data.Document)}.
     * @throws IOException If the test file couldn't be read
     */
    public final void testCreateDocument() throws IOException {
        //test a document with a fulltext longer than 4000 character
        String file = "Fulltext > 4000 character TestFile1";
        Set<DocumentAuthor> authors = 
            new HashSet<DocumentAuthor>(2);
        authors.add(
                DocumentAuthorFactory.create("junitLast11", "junitFirst11"));
        authors.add(
                DocumentAuthorFactory.create("junitLast21", "junitFirst21"));
        //get the more then 4000 character test file
        java.net.URL configFile = 
            AiNetHibernateUtil.class.getClassLoader().getResource(
                    CHARACTER_TESTFILE_URL);
        final FileReader r = new FileReader(configFile.getFile());
        final BufferedReader br = new BufferedReader(r);
        StringBuilder builder = new StringBuilder();
        String line = br.readLine();
        while(line != null) {
            builder.append(line);
            line = br.readLine();
        }
        br.close();
        r.close();
        String fullText = builder.toString();
        PublicationDate date = new PublicationDate();
        String title = "Junit document title12";
        Document expectedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        Document retrievedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        assertEquals(expectedDoc, retrievedDoc);
        //test a document with no authors
        file = "noAuthorTestFile";
        authors.clear();
        fullText = 
            "In der einfachen Suche können Sie in die Suchmaske ein oder ";
        title = "Junit document title21";
        expectedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        retrievedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        assertEquals(expectedDoc, retrievedDoc);
        
        //test a document with none db objects
        file = "noDBObjectsTestFile";
        authors.clear();
        authors.add(
                new DocumentAuthor("junitLast1", "junitFirst1"));
        authors.add(
                new DocumentAuthor("junitLast3", "junitFirst3"));
        date = new PublicationDate(2007, 1);
         fullText = 
            "JUnit test with none db objects ";
        title = "Junit document title3 - None db objects1";
        Document doc = new Document(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        expectedDoc = DocumentFactory.create(doc);
        retrievedDoc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        assertEquals(expectedDoc, retrievedDoc);
    }

    /**
     * Test method for Document creation
     * {@link org.knime.ext.ainet.data.textmining.
     * TermDocumentFrequencySetFactory#create(
     * org.knime.ext.textmining.data.TermDocumentFrequencySet)}.
     */
    public final void testCreateTermDocumentFrequenceSet() {
        String file = "termDocumentFrequenceTestFile";
        Set<DocumentAuthor> authors = 
            new HashSet<DocumentAuthor>(2);
        authors.add(
                DocumentAuthorFactory.create("junitLast11", "junitFirst11"));
        authors.add(
                DocumentAuthorFactory.create("junitLast21", "junitFirst21"));
        PublicationDate date = new PublicationDate(2007);
         String fullText = 
            "In der einfachen Suche können Sie in die Suchmaske ein oder ";
        String title = "Junit document title21";
        Document doc = DocumentFactory.create(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        Term term = TermFactory.create("junitTerm", PartOfSpeechTag.UNKNOWN);
        ProcessingInfo info = 
            ProcessingInfoFactory.create("junitTest", "junit test cases");
        TermDocumentFrequencySet expectedSet = 
            TermDocumentFrequenceSetFactory.create(term, doc, 123, info);
        TermDocumentFrequencySet retrievedSet = 
            TermDocumentFrequenceSetFactory.create(term, doc, 123, info);
        assertEquals(expectedSet, retrievedSet);
        //test it with none db document and terms
        file = "noDBObjectsTestFile";
        authors.clear();
        authors.add(
                new DocumentAuthor("junitLast1", "junitFirst1"));
        authors.add(
                new DocumentAuthor("junitLast3", "junitFirst3"));
        date = new PublicationDate(2007, 1);
         fullText = 
            "JUnit test with none db objects ";
        title = "Junit document - None db objects1";
        doc = new Document(title, fullText, 
                fullText, file, authors, date, DocumentType.UNKNOWN,
                m_testSources, m_testCategories);
        term = new Term("junitTerm2", PartOfSpeechTag.UNKNOWN);
        info = new ProcessingInfo("junitTest", "junit test cases");
        expectedSet = 
            TermDocumentFrequenceSetFactory.create(term, doc, 123, info);
        retrievedSet = 
            TermDocumentFrequenceSetFactory.create(term, doc, 123, info);
        assertEquals(expectedSet, retrievedSet);
    }
}
