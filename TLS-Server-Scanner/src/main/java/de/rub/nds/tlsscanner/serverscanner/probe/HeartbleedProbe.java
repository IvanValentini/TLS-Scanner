/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe;

import de.rub.nds.tlsattacker.attacks.config.HeartbleedCommandConfig;
import de.rub.nds.tlsattacker.attacks.impl.HeartbleedAttacker;
import de.rub.nds.tlsattacker.core.config.delegate.CipherSuiteDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.ClientDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.ProtocolVersionDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.StarttlsDelegate;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResults;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.requirements.ProbeRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HeartbleedProbe extends TlsProbe {

    private List<CipherSuite> supportedCiphers;
    private TestResult vulnerable;

    public HeartbleedProbe(ScannerConfig config, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, ProbeType.HEARTBLEED, config);
        super.properties.add(AnalyzedProperty.VULNERABLE_TO_HEARTBLEED);
    }

    @Override
    public void executeTest() {
        HeartbleedCommandConfig heartbleedConfig = new HeartbleedCommandConfig(getScannerConfig().getGeneralDelegate());
        ClientDelegate delegate = (ClientDelegate) heartbleedConfig.getDelegate(ClientDelegate.class);
        delegate.setHost(getScannerConfig().getClientDelegate().getHost());
        delegate.setSniHostname(getScannerConfig().getClientDelegate().getSniHostname());
        if (getScannerConfig().getDtlsDelegate().isDTLS()) {
            ProtocolVersionDelegate protocolVersionDelegate =
                (ProtocolVersionDelegate) heartbleedConfig.getDelegate(ProtocolVersionDelegate.class);
            protocolVersionDelegate.setProtocolVersion(ProtocolVersion.DTLS12);
        }
        StarttlsDelegate starttlsDelegate = (StarttlsDelegate) heartbleedConfig.getDelegate(StarttlsDelegate.class);
        starttlsDelegate.setStarttlsType(getScannerConfig().getStarttlsDelegate().getStarttlsType());
        if (supportedCiphers != null) {
            CipherSuiteDelegate cipherSuiteDelegate =
                (CipherSuiteDelegate) heartbleedConfig.getDelegate(CipherSuiteDelegate.class);
            cipherSuiteDelegate.setCipherSuites(supportedCiphers);
        }
        HeartbleedAttacker attacker = new HeartbleedAttacker(heartbleedConfig, heartbleedConfig.createConfig());
        this.vulnerable = Objects.equals(attacker.isVulnerable(), Boolean.TRUE) ? TestResults.TRUE : TestResults.FALSE;
    }

    @Override
    protected ProbeRequirement getRequirements(SiteReport report) {
        return new ProbeRequirement(report).requireProbeTypes(ProbeType.EXTENSIONS).requireExtensionTyes(ExtensionType.HEARTBEAT);
    }

    @Override
    public void adjustConfig(SiteReport report) {
        if (report.getCipherSuites() != null && !report.getCipherSuites().isEmpty()) {
            supportedCiphers = new ArrayList<>(report.getCipherSuites());
        } else {
            supportedCiphers = CipherSuite.getImplemented();
        }
    }

    @Override
    public TlsProbe getCouldNotExecuteResult() {
        this.vulnerable = TestResults.COULD_NOT_TEST;
        return this;
    }

	@Override
	protected void mergeData(SiteReport report) {
        super.setPropertyReportValue(AnalyzedProperty.VULNERABLE_TO_HEARTBLEED, vulnerable);		
	}
}
