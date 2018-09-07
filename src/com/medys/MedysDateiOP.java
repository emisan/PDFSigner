package com.medys;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;


/**
 * Abstrakte Klasse f&uuml;r die Handhabung einer PDF-Datei
 * 
 * Die Handhabung beschr&auml;nkt sich akutell auf folgende 
 * Operationen <br />
 * 
 * <ul>
 *  <li>Pr&uuml;fung auf PDF-Dateityp</li>
 *  <li>Existenzpr&uuml;fung einer PDF-Datei</li>
 *  <li>Verzeichnispfadangabe - Existenzpr&uuml;fung und 
 *  	absolute Verzeichnispfadnamen-R&uuml;ckgabe</li>
 * </ul>
 * 
 * <br /><br/>
 * 
 * <u><b>INFO</b></u><br /><br />
 * Diese Klasse ist an den Logger gebunden: {@link com.medys.MedysPDFLogger}
 * 
 * <br /><br />
 * 
 * @author Hayri Emrah Kayaman, MEDYS GmbH W&uuml;lrath 2015
 */
public abstract class MedysDateiOP {

	protected FileHandler fileHandler;
	
	protected Formatter formatter;
	
	private MedysPDFLogger logger = new MedysPDFLogger();
	
	protected MedysPDFException medPdfExcep = new MedysPDFException(logger);
	
	/**
	 * pr&uuml;ft, ob eine Datei existiert.
	 * <br />
	 * @param file die Datei, die zu &uuml;berpr&uuml;fen gilt 
	 * 		  und kein Verzeichnis darstellt
	 * @return <b>true</b> wenn existiert und kein Verzeichnis ist,<br /> 
	 *         sonst <b>false</b> 
	 */
	protected boolean isFileNotDirectoryAndExists(File file)
	{
		return file.exists() && !file.isDirectory();
	}
	
	/**
	 * pr&uuml;ft, ob eine Verzeichnis existiert.
	 * <br />
	 * @param verzeichnisPfad die absolute Verzeichnispfasdangabe
	 * @return <b>true</b> wenn das Verzeichnis anhand der Pfadangabe existiert,
	 * 		   <br />sonst <b>false</b> 
	 */
	protected boolean isDirectoryAndExists(String verzeichnisPfad)
	{
		File file = new File(verzeichnisPfad);
		
		return file.exists() && file.isDirectory();
	}
	
	/**
	 * pr&uuml;ft, ob eine Verzeichnis existiert.
	 * <br />
	 * @param file die Datei, die eine Verzeichnisstruktur darstellt
	 * @return <b>true</b> wenn existiert und eine Verzeichnis ist,<br /> 
	 *         sonst <b>false</b> 
	 */
	protected boolean isDirectoryAndExists(File file)
	{
		return file.exists() && file.isDirectory();
	}
	
	
	/**
     * pr&uuml;ft, ob eine Datei mit der Endung &quot;.pdf&quot; endet
     * 
     * @param fileName der Name der angeblichen PDF-Datei
     * @return <b>true</b> wenn die Datei mit &quot;.pdf&quot; endet, 
     * 		   <br />sonst <b>false</b>
     */
    protected boolean isValidPdfDocumentName(String fileName) 
    {
    	boolean validState = false;
    	
    	if(fileName != null)
    	{
			validState = fileName.endsWith(".pdf");
		}
    	else
    	{
    		medPdfExcep.setMessage("info", 
    				"Die Datei " + fileName + " ist kein PDF-Dokument");
    	}
    	
    	return validState;
    }
	
    /**
	 * Legt eine neue Datei an
	 * 
	 * @param dateiPfad der Speicherort/das Verzeichnis der Datei
	 * @param dateiName der Name der Datei
	 * @return <i>java.io.File</i> wenn die Datei angelegt wurde, 
	 * 		   sonst <i>NULL</i>
	 * @throws NullPOinterException falls eine neue Datei nicht angelegt 
	 * 								werden konnte
	 */
	protected File legeDateiAn(String dateiPfad, String dateiName) 
			throws NullPointerException
	{
		File file = null;
		
		if((dateiName.length() > 0) && (dateiName != null))
		{
			String dateiMitPfad = getValidFolderPath(dateiPfad) + dateiName;
			
			file = new File(dateiMitPfad);
			
			FileWriter fw = null;
			
			if(isFileNotDirectoryAndExists(file))
			{
				file.delete();
			}
			
			try
			{
				fw = new FileWriter(file);
				
				fw.close();
			}
			catch(IOException creationFailed)
			{
				medPdfExcep.setMessage(
						"warning",
						"ERROR:CERT_NOT_GENERATED_" + dateiName);
				
				creationFailed.printStackTrace();
			}
		}
		return file;
	}

	/**
	 * Zeichnet eine Nachricht mit dem internen Logger 
	 * &quot;<i>MedysPDF_Logger</i>&quot; auf
	 *  
	 * @param message die Nachricht
	 */
	protected void logConfigMessage(String message) 
	{
		logger.logConfigMessage("config", message);
	}

	/**
	 * Liefert die Datei aus einem angegebenen Ordner
	 * 
	 * <br />
	 * 
	 * @param pathToFile der absolute Pfad zum Ordner
	 * @param fileName der Name der Datei
	 * @return die Datei aus dem Ordner falls sie existiert, sonst NULL
	 * @throws NullPointerException wenn der absolute Pfad nicht gegeben ist 
	 */
	protected File gibDatei(String pathToFile, String fileName) 
			throws NullPointerException
	{
		String folder = getValidFolderPath(pathToFile);
		
		File file = null;
		
		if((folder != null) && !folder.isEmpty())
		{
			file = new File(folder + fileName);
		}
		return isFileNotDirectoryAndExists(file) ? file : null;
	}
	
