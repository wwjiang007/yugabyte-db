// Copyright (c) YugaByte, Inc.

package com.yugabyte.yw.common.certmgmt;

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.yugabyte.yw.common.PlatformServiceException;
import com.yugabyte.yw.common.certmgmt.CertificateCustomInfo.CertConfigType;
import com.yugabyte.yw.common.certmgmt.providers.CertificateProviderInterface;
import com.yugabyte.yw.common.certmgmt.providers.CertificateSelfSigned;
import com.yugabyte.yw.common.kms.util.hashicorpvault.HashicorpVaultConfigParams;
import com.yugabyte.yw.forms.CertificateParams;
import com.yugabyte.yw.forms.CertsRotateParams;
import com.yugabyte.yw.forms.TlsToggleParams;
import com.yugabyte.yw.forms.ToggleTlsParams;
import com.yugabyte.yw.forms.UniverseDefinitionTaskParams;
import com.yugabyte.yw.models.CertificateInfo;
import com.yugabyte.yw.models.helpers.CommonUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.flywaydb.play.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ebean.annotation.EnumValue;
import play.libs.Json;

/** Helper class for Certificates */
public class CertificateHelper {

  public static final Logger LOG = LoggerFactory.getLogger(CertificateHelper.class);

  public static final String CERT_PATH = "%s/certs/%s/%s";

  public static final String ROOT_CERT = "root.crt";

  public static final String SERVER_CERT = "server.crt";
  public static final String SERVER_KEY = "server.key.pem";

  public static final String CLIENT_NODE_SUFFIX = "-client";
  public static final String DEFAULT_CLIENT = "yugabyte";

  public static final String CLIENT_CERT = "yugabytedb.crt";
  public static final String CLIENT_KEY = "yugabytedb.key";

  public static final String SIGNATURE_ALGO = "SHA256withRSA";
  public static final Integer CERT_EXPIRY_IN_YEARS = 4;

  public enum CertificateType {
    @EnumValue("ROOT_CA_CERT")
    ROOT_CA_CERT,

    @EnumValue("CLIENT_CA_CERT")
    CLIENT_CA_CERT,

    @EnumValue("USER_NODE_CERT")
    USER_NODE_CERT,

    @EnumValue("USER_CLIENT_CERT")
    USER_CLIENT_CERT
  }

  public static void ManipulateRootCA(CertsRotateParams requestParams) {
    // TODO: remove following
    // hardcoded config of Hashicorp as workaround of UI changes (replace yb-admin-demo1)
    LOG.info("__YD:Called from: {}", CommonUtils.GetStackTraceHere());

    // yb-admin-demo1 to HCPKI4jan
    if (requestParams.rootCA == null) return;
    if (requestParams.rootCA.equals(UUID.fromString("c2a8bcc1-4d64-4ec1-9bbb-20a551d7342a"))) {
      requestParams.rootCA = UUID.fromString("15cca09a-ffcc-4769-9b1a-21e6b79657b6");
      LOG.info("__YD: 1 Changing the rootCA cert uuid to {}", requestParams.rootCA.toString());
      if (requestParams.clientRootCA != null) requestParams.clientRootCA = requestParams.rootCA;
    }

    // yb-admin-ydhawale-aws to p24jan
    if (requestParams.rootCA.equals(UUID.fromString("fefa2cd5-8f03-450e-968c-0a183032f9b1"))) {
      requestParams.rootCA = UUID.fromString("46ec71b8-4f8a-4f78-a11d-052507722484");
      LOG.info("__YD: 2 Changing the rootCA cert uuid to {}", requestParams.rootCA.toString());
      if (requestParams.clientRootCA != null) requestParams.clientRootCA = requestParams.rootCA;
    }
  }

