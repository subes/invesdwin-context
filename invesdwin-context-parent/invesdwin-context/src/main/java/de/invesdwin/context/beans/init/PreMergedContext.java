package de.invesdwin.context.beans.init;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.Resource;

import de.invesdwin.context.ContextProperties;
import de.invesdwin.context.beans.init.locations.IContextLocation;
import de.invesdwin.context.beans.init.locations.PositionedResource;
import de.invesdwin.context.log.error.Err;
import de.invesdwin.util.assertions.Assertions;

/**
 * This should only be used by infrastructure classes.
 */
@ThreadSafe
public final class PreMergedContext extends ADelegateContext {

    @GuardedBy("this")
    private static PreMergedContext instance;

    static {
        if (InvesdwinInitializationProperties.isInvesdwinInitializationAllowed()) {
            try {
                final InvesdwinInitializers initializers = InvesdwinInitializationProperties
                        .getInvesdwinInitializers();
                initializers.initInstrumentation();
                Assertions.assertThat(ContextProperties.TEMP_CLASSPATH_DIRECTORY).isNotNull();
                Assertions.assertThat(Err.UNCAUGHT_EXCEPTION_HANDLER).isNotNull();
                initializers.initProtocolRegistration();
                initializers.initDefaultTimezoneConfigurer();
                /*
                 * this must happen after properties have been loaded so that an overwritten property gets detected
                 */
                initializers.initFileEncodingChecker();
            } catch (final Throwable t) {
                InvesdwinInitializationProperties.logInitializationFailedIsIgnored(t);
            }
        }
    }

    private PreMergedContext(final GenericApplicationContext delegate) {
        super(delegate);
    }

    @Override
    public GenericXmlApplicationContext getDelegate() {
        return (GenericXmlApplicationContext) delegate;
    }

    public static PreMergedContext getInstance() {
        return getInstance(false);
    }

    public static synchronized PreMergedContext getInstance(final boolean reset) {
        if ((instance == null || reset)) {
            InvesdwinInitializationProperties.assertInitializationNotSkipped();
            final GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
            ctx.registerShutdownHook();
            final PreMergedContext newInstance = new PreMergedContext(ctx);
            instance = newInstance;
            for (final Entry<String, Resource> e : new ComponentScanConfigurer().getApplicationContextXmlConfigs(true)
                    .entrySet()) {
                instance.getDelegate().load(e.getValue());
            }
            disableConfigurationAnnotationProcessing(ctx);
            instance.refresh();
        }
        return instance;
    }

    /**
     * This should only be used by infrastructure classes.
     */
    public static synchronized List<PositionedResource> collectMergedContexts() {
        //First only collect the context files
        final Map<String, IContextLocation> mergers = getInstance().getBeansOfType(IContextLocation.class);
        final List<PositionedResource> contexts = new ArrayList<PositionedResource>();
        for (final IContextLocation merger : mergers.values()) {
            final List<PositionedResource> contextResources = merger.getContextResources();
            if (contextResources != null) {
                for (final PositionedResource contextResource : contextResources) {
                    if (contextResource != null) {
                        contexts.add(contextResource);
                    }
                }
            }
        }
        PositionedResource.COMPARATOR.sort(contexts, true);
        return contexts;
    }

    /**
     * Deactivates the loading of @Configuration classes.
     */
    private static void disableConfigurationAnnotationProcessing(final ConfigurableApplicationContext ctx) {
        Assertions.assertThat(ctx).isInstanceOf(BeanDefinitionRegistry.class);
        final String[] matches = ctx.getBeanFactory().getBeanNamesForType(ConfigurationClassPostProcessor.class, false,
                false);
        final BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) ctx;
        for (final String beanName : matches) {
            beanDefinitionRegistry.removeBeanDefinition(beanName);
        }
    }

}
