package de.rub.nds.tlsscanner.clientscanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rub.nds.tlsscanner.clientscanner.client.ClientInfo;
import de.rub.nds.tlsscanner.clientscanner.client.IOrchestrator;
import de.rub.nds.tlsscanner.clientscanner.probe.IProbe;
import de.rub.nds.tlsscanner.clientscanner.report.ClientReport;
import de.rub.nds.tlsscanner.clientscanner.report.result.ClientProbeResult;

public class ClientScanExecutor implements Observer {
    private static final Logger LOGGER = LogManager.getLogger();
    private Collection<IProbe> notScheduledTasks;
    private Collection<ProbeAndResultFuture> futureResults;
    private final ExecutorService executor;
    private final IOrchestrator orchestrator;

    public ClientScanExecutor(Collection<IProbe> probesToRun, IOrchestrator orchestrator, ExecutorService executor) {
        this.notScheduledTasks = new ArrayList<>(probesToRun);
        this.futureResults = new LinkedList<>();
        this.executor = executor;
        this.orchestrator = orchestrator;
    }

    public ClientReport execute() {
        ClientInfo clientInfo = orchestrator.getReportInformation();
        try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.push(clientInfo.toShortString())) {
            orchestrator.start();
            ClientReport report = executeInternal(clientInfo);
            orchestrator.postProcessing(report);
            return report;
        } finally {
            orchestrator.cleanup();
        }
    }

    protected ClientReport executeInternal(ClientInfo clientInfo) {
        ClientReport report = new ClientReport(clientInfo);
        LOGGER.info("Starting scan");
        checkForExecutableProbes(report);
        executeProbesTillNoneCanBeExecuted(report);
        updateClientReporttWithNotExecutedProbes(report);
        reportAboutNotExecutedProbes();
        collectStatistics(report);
        executeAfterProbes(report);
        LOGGER.info("Finished scan");
        return report;
    }

    private void checkForExecutableProbes(ClientReport report) {
        update(report, null);
    }

    @SuppressWarnings("squid:S3776") // sonarlint: Cognitive Complexity of methods should not be too high
    private void executeProbesTillNoneCanBeExecuted(ClientReport report) {
        do {
            long lastMerge = System.currentTimeMillis();
            List<ProbeAndResultFuture> finishedFutures = new LinkedList<>();
            for (ProbeAndResultFuture probeAndResultFuture : futureResults) {
                Future<ClientProbeResult> result = probeAndResultFuture.future;
                if (result.isDone()) {
                    lastMerge = System.currentTimeMillis();
                    try {
                        ClientProbeResult probeResult = result.get();
                        finishedFutures.add(probeAndResultFuture);
                        // TODO report.markProbeAsExecuted(result.get().getType())
                        if (probeResult != null) {
                            LOGGER.info("+++ {} executed", probeAndResultFuture.probe);
                            probeResult.merge(report);
                        } else {
                            LOGGER.error("Got null result from probe {}", probeAndResultFuture.probe);
                        }

                    } catch (Exception ex) {
                        LOGGER.error("Encountered an exception before we could merge the result. Killing the task.",
                                ex);
                        result.cancel(true);
                        finishedFutures.add(probeAndResultFuture);
                    }
                }

                if (lastMerge + 1000 * 60 * 30 < System.currentTimeMillis()) {
                    LOGGER.error(
                            "Last result merge is more than 30 minutes ago. Starting to kill threads to unblock...");
                    try {
                        ClientProbeResult probeResult = result.get(1, TimeUnit.MINUTES);
                        finishedFutures.add(probeAndResultFuture);
                        probeResult.merge(report);
                    } catch (Exception ex) {
                        result.cancel(true);
                        finishedFutures.add(probeAndResultFuture);
                        LOGGER.error("Killed task", ex);
                    }
                }
            }
            futureResults.removeAll(finishedFutures);
            update(report, this);
        } while (!futureResults.isEmpty());
    }

    private void updateClientReporttWithNotExecutedProbes(ClientReport report) {
        for (IProbe probe : notScheduledTasks) {
            probe.getCouldNotExecuteResult(report).merge(report);
        }
    }

    private void reportAboutNotExecutedProbes() {
        if (LOGGER.isWarnEnabled() && !notScheduledTasks.isEmpty()) {
            LOGGER.warn("Did not execute the following probes:");
            for (IProbe probe : notScheduledTasks) {
                LOGGER.warn(probe.getClass().getName());
            }
        }
    }

    private void collectStatistics(ClientReport report) {
        // TODO
    }

    private void executeAfterProbes(ClientReport report) {
        // TODO
    }

    @Override
    public synchronized void update(Observable o, Object arg) {
        if (o != null && o instanceof ClientReport) {
            ClientReport report = (ClientReport) o;
            // iterate over a copy of the list, as we might remove elements
            for (IProbe probe : new ArrayList<>(notScheduledTasks)) {
                if (probe.canBeExecuted(report)) {
                    notScheduledTasks.remove(probe);
                    Future<ClientProbeResult> future = executor.submit(probe);
                    futureResults.add(new ProbeAndResultFuture(probe, future));
                }
            }
        } else {
            LOGGER.error("Received an update from a non-ClientReport");
        }
    }

    protected static class ProbeAndResultFuture {
        public final IProbe probe;
        public final Future<ClientProbeResult> future;

        public ProbeAndResultFuture(IProbe probe, Future<ClientProbeResult> future) {
            this.probe = probe;
            this.future = future;
        }
    }
}