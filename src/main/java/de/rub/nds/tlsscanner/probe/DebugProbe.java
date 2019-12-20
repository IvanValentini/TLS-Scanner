/**
 * TLS-Scanner - A TLS configuration and analysis tool based on TLS-Attacker.
 *
 * Copyright 2017-2019 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsscanner.probe;

import de.rub.nds.tlsattacker.attacks.cca.CcaCertificateGenerator;
import de.rub.nds.tlsattacker.attacks.cca.CcaCertificateType;
import de.rub.nds.tlsattacker.attacks.cca.CcaWorkflowGenerator;
import de.rub.nds.tlsattacker.attacks.cca.CcaWorkflowType;
import de.rub.nds.tlsattacker.attacks.config.CcaCommandConfig;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.config.delegate.CcaDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.ClientDelegate;
import de.rub.nds.tlsattacker.core.constants.*;
import de.rub.nds.tlsattacker.core.protocol.message.CertificateMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.constants.ScannerDetail;
import de.rub.nds.tlsscanner.rating.TestResult;
import de.rub.nds.tlsscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.report.SiteReport;
import de.rub.nds.tlsscanner.report.result.CcaResult;
import de.rub.nds.tlsscanner.report.result.ProbeResult;
import de.rub.nds.tlsscanner.report.result.VersionSuiteListPair;
import de.rub.nds.tlsscanner.report.result.cca.CcaTestResult;

import java.util.LinkedList;
import java.util.List;

public class DebugProbe extends TlsProbe {
    private List<VersionSuiteListPair> versionSuiteListPairsList;

    public DebugProbe(ScannerConfig config, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, ProbeType.CCA, config, 5);
        versionSuiteListPairsList = new LinkedList<>();
    }

    @Override
    public ProbeResult executeTest() {
        CcaCommandConfig ccaConfig = new CcaCommandConfig(getScannerConfig().getGeneralDelegate());
        ClientDelegate delegate = (ClientDelegate) ccaConfig.getDelegate(ClientDelegate.class);
        delegate.setHost(getScannerConfig().getClientDelegate().getHost());
        delegate.setSniHostname(getScannerConfig().getClientDelegate().getSniHostname());
        CcaDelegate ccaDelegate = (CcaDelegate) getScannerConfig().getDelegate(CcaDelegate.class);

        /**
         * Add any protocol version (1.0-1.2) to the versions we iterate
         */
        List<ProtocolVersion> desiredVersions = new LinkedList<>();
//        desiredVersions.add(ProtocolVersion.TLS11);
        desiredVersions.add(ProtocolVersion.TLS10);
//        desiredVersions.add(ProtocolVersion.TLS12);


        /**
         * Add any VersionSuitePair that is supported by the target
         * and by our test cases (Version 1.0 - 1.2)
         */

        /**
         * If we do not want a detailed scan, use only one cipher suite per protocol version.
         * TODO: Do I want to make sure it's the same for all? If yes I'd have the take a DH/DHE suite from the lowest
         * protocol version and use that.
         */


        List<CipherSuite> cipherSuites = new LinkedList<>();

        cipherSuites.add(CipherSuite.TLS_AES_256_GCM_SHA384);
        cipherSuites.add(CipherSuite.TLS_CHACHA20_POLY1305_SHA256);
        cipherSuites.add(CipherSuite.TLS_AES_128_GCM_SHA256);
        cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384);
        cipherSuites.add(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384);
        cipherSuites.add(CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384);
        cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256);
        cipherSuites.add(CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256);
        cipherSuites.add(CipherSuite.TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256);
        cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256);
        cipherSuites.add(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256);
        cipherSuites.add(CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256);
        cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384);
        cipherSuites.add(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384);
        cipherSuites.add(CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256);
        cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256);
        cipherSuites.add(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256);
        cipherSuites.add(CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA256);
        cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA);
        cipherSuites.add(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA);
        cipherSuites.add(CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA);
        cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA);
        cipherSuites.add(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA);
        cipherSuites.add(CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA);
        cipherSuites.add(CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384);
        cipherSuites.add(CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256);
        cipherSuites.add(CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA256);
        cipherSuites.add(CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA256);
        cipherSuites.add(CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA);
        cipherSuites.add(CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA);
        cipherSuites.add(CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);

