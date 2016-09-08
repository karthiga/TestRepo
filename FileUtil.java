package com.wellsfargo.mwf.testingframework.facilities;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 * 
 * @author Ram Lakshmanan
 */
public class FileUtil {
	
	// ToDo: Need to replace it
	private static final String MOCK_RESPONSE_DIR = "com\\wellsfargo\\mwf\\delegate\\";
	
	public static String readMockContent(String relativeFilePath) {
		
		return readFileContents(MOCK_RESPONSE_DIR + relativeFilePath);
	}

	public static String readFileContents(String filePath) {
		
		try {
			
			InputStream iStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);						
			return IOUtils.toString(iStream);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		return null;
	}
}