  public static void ManipulateRootCA2(TlsToggleParams requestParams) {
    // TODO: remove following
    // hardcoded config of Hashicorp as workaround of UI changes (replace yb-admin-demo1)
    LOG.info("__YD:Called from: {}", CommonUtils.GetStackTraceHere());

    if (requestParams.rootCA == null) return;
    if (requestParams.rootCA.equals(UUID.fromString("c2a8bcc1-4d64-4ec1-9bbb-20a551d7342a"))) {
      LOG.info("_YD: 3 Changing the rootCA cert uuid to 15cca09a-ffcc-4769-9b1a-21e6b79657b6");
      requestParams.rootCA = UUID.fromString("15cca09a-ffcc-4769-9b1a-21e6b79657b6");
      if (requestParams.clientRootCA != null) requestParams.clientRootCA = requestParams.rootCA;
    }
    // yb-admin-ydhawale-aws to p24jan
    if (requestParams.rootCA.equals(UUID.fromString("fefa2cd5-8f03-450e-968c-0a183032f9b1"))) {
      requestParams.rootCA = UUID.fromString("46ec71b8-4f8a-4f78-a11d-052507722484");
      LOG.info("__YD: 4 Changing the rootCA cert uuid to {}", requestParams.rootCA.toString());
      if (requestParams.clientRootCA != null) requestParams.clientRootCA = requestParams.rootCA;
    }
  }

  public static void ManipulateRootCA3(ToggleTlsParams requestParams) {
    // TODO: remove following
    // hardcoded config of Hashicorp as workaround of UI changes (replace yb-admin-demo1)
    LOG.info("__YD:Called from: {}", CommonUtils.GetStackTraceHere());

    if (requestParams.rootCA == null) return;
    if (requestParams.rootCA.equals(UUID.fromString("c2a8bcc1-4d64-4ec1-9bbb-20a551d7342a"))) {
      LOG.info("_YD: 5 Changing the rootCA cert uuid to 15cca09a-ffcc-4769-9b1a-21e6b79657b6");
      requestParams.rootCA = UUID.fromString("15cca09a-ffcc-4769-9b1a-21e6b79657b6");
      if (requestParams.clientRootCA != null) requestParams.clientRootCA = requestParams.rootCA;
    }

    // yb-admin-ydhawale-aws to p24jan
    if (requestParams.rootCA.equals(UUID.fromString("fefa2cd5-8f03-450e-968c-0a183032f9b1"))) {
      requestParams.rootCA = UUID.fromString("46ec71b8-4f8a-4f78-a11d-052507722484");
      LOG.info("__YD: 6 Changing the rootCA cert uuid to {}", requestParams.rootCA.toString());
      if (requestParams.clientRootCA != null) requestParams.clientRootCA = requestParams.rootCA;
    }
  }

  public static void ManipulateRootCA4(UniverseDefinitionTaskParams requestParams) {
    // TODO: remove following
    // hardcoded config of Hashicorp as workaround of UI changes (replace yb-admin-demo1)
    LOG.info("__YD:Called from: {}", CommonUtils.GetStackTraceHere());

    if (requestParams.rootCA == null) return;
    if (requestParams.rootCA.equals(UUID.fromString("c2a8bcc1-4d64-4ec1-9bbb-20a551d7342a"))) {
      LOG.info("_YD: 7 Changing the rootCA cert uuid to 15cca09a-ffcc-4769-9b1a-21e6b79657b6");
      requestParams.rootCA = UUID.fromString("15cca09a-ffcc-4769-9b1a-21e6b79657b6");
      if (requestParams.clientRootCA != null) requestParams.clientRootCA = requestParams.rootCA;
    }

    // yb-admin-ydhawale-aws to p24jan
    if (requestParams.rootCA.equals(UUID.fromString("fefa2cd5-8f03-450e-968c-0a183032f9b1"))) {
      requestParams.rootCA = UUID.fromString("46ec71b8-4f8a-4f78-a11d-052507722484");
      LOG.info("__YD: 8 Changing the rootCA cert uuid to {}", requestParams.rootCA.toString());
      if (requestParams.clientRootCA != null) requestParams.clientRootCA = requestParams.rootCA;
    }
  }