//        cipherSuites.addAll(CipherSuite.getImplemented());
//        cipherSuites.add(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384);

        List<CcaTestResult> resultList = new LinkedList<>();
        Boolean bypassable = false;
//        for (CcaWorkflowType ccaWorkflowType : CcaWorkflowType.values()) {
        CcaWorkflowType ccaWorkflowType = CcaWorkflowType.CRT_CKE_ZFIN;
        CcaCertificateType ccaCertificateType = CcaCertificateType.CLIENT_INPUT;
//            for (CcaCertificateType ccaCertificateType : CcaCertificateType.values()) {
                for (ProtocolVersion protocolVersion : desiredVersions) {
                    CipherSuite cipherSuite = CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA;
                    CertificateMessage certificateMessage = null;
                        Config tlsConfig = ccaConfig.createConfig();
                        tlsConfig.setDefaultClientSupportedCiphersuites(cipherSuites);
                        tlsConfig.setHighestProtocolVersion(ProtocolVersion.TLS12);
                        tlsConfig.setDefaultSelectedProtocolVersion(ProtocolVersion.TLS10);
                        tlsConfig.setWorkflowTraceType(WorkflowTraceType.HELLO);
                        certificateMessage = CcaCertificateGenerator.generateCertificate(ccaDelegate, ccaCertificateType);
                        WorkflowTrace trace = CcaWorkflowGenerator.generateWorkflow(tlsConfig, ccaWorkflowType,
                                certificateMessage);
                        State state = new State(tlsConfig, trace);
                        try {
                            executeState(state);
                        } catch (Exception E) {
                            LOGGER.error("Error while testing for client authentication bypasses." + E);
                        }
                        if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.FINISHED, state.getWorkflowTrace())) {
                            bypassable = true;
                            resultList.add(new CcaTestResult(true, ccaWorkflowType, ccaCertificateType,
                                    protocolVersion, cipherSuite));
                        } else {
                            resultList.add(new CcaTestResult(false, ccaWorkflowType, ccaCertificateType,
                                    protocolVersion, cipherSuite));
                        }


        }
        return new CcaResult(bypassable ? TestResult.TRUE : TestResult.FALSE, resultList);
    }

    @Override
    public boolean canBeExecuted(SiteReport report) {
       return true;
    }

    @Override
    public void adjustConfig(SiteReport report) {
        //this.versionSuiteListPairsList.addAll(report.getVersionSuitePairs());
    }

    @Override
    public ProbeResult getCouldNotExecuteResult() {
        return new CcaResult(TestResult.COULD_NOT_TEST, null);
    }

}


/**
 * TODO: Note that when using a pem encoded certificate we still got the following results
 * check what this means.
 * Client authentication
 *
 * Supported			 : true
 * CRT_CKE_CCS_FIN.CLIENT_INPUT.TLS10.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CRT_CKE_CCS_FIN.CLIENT_INPUT.TLS11.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CRT_CKE_CCS_FIN.CLIENT_INPUT.TLS12.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CKE_CCS_FIN.CLIENT_INPUT.TLS10.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CKE_CCS_FIN.CLIENT_INPUT.TLS11.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CKE_CCS_FIN.CLIENT_INPUT.TLS12.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CKE_CCS_FIN.EMPTY.TLS10.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CKE_CCS_FIN.EMPTY.TLS11.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CKE_CCS_FIN.EMPTY.TLS12.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CKE_CCS_CRT_FIN_CCS_RND.CLIENT_INPUT.TLS10.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CKE_CCS_CRT_FIN_CCS_RND.CLIENT_INPUT.TLS11.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 * CKE_CCS_CRT_FIN_CCS_RND.CLIENT_INPUT.TLS12.TLS_DHE_RSA_WITH_AES_128_CBC_SHA : true
 */
