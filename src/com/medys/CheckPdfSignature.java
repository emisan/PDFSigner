package com.medys;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;

/**
 * CheckPDFSignature.java ermittelt aus einem gegebenen PDF-Dokument mit Hilfe der Apache-PDFBox Klassen <br />
 * die Signatur und &uunml;berpr&uuml;ft sie auf ihre G&uuml;ltigkeit. <br /><br />
 * 
 * Die &Uuml;berpr&uuml;fung der G&uuml;ltigkeit ist momentan beschr&auml;kt <br /><br />
 * d.h. Sie beruht sich auf darauf, ob die Signatur einen Inhalt hat und     <br />
 * die Methode 
 * @author hk
 *
 */
public class CheckPdfSignature extends MedysDateiOP {

	private boolean certIsValid;
	
	private String certificateDateBefore, certificateDateAfter;
	
	public CheckPdfSignature()
	{
		certificateDateAfter = null;
		certificateDateBefore = null;
	}
    
    public File gibPDFSignature(String filePath, String fileName) 
    {
    	filePath = getValidFolderPath(filePath);
    	
    	File signatureFile = null;
    	
    	// ist fileName eine im PDF-Format benannte Datei und Pfad existiert
    	//
		if (isValidPdfDocumentName(fileName) && isDirectoryAndExists(filePath))
		{
			try 
			{
				File pdfFile = gibDatei(filePath, fileName);

				if (isFileNotDirectoryAndExists(pdfFile)) 
				{
					PDDocument document = PDDocument.load(pdfFile);

					PDSignature signature = document
							.getLastSignatureDictionary();

					System.out.println("Signatur Kontaktinfo : "
							+ signature.getContactInfo());

					System.out.println("Signatur Location	 : "
							+ signature.getLocation());

					System.out.println("Signatur SubFilter 	 : "
							+ signature.getSubFilter());

					System.out.println("Signatur 			 : "
							+ signature.getFilter());

					Calendar gueltigkeitsDatum = signature.getSignDate();

					System.out.println("Signaturdatum	     : "
							+ gueltigkeitsDatum.getTime());

					COSString certString = (COSString) signature.getCOSObject()
							.getDictionaryObject(COSName.CONTENTS);

					if (certString.getString() != null) {

						 System.out.println("Cert of ASCII\n\n" +
						 certString.getString());

						// speicher den Inhalt der PDF-Signatur in eine neue
						// Datei
						//
						// der Verzeichnispfad ist hier bereits valide

						fileName = "NewTempCert.der";

						signatureFile = new File(filePath + fileName);

						if (signatureFile.exists()) {
							signatureFile.delete();
						}

						FileWriter fw = new FileWriter(signatureFile);

						fw.write(certString.getString());

						fw.close();
					}
					else 
					{
						System.out.println("The certificate is NULL (empty).");
					}
				}
			}
			catch (IOException noDoc)
			{
				System.out.println(
						"IOEXception in checkPDFSignature(String, String)");

				noDoc.printStackTrace();
			}
		}
		
		return signatureFile;
    }
    
    public void printCertificate(String filePath, String fileName)
    {
    	System.out.println("Dokument " + fileName);
		
		String fileWithPath = getValidFolderPath(filePath) + fileName;
		
		File file = new File(fileWithPath);
		
		try
		{
			FileInputStream fis = new FileInputStream(file);
			
			try 
			{
				CertificateFactory factory = 
						CertificateFactory.getInstance("X.509");
				
				X509Certificate cert = 
						(X509Certificate)factory.generateCertificate(fis);
				
				cert.checkValidity();
				
				// die nächste Methode wird nur dann erreichbar sein , wenn checkValidity 
				// keine Exception auswirft = Validierung OK ist !!
				//
				
				if(isValidCertificate(cert))
				{
					readCertificate(cert);
				}
			} 
			catch (CertificateException e) {
				e.printStackTrace();
			}
			finally {
				fis.close();
			}
		}
		catch(IOException io)
		{
			System.out.println("Certification exception aufgetreten " 
					+ "in printCertificate(String, String)");
			io.printStackTrace();
		}
    }
    
