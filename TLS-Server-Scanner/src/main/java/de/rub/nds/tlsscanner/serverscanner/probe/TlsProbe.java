/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe;

import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.probe.stats.StatsWriter;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.serverscanner.report.PerformanceData;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.report.result.ProbeResult;
import de.rub.nds.tlsscanner.serverscanner.requirements.ProbeRequirement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public abstract class TlsProbe implements Runnable {

    protected static final Logger LOGGER = LogManager.getLogger(TlsProbe.class.getName());

    protected final ScannerConfig scannerConfig;
    protected final ProbeType type;
    protected long startTime;
    protected long stopTime;

    private final ParallelExecutor parallelExecutor;

    private final StatsWriter writer;

    private AtomicBoolean readyForExecution = new AtomicBoolean(false);
    
    protected List<AnalyzedProperty> properties;
    private Map<AnalyzedProperty, TestResult> propertiesMap;

    public TlsProbe(ParallelExecutor parallelExecutor, ProbeType type, ScannerConfig scannerConfig) {
        this.scannerConfig = scannerConfig;
        this.type = type;
        this.parallelExecutor = parallelExecutor;
        this.writer = new StatsWriter();
        this.properties=new ArrayList<>();
    }

    public final ScannerConfig getScannerConfig() {
        return scannerConfig;
    }

    public String getProbeName() {
        return type.name();
    }

    public ProbeType getType() {
        return type;
    }

    @Override
    public void run() {
        ThreadContext.put("host",
            this.scannerConfig.getClientDelegate().getSniHostname() == null
                ? this.scannerConfig.getClientDelegate().getHost()
                : this.scannerConfig.getClientDelegate().getSniHostname());
        LOGGER.debug("Executing:" + getProbeName());
        this.startTime = System.currentTimeMillis();

        try {
            executeTest();
        } catch (Exception e) {
            // InterruptedException are wrapped in the ParallelExceutor of Tls-Attacker so we unwrap them here
            if (e.getCause() instanceof InterruptedException) {
                LOGGER.error("Timeout on " + getProbeName());
            } else {
                LOGGER.error("Could not scan for " + getProbeName(), e);
            }
            getCouldNotExecuteResult();
        } finally {
            this.stopTime = System.currentTimeMillis();
           // } else {
             //   LOGGER.warn("" + getProbeName() + " - is null result");
            LOGGER.debug("Finished " + getProbeName() + " -  Took " + (this.stopTime - this.startTime) / 1000 + "s");
            ThreadContext.remove("host");
        }
        return;
    }

    public final void executeState(State... states) {
        this.executeState(new ArrayList<State>(Arrays.asList(states)));

    }

    public final void executeState(List<State> states) {
        parallelExecutor.bulkExecuteStateTasks(states);
        for (State state : states) {
            writer.extract(state);
        }

    }

    public abstract void executeTest();

    public void executeAndMerge(SiteReport report) {
        //ProbeResult result = this.call();
        this.run();
    	merge(report);
    }

    /**
     * Override for individual requirements.
     * @param report
     * @return ProbeRequirement object without requirements (default)
     */
    protected ProbeRequirement getRequirements(SiteReport report) {
        return new ProbeRequirement(report);
    }

    public boolean canBeExecuted(SiteReport report) {
        return getRequirements(report).evaluateRequirements();
    }

    public abstract ProbeResult getCouldNotExecuteResult();

    public abstract void adjustConfig(SiteReport report);

    public ParallelExecutor getParallelExecutor() {
        return parallelExecutor;
    }

    public StatsWriter getWriter() {
        return writer;
    }

    public AtomicBoolean getReadyForExecution() {
        return readyForExecution;
    }
    
    protected void setPropertyReportValue(AnalyzedProperty aProp, TestResult result) {
    	if (this.propertiesMap!=null) {
    		if (this.propertiesMap.containsKey(aProp))
    			this.propertiesMap.replace(aProp, result);
    		else // avoid unregistered properties are set
    			System.out.println("FORBIDDEN PROPERTY!!!"); //TODO exception? logging error? 
    	}
    	else {
    		this.propertiesMap = new HashMap<>();
    		for (AnalyzedProperty property : this.properties)
    			this.propertiesMap.put(property, null);    		
    	}
    }
    
    // can be overwritten if some data must be set manually
    protected abstract void mergeData(SiteReport report);
    
    protected void merge(SiteReport report) {
    	// catch case that no properties are set
    	if(this.propertiesMap==null) {
    		this.propertiesMap = new HashMap<>();
    		for (AnalyzedProperty property : this.properties)
    			this.propertiesMap.put(property, null);    
    	}
    	
    	// check whether every property has been set
    	for (AnalyzedProperty aProp : this.properties) {
    		if (this.propertiesMap.get(aProp) == null)
    			System.out.println("UNSET PROPERTY!!!"); //TODO was nu?
    	}
    	// merge data
    	if (this.startTime != 0 && this.stopTime != 0) {
            report.getPerformanceList().add(new PerformanceData(this.type, this.startTime, this.stopTime));
        }
    	for (AnalyzedProperty aProp : this.properties) // TODO wäre es ok, aprops zu setzen, auch wenn nicht alle props gesetzt wurden vorher?
        	report.putResult(aProp, this.propertiesMap.get(aProp)); 
   		this.mergeData(report);
    	report.markAsChangedAndNotify(); // was macht die???
    }
}
