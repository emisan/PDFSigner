package com.medys;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

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
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.StoreException;


public class PDFSignatureChecker {

	private SimpleDateFormat sdf;
	
	protected String pdfFilePath, pdfFileName;
	
	public PDFSignatureChecker(String pdfFilePath, String pdfFileName)
	{
		sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		
		this.pdfFileName = pdfFileName;
		this.pdfFilePath = pdfFilePath;
	}
	
	public boolean hasSignature() throws CertificateException, StoreException, OperatorCreationException, CMSException, NoSuchAlgorithmException
	{
		boolean hasSig = false;
		boolean pdfPathOK = false;
		byte[] signatureContent = null;
		
		if(!pdfFilePath.toLowerCase().endsWith(".pdf"))
		{
			if(!pdfFilePath.endsWith(File.separator))
			{
				pdfFilePath += File.separator;
			}
		}
		
		if(pdfFilePath.endsWith(File.separator))
		{
			pdfPathOK = true;
		}
		
		if(pdfPathOK)
		{
			if(pdfFileName.toLowerCase().endsWith(".pdf"))
			{
				File pdf = new File(pdfFilePath+pdfFileName);
				
				try 
				{
					PDDocument pDoc = PDDocument.load(pdf);
					
					List<PDSignature> signatureDictionaries = pDoc.getSignatureDictionaries();
					
					for(PDSignature signature : signatureDictionaries)
					{
						
						COSDictionary sigDict = signature.getCOSObject();
						COSString contents = (COSString) sigDict.getDictionaryObject(COSName.CONTENTS);
						
						signatureContent = signature.getSignedContent(contents.getBytes());
						
						 System.out.println("Signature found");
						 System.out.println("Name:     " + signature.getName());
						 System.out.println("Modified: " + sdf.format(signature.getSignDate().getTime()));
						 String subFilter = signature.getSubFilter();
						 
						 if (subFilter != null)
						 {
							 if (subFilter.equals("adbe.pkcs7.detached"))
							 {
								 if(verifyPKCS7(signatureContent, contents, signature))
								 {
									 System.out.println("adbe.pkcs7.detached Zertifikat verifiziert");
									 
									 hasSig = true;
								 }
								 //TODO check certificate chain, revocation lists, timestamp...
							 }
							 else if (subFilter.equals("adbe.pkcs7.sha1"))
							 {
								 // example: PDFBOX-1452.pdf
								 COSString certString = (COSString) sigDict.getDictionaryObject(
										 COSName.CONTENTS);
								 byte[] certData = certString.getBytes();
								 CertificateFactory factory = CertificateFactory.getInstance("X.509");
								 ByteArrayInputStream certStream = new ByteArrayInputStream(certData);
								 Collection<? extends Certificate> certs = factory.generateCertificates(certStream);
								 System.out.println("certs=" + certs);
							 
								 byte[] hash = MessageDigest.getInstance("SHA1").digest(signatureContent);
								 if(verifyPKCS7(hash, contents, signature))
								 {
									 System.out.println("adbe.pkcs7.sha1 Zertifikat verifiziert");
									 
									 hasSig = true;
								 }
							 
								 //TODO check certificate chain, revocation lists, timestamp...
							}
							else if (subFilter.equals("adbe.x509.rsa_sha1"))
							{
								 // example: PDFBOX-2693.pdf
								 COSString certString = (COSString) sigDict.getDictionaryObject(
								 COSName.getPDFName("Cert"));
								 byte[] certData = certString.getBytes();
								 CertificateFactory factory = CertificateFactory.getInstance("X.509");
								 ByteArrayInputStream certStream = new ByteArrayInputStream(certData);
								 Collection<? extends Certificate> certs = factory.generateCertificates(certStream);
								 
								 if(certs.size()>0)
								 {
									 System.out.println("adbe.x509.rsa_sha1 Zertifikate =" + certs);
									 
									 hasSig = true;
								 }
								 
								 //TODO verify signature
							}
							else
							{
								System.err.println("Unbekannter Zertifikat Typ: " + subFilter);
							}
						 }
					}
				}
				catch(IOException noPDLoad)
				{
					// nur beim DEBUGGEN sichtbar
					//
					noPDLoad.printStackTrace();
				}
			}
		}
		
		return hasSig;
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
	throws CMSException, CertificateException, StoreException, OperatorCreationException, CertificateException
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
		
		X509Certificate certFromSignedData = new JcaX509CertificateConverter().getCertificate(certificateHolder);
		
		System.out.println("certFromSignedData: " + certFromSignedData);
		
		certFromSignedData.checkValidity(sig.getSignDate().getTime());

		if (signerInformation.verify(new JcaSimpleSignerInfoVerifierBuilder()
				.build(certFromSignedData)))
		{
			verified = true;
			System.out.println("Signature verified");
		}
		else
		{
			System.out.println("Signature verification failed");
		}
		
		return verified;
	}
	
	public void printSignature() throws CertificateException, StoreException, OperatorCreationException, NoSuchAlgorithmException, CMSException
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("------ PDF Signature  Report-Start --------\n");
		
		if(hasSignature())
		{
			System.out.println("SIGNATURE VORHANDEN");
		}
		else
		{
			System.out.println("KEINE SIGNATURE VORHANDEN");
		}
		
		sb.append("------ PDF Signature  Report-End  --------");
	}
}
