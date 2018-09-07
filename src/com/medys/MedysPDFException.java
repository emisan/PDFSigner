package com.medys;

/**
 * Klasse f&uuml;r die Fehlerbehandlung
 * <br /><br />
 * Die erhaltenen Fehler werden an den internen Logger 
 * &quot;<i>MedysPDFException_Logger</i>&quot; weitergeleitet.
 * <br />
 * 
 * @author Hayri Emrah Kayaman, MEDYS GmbH W&uuml;lrath 2015
 *
 */
public class MedysPDFException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1013183096794471087L;
	
	private final String CLASS_NAME = "MedysPDFException";
	
	private final String LOG_PREFIX = "\n" + CLASS_NAME + " ---- \n\n";
	
	private MedysPDFLogger logger;
	
	/**
	 * Erstellt eine neue Instanz von MedysPDFException
	 */
	public MedysPDFException()
	{
		super();
		
		setMedysPDFLogger(new MedysPDFLogger());
	}
	
	/**
	 * Erstellt eine neue Instanz von MedysPDFException
	 * mit einem internen Logger (Prozessflussdokumentierer)
	 *  
	 * @param logger eine Instanz von MedysPDFLogger
	 */
	public MedysPDFException(MedysPDFLogger logger)
	{
		this.logger = logger;
	}
	
	/**
	 * Erstellt eine neue Instanz von MedysPDFException mit einer 
	 * Ausnahmemeldung
	 * <br />
	 * @param message die Ausnahmemeldung der Exception
	 */
	public MedysPDFException(String message)
	{
		super(message);
		
		setMedysPDFLogger(new MedysPDFLogger());
		
		if(logger != null)
		{
			logger.logConfigMessage("warning", LOG_PREFIX + message);
		}
		else
		{
			logger = new MedysPDFLogger();
			
			setMedysPDFLogger(logger);
			
			logger.logConfigMessage("warning", LOG_PREFIX + message);
		}
	}
	
	/**
	 * Erstellt eine neue Instanz von MedysPDFException mit 
	 * einer verwerfbaren Ausnahmebehandlung
	 * 
	 * <br />
	 * @param throwable die verwerfbare Ausnahmebehandlung
	 */
	public MedysPDFException(Throwable throwable)
	{
		super(throwable);
		
		setMedysPDFLogger(new MedysPDFLogger());
		
		if(logger != null)
		{
			logger.logConfigMessage("warning", 
					LOG_PREFIX + throwable.getMessage());
		}
		else
		{
			logger = new MedysPDFLogger();
			
			setMedysPDFLogger(logger);
			
			logger.logConfigMessage("warning", 
					LOG_PREFIX + throwable.getMessage());
		}	
	}
	
	/**
	 * Erstellt eine neue instanz von MedysPDFException mit einem
	 * Ausnahmegrund
	 * 
	 * <br />
	 * @param message der Grund
	 * @param cause die verwerfbare Ausnahmebehandlung
	 */
	public MedysPDFException(String message, Throwable cause)
	{
		super(message, cause);

		if(logger != null)
		{
			logger.logConfigMessage("warning", 
					LOG_PREFIX + cause.getMessage());
		}
		else
		{
			logger = new MedysPDFLogger();
			
			setMedysPDFLogger(logger);
			
			logger.logConfigMessage("warning", 
					LOG_PREFIX + cause.getMessage());
		}
	}
	
	/**
	 * Setzt den Logger f&uuml;r MedysPDFException
	 * 
	 * @param logger eine Instanz von MedysPDFLogger
	 */
	public void setMedysPDFLogger(MedysPDFLogger logger)
	{
		this.logger = logger;
	}

	/**
	 * Setzt die Fehlernachricht
	 * 
	 * @param message die Fehlernachricht
	 */
	public void setMessage(String level, String message)
	{
		logger.logConfigMessage(level, message);
	}
}

