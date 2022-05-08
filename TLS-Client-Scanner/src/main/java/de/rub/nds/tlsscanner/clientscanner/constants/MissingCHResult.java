/**
 * TLS-Client-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.clientscanner.constants;

import de.rub.nds.scanner.core.constants.TestResult;

public class MissingCHResult implements TestResult {

    @Override
    public String name() {
        return "Could not test due to missing Client Hello!";
    }
}