  public static String getCACertPath(String storagePath, UUID customerUUID, UUID caCertUUID) {
    return String.format(
        CERT_PATH + "/ca.%s",
        storagePath,
        customerUUID.toString(),
        caCertUUID.toString(),
        ROOT_CERT);
  }

  public static String getCAKeyPath(String storagePath, UUID customerUUID, UUID caCertUUID) {
    return String.format(
        CERT_PATH + "/ca.key.pem", storagePath, customerUUID.toString(), caCertUUID.toString());
  }

  private static String generateUniqueRootCALabel(String nodePrefix, CertConfigType certType) {

    // Default the cert label with node prefix.
    // If cert with the label already exists append number

    String certLabel = nodePrefix;
    List<CertificateInfo> certificateInfoList =
        CertificateInfo.getWhereLabelStartsWith(nodePrefix, certType);
    if (!certificateInfoList.isEmpty()) {
      certificateInfoList.sort(Comparator.comparing(a -> a.label, Comparator.reverseOrder()));
      String[] labelArray = certificateInfoList.get(0).label.split("~");
      int lastCount = 0;
      try {
        lastCount = Integer.parseInt(labelArray[labelArray.length - 1]);
      } catch (NumberFormatException ignored) {
      }
      certLabel = nodePrefix + "~" + (++lastCount);
    }
    return certLabel;
  }

  public static UUID createRootCA(String nodePrefix, UUID customerUUID, String storagePath) {
    LOG.info("Creating root certificate for {}", nodePrefix);
    LOG.info(
        "_YD:Creating root certificate for {} from: {}",
        nodePrefix,
        CommonUtils.GetStackTraceHere());

    try {
      CertConfigType certType = CertConfigType.SelfSigned;
      String certLabel = generateUniqueRootCALabel(nodePrefix, certType);

      UUID rootCA_UUID = UUID.randomUUID();
      KeyPair keyPair = getKeyPairObject();

      CertificateSelfSigned obj = new CertificateSelfSigned(rootCA_UUID);
      X509Certificate x509 = obj.generateRootCertificate(certLabel, CERT_EXPIRY_IN_YEARS, keyPair);
      Pair<String, String> location = obj.dumpCACertBundle(storagePath, customerUUID);
      Date certStart = x509.getNotBefore();
      Date certExpiry = x509.getNotAfter();

      LOG.info(
          "Generated self signed cert label {} uuid {} of type {} for customer {} at paths {}, {}",
          certLabel,
          rootCA_UUID,
          certType,
          customerUUID,
          location.getLeft(),
          location.getRight());

      CertificateInfo cert =
          CertificateInfo.create(
              rootCA_UUID,
              customerUUID,
              certLabel,
              certStart,
              certExpiry,
              location.getRight(),
              location.getLeft(),
              certType);

      LOG.info("Created Root CA for universe {}.", certLabel);
      return cert.uuid;
    } catch (Exception e) {
      LOG.error("Unable to create RootCA for universe {}", nodePrefix, e);
      return null;
    }
  }

  public static UUID CreateConfigInfoDBEntry(
      UUID configUUID,
      UUID customerUUID,
      String label,
      X509Certificate x509,
      String certPath,
      String keyPath,
      HashicorpVaultConfigParams hcVaultParams)
      throws Exception {
    Date certStart = x509.getNotBefore();
    Date certExpiry = x509.getNotAfter();

    CertificateInfo cert =
        CertificateInfo.create(
            configUUID, customerUUID, label, certStart, certExpiry, certPath, hcVaultParams);
    LOG.info("Created Root CA for universe {}.", label);

    return cert.uuid;
  }

  public static UUID createClientRootCA(String nodePrefix, UUID customerUUID, String storagePath) {
    LOG.info("Creating a client root certificate for {}", nodePrefix);
    return createRootCA(nodePrefix + CLIENT_NODE_SUFFIX, customerUUID, storagePath);
  }