	/**
	 * Liefert die Bytes des Inhalts einer Datei
	 * 
	 * @param pathToFile der Ordner der Datei (Verzeichnispfad ohne Dateiname)
	 * @param fileName der Name der Datei
	 * @return der Inhalt einer Datei in einem ByteArray
	 */
	protected byte[] getFileContent(String pathToFile, String fileName) 
	{
		byte[] documentInBytes = new byte[0];
		
		try
		{
			File file = gibDatei(pathToFile, fileName);
	
			if(isFileNotDirectoryAndExists(file))
			{
				documentInBytes = getFileContent(file);
			}
		}
		catch(IOException ioExcep)
		{
			medPdfExcep.setMessage("warning",
					"aus getFileContentInCharset(..) " + ioExcep.getMessage());
			
			ioExcep.printStackTrace();
		}
		return documentInBytes;
	}
	
	/**
     * Liest den Inhalt einer Datei aus <br />
     * und liefert diesen in einem ByteArray zur&uuml;ck.
     * 
     * <br /><br />
     * 
     * @param dtei die Datei, die ausgelesen werden soll
     * @param os output stream
     * @throws IOException Fehler beim Lesen oder Schreiben auf der Datei
     */
    public byte[] getFileContent(File datei) throws IOException
    {
    	byte[] content = new byte[0];
    	
    	try
        {
    		FileInputStream fis = new FileInputStream(datei);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            MedysDateiOP.moveStreamData(fis, baos);
            
            content = baos.toByteArray();
        }
        catch (Exception e)
        {
            medPdfExcep.setMessage("warning",
            		"aus MedysDateiOP.getFileContent(FileInutStream):\n" 
            		+ "Kein Dokumentinhalt vorhanden");
        }
    	return content;
    }
    
	/**
     * Liest den Inhalt einer Datei aus dem Dateistrom (fis) aus <br />
     * und liefert diesen in einem ByteArray zur&uuml;ck.
     * 
     * <br /><br />
     * 
     * @param fis FileInputStream, das zu einer Datei verbunden ist
     * @throws IOException Fehler beim Lesen oder Schreiben der Datei
     */
    public byte[] getFileContent(FileInputStream fis) throws IOException
    {
    	byte[] content = new byte[0];
    	
    	try
        {
            ByteArrayOutputStream pdfBaos = new ByteArrayOutputStream();
            
            int copyBufferLen = 2000;
            
            byte[] copyBuffer = new byte[copyBufferLen];

            while (true)
            {
                int bytesRead = fis.read(copyBuffer);
                
                if (0 > bytesRead)
                {
                    break;
                }
                pdfBaos.write(copyBuffer, 0, bytesRead);
            }
            content = pdfBaos.toByteArray();
            
            fis.close();
            pdfBaos.close();
        }
        catch (Exception e)
        {
            medPdfExcep.setMessage("warning",
            		"aus MedysDateiOP.getFileContent(FileInutStream):\n" 
            		+ "Kein Dokumentinhalt vorhanden");
        }
    	return content;
    }

	/**
	 * Liefert einen system-validen Verzeichnispfad einer Datei 
	 * (mit der Endung /)
	 * 
	 * <br /><br />
	 * 
	 * @param folderPath der urspr&uuml;gliche Verzeichnispfad zu einer Datei
	 * @return die system-valide Verzeichnispfad angabe, falls das Verzeichnis 
	 * 		   existiert, sonst NULL
	 */
	protected String getValidFolderPath(String folderPath)
	{
		String rtValue = "";
		
		if(!(folderPath == null) && !folderPath.isEmpty()) 
		{
			File folder = new File(folderPath);
		
			if (!folder.exists()) 
			{
				folder.mkdir();
			}
			if (folder.isDirectory()) 
			{
				return folderPath.endsWith("/") 
						? folderPath 
						: folderPath + "/";
			}
		}
		return rtValue;
	}

	protected MedysPDFException getMedysPDFException()
	{
		return medPdfExcep;
	}

	/**
     * Moves stream data from input stream to output stream. Will copy the complete stream.
     * @param is input stream
     * @param os output stream
     * @exception IOException Fehler beim Lesen oder Schreiben
     */
    public static void moveStreamData(InputStream is, OutputStream os) 
    		throws IOException
    {
        int copyBufferLen = 2000;
        byte[] copyBuffer = new byte[copyBufferLen];

        while (true)
        {
            int bytesRead = is.read(copyBuffer);
            if (0 > bytesRead)
            {
                break;
            }
            os.write(copyBuffer, 0, bytesRead);
        }
    }
    
	/**
	 * Schreibt die Meldung einer Ausnahme in die angegebene Datei
	 * 
	 * @param ausnahmemeldung die Ausnahmemeldung
	 * @param dateiOrdner das Verzeichnis der Datei
	 * @param dateiName die vollst&auml;ndige Dateiname 
	 * 					(Dateiname inkl der Dateiendung),
	 * 					in die geschrieben werden soll
	 */
	protected void schreibAusnahmeInDatei(
			String ausnahmemeldung, 
			String dateiOrdner,
			String dateiName)
	{
		File statusFile = legeDateiAn(dateiOrdner, dateiName);
		
		if (statusFile.exists()) 
		{
			try 
			{
				FileWriter fw = new FileWriter(statusFile);
				fw.write(ausnahmemeldung);
				fw.close();
			}
			catch (IOException io) {
				medPdfExcep.setMessage("warning",
						"SECSIGNER_ABORT" + io.getMessage());
			}
		}		
	}
}
