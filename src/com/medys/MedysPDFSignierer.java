package com.medys;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import seccommerce.pdf.PDFAnnotationDataItem;
import seccommerce.secsignersigg.SecSigner;
import seccommerce.secsignersigg.SecSignerConstants;
import seccommerce.secsignersigg.SecSignerException;
import seccommerce.secsignersigg.SecSignerInitResult;
import seccommerce.secsignersigg.SignatureRecord;

/**
 * Klasse, um ein PDF-Dokument mittels der SecSigner-Anwendung mit den Daten<br />
 * der HBA-Karte oder einer anderen anwendungszul&auml;ssigen Karte f&uuml;r die eArztbrief-Anwendung zu signieren.
 * 
 * <br/><br />
 * 
 * <u>Medys interne Verzeichnisstruktur</u><br /><br />
 * 
 * <b>Ablageordner f&uuml;r signierte Dokumente:</b><br />
 * <i>{Medys-Zusatzpfad}/med_eArztbrief/signierung/<b>_pdf</b></i>
 * 
 * <br /><br />
 * 
 * <u>Statusmeldung &uuml;ber den Ablauf der Signierung</u><br /><br />
 * <i>{Medys-Zusatzpfad}/med_eArztbrief/signierung/<b>_status/Status.txt</b></i> <br /><br />
 * 
 * <u>Statusmeldungen</u><br />
 * <ul>
 * 	<li>secsigner_abbruch</li>
 *  <li>secsigner_signiert</li>
 * </ul>
 *
 * <br />
 * Aufruf des Signierers siehe {@link com.medys.MedysPDFSignierungValidierung}
 * <br />
 * @author Hayri Emrah Kayaman, MEDYS GmbH W&uuml;lrath 2015
 *
 */
public class MedysPDFSignierer extends MedysDateiOP {
	
	
	@SuppressWarnings("unused")
	private byte[] sigCertFileBytes, authCertFileBytes, encrCertFileBytes;
	
	private String workspaceDirectory, fileDirectory;
	
	// für Signatur, Authentifikationszertifikat, codiertes Zertifikat von der Karte
	//
	private File sigCertFile, authCertFile, encrCertFile; 
	
	// DEBUG ONLY 
//	private File authCertFile, encrCertFile;
	
	private SecSigner secSigner;

	private MedysPDFException medysPdfException;
	
	public MedysPDFSignierer(
			String arbeitsverzeichnis,
			String dateiVerzeichnis, 
			String dateiName) 
	{

		sigCertFileBytes = null;
		authCertFileBytes = null;
		encrCertFileBytes = null;
		
		medysPdfException = super.getMedysPDFException();
		
		// setze arbeits- und dateioperations verzeichnis
		
		workspaceDirectory = getValidFolderPath(arbeitsverzeichnis);
		
		if((workspaceDirectory != null) && (workspaceDirectory.length() > 0))
		{	
			// generelle Annahme, das irgend etwas die Validierung start oder
			// abbrechen lässt
			
			schreibAusnahmeInDatei(
					"secsigner_abbruch", 
					getWorkspaceDirectory()	+ "_status/", "Status.txt");
				
			fileDirectory = 
					getValidFolderPath(getWorkspaceDirectory() + dateiVerzeichnis);
		
		}
		
		// lade Property-Datei "secsigner.properties" f�r den SecSigner
		//
		Properties secSignerProperties = new Properties();

		InputStream is;

		try 
		{
			is = getClass().getClassLoader().getResourceAsStream("secsigner.properties");
			
			secSignerProperties.load(is);
			is.close();

			try 
			{
				secSigner = new SecSigner(null, secSignerProperties);
				
				if(secSigner != null)
				{
					signier(fileDirectory, dateiName);
				}
			}
			catch (SecSignerException e) 
			{
				// DEBUG only
				//
				System.err.println("SecSigner Initialisierungsfehler: "
						+ e.getMessage());
				
				medysPdfException.setMessage(
						"FINEST", 
						"SecSigner Initialisierungsfehler: "
						+ e.getMessage());
				
				System.exit(0);
			}
		} 
		catch(Exception others)
		{
			medysPdfException.setMessage(
					"FINEST", 
					"SecSigner Initialisierungsfehler: "
					+ others.getMessage());
			
			// DEBUG only
			System.out.println("Fehler: " + others.getMessage());
			
			others.printStackTrace();
		}
	}
	