  public static CertificateDetails dumpNewCertsToFile(
      String storagePath,
      String certFileName,
      String certKeyName,
      X509Certificate clientCert,
      PrivateKey pKey)
      throws IOException {
    JcaPEMWriter certificateWriter = null;
    JcaPEMWriter certKeyWriter = null;
    CertificateDetails certificateDetails = new CertificateDetails();

    try {
      if (storagePath != null) {
        // get file path write it there
        String clientCertPath = String.format("%s/%s", storagePath, certFileName);
        String clientKeyPath = String.format("%s/%s", storagePath, certKeyName);
        writeCertFileContentToCertPath(clientCert, clientCertPath);
        writeKeyFileContentToKeyPath(pKey, clientKeyPath);
        LOG.info(
            "Dumping certificate {} at Path {}",
            clientCert.getSubjectDN().toString(),
            clientCertPath);

      } else {
        // storagePath is null, converting it to string and returning it.
        StringWriter certWriter = new StringWriter();
        StringWriter keyWriter = new StringWriter();
        certificateWriter = new JcaPEMWriter(certWriter);
        certKeyWriter = new JcaPEMWriter(keyWriter);
        certificateWriter.writeObject(clientCert);
        certificateWriter.flush();
        certKeyWriter.writeObject(pKey);
        certKeyWriter.flush();
        certificateDetails.crt = certWriter.toString();
        certificateDetails.key = keyWriter.toString();
        LOG.info("Returning certificate {} as Strings", clientCert.getSubjectDN().toString());
      }
    } finally {
      if (certificateWriter != null) {
        certificateWriter.close();
      }
      if (certKeyWriter != null) {
        certKeyWriter.close();
      }
    }
    return certificateDetails;
  }

  public static CertificateDetails createClientCertificate(
      UUID rootCA, String storagePath, String username, Date certStart, Date certExpiry) {
    LOG.info("Creating client certificate for {}", username);

    CertificateInfo rootCertConfigInfo = CertificateInfo.get(rootCA);
    CertificateProviderInterface certProvider =
        EncryptionAtTransitUtil.getCertificateProviderInstance(rootCertConfigInfo);

    return certProvider.createCertificate(
        storagePath,
        username,
        certStart,
        certExpiry,
        CertificateHelper.CLIENT_CERT,
        CertificateHelper.CLIENT_KEY);
  }

  public static CertificateDetails createServerCertificate(
      UUID rootCA,
      String storagePath,
      String username,
      Date certStart,
      Date certExpiry,
      String certFileName,
      String certKeyName) {
    LOG.info("Creating server certificate for {}", username);

    CertificateInfo rootCertConfigInfo = CertificateInfo.get(rootCA);
    CertificateProviderInterface certProvider =
        EncryptionAtTransitUtil.getCertificateProviderInstance(rootCertConfigInfo);

    return certProvider.createCertificate(
        storagePath, username, certStart, certExpiry, certFileName, certKeyName);
  }

  /**
   * Extract start and end dates from cert bundle/list
   *
   * @param x509CACerts
   * @return Pair of <StartDate, EndDate>
   */
  public static Pair<Date, Date> extractDatesFromCertBundle(List<X509Certificate> x509CACerts) {
    long certStartTimestamp = 0L;
    long certExpiryTimestamp = Long.MAX_VALUE;

    for (X509Certificate cert : x509CACerts) {
      if (cert.getNotBefore().getTime() > certStartTimestamp) {
        certStartTimestamp = cert.getNotBefore().getTime();
      }
      if (cert.getNotAfter().getTime() < certExpiryTimestamp) {
        certExpiryTimestamp = cert.getNotAfter().getTime();
      }
    }

    return new ImmutablePair<>(new Date(certStartTimestamp), new Date(certExpiryTimestamp));
  }

