/**
 * TLS-Client-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.clientscanner.probe;

import de.rub.nds.scanner.core.constants.TestResult;
import de.rub.nds.scanner.core.constants.TestResults;
import de.rub.nds.scanner.core.probe.requirements.Requirement;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CompressionMethod;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.protocol.message.FinishedMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveTillAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.clientscanner.config.ClientScannerConfig;
import de.rub.nds.tlsscanner.clientscanner.probe.requirements.ProbeRequirement;
import de.rub.nds.tlsscanner.clientscanner.report.ClientReport;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlsscanner.core.constants.TlsProbeType;
import de.rub.nds.tlsscanner.core.probe.TlsProbe;

public class ForcedCompressionProbe extends TlsProbe<ClientScannerConfig, ClientReport> {

    private TestResult result;

    public ForcedCompressionProbe(ParallelExecutor executor, ClientScannerConfig scannerConfig) {
        super(executor, TlsProbeType.FORCED_COMPRESSION, scannerConfig);
        super.register(TlsAnalyzedProperty.FORCED_COMPRESSION);
    }

    @Override
    public void executeTest() {
        Config config = getScannerConfig().createConfig();
        config.setEnforceSettings(true);
        config.setDefaultServerSupportedCompressionMethods(CompressionMethod.DEFLATE, CompressionMethod.LZS);
        config.setDefaultSelectedCompressionMethod(CompressionMethod.DEFLATE);
        WorkflowConfigurationFactory factory = new WorkflowConfigurationFactory(config);
        WorkflowTrace workflowTrace = factory.createWorkflowTrace(WorkflowTraceType.HELLO, RunningModeType.SERVER);
        workflowTrace.addTlsAction(new ReceiveTillAction(new FinishedMessage()));
        State state = new State(config, workflowTrace);
        executeState(state);
        result = state.getWorkflowTrace().executedAsPlanned() ? TestResults.TRUE : TestResults.FALSE;
    }

    @Override
    public void adjustConfig(ClientReport report) {
    }

    @Override
    protected Requirement getRequirements(ClientReport report) {
        return ProbeRequirement.NO_REQUIREMENT;
    }

    @Override
    protected void mergeData(ClientReport report) {
        super.put(TlsAnalyzedProperty.FORCED_COMPRESSION, result);
    }
}
