package com.medys;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Java-Logger implementierung f&uuml;r die MedysPDFSignierungValidierung
 * <br />
 * @author Hayri Emrah Kayaman, MEDYS GmbH W&uuml;lrath 2015
 *
 */
public class MedysPDFLogger {

	private Logger logger;
	
	private FileHandler fileHandler;
	
	private Formatter formatter;
	
	public MedysPDFLogger()
	{
		initLogger();
	}
	
	private void initLogger() 
	{
		logger = Logger.getLogger("MedysPDF_Logger.txt");
		
		try {
			File file = new File(logger.getName());
			
			if(!file.exists())
			{
				fileHandler = new FileHandler("MedysPDF_Logger.txt");

				formatter = new SimpleFormatter();

				fileHandler.setFormatter(formatter);

				logger.addHandler(fileHandler);
			}
		} 
		catch (SecurityException e) 
		{
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void logConfigMessage(String level, String message) 
	{
		try
		{
			Level logLevel = Level.parse(level.toUpperCase());
			logger.log(logLevel, message);
		}
		catch(Exception wrongLog)
		{
			logger.log(Level.FINEST, "LOGGING_ERROR:" + wrongLog.getMessage());
		}
	}
}
