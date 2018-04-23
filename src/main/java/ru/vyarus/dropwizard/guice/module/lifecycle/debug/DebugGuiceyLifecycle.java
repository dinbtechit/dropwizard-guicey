package ru.vyarus.dropwizard.guice.module.lifecycle.debug;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import ru.vyarus.dropwizard.guice.module.lifecycle.GuiceyLifecycleAdapter;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.configuration.ConfiguratorsProcessedEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.configuration.InitializationEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.hk.HkConfigurationEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.hk.HkExtensionsInstalledEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.run.*;

import java.util.Collections;
import java.util.List;

/**
 * Debug guicey lifecycle listener. Could be installed with bundle shortcut:
 * {@link ru.vyarus.dropwizard.guice.GuiceBundle.Builder#printLifecyclePhases()}.
 * <p>
 * Use system out instead of logger because logger in not initialized in time of first events and for
 * more clarity.
 * <p>
 * Split logs with current phase name and startup timer. This should clarify custom logic execution times.
 *
 * @author Vyacheslav Rusakov
 * @since 17.04.2018
 */
public class DebugGuiceyLifecycle extends GuiceyLifecycleAdapter {

    // counting time from listener creation (~same as bundle registration and app initial configuration)
    private final StopWatch timer = StopWatch.createStarted();

    @Override
    protected void configuratorsProcessed(final ConfiguratorsProcessedEvent event) {
        log("%s configurators processed", event.getConfigurators().size());
    }

    @Override
    protected void initialization(final InitializationEvent event) {
        if (!event.getCommands().isEmpty()) {
            log("%s commands installed", event.getCommands().size());
        }
    }

    @Override
    protected void dwBundlesResolved(final BundlesFromDwResolvedEvent event) {
        log("%s dw bundles recognized", event.getBundles().size());
    }

    @Override
    protected void lookupBundlesResolved(final BundlesFromLookupResolvedEvent event) {
        log("%s lookup bundles recognized", event.getBundles().size());
    }

    @Override
    protected void bundlesProcessed(final BundlesProcessedEvent event) {
        log("Configured from %s%s GuiceyBundles", event.getBundles().size(), disabled(event.getDisabled()));
    }

    @Override
    protected void injectorCreation(final InjectorCreationEvent event) {
        log("Staring guice with %s/%s%s modules...",
                event.getModules().size(), event.getOverridingModules().size(), disabled(event.getDisabled()));
    }

    @Override
    protected void installersResolved(final InstallersResolvedEvent event) {
        log("%s%s installers initialized", event.getInstallers().size(), disabled(event.getDisabled()));
    }

    @Override
    protected void extensionsResolved(final ExtensionsResolvedEvent event) {
        log("%s%s extensions found", event.getExtensions().size(), disabled(event.getDisabled()));
    }

    @Override
    protected void extensionsInstalled(final ExtensionsInstalledEvent event) {
        log("%s extensions installed", event.getExtensions().size());
    }

    @Override
    protected void applicationRun(final ApplicationRunEvent event) {
        log("Guice started, app running...");
        event.registerJettyListener(new JettyLifecycleListener());
        event.registerJerseyListener(new JerseyEventListener());
    }

    @Override
    protected void hkConfiguration(final HkConfigurationEvent event) {
        log("Configuring HK...");
    }

    @Override
    protected void hkExtensionsInstalled(final HkExtensionsInstalledEvent event) {
        log("%s HK extensions installed", event.getExtensions().size());
    }

    @SuppressWarnings("PMD.SystemPrintln")
    private void log(final String message, final Object... args) {
        final int gap = 70;
        final String time = timer.toString();
        final String msg = String.format(message, args);
        final String topLine = String.format("%" + (gap + 3) + "s", "")
                + String.join("", Collections.nCopies(msg.length(), "─"));
        final String prefix = "__[ " + time + " ]" + String.join("",
                Collections.nCopies((gap - 6) - time.length(), "_"));
        System.out.println("\n\n" + topLine + "\n" + prefix + "/  " + msg + "  \\____\n");
    }

    private String disabled(final List items) {
        return " (-" + items.size() + ")";
    }

    /**
     * Jetty listener.
     */
    private class JettyLifecycleListener extends AbstractLifeCycle.AbstractLifeCycleListener {
        @Override
        public void lifeCycleStarting(final LifeCycle event) {
            log("Jetty starting...");
        }

        @Override
        public void lifeCycleStarted(final LifeCycle event) {
            log("Jetty started");
        }

        @Override
        public void lifeCycleStopping(final LifeCycle event) {
            timer.reset();
            log("Stopping Jetty...");
        }

        @Override
        public void lifeCycleStopped(final LifeCycle event) {
            log("Jetty stopped");
        }
    }

    /**
     * Jersey listener.
     */
    private class JerseyEventListener implements ApplicationEventListener {
        @Override
        @SuppressWarnings({"checkstyle:MissingSwitchDefault", "PMD.SwitchStmtsShouldHaveDefault"})
        @SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
        public void onEvent(final ApplicationEvent event) {
            switch (event.getType()) {
                case INITIALIZATION_START:
                    log("Initializing jersey app...");
                    break;
                case INITIALIZATION_APP_FINISHED:
                    log("Jersey app initialized");
                    break;
                case INITIALIZATION_FINISHED:
                    log("Jersey initialized");
                    break;
                case DESTROY_FINISHED:
                    log("Jersey app destroyed");
                    break;
            }
        }

        @Override
        public RequestEventListener onRequest(final RequestEvent requestEvent) {
            return null;
        }
    }
}