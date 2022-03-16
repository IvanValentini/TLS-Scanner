/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.rating;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;


@XmlRootElement
@XmlSeeAlso({TestResults.class})
//@XmlType(propOrder = { "result", "shortDescription", "handlingRecommendation", "detailedDescription" })
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyResultRecommendation {

	@XmlAnyElement(lax = true)
    private TestResult result;

    private String shortDescription;

    private String handlingRecommendation;

    private String detailedDescription;

    public PropertyResultRecommendation() {

    }

    public PropertyResultRecommendation(TestResult result, String resultStatus, String handlingRecommendation) {
        this.result = result;
        this.shortDescription = resultStatus;
        this.handlingRecommendation = handlingRecommendation;
    }

    public PropertyResultRecommendation(TestResult result, String resultStatus, String handlingRecommendation,
        String detailedDescription) {
        this(result, resultStatus, handlingRecommendation);
        this.detailedDescription = detailedDescription;
    }

    public TestResult getResult() {
        return result;
    }

    public void setResult(TestResult result) {
        this.result = result;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getHandlingRecommendation() {
        return handlingRecommendation;
    }

    public void setHandlingRecommendation(String handlingRecommendation) {
        this.handlingRecommendation = handlingRecommendation;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }
}
