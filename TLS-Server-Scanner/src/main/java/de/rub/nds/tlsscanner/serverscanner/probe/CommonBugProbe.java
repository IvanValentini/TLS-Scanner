/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe;

import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.AlpnProtocol;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.HelloVerifyRequestMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloDoneMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.EllipticCurvesExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.ExtendedMasterSecretExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.SignatureAndHashAlgorithmsExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.UnknownExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.preparator.ClientHelloPreparator;
import de.rub.nds.tlsattacker.core.protocol.serializer.ClientHelloSerializer;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.state.TlsContext;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveTillAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResults;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.selector.ConfigSelector;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CommonBugProbe extends TlsProbe {

    // does it handle unknown extensions correctly?
    private TestResult extensionIntolerance;
    // does it handle unknown cipher suites correctly?
    private TestResult cipherSuiteIntolerance;
    // does it handle long cipher suite length values correctly?
    private TestResult cipherSuiteLengthIntolerance512;
    // does it handle unknown compression algorithms correctly?
    private TestResult compressionIntolerance;
    // does it handle unknown versions correctly?
    private TestResult versionIntolerance;
    // does it handle unknown alpn strings correctly?
    private TestResult alpnIntolerance;
    // 256 - 511 <-- ch should be bigger than this?
    private TestResult clientHelloLengthIntolerance;
    // does it break on empty last extension?
    private TestResult emptyLastExtensionIntolerance;
    // is only the second byte of the cipher suite evaluated?
    private TestResult onlySecondCipherSuiteByteEvaluated;
    // does it handle unknown groups correctly?
    private TestResult namedGroupIntolerant;
    // does it handle signature and hash algorithms correctly?
    private TestResult namedSignatureAndHashAlgorithmIntolerance;
    // does it ignore the offered cipher suites?
    private TestResult ignoresCipherSuiteOffering;
    // does it reflect the offered cipher suites?
    private TestResult reflectsCipherSuiteOffering;
    // does it ignore the offered named groups?
    private TestResult ignoresOfferedNamedGroups;
    // does it ignore the sig hash algorithms?
    private TestResult ignoresOfferedSignatureAndHashAlgorithms;
    // server does not like really big client hello messages
    private TestResult maxLengthClientHelloIntolerant;
    // does it accept grease values in the supported groups extension?
    private TestResult greaseNamedGroupIntolerance;
    // does it accept grease values in the cipher suites list?
    private TestResult greaseCipherSuiteIntolerance;
    // does it accept grease values in the signature and hash algorithms extension?
    private TestResult greaseSignatureAndHashAlgorithmIntolerance;

    public CommonBugProbe(ScannerConfig config, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, ProbeType.COMMON_BUGS, config);
        super.properties.add(AnalyzedProperty.HAS_EXTENSION_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_CIPHER_SUITE_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_CIPHER_SUITE_LENGTH_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_COMPRESSION_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_VERSION_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_ALPN_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_CLIENT_HELLO_LENGTH_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_EMPTY_LAST_EXTENSION_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_SECOND_CIPHER_SUITE_BYTE_BUG);
        super.properties.add(AnalyzedProperty.HAS_NAMED_GROUP_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_SIG_HASH_ALGORITHM_INTOLERANCE);
        super.properties.add(AnalyzedProperty.IGNORES_OFFERED_CIPHER_SUITES);
        super.properties.add(AnalyzedProperty.REFLECTS_OFFERED_CIPHER_SUITES);
        super.properties.add(AnalyzedProperty.IGNORES_OFFERED_NAMED_GROUPS);
        super.properties.add(AnalyzedProperty.IGNORES_OFFERED_SIG_HASH_ALGOS);
        super.properties.add(AnalyzedProperty.HAS_BIG_CLIENT_HELLO_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_GREASE_NAMED_GROUP_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_GREASE_CIPHER_SUITE_INTOLERANCE);
        super.properties.add(AnalyzedProperty.HAS_GREASE_SIGNATURE_AND_HASH_ALGORITHM_INTOLERANCE);
    }

    @Override
    public void executeTest() {
        extensionIntolerance = hasExtensionIntolerance();
        cipherSuiteIntolerance = hasCipherSuiteIntolerance();
        cipherSuiteLengthIntolerance512 = hasCipherSuiteLengthIntolerance512();
        compressionIntolerance = hasCompressionIntolerance();
        versionIntolerance = hasVersionIntolerance();
        alpnIntolerance = hasAlpnIntolerance();
        clientHelloLengthIntolerance = hasClientHelloLengthIntolerance();
        emptyLastExtensionIntolerance = hasEmptyLastExtensionIntolerance();
        onlySecondCipherSuiteByteEvaluated = hasOnlySecondCipherSuiteByteEvaluatedBug();
        namedGroupIntolerant = hasNamedGroupIntolerance();
        namedSignatureAndHashAlgorithmIntolerance = hasSignatureAndHashAlgorithmIntolerance();
        adjustCipherSuiteSelectionBugs();
        ignoresOfferedNamedGroups = hasIgnoresNamedGroupsOfferingBug();
        ignoresOfferedSignatureAndHashAlgorithms = hasIgnoresSigHashAlgoOfferingBug();
        maxLengthClientHelloIntolerant = hasBigClientHelloIntolerance();
        greaseNamedGroupIntolerance = hasGreaseNamedGroupIntolerance();
        greaseCipherSuiteIntolerance = hasGreaseCipherSuiteIntolerance();
        greaseSignatureAndHashAlgorithmIntolerance = hasGreaseSignatureAndHashAlgorithmIntolerance();
    }

    private Config getWorkingConfig() {
        Config config = ConfigSelector.getNiceConfig(scannerConfig);
        config.setStopActionsAfterIOException(true);
        config.setStopReceivingAfterFatal(true);
        return config;
    }

    @Override
    public void adjustConfig(SiteReport report) {
    }

    private int getClientHelloLength(ClientHelloMessage message, Config config) {
        TlsContext context = new TlsContext(config);
        ClientHelloPreparator preparator = new ClientHelloPreparator(context.getChooser(), message);
        preparator.prepare();
        ClientHelloSerializer serializer =
            new ClientHelloSerializer(message, config.getDefaultHighestClientProtocolVersion());
        return serializer.serialize().length;
    }

    private WorkflowTrace getWorkflowTrace(Config config, ClientHelloMessage clientHello) {
        WorkflowConfigurationFactory factory = new WorkflowConfigurationFactory(config);
        WorkflowTrace trace = factory.createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
        trace.addTlsAction(new SendAction(clientHello));
        if (config.getHighestProtocolVersion().isDTLS() && config.isDtlsCookieExchange()) {
            trace.addTlsAction(new ReceiveAction(new HelloVerifyRequestMessage(config)));
            trace.addTlsAction(new SendAction(clientHello));
        }
        trace.addTlsAction(new ReceiveTillAction(new ServerHelloDoneMessage(config)));
        return trace;
    }

    private TestResult hasExtensionIntolerance() {
        try {
            Config config = getWorkingConfig();
            ClientHelloMessage message = new ClientHelloMessage(config);
            UnknownExtensionMessage extension = new UnknownExtensionMessage();
            extension.setTypeConfig(new byte[] { (byte) 3F, (byte) 3F });
            extension.setDataConfig(new byte[] { 00, 11, 22, 33 });
            message.getExtensions().add(extension);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasBigClientHelloIntolerance() {
        try {
            Config config = getWorkingConfig();
            config.setAddPaddingExtension(true);
            config.setDefaultPaddingExtensionBytes(new byte[14000]);
            ClientHelloMessage message = new ClientHelloMessage(config);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasIgnoresSigHashAlgoOfferingBug() {
        try {
            Config config = getWorkingConfig();
            config.setAddSignatureAndHashAlgorithmsExtension(false);
            List<CipherSuite> suiteList = new LinkedList<>();
            for (CipherSuite suite : CipherSuite.getImplemented()) {
                if (suite.isEphemeral()) {
                    suiteList.add(suite);
                }
            }
            config.setDefaultClientSupportedCipherSuites(suiteList);
            config.setAddECPointFormatExtension(true);
            config.setAddEllipticCurveExtension(true);
            ClientHelloMessage message = new ClientHelloMessage(config);
            SignatureAndHashAlgorithmsExtensionMessage extension = new SignatureAndHashAlgorithmsExtensionMessage();
            extension.setSignatureAndHashAlgorithms(Modifiable.explicit(new byte[] { (byte) 0xED, (byte) 0xED }));
            message.addExtension(extension);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.TRUE
                : TestResults.FALSE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasIgnoresNamedGroupsOfferingBug() {
        try {
            Config config = getWorkingConfig();
            config.setAddSignatureAndHashAlgorithmsExtension(true);
            List<CipherSuite> suiteList = new LinkedList<>();
            for (CipherSuite suite : CipherSuite.getImplemented()) {
                if (suite.isEphemeral() && suite.name().contains("EC")) {
                    suiteList.add(suite);
                }
            }
            config.setDefaultClientSupportedCipherSuites(suiteList);
            config.setAddECPointFormatExtension(true);
            config.setAddEllipticCurveExtension(false);
            ClientHelloMessage message = new ClientHelloMessage(config);
            EllipticCurvesExtensionMessage extension = new EllipticCurvesExtensionMessage();
            extension.setSupportedGroups(Modifiable.explicit(new byte[] { (byte) 0xED, (byte) 0xED }));
            message.addExtension(extension);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace)) {
                LOGGER.debug("Received a SH for invalid NamedGroup, server selected: "
                    + state.getTlsContext().getSelectedGroup().name());
                return TestResults.TRUE;
            }
            return TestResults.FALSE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private void adjustCipherSuiteSelectionBugs() {
        try {
            Config config = getWorkingConfig();
            ClientHelloMessage message = new ClientHelloMessage(config);
            message.setCipherSuites(Modifiable.explicit(new byte[] { (byte) 0xEE, (byte) 0xCC }));
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            boolean receivedShd = WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace);
            ServerHelloMessage serverHelloMessage = (ServerHelloMessage) WorkflowTraceUtil
                .getFirstReceivedMessage(HandshakeMessageType.SERVER_HELLO, trace);
            if (receivedShd) {
                if (Arrays.equals(serverHelloMessage.getSelectedCipherSuite().getValue(),
                    new byte[] { (byte) 0xEE, (byte) 0xCC })) {
                    reflectsCipherSuiteOffering = TestResults.TRUE;
                    ignoresCipherSuiteOffering = TestResults.FALSE;
                } else {
                    reflectsCipherSuiteOffering = TestResults.FALSE;
                    ignoresCipherSuiteOffering = TestResults.TRUE;
                }
            } else {
                reflectsCipherSuiteOffering = TestResults.FALSE;
                ignoresCipherSuiteOffering = TestResults.FALSE;
            }
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            reflectsCipherSuiteOffering = TestResults.ERROR_DURING_TEST;
            ignoresCipherSuiteOffering = TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasSignatureAndHashAlgorithmIntolerance() {
        try {
            Config config = getWorkingConfig();
            config.setAddSignatureAndHashAlgorithmsExtension(false);
            List<CipherSuite> suiteList = new LinkedList<>();
            for (CipherSuite suite : CipherSuite.getImplemented()) {
                if (suite.isEphemeral()) {
                    suiteList.add(suite);
                }
            }
            config.setDefaultClientSupportedCipherSuites(suiteList);
            config.setAddECPointFormatExtension(true);
            config.setAddEllipticCurveExtension(true);
            ClientHelloMessage message = new ClientHelloMessage(config);
            SignatureAndHashAlgorithmsExtensionMessage extension = new SignatureAndHashAlgorithmsExtensionMessage();
            extension.setSignatureAndHashAlgorithms(Modifiable.insert(new byte[] { (byte) 0xED, (byte) 0xED }, 0));
            message.addExtension(extension);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasNamedGroupIntolerance() {
        try {
            Config config = getWorkingConfig();
            config.setAddSignatureAndHashAlgorithmsExtension(true);
            List<CipherSuite> suiteList = new LinkedList<>();
            for (CipherSuite suite : CipherSuite.getImplemented()) {
                if (suite.isEphemeral() && suite.name().contains("EC")) {
                    suiteList.add(suite);
                }
            }
            config.setDefaultClientSupportedCipherSuites(suiteList);
            config.setAddECPointFormatExtension(true);
            config.setAddEllipticCurveExtension(false);
            ClientHelloMessage message = new ClientHelloMessage(config);
            EllipticCurvesExtensionMessage extension = new EllipticCurvesExtensionMessage();
            message.addExtension(extension);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            boolean receivedShd = WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace);
            if (receivedShd) {
                trace.reset();
                extension.setSupportedGroups(Modifiable.insert(new byte[] { (byte) 0xED, (byte) 0xED }, 0));
                state = new State(config, trace);
                executeState(state);
                receivedShd = WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace);
                return !receivedShd == true ? TestResults.TRUE : TestResults.FALSE;
            } else {
                return TestResults.FALSE;
            }
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasOnlySecondCipherSuiteByteEvaluatedBug() {
        try {
            Config config = getWorkingConfig();
            ClientHelloMessage message = new ClientHelloMessage(config);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            for (CipherSuite suite : CipherSuite.values()) {
                if (suite.getByteValue()[0] == 0x00) {
                    try {
                        stream.write(new byte[] { (byte) 0xDF, suite.getByteValue()[1] });
                    } catch (IOException ex) {
                        LOGGER.debug(ex);
                    }
                }
            }
            message.setCipherSuites(Modifiable.explicit(stream.toByteArray()));
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            boolean receivedShd = WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace);
            return receivedShd ? TestResults.TRUE : TestResults.FALSE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasEmptyLastExtensionIntolerance() {
        try {
            Config config = getWorkingConfig();
            ClientHelloMessage message = new ClientHelloMessage(config);
            ExtendedMasterSecretExtensionMessage extension = new ExtendedMasterSecretExtensionMessage();
            message.getExtensions().add(extension);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasVersionIntolerance() {
        try {
            Config config = getWorkingConfig();
            ClientHelloMessage message = new ClientHelloMessage(config);
            message.setProtocolVersion(Modifiable.explicit(new byte[] { 0x03, 0x05 }));
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasCompressionIntolerance() {
        try {
            Config config = getWorkingConfig();
            ClientHelloMessage message = new ClientHelloMessage(config);
            message.setCompressions(new byte[] { (byte) 0xFF, (byte) 0x00 });
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasCipherSuiteLengthIntolerance512() {
        try {
            Config config = getWorkingConfig();
            List<CipherSuite> toTestList = new LinkedList<>();
            toTestList.addAll(Arrays.asList(CipherSuite.values()));
            toTestList.remove(CipherSuite.TLS_FALLBACK_SCSV);
            toTestList.remove(CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);
            config.setDefaultClientSupportedCipherSuites(toTestList);
            ClientHelloMessage message = new ClientHelloMessage(config);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasCipherSuiteIntolerance() {
        try {
            Config config = getWorkingConfig();
            ClientHelloMessage message = new ClientHelloMessage(config);
            message.setCipherSuites(Modifiable.insert(new byte[] { (byte) 0xCF, (byte) 0xAA }, 1));
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasAlpnIntolerance() {
        try {
            Config config = getWorkingConfig();
            config.setAddAlpnExtension(true);
            List<String> alpnProtocols = new LinkedList<>();
            for (AlpnProtocol protocol : AlpnProtocol.values()) {
                alpnProtocols.add(protocol.getConstant());
            }
            alpnProtocols.add("This is not an ALPN Protocol");
            config.setDefaultProposedAlpnProtocols(alpnProtocols);
            ClientHelloMessage message = new ClientHelloMessage(config);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasClientHelloLengthIntolerance() {
        try {
            Config config = ConfigSelector.getNiceConfig(scannerConfig);
            config.setAddAlpnExtension(true);
            config.setAddPaddingExtension(true);
            ClientHelloMessage message = new ClientHelloMessage(config);
            int newLength = 512 - 4 - getClientHelloLength(message, config);
            if (newLength > 0) {
                config.setDefaultPaddingExtensionBytes(new byte[newLength]);
            } else {
                // TODO this is currently not working as intended
            }
            message = new ClientHelloMessage(config);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    private TestResult hasGreaseCipherSuiteIntolerance() {
        Config config = getWorkingConfig();
        Arrays.asList(CipherSuite.values()).stream().filter(cipherSuite -> cipherSuite.isGrease())
            .forEach(greaseCipher -> config.getDefaultClientSupportedCipherSuites().add(greaseCipher));
        return hasGreaseIntolerance(config);
    }

    private TestResult hasGreaseNamedGroupIntolerance() {
        Config config = getWorkingConfig();
        Arrays.asList(NamedGroup.values()).stream().filter(group -> group.isGrease())
            .forEach(greaseGroup -> config.getDefaultClientNamedGroups().add(greaseGroup));
        return hasGreaseIntolerance(config);
    }

    private TestResult hasGreaseSignatureAndHashAlgorithmIntolerance() {
        Config config = getWorkingConfig();
        Arrays.asList(SignatureAndHashAlgorithm.values()).stream().filter(algorithm -> algorithm.isGrease()).forEach(
            greaseAlgorithm -> config.getDefaultClientSupportedSignatureAndHashAlgorithms().add(greaseAlgorithm));
        return hasGreaseIntolerance(config);
    }

    private TestResult hasGreaseIntolerance(Config config) {
        try {
            ClientHelloMessage message = new ClientHelloMessage(config);
            WorkflowTrace trace = getWorkflowTrace(config, message);
            State state = new State(config, trace);
            executeState(state);
            return WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO_DONE, trace) ? TestResults.FALSE
                : TestResults.TRUE;
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
                throw new RuntimeException(e);
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            return TestResults.ERROR_DURING_TEST;
        }
    }

    @Override
    public void getCouldNotExecuteResult() {
    	this.extensionIntolerance =  this.cipherSuiteIntolerance = this.cipherSuiteLengthIntolerance512 
    			= this.compressionIntolerance = this.versionIntolerance = this.alpnIntolerance 
    			= this.clientHelloLengthIntolerance = this.emptyLastExtensionIntolerance 
    			= this.onlySecondCipherSuiteByteEvaluated = this.namedGroupIntolerant 
    			= this.namedSignatureAndHashAlgorithmIntolerance = this.ignoresCipherSuiteOffering 
    			= this.reflectsCipherSuiteOffering = this.ignoresOfferedNamedGroups 
    			= this.ignoresOfferedSignatureAndHashAlgorithms = this.maxLengthClientHelloIntolerant 
    			= this.greaseNamedGroupIntolerance = this.greaseCipherSuiteIntolerance 
    			= this.greaseSignatureAndHashAlgorithmIntolerance = TestResults.COULD_NOT_TEST;
    }

	@Override
	protected void mergeData(SiteReport report) {
		super.setPropertyReportValue(AnalyzedProperty.HAS_EXTENSION_INTOLERANCE, this.extensionIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.HAS_CIPHER_SUITE_INTOLERANCE, this.cipherSuiteIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.HAS_CIPHER_SUITE_LENGTH_INTOLERANCE, this.cipherSuiteLengthIntolerance512);
        super.setPropertyReportValue(AnalyzedProperty.HAS_COMPRESSION_INTOLERANCE, this.compressionIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.HAS_VERSION_INTOLERANCE, this.versionIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.HAS_ALPN_INTOLERANCE, this.alpnIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.HAS_CLIENT_HELLO_LENGTH_INTOLERANCE, this.clientHelloLengthIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.HAS_EMPTY_LAST_EXTENSION_INTOLERANCE, this.emptyLastExtensionIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.HAS_SECOND_CIPHER_SUITE_BYTE_BUG, this.onlySecondCipherSuiteByteEvaluated);
        super.setPropertyReportValue(AnalyzedProperty.HAS_NAMED_GROUP_INTOLERANCE, this.namedGroupIntolerant);
        super.setPropertyReportValue(AnalyzedProperty.HAS_SIG_HASH_ALGORITHM_INTOLERANCE,
        		this.namedSignatureAndHashAlgorithmIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.IGNORES_OFFERED_CIPHER_SUITES, this.ignoresCipherSuiteOffering);
        super.setPropertyReportValue(AnalyzedProperty.REFLECTS_OFFERED_CIPHER_SUITES, this.reflectsCipherSuiteOffering);
        super.setPropertyReportValue(AnalyzedProperty.IGNORES_OFFERED_NAMED_GROUPS, this.ignoresOfferedNamedGroups);
        super.setPropertyReportValue(AnalyzedProperty.IGNORES_OFFERED_SIG_HASH_ALGOS, this.ignoresOfferedSignatureAndHashAlgorithms);
        super.setPropertyReportValue(AnalyzedProperty.HAS_BIG_CLIENT_HELLO_INTOLERANCE, this.maxLengthClientHelloIntolerant);
        super.setPropertyReportValue(AnalyzedProperty.HAS_GREASE_NAMED_GROUP_INTOLERANCE, this.greaseNamedGroupIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.HAS_GREASE_CIPHER_SUITE_INTOLERANCE, this.greaseCipherSuiteIntolerance);
        super.setPropertyReportValue(AnalyzedProperty.HAS_GREASE_SIGNATURE_AND_HASH_ALGORITHM_INTOLERANCE,
        		this.greaseSignatureAndHashAlgorithmIntolerance);		
	}
}