  public static UUID uploadRootCA(
      String label,
      UUID customerUUID,
      String storagePath,
      String certContent,
      String keyContent,
      Date certStart,
      Date certExpiry,
      CertConfigType certType,
      CertificateParams.CustomCertPathParams customCertPathParams,
      CertificateParams.CustomServerCertParams customSrvCertParams) {
    LOG.debug("uploadRootCA: Label: {}, customerUUID: {}", label, customerUUID.toString());
    try {
      if (certContent == null) {
        throw new PlatformServiceException(BAD_REQUEST, "Certfile can't be null");
      }
      UUID rootCA_UUID = UUID.randomUUID();
      String keyPath = null;
      CertificateInfo.CustomServerCertInfo customServerCertInfo = null;
      List<X509Certificate> x509CACerts = getX509CertificateCertObject(certContent);

      Pair<Date, Date> dates = extractDatesFromCertBundle(x509CACerts);
      certStart = dates.getLeft();
      certExpiry = dates.getRight();

      // Verify the uploaded cert is a verified cert chain.
      verifyCertValidity(x509CACerts);
      if (certType == CertConfigType.SelfSigned) {
        // The first entry in the file should be the cert we want to use for generating server
        // certs.
        verifyCertSignatureAndOrder(x509CACerts, keyContent);
        keyPath = getCAKeyPath(storagePath, customerUUID, rootCA_UUID);

      } else if (certType == CertConfigType.CustomServerCert) {
        // Verify the upload Server Cert is a verified cert chain.
        List<X509Certificate> x509ServerCertificates =
            getX509CertificateCertObject(customSrvCertParams.serverCertContent);
        // Verify that the uploaded server cert was signed by the uploaded CA cert
        List<X509Certificate> combinedArrayList = new ArrayList<>(x509ServerCertificates);
        combinedArrayList.addAll(x509CACerts);
        verifyCertValidity(combinedArrayList);
        // The first entry in the file should be the cert we want to use for generating server
        // certs.
        verifyCertSignatureAndOrder(x509ServerCertificates, customSrvCertParams.serverKeyContent);
        String serverCertPath =
            String.format(
                "%s/certs/%s/%s/%s",
                storagePath, customerUUID.toString(), rootCA_UUID.toString(), SERVER_CERT);
        String serverKeyPath =
            String.format(
                "%s/certs/%s/%s/%s",
                storagePath, customerUUID.toString(), rootCA_UUID.toString(), SERVER_KEY);
        writeCertBundleToCertPath(x509ServerCertificates, serverCertPath);
        writeKeyFileContentToKeyPath(
            getPrivateKey(customSrvCertParams.serverKeyContent), serverKeyPath);
        customServerCertInfo =
            new CertificateInfo.CustomServerCertInfo(serverCertPath, serverKeyPath);
      } else if (certType == CertConfigType.HashicorpVaultPKI) {
        // Not applicable
      }
      String certPath = getCACertPath(storagePath, customerUUID, rootCA_UUID);
      writeCertBundleToCertPath(getX509CertificateCertObject(certContent), certPath);

      CertificateInfo cert;
      switch (certType) {
        case SelfSigned:
          {
            writeKeyFileContentToKeyPath(getPrivateKey(keyContent), keyPath);
            cert =
                CertificateInfo.create(
                    rootCA_UUID,
                    customerUUID,
                    label,
                    certStart,
                    certExpiry,
                    keyPath,
                    certPath,
                    certType);
            break;
          }
        case CustomCertHostPath:
          {
            cert =
                CertificateInfo.create(
                    rootCA_UUID,
                    customerUUID,
                    label,
                    certStart,
                    certExpiry,
                    certPath,
                    customCertPathParams);
            break;
          }
        case CustomServerCert:
          {
            cert =
                CertificateInfo.create(
                    rootCA_UUID,
                    customerUUID,
                    label,
                    certStart,
                    certExpiry,
                    certPath,
                    customServerCertInfo);
            break;
          }
        case HashicorpVaultPKI:
          {
            // not applicable
          }
        default:
          {
            throw new PlatformServiceException(BAD_REQUEST, "certType should be valid.");
          }
      }
      LOG.info(
          "Uploaded cert label {} (uuid {}) of type {} at paths"
              + " '{}', '{}' with custom cert info {}",
          label,
          rootCA_UUID,
          certType,
          certPath,
          String.valueOf(keyPath),
          Json.toJson(customCertPathParams));
      return cert.uuid;
    } catch (IOException | NoSuchAlgorithmException e) {
      LOG.error(
          "uploadRootCA: Could not generate checksum for cert {} for customer {}",
          label,
          customerUUID.toString());
      throw new PlatformServiceException(
          INTERNAL_SERVER_ERROR, "uploadRootCA: Checksum generation failed.");
    }
  }

