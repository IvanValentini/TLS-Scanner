/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner;

import de.rub.nds.tlsattacker.attacks.connectivity.ConnectivityChecker;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.StarttlsType;
import de.rub.nds.tlsattacker.core.workflow.NamedThreadFactory;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.probe.*;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.report.after.AfterProbe;
import de.rub.nds.tlsscanner.serverscanner.report.after.DhValueAfterProbe;
import de.rub.nds.tlsscanner.serverscanner.report.after.EcPublicKeyAfterProbe;
import de.rub.nds.tlsscanner.serverscanner.report.after.EvaluateRandomnessAfterProbe;
import de.rub.nds.tlsscanner.serverscanner.report.after.FreakAfterProbe;
import de.rub.nds.tlsscanner.serverscanner.report.after.LogjamAfterProbe;
import de.rub.nds.tlsscanner.serverscanner.report.after.PaddingOracleIdentificationAfterProbe;
import de.rub.nds.tlsscanner.serverscanner.report.after.PoodleAfterProbe;
import de.rub.nds.tlsscanner.serverscanner.report.after.RaccoonAttackAfterProbe;
import de.rub.nds.tlsscanner.serverscanner.report.after.Sweet32AfterProbe;
import de.rub.nds.tlsscanner.serverscanner.trust.TrustAnchorManager;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Robert Merget - {@literal <robert.merget@rub.de>}
 */
public class TlsScanner {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ParallelExecutor parallelExecutor;
    private final ScannerConfig config;
    private boolean closeAfterFinishParallel;
    private final List<TlsProbe> probeList;
    private final List<AfterProbe> afterList;
    private final List<ProbeType> probesToExecute;

    public TlsScanner(ScannerConfig config) {

        this.config = config;
        closeAfterFinishParallel = true;
        parallelExecutor = new ParallelExecutor(config.getOverallThreads(), 3,
            new NamedThreadFactory(config.getClientDelegate().getHost() + "-Worker"));
        this.probeList = new LinkedList<>();
        this.afterList = new LinkedList<>();
        this.probesToExecute = config.getProbes();
        fillDefaultProbeLists(config.getVulns());
    }

    public TlsScanner(ScannerConfig config, ParallelExecutor parallelExecutor) {
        this.config = config;
        this.parallelExecutor = parallelExecutor;
        closeAfterFinishParallel = true;
        this.probeList = new LinkedList<>();
        this.afterList = new LinkedList<>();
        this.probesToExecute = config.getProbes();
        fillDefaultProbeLists(config.getVulns());
    }

    public TlsScanner(ScannerConfig config, ParallelExecutor parallelExecutor, List<TlsProbe> probeList,
        List<AfterProbe> afterList) {
        this.parallelExecutor = parallelExecutor;
        this.config = config;
        this.probeList = probeList;
        this.afterList = afterList;
        this.probesToExecute = config.getProbes();
        closeAfterFinishParallel = true;
    }

