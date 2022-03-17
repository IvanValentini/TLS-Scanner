/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResults;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.report.result.ProbeResult;
import de.rub.nds.tlsscanner.serverscanner.report.result.SniResult;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SniProbe extends TlsProbe {

    public SniProbe(ScannerConfig scannerConfig, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, ProbeType.SNI, scannerConfig);
    }

    @Override
    public ProbeResult executeTest() {
        Config config = scannerConfig.createConfig();
        config.setAddRenegotiationInfoExtension(true);
        config.setAddServerNameIndicationExtension(false);
        config.setQuickReceive(true);
        config.setEarlyStop(true);
        config.setStopReceivingAfterFatal(true);
        config.setStopActionsAfterIOException(true);
        config.setStopActionsAfterFatal(true);
        List<CipherSuite> toTestList = new LinkedList<>();
        toTestList.addAll(Arrays.asList(CipherSuite.values()));
        toTestList.remove(CipherSuite.TLS_FALLBACK_SCSV);
        toTestList.remove(CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);
        config.setDefaultClientSupportedCipherSuites(toTestList);
        WorkflowTrace trace = new WorkflowConfigurationFactory(config)
            .createWorkflowTrace(WorkflowTraceType.DYNAMIC_HELLO, RunningModeType.CLIENT);
        State state = new State(config, trace);
        executeState(state);
        if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, trace)) {
            return new SniResult(TestResults.FALSE);
        }
        // Test if we can get a hello with SNI
        config.setAddServerNameIndicationExtension(true);
        trace = new WorkflowConfigurationFactory(config).createWorkflowTrace(WorkflowTraceType.DYNAMIC_HELLO,
            RunningModeType.CLIENT);
        state = new State(config, trace);
        executeState(state);
        if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, trace)) {
            return new SniResult(TestResults.TRUE);
        }
        // We cannot get a ServerHello from this Server...
        LOGGER.debug("SNI Test could not get a ServerHello message from the Server!");
        return new SniResult(TestResults.UNCERTAIN);
    }

    @Override
    public void adjustConfig(SiteReport report) {
    }

    @Override
    public ProbeResult getCouldNotExecuteResult() {
        return new SniResult(TestResults.COULD_NOT_TEST);
    }
}
