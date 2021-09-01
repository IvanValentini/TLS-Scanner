/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2021 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.guideline;

import de.rub.nds.tlsscanner.serverscanner.guideline.results.CertificateGuidelineCheckResult;
import de.rub.nds.tlsscanner.serverscanner.probe.certificate.CertificateChain;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;

import java.util.Objects;

public abstract class CertificateGuidelineCheck extends GuidelineCheck {

    /**
     * <code>true</code> if only one certificate has to pass the check. Otherwise all certificates have to pass.
     */
    private boolean onlyOneCertificate;

    public CertificateGuidelineCheck(String name, RequirementLevel requirementLevel) {
        this(name, requirementLevel, false);
    }

    public CertificateGuidelineCheck(String name, RequirementLevel requirementLevel, boolean onlyOneCertificate) {
        super(name, requirementLevel);
        this.onlyOneCertificate = onlyOneCertificate;
    }

    public CertificateGuidelineCheck(String name, RequirementLevel requirementLevel, GuidelineCheckCondition condition,
        boolean onlyOneCertificate) {
        super(name, requirementLevel, condition);
        this.onlyOneCertificate = onlyOneCertificate;
    }

    @Override
    public GuidelineCheckResult evaluate(SiteReport report) {
        boolean passFlag = false;
        boolean failFlag = false;
        boolean uncertainFlag = false;
        CertificateGuidelineCheckResult result = new CertificateGuidelineCheckResult();
        for (int i = 0; i < report.getCertificateChainList().size(); i++) {
            CertificateChain chain = report.getCertificateChainList().get(i);
            GuidelineCheckResult currentResult = this.evaluateChain(chain);
            result.addResult(currentResult);
            if (Objects.equals(TestResult.TRUE, currentResult.getResult())) {
                passFlag = true;
            } else if (Objects.equals(TestResult.FALSE, currentResult.getResult())) {
                failFlag = true;
            } else {
                uncertainFlag = true;
            }
        }
        if (this.onlyOneCertificate && passFlag) {
            result.setResult(TestResult.TRUE);
        } else if (passFlag && !uncertainFlag && !failFlag) {
            result.setResult(TestResult.TRUE);
        } else if (failFlag) {
            result.setResult(TestResult.FALSE);
        } else {
            result.setResult(TestResult.UNCERTAIN);
        }
        return result;
    }

    public abstract GuidelineCheckResult evaluateChain(CertificateChain chain);

    public boolean isOnlyOneCertificate() {
        return onlyOneCertificate;
    }

    public void setOnlyOneCertificate(boolean onlyOneCertificate) {
        this.onlyOneCertificate = onlyOneCertificate;
    }
}