	//TODO: aber nicht zwingend 
//	protected String gibDechiffrierteDaten(byte[] chiffrierteDaten)
//	{
//		boolean modal = true;
//		
//		SignatureRecord signatureRecord = new SignatureRecord();
//
//	    signatureRecord.setSignature(chiffrierteDaten);
//
//	    String dechiffrierteDaten = "";
//	    
//		boolean offerFileOpenDlg = true;
//
//		try 
//		{
//			int status = 
//					secSigner.decrypt(
//							signatureRecord, 
//							offerFileOpenDlg, modal).getStatus();
//			
//			switch (status) 
//			{
//			
//			case SecSignerConstants.SECSIGNER_VERIFY_NOTINITED:
//				System.out.println("SecSigner nicht initialisiert");
//				break;
//			case SecSignerConstants.SECSIGNER_VERIFY_DECODED_UNSIGNED_DATA:
//				System.out.println("Daten entschluesselt");
//				System.out.println("Daten="
//						+ new String(signatureRecord.getDocument(), "UTF-8"));
//				break;
//			default:
//				System.out
//						.println("Unbekannter Statuscode von der Entschluesselung");
//			}
//		}
//		catch (Exception e)
//		{
//			System.err.println(e.getMessage());
//			e.printStackTrace(System.out);
//			System.exit(1);
//		}
//
//		return dechiffrierteDaten;
//	}
	
	/**
	 * Gibt das aktuell festgelegte Arbeitsverzeichnis zur&uuml;ck
	 * 
	 * @return workspaceDirectory
	 */
	public String getWorkspaceDirectory() {
		return workspaceDirectory;
	}
    
	/*
     * initialisiert den SmartCard Kartenleser und liest die Signature und andere
     * nützliche Karteninformationen von der eignesteckten Karte 
     */
    private synchronized void initSignUnits()
    {
        // Der SecSigner-Dialog kann modal sein.
        boolean modal = true;
        boolean verifyCertificateOnCard = true;
        
        String certDir = getValidFolderPath(getWorkspaceDirectory() + "_certs");
        
		if (certDir != null) 
		{
			sigCertFile = legeDateiAn(certDir,	"sigCert.der");

			authCertFile = legeDateiAn(certDir, "authCert.der");

			encrCertFile = legeDateiAn(certDir, "encrCert.der");
			
			if (sigCertFile.exists())  // && authCertFile.exists()	&& encrCertFile.exists()) 
			{
				try 
				{
					// sollen Dokumente nicht nur signiert, sondern auch verschlüsselt werden,
					// dann benötigt man Zufallszahlen zur Erzeugung eines Schlüssels
					// (dafür sieht man dann eine Progressbar in der Anwendung)
					//
//					secSigner.setCollectRandom(true);
					
					// erster Parameter blockt andere Fenster im Hintergrund
					// zweiter Paramter validiert das kartenzertifikat
					//
					SecSignerInitResult secSignerInitResult = 
							secSigner.initSignUnits(modal, verifyCertificateOnCard);

					FileOutputStream certFos = new FileOutputStream(sigCertFile);
					certFos.write(secSignerInitResult.getSigCert());
					sigCertFileBytes = secSignerInitResult.getSigCert();
					certFos.close();

					certFos = new FileOutputStream(authCertFile);
					certFos.write(secSignerInitResult.getAuthCert());
					authCertFileBytes = secSignerInitResult.getAuthCert();
					certFos.close();

					certFos = new FileOutputStream(encrCertFile);
					certFos.write(secSignerInitResult.getDecryptCert());
					encrCertFileBytes = secSignerInitResult.getDecryptCert();
					certFos.close();
				}
				catch (SecSignerException sse)
				{
					// Wenn in SecSigner "Abbruch"-Button gedrückt wird

					// DEBUG ONLY
					// System.err.println(sse.getMessage());
					// System.err.println(sse.getStatus());
					
					// sse.printStackTrace(System.out);
					
					medysPdfException.setMessage("warning", 
							sse.getMessage());
					
					secSigner.close();
					
					// Annahme aus dem Konstruktor wird in Status.txt
					// erhalten bleiben
					
				}
				catch (IOException ioExcep) 
				{
					medysPdfException.setMessage("warning", 
							ioExcep.getMessage());
					// Annahme aus dem Konstruktor wird in Status.txt
					// erhalten bleiben
				}
			}
			else 
			{
				medysPdfException.setMessage("warning",
						"NO_CERTIFICATE_DATA_GENERATED");
				// else entfällt, da die Annahme aus dem Konstruktor
				// in der Status.txt gilt
			}
		}
		else 
		{
			medysPdfException.setMessage("warning",
					"NO_CERTIFICATE_FOLDER_GIVEN");
			// else entfällt, da die Annahme aus dem Konstruktor
			// in der Status.txt gilt
		}
	}
    
