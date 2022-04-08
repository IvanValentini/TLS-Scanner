/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.guideline;

import de.rub.nds.scanner.core.constants.TestResult;
import de.rub.nds.tlsscanner.core.guideline.GuidelineCheckResult;
import de.rub.nds.tlsscanner.serverscanner.guideline.checks.CertificateAgilityGuidelineCheck;
<<<<<<< HEAD
import de.rub.nds.tlsscanner.serverscanner.rating.TestResults;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
=======
import de.rub.nds.tlsscanner.serverscanner.report.ServerReport;
>>>>>>> fixing_imports_and_packages
import org.junit.Assert;
import org.junit.Test;

public class CertificateAgilityGuidelineCheckTest {

    @Test
    public void testNegative() {
        ServerReport report = new ServerReport("test", 443);

        CertificateAgilityGuidelineCheck check = new CertificateAgilityGuidelineCheck(null, null);

        GuidelineCheckResult result = check.evaluate(report);

        Assert.assertEquals(TestResults.FALSE, result.getResult());
    }
}
