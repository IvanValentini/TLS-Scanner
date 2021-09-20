/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe.result;

import de.rub.nds.scanner.core.probe.result.ProbeResult;
import de.rub.nds.tlsscanner.core.constants.TlsProbeType;
import de.rub.nds.tlsscanner.serverscanner.leak.PaddingOracleTestInfo;
import de.rub.nds.scanner.core.constants.TestResult;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.vectorstatistics.InformationLeakTest;
import java.util.LinkedList;
import de.rub.nds.scanner.core.vectorstatistics.InformationLeakTest;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Robert Merget {@literal <robert.merget@rub.de>}
 */
public class PaddingOracleResult extends ProbeResult<SiteReport> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final List<InformationLeakTest<PaddingOracleTestInfo>> resultList;

    private TestResult vulnerable;

    public PaddingOracleResult(TestResult result) {
        super(ProbeType.PADDING_ORACLE);
        this.vulnerable = result;
        resultList = new LinkedList<>();
    }

    public PaddingOracleResult(List<InformationLeakTest<PaddingOracleTestInfo>> resultList) {
        super(TlsProbeType.PADDING_ORACLE);
        this.resultList = resultList;
        if (this.resultList != null) {
            vulnerable = TestResult.FALSE;
            for (InformationLeakTest informationLeakTest : resultList) {
                if (informationLeakTest.isSignificantDistinctAnswers()) {
                    vulnerable = TestResult.TRUE;
                }
            }
        } else {
            vulnerable = TestResult.ERROR_DURING_TEST;
        }
    }

    @Override
    public void mergeData(SiteReport report) {
        report.setPaddingOracleTestResultList(resultList);
        report.putResult(TlsAnalyzedProperty.VULNERABLE_TO_PADDING_ORACLE, vulnerable);
    }

    public List<InformationLeakTest<PaddingOracleTestInfo>> getResultList() {
        return resultList;
    }

}