  public static String getCertPEMFileContents(UUID rootCA) {
    CertificateInfo cert = CertificateInfo.get(rootCA);
    String certPEM = FileUtils.readFileToString(new File(cert.certificate));
    return certPEM;
  }

  public static String getCertPEM(UUID rootCA) {
    String certPEM = getCertPEMFileContents(rootCA);
    certPEM = Base64.getEncoder().encodeToString(certPEM.getBytes());
    return certPEM;
  }

  public static String getCertPEM(CertificateInfo cert) {
    String certPEM = FileUtils.readFileToString(new File(cert.certificate));
    certPEM = Base64.getEncoder().encodeToString(certPEM.getBytes());
    return certPEM;
  }

  public static String getKeyPEM(CertificateInfo cert) {
    String privateKeyPEM = FileUtils.readFileToString(new File(cert.privateKey));
    privateKeyPEM = Base64.getEncoder().encodeToString(privateKeyPEM.getBytes());
    return privateKeyPEM;
  }

  public static String getKeyPEM(UUID rootCA) {
    CertificateInfo cert = CertificateInfo.get(rootCA);
    String privateKeyPEM = FileUtils.readFileToString(new File(cert.privateKey));
    privateKeyPEM = Base64.getEncoder().encodeToString(privateKeyPEM.getBytes());
    return privateKeyPEM;
  }

  public static String getClientCertFile(UUID rootCA) {
    CertificateInfo cert = CertificateInfo.get(rootCA);
    File certFile = new File(cert.certificate);
    String path = certFile.getParentFile().toString();
    return String.format("%s/%s", path, CLIENT_CERT);
  }

  public static String getClientKeyFile(UUID rootCA) {
    CertificateInfo cert = CertificateInfo.get(rootCA);
    File certFile = new File(cert.certificate);
    String path = certFile.getParentFile().toString();
    return String.format("%s/%s", path, CLIENT_KEY);
  }

  public static boolean areCertsDiff(UUID cert1, UUID cert2) {
    try {
      CertificateInfo cer1 = CertificateInfo.get(cert1);
      CertificateInfo cer2 = CertificateInfo.get(cert2);
      FileInputStream is1 = new FileInputStream(new File(cer1.certificate));
      FileInputStream is2 = new FileInputStream(new File(cer2.certificate));
      CertificateFactory fact = CertificateFactory.getInstance("X.509");
      X509Certificate certObj1 = (X509Certificate) fact.generateCertificate(is1);
      X509Certificate certObj2 = (X509Certificate) fact.generateCertificate(is2);
      return !certObj2.equals(certObj1);
    } catch (IOException | CertificateException e) {
      LOG.error("Unable to read certs {}: {}", cert1.toString(), cert2.toString());
      throw new RuntimeException("Could not read certs to compare. " + e);
    }
  }

  public static boolean arePathsSame(UUID cert1, UUID cert2) {
    CertificateInfo cer1 = CertificateInfo.get(cert1);
    CertificateInfo cer2 = CertificateInfo.get(cert2);
    return (cer1.getCustomCertPathParams()
            .nodeCertPath
            .equals(cer2.getCustomCertPathParams().nodeCertPath)
        || cer1.getCustomCertPathParams()
            .nodeKeyPath
            .equals(cer2.getCustomCertPathParams().nodeKeyPath));
  }

