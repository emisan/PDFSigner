package com.medys;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.StoreException;
import org.bouncycastle.util.encoders.Base64;

import seccommerce.secsignersigg.SecSigner;
import seccommerce.secsignersigg.SecSignerConstants;
import seccommerce.secsignersigg.SecSignerException;
import seccommerce.secsignersigg.SecSignerVerifyResult;
import seccommerce.secsignersigg.SignatureRecord;

/**
 * Diese Klasse dient f&uuml;r die &Uuml;berpr&uuml;fung 
 * des Erhalts des Validierungsstatus eines signierten, 
 * certifizierten PDF-Dokuments.
 * 
 * <br /><br />
 * 
 * <u>Medys interne Abhandlung der Validierung</u><br /><br />
 * 
 * Die Validierung erfolgt &uuml;ber die SecSigner-Anwendung. <br /><br />
 * 
 * Der jeweilige Statuscode, den SecSigner am Ende einer Validierung 
 * zur&uuml;ckliefert, wird in der Datei &quot;<i><b>Status.txt</b></i>&quot; 
 * im Unterordner <br /><i>{Medys-Zusatzpfad}/med_eArztbrief/signierung/
 * <b>_status</b></i> hinterlegt
 * 
 * <br /><br />
 * 
 * Aufruf des Validerers siehe {@link com.medys.MedysPDFSignierungValidierung}
 * 
 * @author Hayri Emrah Kayaman, MEDYS GmbH W&uuml;lrath 2015
 */
public class MedysPDFValidierer extends MedysDateiOP {
	
	private String workspaceDirectory, fileDirectory;
	
	private SecSigner secSigner;
	
	private MedysPDFException medysPdfException;
	