    private void fillDefaultProbeLists(String vulns) {
        if (vulns == "") {
            addProbeToProbeList(new CommonBugProbe(config, parallelExecutor));
            addProbeToProbeList(new SniProbe(config, parallelExecutor));
            addProbeToProbeList(new CompressionsProbe(config, parallelExecutor));
            addProbeToProbeList(new NamedCurvesProbe(config, parallelExecutor));
            addProbeToProbeList(new AlpnProbe(config, parallelExecutor));
            addProbeToProbeList(new AlpacaProbe(config, parallelExecutor));
            addProbeToProbeList(new CertificateProbe(config, parallelExecutor));
            addProbeToProbeList(new OcspProbe(config, parallelExecutor));
            addProbeToProbeList(new ProtocolVersionProbe(config, parallelExecutor));
            addProbeToProbeList(new CipherSuiteProbe(config, parallelExecutor));
            addProbeToProbeList(new DirectRaccoonProbe(config, parallelExecutor));
            addProbeToProbeList(new CipherSuiteOrderProbe(config, parallelExecutor));
            addProbeToProbeList(new ExtensionProbe(config, parallelExecutor));
            addProbeToProbeList(new TokenbindingProbe(config, parallelExecutor));
            addProbeToProbeList(new HttpHeaderProbe(config, parallelExecutor));
            addProbeToProbeList(new HttpFalseStartProbe(config, parallelExecutor));
            addProbeToProbeList(new ECPointFormatProbe(config, parallelExecutor));
            addProbeToProbeList(new ResumptionProbe(config, parallelExecutor));
            addProbeToProbeList(new RenegotiationProbe(config, parallelExecutor));
            addProbeToProbeList(new SessionTicketZeroKeyProbe(config, parallelExecutor));
            addProbeToProbeList(new HeartbleedProbe(config, parallelExecutor));
            addProbeToProbeList(new PaddingOracleProbe(config, parallelExecutor));
            addProbeToProbeList(new BleichenbacherProbe(config, parallelExecutor));
            addProbeToProbeList(new TlsPoodleProbe(config, parallelExecutor));
            addProbeToProbeList(new InvalidCurveProbe(config, parallelExecutor));
            addProbeToProbeList(new DrownProbe(config, parallelExecutor));
            addProbeToProbeList(new EarlyCcsProbe(config, parallelExecutor));
            // addProbeToProbeList(new MacProbe(config, parallelExecutor));
            addProbeToProbeList(new CcaSupportProbe(config, parallelExecutor));
            addProbeToProbeList(new CcaRequiredProbe(config, parallelExecutor));
            addProbeToProbeList(new CcaProbe(config, parallelExecutor));
            addProbeToProbeList(new EsniProbe(config, parallelExecutor));
            addProbeToProbeList(new CertificateTransparencyProbe(config, parallelExecutor));
            addProbeToProbeList(new RecordFragmentationProbe(config, parallelExecutor));
            addProbeToProbeList(new HelloRetryProbe(config, parallelExecutor));
            afterList.add(new Sweet32AfterProbe());
            afterList.add(new PoodleAfterProbe());
            afterList.add(new FreakAfterProbe());
            afterList.add(new LogjamAfterProbe());
            afterList.add(new EvaluateRandomnessAfterProbe());
            afterList.add(new EcPublicKeyAfterProbe());
            afterList.add(new DhValueAfterProbe());
            afterList.add(new PaddingOracleIdentificationAfterProbe());
            afterList.add(new RaccoonAttackAfterProbe());
        } else {
            String[] vulnsList = vulns.split(",");
            for (String v : vulnsList) {
                // System.out.println(v);
                switch (v) {
                    case "CommonBugProbe":
                        addProbeToProbeList(new CommonBugProbe(config, parallelExecutor));
                        break;
                    case "SniProbe":
                        addProbeToProbeList(new SniProbe(config, parallelExecutor));
                        break;
                    case "CompressionsProbe":
                        addProbeToProbeList(new CompressionsProbe(config, parallelExecutor));
                        break;
                    case "NamedCurvesProbe":
                        addProbeToProbeList(new NamedCurvesProbe(config, parallelExecutor));
                        break;
                    case "AlpnProbe":
                        addProbeToProbeList(new AlpnProbe(config, parallelExecutor));
                        break;
                    case "AlpacaProbe":
                        addProbeToProbeList(new AlpacaProbe(config, parallelExecutor));
                        break;
                    case "CertificateProbe":
                        addProbeToProbeList(new CertificateProbe(config, parallelExecutor));
                        break;
                    case "OcspProbe":
                        addProbeToProbeList(new OcspProbe(config, parallelExecutor));
                        break;
                    case "ProtocolVersionProbe":
                        addProbeToProbeList(new ProtocolVersionProbe(config, parallelExecutor));
                        break;
                    case "CipherSuiteProbe":
                        addProbeToProbeList(new CipherSuiteProbe(config, parallelExecutor));
                        break;
                    case "DirectRaccoonProbe":
                        addProbeToProbeList(new DirectRaccoonProbe(config, parallelExecutor));
                        break;
                    case "CipherSuiteOrderProbe":
                        addProbeToProbeList(new CipherSuiteOrderProbe(config, parallelExecutor));
                        break;
                    case "ExtensionProbe":
                        addProbeToProbeList(new ExtensionProbe(config, parallelExecutor));
                        break;
                    case "TokenbindingProbe":
                        addProbeToProbeList(new TokenbindingProbe(config, parallelExecutor));
                        break;
                    case "HttpHeaderProbe":
                        addProbeToProbeList(new HttpHeaderProbe(config, parallelExecutor));
                        break;
                    case "HttpFalseStartProbe":
                        addProbeToProbeList(new HttpFalseStartProbe(config, parallelExecutor));
                        break;
                    case "ECPointFormatProbe":
                        addProbeToProbeList(new ECPointFormatProbe(config, parallelExecutor));
                        break;
                    case "ResumptionProbe":
                        addProbeToProbeList(new ResumptionProbe(config, parallelExecutor));
                        break;
                    case "RenegotiationProbe":
                        addProbeToProbeList(new RenegotiationProbe(config, parallelExecutor));
                        break;
                    case "SessionTicketZeroKeyProbe":
                        addProbeToProbeList(new SessionTicketZeroKeyProbe(config, parallelExecutor));
                        break;
                    case "HeartbleedProbe":
                        addProbeToProbeList(new HeartbleedProbe(config, parallelExecutor));
                        break;
                    case "PaddingOracleProbe":
                        addProbeToProbeList(new PaddingOracleProbe(config, parallelExecutor));
                        break;
                    case "BleichenbacherProbe":
                        addProbeToProbeList(new BleichenbacherProbe(config, parallelExecutor));
                        break;
                    case "TlsPoodleProbe":
                        addProbeToProbeList(new TlsPoodleProbe(config, parallelExecutor));
                        break;
                    case "InvalidCurveProbe":
                        addProbeToProbeList(new InvalidCurveProbe(config, parallelExecutor));
                        break;
                    case "DrownProbe":
                        addProbeToProbeList(new DrownProbe(config, parallelExecutor));
                        break;
                    case "EarlyCcsProbe":
                        addProbeToProbeList(new EarlyCcsProbe(config, parallelExecutor));
                        break;
                    case "MacProbe":
                        addProbeToProbeList(new MacProbe(config, parallelExecutor));
                        break;
                    case "CcaSupportProbe":
                        addProbeToProbeList(new CcaSupportProbe(config, parallelExecutor));
                        break;
                    case "CcaRequiredProbe":
                        addProbeToProbeList(new CcaRequiredProbe(config, parallelExecutor));
                        break;
                    case "CcaProbe":
                        addProbeToProbeList(new CcaProbe(config, parallelExecutor));
                        break;
                    case "EsniProbe":
                        addProbeToProbeList(new EsniProbe(config, parallelExecutor));
                        break;
                    case "CertificateTransparencyProbe":
                        addProbeToProbeList(new CertificateTransparencyProbe(config, parallelExecutor));
                        break;
                    case "RecordFragmentationProbe":
                        addProbeToProbeList(new RecordFragmentationProbe(config, parallelExecutor));
                        break;
                    case "HelloRetryProbe":
                        addProbeToProbeList(new HelloRetryProbe(config, parallelExecutor));
                        break;
                    case "Sweet32AfterProbe":
                        afterList.add(new Sweet32AfterProbe());
                        break;
                    case "PoodleAfterProbe":
                        afterList.add(new PoodleAfterProbe());
                        break;
                    case "FreakAfterProbe":
                        afterList.add(new FreakAfterProbe());
                        break;
                    case "LogjamAfterProbe":
                        afterList.add(new LogjamAfterProbe());
                        break;
                    case "EvaluateRandomnessAfterProbe":
                        afterList.add(new EvaluateRandomnessAfterProbe());
                        break;
                    case "EcPublicKeyAfterProbe":
                        afterList.add(new EcPublicKeyAfterProbe());
                        break;
                    case "DhValueAfterProbe":
                        afterList.add(new DhValueAfterProbe());
                        break;
                    case "PaddingOracleIdentificationAfterProbe":
                        afterList.add(new PaddingOracleIdentificationAfterProbe());
                        break;
                    case "RaccoonAttackAfterProbe":
                        afterList.add(new RaccoonAttackAfterProbe());
                        break;
                    default:
                        LOGGER.warn("Unkown vuln type: " + v);
                }
            }
        }
    }

