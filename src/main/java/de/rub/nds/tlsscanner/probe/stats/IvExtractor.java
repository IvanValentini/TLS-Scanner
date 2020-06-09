/**
 * TLS-Scanner - A TLS configuration and analysis tool based on TLS-Attacker.
 *
 * Copyright 2017-2019 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsscanner.probe.stats;

import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.crypto.ec.Point;
import de.rub.nds.tlsattacker.core.crypto.ec.PointFormatter;
import de.rub.nds.tlsattacker.core.protocol.message.ApplicationMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ECDHEServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ProtocolMessage;
import de.rub.nds.tlsattacker.core.record.AbstractRecord;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceivingAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class IvExtractor extends StatExtractor<ComparableByteArray> {

    private static final Logger LOGGER = LogManager.getLogger();

    public IvExtractor() {
        super(TrackableValueType.IV);
    }

    @Override
    public void extract(State state) {
        WorkflowTrace trace = state.getWorkflowTrace();

        // List<AbstractRecord> allReceivedRecords =
        // WorkflowTraceUtil.getAllReceivedRecords(trace);
        List<AbstractRecord> allReceivedRecords = new LinkedList<>();

        for (ReceivingAction action : trace.getReceivingActions()) {
            if (!(action.getReceivedMessages() == null)) {
                allReceivedRecords.addAll(action.getReceivedRecords());
            }
        }

        // No Ciphersuite selected == No Full Handshake
        if (state.getTlsContext().getSelectedCipherSuite() == null) {
            return;
        }

        // Currently only support for CBC Mode
        if (!state.getTlsContext().getSelectedCipherSuite().name().contains("CBC")) {
            return;
        }

        // Currently only checking for IV of length 32
        for (AbstractRecord message : allReceivedRecords) {

            if (message.getContentMessageType() == ProtocolMessageType.APPLICATION_DATA) {
                ModifiableByteArray extractedIv = ((Record) message).getComputations().getCbcInitialisationVector();
                put(new ComparableByteArray(extractedIv.getValue()));
            }

        }
    }
}