  public static void createChecksums() {
    List<CertificateInfo> certs = CertificateInfo.getAllNoChecksum();
    for (CertificateInfo cert : certs) {
      try {
        cert.setChecksum();
      } catch (IOException | NoSuchAlgorithmException e) {
        // Log error, but don't cause it to error out.
        LOG.error("Could not generate checksum for cert: {}", cert.certificate);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static List<X509Certificate> getX509CertificateCertObject(String certContent) {
    try {
      InputStream in = null;
      byte[] certEntryBytes = certContent.getBytes();
      in = new ByteArrayInputStream(certEntryBytes);
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
      return (List<X509Certificate>) (List<?>) certFactory.generateCertificates(in);
    } catch (CertificateException e) {
      LOG.error(e.getMessage());
      throw new RuntimeException("Unable to get cert Object");
    }
  }

  public static String getCertificateProperties(X509Certificate cert) {

    String san = "";
    try {
      san = cert.getSubjectAlternativeNames().toString();
    } catch (Exception e) {
      san = "exception";
    }
    String ret;
    ret = "cert info: " + System.lineSeparator();
    ret += String.format("\t dn:%s", cert.getIssuerDN().toString());
    ret += String.format("\t subject:%s", cert.getSubjectDN().toString());
    ret += System.lineSeparator();
    ret += String.format("\t ip_san:%s", san);
    ret += System.lineSeparator();
    ret += String.format("\t beforeDate:%s", cert.getNotBefore().toString());
    ret += String.format("\t AfterDate:%s", cert.getNotAfter().toString());
    ret += System.lineSeparator();

    ret += String.format("\t serial:%s", cert.getSerialNumber().toString(16));
    return ret;
  }

  public static X509Certificate convertStringToX509Cert(String certificate) throws Exception {
    java.security.Security.addProvider(new BouncyCastleProvider());

    certificate = certificate.replace("\\n", "");
    certificate = certificate.replaceAll("^\"+|\"+$", "");
    certificate = certificate.replace("-----BEGIN CERTIFICATE-----", "");
    certificate = certificate.replace("-----END CERTIFICATE-----", "");

    byte[] certificateData = Base64.getMimeDecoder().decode(certificate);
    CertificateFactory cf = CertificateFactory.getInstance("X509");
    return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateData));
  }

  public static PrivateKey convertStringToPrivateKey(String strKey) throws Exception {

    java.security.Security.addProvider(new BouncyCastleProvider());

    strKey = strKey.replace(System.lineSeparator(), "");
    strKey = strKey.replaceAll("^\"+|\"+$", "");
    strKey = strKey.replace("-----BEGIN PRIVATE KEY-----", "");
    strKey = strKey.replace("-----END PRIVATE KEY-----", "");
    strKey = strKey.replace("-----BEGIN RSA PRIVATE KEY-----", "");
    strKey = strKey.replace("-----END RSA PRIVATE KEY-----", "");

    byte[] decoded = Base64.getMimeDecoder().decode(strKey);

    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(spec);
  }

  public static PrivateKey getPrivateKey(String keyContent) {
    try (PemReader pemReader = new PemReader(new StringReader(new String(keyContent)))) {
      PemObject pemObject = pemReader.readPemObject();
      byte[] bytes = pemObject.getContent();
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(spec);
    } catch (Exception e) {
      LOG.error(e.getMessage());
      throw new RuntimeException("Unable to get Private Key");
    }
  }

  public static void writeCertFileContentToCertPath(X509Certificate cert, String certPath) {
    File certFile = new File(certPath);
    try (JcaPEMWriter certWriter = new JcaPEMWriter(new FileWriter(certFile))) {
      certWriter.writeObject(cert);
      certWriter.flush();
    } catch (Exception e) {
      LOG.error(e.getMessage());
      throw new RuntimeException("Save certificate failed.");
    }
  }

  public static void writeKeyFileContentToKeyPath(PrivateKey keyContent, String keyPath) {
    File keyFile = new File(keyPath);
    try (JcaPEMWriter keyWriter = new JcaPEMWriter(new FileWriter(keyFile))) {
      keyWriter.writeObject(keyContent);
      keyWriter.flush();
    } catch (Exception e) {
      LOG.error(e.getMessage());
      throw new RuntimeException("Save privateKey failed.");
    }
  }

  public static void writeCertBundleToCertPath(List<X509Certificate> certs, String certPath) {
    File certfile = new File(certPath);
    // Create directory to store the certFile.
    certfile.getParentFile().mkdirs();
    LOG.info("Dumpting certs at path: {}", certPath);
    try (JcaPEMWriter certWriter = new JcaPEMWriter(new FileWriter(certfile))) {
      for (X509Certificate cert : certs) {
        LOG.info(getCertificateProperties(cert));
        certWriter.writeObject(cert);
        certWriter.flush();
      }
    } catch (Exception e) {
      LOG.error(e.getMessage());
      throw new RuntimeException("Save certContent failed.");
    }
  }

  public static KeyPair getKeyPairObject() {
    try {
      // Add the security provider in case it was never called.
      Security.addProvider(new BouncyCastleProvider());
      KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("RSA");
      keypairGen.initialize(2048);
      return keypairGen.generateKeyPair();
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return null;
  }

  private static boolean verifySignature(X509Certificate cert, String key) {
    try {
      // Add the security provider in case verifySignature was never called.
      getKeyPairObject();
      RSAPrivateKey privKey = (RSAPrivateKey) getPrivateKey(key);
      RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
      return privKey.getModulus().toString().equals(publicKey.getModulus().toString());
    } catch (Exception e) {
      LOG.error("Cert or key is invalid." + e.getMessage());
    }
    return false;
  }

  // Verify that each certificate in the root chain has been signed by
  // another cert present in the uploaded file.
  private static void verifyCertValidity(List<X509Certificate> certs) {
    certs
        .stream()
        .forEach(
            cert -> {
              if (!certs
                  .stream()
                  .anyMatch(potentialRootCert -> verifyCertValidity(cert, potentialRootCert))) {
                X500Name x500Name = new X500Name(cert.getSubjectX500Principal().getName());
                RDN cn = x500Name.getRDNs(BCStyle.CN)[0];
                throw new PlatformServiceException(
                    BAD_REQUEST,
                    "Certificate with CN = "
                        + cn.getFirst().getValue()
                        + " has no associated root");
              }
              verifyCertDateValidity(cert);
            });
  }

  // Verify that certificate is currently valid and valid for 1 day
  private static void verifyCertDateValidity(X509Certificate cert) {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, 1);
    Date oneDayAfterToday = cal.getTime();
    try {
      cert.checkValidity();
      cert.checkValidity(oneDayAfterToday);
    } catch (Exception e) {
      X500Name x500Name = new X500Name(cert.getSubjectX500Principal().getName());
      RDN cn = x500Name.getRDNs(BCStyle.CN)[0];
      throw new PlatformServiceException(
          BAD_REQUEST,
          "Certificate with CN = " + cn.getFirst().getValue() + " has invalid start/end dates.");
    }
  }

  private static boolean verifyCertValidity(
      X509Certificate cert, X509Certificate potentialRootCert) {
    try {
      cert.verify(potentialRootCert.getPublicKey());
      return true;
    } catch (Exception exp) {
      // Exception means the verify failed.
      return false;
    }
  }

  private static boolean verifyCertSignatureAndOrder(
      List<X509Certificate> x509Certificates, String keyContent) {
    if (!verifySignature(x509Certificates.get(0), keyContent)) {
      // If the first certificate is not the right one, maybe the user has entered the
      // certificates in the wrong order. Check and update the customer with the right
      // message.
      x509Certificates
          .stream()
          .forEach(
              x509Certificate -> {
                if (verifySignature(x509Certificate, keyContent)) {
                  X500Name x500Name =
                      new X500Name(x509Certificate.getSubjectX500Principal().getName());
                  RDN cn = x500Name.getRDNs(BCStyle.CN)[0];
                  throw new PlatformServiceException(
                      BAD_REQUEST,
                      "Certificate with CN = "
                          + cn.getFirst().getValue()
                          + "should be the first entry in the file.");
                }
              });
      throw new PlatformServiceException(BAD_REQUEST, "Certificate and key don't match.");
    }
    return true;
  }
}