	public MedysPDFValidierer(
			String arbeitsverzeichnis, 
			String dateiVerzeichnis, 
			String dateiName)
	{
		super();
		
		workspaceDirectory = getValidFolderPath(arbeitsverzeichnis);
		
		dateiVerzeichnis = getValidFolderPath(dateiVerzeichnis);
		
		fileDirectory = getValidFolderPath(workspaceDirectory 
											+ dateiVerzeichnis);
		
		// generelle Annahme, das irgend etwas die 
		// Validierung stört oder abbrechen lässt
		//
		schreibAusnahmeInDatei(
				"secsigner_abbruch", 
				getWorkspaceDirectory()
				+ "_status/", "Status.txt");
				
		medysPdfException = new MedysPDFException();
		
		// lade Property-Datei "secsigner.properties" für den SecSigner
		//
		Properties secSignerProperties = new Properties();

		InputStream is;

		try 
		{
			is = getClass().getClassLoader().getResourceAsStream(
					"secsigner.properties");
			secSignerProperties.load(is);
			is.close();

			try 
			{
				secSigner = new SecSigner(null, secSignerProperties);
				
				if(secSigner != null)
				{
					secSignerValidier(fileDirectory, dateiName);
				}
			} 
			catch (SecSignerException e) 
			{
				medysPdfException.setMessage(
						"FINEST", 
						"SecSigner Initialisierungsfehler: "
						+ e.getMessage());
				
				Thread.dumpStack();
				
				System.exit(1);
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (StoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OperatorCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		catch (SecurityException e) 
		{
			medysPdfException.setMessage("warning",
					"VALIDATION_STATUS_FAILED, "
					+ e.getMessage());
			
			// DEBUG only
			System.out.println("Fehler: " + e.getMessage());
			e.printStackTrace();
		}
		catch (IOException ioExcep) 
		{
			medysPdfException.setMessage("warning",
					"VALIDATION_STATUS_FAILED, " 
					+ ioExcep.getMessage());
			
			// DEBUG only
			System.out.println("Fehler: " + ioExcep.getMessage());
			ioExcep.printStackTrace();
		}
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
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
            	.append(HEXES.charAt((b & 0x0F)));
        }

        return hex.toString();
    }
    
    
	public byte[] gibPDFSignature(byte[] documentContent) throws CertificateException, StoreException, OperatorCreationException, CMSException 
	{
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		
		byte[] signatureInBytes = null;

		if (documentContent.length > 0) 
		{
			try 
			{
				PDDocument document = PDDocument.load(documentContent);
				
				List<PDSignature> signatureDictionaries = document.getSignatureDictionaries();
				
				for(PDSignature signature : signatureDictionaries)
				{
					COSDictionary sigDict = signature.getCOSObject();
					
					COSString contents = (COSString) sigDict.getDictionaryObject(COSName.CONTENTS);
					
					signatureInBytes = signature.getSignedContent(contents.getBytes());
					
					System.out.println("Signature found");
					System.out.println("Name:     " + signature.getName());
					System.out.println("Modified: " + sdf.format(signature.getSignDate().getTime()));
					System.out.println("Signatur Kontaktinfo : "
					+ signature.getContactInfo());

					System.out.println("Signatur Location	 : " + signature.getLocation());

					System.out.println("Signatur SubFilter 	 : " + signature.getSubFilter());

					System.out.println("Signatur 			 : " + signature.getFilter());

					Calendar gueltigkeitsDatum = signature.getSignDate();

					System.out.println("Signaturdatum	     : " + gueltigkeitsDatum.getTime());
					
					String subFilter = signature.getSubFilter();
					
					if (subFilter.equals("adbe.pkcs7.detached"))
					 {
						document.close();
						
						 if(verifyPKCS7(signatureInBytes, contents, signature))
						 {
							 System.out.println("adbe.pkcs7.detached Zertifikat verifiziert");
						 }
						 else
						 {
							 signatureInBytes = null;
							 break;
						 }
					 }
					else
					{
						signatureInBytes = null;
						break;
					}
				}
				
//				PDSignature sig = document.getLastSignatureDictionary();
//
//				System.out.println("Signatur Kontaktinfo : "
//						+ sig.getContactInfo());
//
//				System.out.println("Signatur Location	 : " + sig.getLocation());
//
//				System.out.println("Signatur SubFilter 	 : "
//						+ sig.getSubFilter());
//
//				System.out.println("Signatur 			 : " + sig.getFilter());
//
//				Calendar gueltigkeitsDatum = sig.getSignDate();
//
//				System.out.println("Signaturdatum	     : "
//						+ gueltigkeitsDatum.getTime());
//
//				COSString certString = (COSString) sig.getCOSObject()
//						.getDictionaryObject(COSName.CONTENTS);
//
//				signatureInBytes = certString.getString().getBytes();
//
//				document.close();

			}
			catch (IOException noDoc) 
			{
				System.out
						.println("IOEXception in checkPDFSignature(String, String)");

				noDoc.printStackTrace();
			}
		}
		return signatureInBytes;
	}

    
	public String getFileDirectory() {
		return fileDirectory;
	}

	public String getWorkspaceDirectory() {
		return workspaceDirectory;
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
     *    <td>Gültigkeit von</td>
     *    <td>Certificate Valid From</td>
     *  </tr>
     *  <tr>
     *    <td>Gültigkeit bis</td>
     *    <td>Certficate Valid To</td>
     *  </tr>
     * </table>
     * @param signature das X509Zertifikat
     * @throws CertificateEncodingException wenn es kein DER-kodiertes Zertifikat ist, <br />
     * 		   NoSuchAlgorithmException wenn es kein Zertifizierungs-Algorithmus enth&auml;t
     */
    public void readCertificate(X509Certificate x509Cert) 
    {
    	String certificateDateBefore = null;
    	String certificateDateAfter  = null;
    	
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
    
    @SuppressWarnings({"rawtypes","unused","unchecked"})
    private void bouncyCastleValidier(byte[] certificate)
    {
    	
    	Provider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        CMSSignedData signedData = null;
		try {
			signedData = new CMSSignedData(Base64.decode(certificate));
		
        CMSProcessable cmsProcesableContent = signedData.getSignedContent();
        
        
			signedData = new CMSSignedData(cmsProcesableContent, Base64.decode(certificate));
		} catch (CMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        // Verify signatureenvelopedData
        Store store = signedData.getCertificates(); 
        SignerInformationStore signers = signedData.getSignerInfos(); 
        Collection c = signers.getSigners(); 
        Iterator it = c.iterator();
        while (it.hasNext()) { 
            SignerInformation signer = (SignerInformation) it.next(); 
            Collection certCollection = store.getMatches(signer.getSID()); 
            Iterator certIt = certCollection.iterator();
            X509CertificateHolder certHolder = (X509CertificateHolder) certIt.next();
            X509Certificate certFromSignedData = null;
			try {
				certFromSignedData = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            try {
				if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(certFromSignedData))) {
				    System.out.println("Signature verified");
				} else {
				    System.out.println("Signature verification failed");
				}
			} catch (OperatorCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
	private void secSignerValidier(String dateiPfad, String dateiName) throws CertificateException, StoreException, OperatorCreationException, CMSException
	{
		File datei = gibDatei(dateiPfad, dateiName);
		
		if(datei != null)
		{
			byte[] dateiInhalt = getFileContent(dateiPfad, dateiName);

			if ((dateiInhalt.length > 0)) 
			{
				SignatureRecord signatureRecord = new SignatureRecord();

				File file = new File(dateiPfad, "TempSignedCert.txt");
				
				if(file.exists())
				{
					file.delete();
				}
				
				try 
				{
					FileWriter fw = new FileWriter(file);
					
					fw.write("\n");
					
					for(byte b : dateiInhalt)
					{
						fw.write(b);
					}
					
					fw.close();
				}
				catch(IOException fileIO)
				{
					
				}
				
				signatureRecord.setDocument(dateiInhalt);

				signatureRecord.setDocumentFileName(dateiName);
				signatureRecord.setDocumentUrl(getFileDirectory());

				byte[] signature = gibPDFSignature(dateiInhalt);
				
				if (signature.length > 0) {
					signatureRecord.setSignature(signature);
				}

				SignatureRecord[] signatureRecords = new SignatureRecord[1];
				signatureRecords[0] = signatureRecord;

				try 
				{
					/*
					 * Parameter 2 = optional
					 * 
					 * verify( 
					 * 	SignatureRecord[] signaturen, 
					 *  byte[][] externSignersCertsInDER-Format, 
					 *  boolean allowSig, 					
					 *  boolean offerFileOpenDlg, 			
					 *  boolean ocspMandatory, 				
					 *  boolean modal) // blockier Hintergrundfenster
					 */
					SecSignerVerifyResult verifier = secSigner.verify(
							signatureRecords, null, true, false, false, false);

					// SecSignerVerifyResult verifier =
					// secSigner.verify(
					// signatureRecords,
					// checkData,
					// false, true, false, false);

					if (SecSignerConstants.SECSIGNER_VERIFY_CERTVALID == 
							verifier.getStatus()) 
					{	
						schreibAusnahmeInDatei("dokument_signiert_und_validiert",
								getWorkspaceDirectory() + "_status/", "Status.txt");
					}

					// immer
					//
					secSigner.close();
				}
				catch (SecSignerException sse) 
				{
					sse.printStackTrace();
				}
			}
		}
	}
	
    @SuppressWarnings("unused")
	private void schreibeSignatureInDatei(
			String dateiPfad, 
			String dateiName, 
			byte[] signature)
	{
		File datei = legeDateiAn(dateiPfad, dateiName);
		
		if(datei != null)
		{
			try 
			{	
				String s = new String(signature);
				
				FileWriter fw = new FileWriter(datei);
				
				fw.write(s);
				
				fw.close();
			}
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
    
    /**
	 * Verify a PKCS7 signature.
	 *
	 * @param byteArray the byte sequence that has been signed
	 * @param contents the /Contents field as a COSString
	 * @param sig the PDF signature (the /V dictionary)
	 * @throws CertificateException
	 * @throws CMSException
	 * @throws StoreException
	 * @throws OperatorCreationException
	 * @throws CertificateException 
	 * @throws java.security.cert.CertificateException
	 * @return TRUE if verified, else FALSE 
	 */
    @SuppressWarnings({"rawtypes","unchecked"})
	private boolean verifyPKCS7(byte[] byteArray, COSString contents, PDSignature sig)
	throws CMSException, StoreException, OperatorCreationException
	{
	// inspiration:
	// http://stackoverflow.com/a/26702631/535646
	// http://stackoverflow.com/a/9261365/535646
		
		boolean verified = false;
		
		CMSProcessable signedContent = new CMSProcessableByteArray(byteArray);
		
		CMSSignedData signedData = 
				new CMSSignedData(signedContent, contents.getBytes());
		
		Store certificatesStore = signedData.getCertificates();
		
		Collection<SignerInformation> signers = signedData.getSignerInfos().getSigners();
		
		SignerInformation signerInformation = signers.iterator().next();
		
		Collection matches = certificatesStore.getMatches(signerInformation.getSID());
		
		X509CertificateHolder certificateHolder = (X509CertificateHolder) matches.iterator().next();
		
		try
		{
			X509Certificate certFromSignedData = new JcaX509CertificateConverter().getCertificate(certificateHolder);
		
			System.out.println("certFromSignedData: " + certFromSignedData);
		
			readCertificate(certFromSignedData);
			
			certFromSignedData.checkValidity();
			
			verified = true;
		}
		catch(CertificateException certExep)
		{
			System.out.println("Verifizierungsfehler in verifyPKCS7():\n\n" + certExep.getMessage());
		}
		
		return verified;
	}
}
