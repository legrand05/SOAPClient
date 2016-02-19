package ind.legrand.mondesir.SOAPClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Util {


	public static Properties getProperties(String filePath) throws FileNotFoundException, IOException {
		Properties proproperties = new Properties();
		
		proproperties.load(new FileReader(new File(filePath)));
		
		return proproperties;
	}
}