	private void signier(String dateiPfad, String dateiName)
	{
		// hole Karteninformationen und Zertifikate
		//
		initSignUnits();
		
		// Typ der zu signierenden Daten fuer die passende Anzeige im SecSigner-Fenster
	       
        int documentType = SecSignerConstants.SIGNDATATYPE_PDF;
        
        // Das Zertifikat des Unterzeichners mit in die Signatur aufnehmen.
        // Sehr empfohlen, um die Pruefung zu erleichtern.
        //
        boolean includeSignersCert = true;
        
		String validPath = getValidFolderPath(dateiPfad);
		
		boolean validPdfName = isValidPdfDocumentName(dateiName);
		
		if((validPath != null) && validPdfName)
		{
			byte[] inhaltInBytes = getFileContent(validPath, dateiName);

			if (inhaltInBytes.length > 0) 
			{
				if(sigCertFileBytes != null)
				{	
					// Property-Einstellungen für die PDFAnnotation übernehmen
					//
//					seccommerce.secsigner.addpdfannotation=on
//					seccommerce.secsigner.pdfannotationposition=6
//					seccommerce.secsigner.pdfannotationwidth=220
//					seccommerce.secsigner.pdfannotationheight=70
//					seccommerce.secsigner.pdfannotationpadding=30
//					seccommerce.secsigner.pdfannotationshowdate=on
//					seccommerce.secsigner.pdfannotationshowlabels=on
//					seccommerce.secsigner.pdfannotationtransparentbg=off
					
					PDFAnnotationDataItem pdfAnnot = new PDFAnnotationDataItem();
					
					pdfAnnot.setPdfDisplayAnnotation(true);
					pdfAnnot.setPdfSigHeight(150);
					pdfAnnot.setPdfSigWidth(50);
					pdfAnnot.setPdfTransparentBackground(true);
					pdfAnnot.setPdfSigPosition(PDFAnnotationDataItem.PDF_ANNOTATION_RIGHT_TOP_NEWPAGE);
					pdfAnnot.setPdfSigShowDate(true);
					pdfAnnot.setPdfSigAnnotLabels(false);
					
					// Aufzeichhnung und Eigenschaften einer Signature festlegen
					//
					SignatureRecord record = new SignatureRecord();
					
					// welcher Inhalt signiert wird
					//
					record.setDocument(inhaltInBytes);
					
//					record.setSignature(sigCertFileBytes);
//					record.setCertificate(authCertFileBytes);
//					record.setSignatureCiphered(encrCertFileBytes);
					// was in der GUI angezeigt wird
					//
					record.setDocumentUrl(getValidFolderPath(dateiPfad));
					record.setDocumentFileName(dateiName);
					record.setDocumentType(documentType);
					
					record.setSignature(sigCertFileBytes);
					record.setCertificate(encrCertFileBytes);
					
					record.setSignatureUrl(getWorkspaceDirectory()); // pfad zur datei, welche signiert ist
					
					dateiName = dateiName.substring(0,dateiName.indexOf(".pdf"));
					
					record.setSignatureFileName(dateiName + "-signed.pdf");
					record.setSignatureFormatType(SecSignerConstants.SIGNATURE_FORMAT_PDF);
					
					record.setPssPadding(true);		// für RSA-PSS
					record.setHashAlgorithm(null); // null=automatisch
					
					record.setPDFAnnotation(pdfAnnot);
					record.setIncludeSignatureIntoPDF(true);
					record.setIncludeSignersCertificate(new Boolean(includeSignersCert));
					
					SignatureRecord[] records = new SignatureRecord[1];
					records[0] = record;

					if(sigCertFileBytes.length > 0)
					{
						try 
						{
							// sign(
							// signatureRecordArray, (beinhaltet alles was wir
							// hier brauchen)
							// fremd zertifikate, (not needed)
							// signatureKey, (optional)
							// signatureCert, (optional) (wir laden das
							// Zertifikat aus der Karte
							// und setzten es in den SignatureRecord)
							// attributeCerts, (optional)
							// parentWindowModal);

							
							secSigner.sign(records, null, null, null, null,
									true);
							
							schreibAusnahmeInDatei("dokument_signiert_nicht_validiert",
									getWorkspaceDirectory() + "_status/",
									"Status.txt");
						} 
						catch (SecSignerException sse) 
						{
							// Annahme aus dem Konstruktor wird in Status.txt
							// erhalten bleiben

							sse.printStackTrace();
							secSigner.cancel();
							secSigner.close();
						}
					}
					// else entfällt, da die Annahme aus dem Konstruktor
					// in der Status.txt gilt 
				}
				// else entfällt, da die Annahme aus dem Konstruktor
				// in der Status.txt gilt
			}
		}
		// else-Programmierlogik entfällt, da die Annahme aus dem Konstruktor
		// in der Status.txt gilt
		
		secSigner.close();
	}
}
