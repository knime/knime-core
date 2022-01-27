package org.knime.core.node.workflow.def;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Question is whether loading an entire object will be very slow and, if yes, we can mitigate this by using a streaming
 * approach to parsing.
 * 
 * Probably would have to make sure that the order of the properties is such that frequently used (or at least flat) items
 * are listed early to avoid parsing to the end of the file.
 *   
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class PartialLoadingWithJackson {

	/**
	 * Simple example on how to use Jackson Parser API.
	 * @throws JsonParseException
	 * @throws IOException
	 */
	private static void simpleExample() throws JsonParseException, IOException {
		String json
		  = "{\"name\":\"Tom\",\"age\":25,\"address\":[\"Poland\",\"5th avenue\"]}";
		JsonFactory jfactory = new JsonFactory();
		JsonParser jParser = jfactory.createParser(json);

		String parsedName = null;
		Integer parsedAge = null;
		List<String> addresses = new LinkedList<>();
		
		
		while (jParser.nextToken() != JsonToken.END_OBJECT) {
		    String fieldname = jParser.getCurrentName();
		    if ("name".equals(fieldname)) {
		        jParser.nextToken();
		        parsedName = jParser.getText();
		    }

		    if ("age".equals(fieldname)) {
		        jParser.nextToken();
		        parsedAge = jParser.getIntValue();
		    }

		    if ("address".equals(fieldname)) {
		        jParser.nextToken();
		        while (jParser.nextToken() != JsonToken.END_ARRAY) {
		            addresses.add(jParser.getText());
		        }
		    }
		}
		jParser.close();
		
		
		assertEquals(parsedName, "Tom");
		assertEquals(parsedAge, (Integer) 25);
		assertEquals(addresses, Arrays.asList("Poland", "5th avenue"));

	}
	@Test
	public void streamRead() throws JsonParseException, IOException {
		String json = Files.readString(Paths.get(new File("src/test/resources").getAbsolutePath(), "Enh11762ExampleOutput.json"));
		JsonFactory jfactory = new JsonFactory();
		JsonParser jParser = jfactory.createParser(json);
		String parsedName = null;
		Integer parsedAge = null;
		List<String> addresses = new ArrayList<>();
		
		while (jParser.nextToken() != JsonToken.END_OBJECT) {
		    String fieldname = jParser.getCurrentName();

		    if ("workflow".equals(fieldname)) {
		        jParser.nextToken();
		        while (jParser.nextToken() != JsonToken.END_ARRAY) {
		            addresses.add(jParser.getText());
		        }
		    }
		}
		jParser.close();
		
		System.out.println(addresses);

		
	}
}