    private void addProbeToProbeList(TlsProbe probe) {
        if (probesToExecute == null || probesToExecute.contains(probe.getType())) {
            probeList.add(probe);
        }
    }

    public SiteReport scan() {
        LOGGER.debug("Initializing TrustAnchorManager");
        TrustAnchorManager.getInstance();
        LOGGER.debug("Finished TrustAnchorManager initialization");

        boolean isConnectable = false;
        ThreadedScanJobExecutor executor = null;
        try {
            if (isConnectable()) {
                LOGGER.debug(config.getClientDelegate().getHost() + " is connectable");
                if ((config.getStarttlsDelegate().getStarttlsType() == StarttlsType.NONE && speaksTls())
                    || (config.getStarttlsDelegate().getStarttlsType() != StarttlsType.NONE && speaksStartTls())) {
                    LOGGER.debug(config.getClientDelegate().getHost() + " is connectable");
                    ScanJob job = new ScanJob(probeList, afterList);
                    executor = new ThreadedScanJobExecutor(config, job, config.getParallelProbes(),
                        config.getClientDelegate().getHost());
                    SiteReport report = executor.execute();
                    return report;
                } else {
                    isConnectable = true;
                }
            }
            SiteReport report = new SiteReport(config.getClientDelegate().getHost());
            report.setServerIsAlive(isConnectable);
            report.setSupportsSslTls(false);
            return report;
        } finally {
            if (executor != null) {
                executor.shutdown();
            }
            closeParallelExecutorIfNeeded();
        }
    }

    private void closeParallelExecutorIfNeeded() {

        if (closeAfterFinishParallel) {
            parallelExecutor.shutdown();
        }
    }

    public boolean isConnectable() {
        try {
            Config tlsConfig = config.createConfig();
            ConnectivityChecker checker = new ConnectivityChecker(tlsConfig.getDefaultClientConnection());
            return checker.isConnectable();
        } catch (Exception e) {
            LOGGER.warn("Could not test if we can connect to the server", e);
            return false;
        }
    }

    private boolean speaksTls() {
        try {
            Config tlsConfig = config.createConfig();
            ConnectivityChecker checker = new ConnectivityChecker(tlsConfig.getDefaultClientConnection());
            return checker.speaksTls(tlsConfig);
        } catch (Exception e) {
            LOGGER.warn("Could not test if the server speaks TLS. Probably could not connect.");
            LOGGER.debug(e);
            return false;
        }
    }

    private boolean speaksStartTls() {
        Config tlsConfig = config.createConfig();
        ConnectivityChecker checker = new ConnectivityChecker(tlsConfig.getDefaultClientConnection());
        return checker.speaksStartTls(tlsConfig);
    }

    public void setCloseAfterFinishParallel(boolean closeAfterFinishParallel) {
        this.closeAfterFinishParallel = closeAfterFinishParallel;
    }

    public boolean isCloseAfterFinishParallel() {
        return closeAfterFinishParallel;
    }
}