    /**
     * Liest den Inhalt einer DER-kodierten Signature aus.
     * 
     * <br/><br />
     * 
     * Ausgelesen wird (falls vorhanden : <br /><br />
     * 
     * <table border="0" cellpadding="2" cellspacing="2">
     *  <tr>
     *  	<th></th>
     *  	<th></th>
     * 	</tr>
     *  <tr>
     *    <td>Besitzer des Zertifikats</td>
     *    <td>Certificate Owner</td>
     *  </tr>
     *  <tr>
     *    <td>Herausgeber des Zertifikats</td>
     *    <td>Certificate Issuer</td>
     *  </tr>
     *  <tr>
     *    <td>Seriennummer des Zertifikats</td>
     *    <td>Certificate Serial Number</td>
     *  </tr>
     *  <tr>
     *    <td>Algorithmus des Zertifikats</td>
     *    <td>Certificate Algorithm</td>
     *  </tr>
     *  <tr>
     *    <td>Version des Zertifikats</td>
     *    <td>Certificate Version</td>
     *  </tr>
     *  <tr>
     *    <td>Objekt-ID des Zertifikats</td>
     *    <td>Certificate OID</td>
     *  </tr>
     *  <tr>
     *    <td>G&uuml;ltigkeit von</td>
     *    <td>Certificate Valid From</td>
     *  </tr>
     *  <tr>
     *    <td>G&uuml;ltigkeit bis</td>
     *    <td>Certficate Valid To</td>
     *  </tr>
     * </table>
     * @param signature das X509Zertifikat
     * @throws CertificateEncodingException wenn es kein DER-kodiertes Zertifikat ist, <br />
     * 		   NoSuchAlgorithmException wenn es kein Zertifizierungs-Algorithmus enth&auml;t
     */
    public void readCertificate(X509Certificate x509Cert) 
    {
		if (x509Cert != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");

			System.out.println("Certificate Owner		: "
					+ x509Cert.getSubjectDN().toString());
			System.out.println("Certificate Issuer		: "
					+ x509Cert.getIssuerDN().toString());
			System.out.println("Certificate Serial Number	: "
					+ x509Cert.getSerialNumber().toString());
			System.out.println("Certificate Algorithm		: "
					+ x509Cert.getSigAlgName());
			System.out.println("Certificate Version		: "
					+ x509Cert.getVersion());
			System.out.println("Certificate OID			: "
					+ x509Cert.getSigAlgOID());
			
			System.out.println("Certificate Valid From		: "
					+ dateFormat.format(x509Cert.getNotBefore()));
			System.out.println("Certificate Valid To		: "
					+ dateFormat.format(x509Cert.getNotAfter()));

			certificateDateBefore = dateFormat.format(x509Cert.getNotBefore());
			
			certificateDateAfter = dateFormat.format(x509Cert.getNotAfter());
			
			if(certificateDateBefore != null)
			{
				System.out.println("Certificate Valid From		: " 
						+ certificateDateBefore);
			}
			if(certificateDateAfter != null)
			{
				System.out.println("Certificate Valid From		: "
						+ certificateDateAfter);
			}
			
			try {
				final MessageDigest md = MessageDigest.getInstance("SHA-256");
				try {
					md.update(x509Cert.getEncoded());
				}
				catch (CertificateEncodingException e) {
					System.out.println("CertificateEncodingException in readCertificate(X509Certificate)");
					e.printStackTrace();
				}

				System.out.println("Certificate SHA-256		: "
						+ getHex(md.digest()));
			}
			catch (NoSuchAlgorithmException e) {
				// DEBUG only
				System.out.println("MessageDigest ERROR: " + e.getMessage()
						+ "\n");

				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Das Zertifikat ist leer oder ncht vorhanden !");
		}
    }
    
    public boolean isValidCertificate(Certificate cert)
    {
    	certIsValid = false;
    	
    	try 
    	{
			byte[] encodedCert = cert.getEncoded();
			
			InputStream is = new ByteArrayInputStream(encodedCert);
			
			try {
				CertificateFactory factory = CertificateFactory.getInstance("X.509");
				
				Certificate newCert = factory.generateCertificate(is);
				
				System.out.println("PDF Cert Type: " + newCert.getType());
				
				readCertificate((X509Certificate)cert);
			}
			catch (CertificateException e) {
				
				e.printStackTrace();
			}
			finally
			{
				try
				{
					is.close();
				}
				catch(IOException streamNotClosed)
				{
					streamNotClosed.printStackTrace();
				}
			}
		}
    	catch (CertificateEncodingException e) {
			
			e.printStackTrace();
		}
    	
    	return certIsValid;
    }
    
    /**
     * Get the hex value of a raw byte array.
     *
     * @param raw  the raw byte array.
     * @return the hex value.
     */
    public static String getHex( byte[] raw ) {
        final String HEXES = "0123456789abcdef";

        if ( (raw == null) || (raw.length == 0) ) {
            return null;
        }

        final StringBuilder hex = new StringBuilder( 2 * raw.length );
        for ( final byte b : raw ) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }

        return hex.toString();
    }
}
