package eu.compassresearch.core.interpreter.test;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TestResult {

	public static TestResult parseTestResultFile(String filePath) throws IOException 
	{
		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		TestResult testResult = null;
		
		try {

			//Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			//parse using builder to get DOM representation of the XML file
			Document dom = db.parse(filePath);

			Element docEle = dom.getDocumentElement();

			//get a nodelist of elements
			NodeList nl = docEle.getElementsByTagName("visibleTrace");
			
			Node n = nl.item(0);
			
			Vector<String> trace = new Vector<String>();
			
			String value = n.getFirstChild().getNodeValue();
			
			for(String s : value.split(";"))
			{
				trace.add(s);
			}
			
			testResult = new TestResult(trace);

		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}
		
		return testResult;
	}
	
	private List<String> visibleTrace;
	
	public TestResult(List<String> visibleTrace)
	{
		this.visibleTrace = visibleTrace;
	}
		
	public List<String> getVisibleTrace()
	{
		return this.visibleTrace;
	}
	
}
