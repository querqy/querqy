package querqy.solr.utils;

import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.util.IOFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.solr.common.SolrException.ErrorCode.SERVER_ERROR;

public final class CoreUtils {

    private CoreUtils() {
        // private
    }

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Executes the passed lambda function on the requested core.
     * Waits for the core to become available in a specified time period.
     *
     * @param lambda the function to execute
     * @param coreName the core to use for the execution
     * @param coreContainer the core container to get the cores from
     * @param waitTimeout the amount of time to wait for the core before
     * @return the result of the function
     * @param <R> the result type of the function
     * @throws IOException might be thrown by the executed function
     * @throws SolrException if the core is not available within the specified timeout
     */
    public static <R> R withCore(
            final IOFunction<SolrCore, R> lambda,
            final String coreName,
            final CoreContainer coreContainer,
            final Duration waitTimeout
    ) throws IOException {
        final Optional<SolrCore> possibleCore = getCoreOrWait(coreName, coreContainer, waitTimeout);
        if (possibleCore.isPresent()) {
            try (final SolrCore core = possibleCore.get()) {
                return lambda.apply(core);
            }
        } else {
            throw new SolrException(SERVER_ERROR, String.format("Core %s not available after %s seconds", coreName, waitTimeout.getSeconds()));
        }
    }

    /**
     * Executes the passed lambda function on the requested core.
     *
     * @param lambda the function to execute
     * @param coreName the core to use for the execution
     * @param coreContainer the core container to get the cores from
     * @return the result of the function
     * @param <R> the result type of the function
     * @throws IOException might be thrown by the executed function
     * @throws SolrException if the core is not available within the specified timeout
     */
    public static <R> R withCore(
            final IOFunction<SolrCore, R> lambda,
            final String coreName,
            final CoreContainer coreContainer
    ) throws IOException {
        final Optional<SolrCore> possibleCore = getCore(coreName, coreContainer);
        if (possibleCore.isPresent()) {
            try (final SolrCore core = possibleCore.get()) {
                return lambda.apply(core);
            }
        } else {
            throw new SolrException(SERVER_ERROR, String.format("Core %s not available", coreName));
        }
    }

    /**
     * Looks up the specified core if it is already loaded
     *
     * @param coreName the name of the core to lookup
     * @param coreContainer the core container to use for the lookup
     *
     * @return an optional core, present if the core was already loaded
     */
    private static Optional<SolrCore> getCore(final String coreName, final CoreContainer coreContainer) {
        return Optional.ofNullable(coreContainer.getCore(coreName));
    }

    /**
     * Looks up the specified core and waits for defined period, if it is not available yet.
     *
     * @param coreName the name of the core to lookup
     * @param coreContainer the core container to use for the lookup
     * @param waitTimeout the duration to wait for the core
     *
     * @return an optional core, empty if the core was not available within the specified wait timeout
     */
    private static Optional<SolrCore> getCoreOrWait(final String coreName, final CoreContainer coreContainer, final Duration waitTimeout) {
        int tries = 0;
        final LocalDateTime start = LocalDateTime.now();
        while (true) {
            final Optional<SolrCore> core = getCore(coreName, coreContainer);
            if (core.isPresent()) {
                return core;
            } else if (Duration.between(start, LocalDateTime.now()).getSeconds() > waitTimeout.getSeconds()) {
                return Optional.empty();
            } else {
                tries++;
                try {
                    TimeUnit.SECONDS.sleep(5L);
                } catch (InterruptedException e) {
                    LOG.warn("Lookup for core {} was interrupted", coreName);
                    Thread.currentThread().interrupt();
                }
                LOG.info("Retrying to lookup core {} (retry={})", coreName, tries);
            }
        }
    }

}
