# invesdwin-context

This is the core of the module system for the invesdwin software product line platform. It allows to configure an application on a per module basis. On application startup the bootstrap process collects the configuration snippets and creates a full application context. It also handles the lifecycle of the application (providing hooks for modules) for running and testing it. Tests also benefit from the flexibility of replacing bean instances with stubs, choosing different spring-xmls to be loaded depending on the circumstances, enabling embedded servers (e.g. webserver=jetty, database=h2, registry=juddi) on a per test basis via annotations. These embedded servers are themselves modules which are available in separate projects and can be even packaged into distributions for use in production environments where appropriate.

This platform shared some of the goals of [spring-boot](http://projects.spring.io/spring-boot/). In comparison this platform provides the following advantages (despite it having been created long before spring-boot was thought of, even though only recently having gone open source):
- per module configuration instead of per application
- explicitly separate technology modules from domain modules and allow combining those per target environment into distributions
- customized Spring ApplicationContext per testcase without having to write one spring-xml per case, instead reusing and just selecting which spring-xml-snippets to use (essentially solving spring-xml-Hell)
- automatically merge individual logback.xmls, ehcache.xmls, properties files, spring-xmls, jaxb.xsds, web-fragment.xmls, aop.xmls or create jpa persistence.xmls during bootstrap from isolated snippets and classpath information

For more information on the concept and ideas behind this platform you can have a look at [this presentation](https://github.com/subes/invesdwin-context/blob/master/invesdwin-context-parent/invesdwin-context/doc/concept/invesdwin-concept.pdf) that was made for an earlier version of this platform where Ant+Ivy+Groovy was used for configuration management. Today these concepts got adapted into a new and improved maven implementation that provides many benefits over the older design while still keeping the same features as before. It was just the case that maven was not where it is today when this platform first came to life, but a switch was finally made when maven became more robust.

## Maven

Releases and snapshots are deployed to this maven repository:
```
http://invesdwin.de/artifactory/invesdwin-oss
```

Dependency declaration:
```xml
<dependency>
	<groupId>de.invesdwin</groupId>
	<artifactId>invesdwin-context-integration</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Base Classes

- **AMain**: this class can be used to implement your own main function that deals with running the application bootstrap and custom console arguments (using [args4j](http://args4j.kohsuke.org/)). It also processes `-Dparams=value` and sets them as system parameters to override existing defaults.
- **ATest**: this class should be extended by your unit tests. It provides test lifecycle methods to override in your tests like you were used to in JUnit 3.x, even though JUnit 4 is used now. It also handles the application bootstrap and decides whether it needs to be reinitialized depending on the current ApplicationContext configuration for the test. The context can be customized via setUpContextLocations(...), hooks like IStub, IContextLocation or by just adding annotations like @WebServerTest to your test (which is an annotation available in invesdwin-context-webserver that runs an embedded jetty server during tests by providing a stub implementation that checks for this annotation automatically for each test). Other such test annotations are available in other invesdwin projects and their respective modules.

## Hooks

The following lifecycle hooks are available, which can either just be added to a spring bean to automatically be picked up during bootstrap or alternatively registered manually in their respective XyzHookManager class.

- **IBasePackageDefinition**: this allows you to extend the classpath scanning packages to include your own ones. Per default only `de.invesdwin` is scanned, so your IBasePackageDefinition bean has to reside in this package as well to tell what other packages should be scanned (e.g. `com.mycompany`) so that the bootstrap discovers beans/hooks/services in those packages too.
- **IInstrumentationHook**: allows to run additional instrumentations before any classes get loaded by the classloader. E.g. to run the [datanucleus enhancer](http://www.datanucleus.org/products/datanucleus/jpa/enhancer.html) automatically without having to add it as a java agent manually when you want to use that framework as your ORM provider.
- **IPreStartupHook**: allows to do application setup before the application bootstrap begins. E.g. to initialize some internal libraries once.
- **IContextLocation**: allows you to provide additional spring.xmls that have to be loaded for the current module. You can even let these spring.xmls be loaded due to some logic to e.g. load a service directly or access it via some remote communication depending on if the service implementation module is present in the distribution (by checking the presence of a class). Sometimes the order in which spring.xmls are loaded matters, in that case you can also define a position for the spring.xmls at hand.
- **IStartupHook**: allows to do some additional setup after the application bootstrap finished. E.g. to start some jobs or to create additional database indexes for specific tables after they were generated by hibernate.
- **IShutdownHook**: allows to do some cleanup tasks when the application is stopped (note that this won't work on [SIGKILL](https://en.wikipedia.org/wiki/Unix_signal#SIGKILL))
- **IErrHook**: provides means to be notified when an error gets logged in the Err class or by the uncaught exception handler. E.g. to show the error in a swing message box for a desktop application.
- **IStub**: to hook into the test lifecycle to implement stubs/mocks which can customize the application context, do file system cleanup after tests, setup additional test infrastructure and so on. Please note that IStub beans are removed from the application context during bootstrap when running in production mode, so they can safely be put into your modules src/main/java without having to worry about if they are only executed inside of tests.

## Tools

- **PreMergedContext**: with this ApplicationContext you can collect spring beans before the actual application is bootstrapped. This is a preliminary context with which the MergedContext is built. When integrating the platform into another platform, you have to make sure the static initializers inside this class are called very early during application startup or else the instrumentation will be too late since too many classes have already been loaded by the classloader. The following things are setup here:
	- load [invesdwin-instrument](https://github.com/subes/invesdwin-instrument) to ensure classes are instrumented by AspectJ and module specific instrumentations
	- discover base packages for further classpath scanning (IBasePackageDefinition)
	- determine if we are running inside a test envinronment (if src/test/java directory exists)
	- initialize our context directories which can be optionally used by our modules (see ContextProperties class):
		- a process specific temp directory for classpath extension with generated classes that gets deleted when the application is exits (e.g. to load a dynamic instrumentation agent as in invesdwin-instrument or to create additional configuration files that get generated from classpath scanning like a persistence.xml) 
		- a cache directory inside our working directly for our application specific cache files that should be remembered between restarts (e.g. to store downloaded files that only have to be updated daily or monthly and should otherwise be remembered between application restarts)
		- a process specific log folder inside our working directory
		- a user specific home directory where files can be stored that can be accessed by different processes and applications (e.g. to store financial data used by multiple instances of parallel running strategy backtest processes)
	- make sure we use the correct [xalan](https://xalan.apache.org/) xslt transformer from our JVM (which might be discovered wrong depending on which libraries are in the classpath which will cause runtime errors)
	- initialize [Logback](http://logback.qos.ch/) as our [slf4j](http://www.slf4j.org/) provider by merging all config snippets we find in our classpath matching the classpath pattern `/META-INF/logback/*logback.xml` or `/META-INF/logback/*logback-test.xml`. Note that the logback-test.xml can be put inside your `src/test/java` to only be loaded for your JUnit tests to increase the log level for specific libraries.
	- load properties files that match the classpath pattern `/META-INF/*.properties` and make them available globally as system properties. You can set up developer specific properties that get loaded during testing in any module by placing a `/META-INF/env/${USERNAME}.properties` and defining your specific property overrides there. Distributions of your applications can package a `/META-INF/env/distribution.properties` to override the properties for your target customer/environment.
	- set a default network timeout to prevent connections from stalling threads because they never get a response. It is generally better to retry since otherwise connections can become stalled and never respond, while another try would get an immediate response depending on what endpoint is tried to be reached (JVM default is an unlimited timeout which is bad)
	- preregister serializable classes for [FST](http://ruedigermoeller.github.io/fast-serialization/) to make deep cloning of value objects faster
	- register an uncaught exception handler so that all exceptions get at least logged once
	- regiter a URI extension to support the `classpath:` protocol which might be needed to easily setup third party frameworks that only support URI paths
	- set the JVM default timezone to UTC to get commonality in timestamps of distributed applications. This makes comparing logs and IPC between servers in different countries easier. Note that you can override this via JVM arguments as noted in the application bootstrap hint to e.g. keep the default timezone for a UI application.
	- ensure the JVM default file encoding is set to UTF-8, else we might get funny problems with special characters on console and when reading/writing files which are really hard to troubleshoot. The file encoding can sometimes be wrong on misconfigured servers and workstations.
- **InvesdwinInitializationProperties**: If you want to prevent the static initializers from running entirely and prevent the bootstrap from happening, you can disable it here. For instance when running inside a restricted environment like a [JForex](https://www.dukascopy.com/swiss/deutsch/forex/jforex/) bundled strategy, the initializers will fail because file system, classloader and other resources is restricted. In that case you can still use most of the utilities available, but have to manage without spring-beans. In another use case you might want to integrate a invesdwin module into a different platform, but you do not want the static initializers to change any JVM defaults since they might interfere with your main platform. In that case disabling the static initializers is the only solution. Though this is not the normal deployment case and you should thus only worry you when you go some uncommon path regarding application integration.
- **MergedContext**: inside this ApplicationContext the actual application runs after it was bootstrapped. The bootstrap is invoked by the first call of MergedContext.autowire(...) which is automatically invoked by AMain or ATest application entry points. Note that you can also do dependency injection manually in your classes (which are not @Configurable or spring beans) to get dependency injection. Also you can provide ApplicationContexts to set them as children of the MergedContext (can be wrapped in DirectChildContext and and ParentContext to change the handling) to create a ApplicationContext hierarchy for special framework integration needs. The bootstrap itself accomplishes the following things:
	- collection all spring.xml IContextLocations that are supposed to build the final ApplicationContext
	- collect all ehcache config files matching the classpath pattern `/META-INF/ehcache/*ehcache.xml` and setup the caches
	- setup spring subsystems like @Configurable, @Transactional, @Scheduled, @Cached, @Async so that your beans can utilize them easily
	- collect xsds matching classpath pattern `/META-INF/xsd/*.xsd` to setup the JAXB validation context properly for use in the Marshaller util inside invesdwin-context-integration (when that module is in the classpath)
	- do classpath scanning for JPA entities to automatically generate a persistence.xml for the appropriate persistence units and ORMs that are configured (see invesdwin-persistence project for further information about this polyglot persistence feature)
	- do other module specific initialization that is integrated using the various hooks (e.g. launching a webserver, juddi registry, ApacheDS LDAP server, some UI, or whatelsenot
	- even though this might seem to be a lot to take care of, the application bootstrap is trimmed to run very fast to allow quick development roundturns (a typical web application with an embedded database and webserver starts in about 8 seconds during development/testing on our workstations)
- **Err**: this utility class can be called to log errors. It ensures errors are only logged only or when the stacktrace of an already logged exceptions gets extended by more exceptions (it will tell you in the log about an already logged reference exception id by concatening the cause ids). It will log the full stacktrace inside log/error.log and a shortened stacktrace on the console and inside log/common.log.
- **Log**: this is a wrapper around slf4j to provide some convenience constructors and to support %s style text placeholders like one is used from String.format(...) instead of the slf4j {}. Sometimes it is hard to know where one should use which notations, so for invesdwin we settled on %s and make sure all our utilities recognize that notation. Nothing is worse than a broken log statement when you want to troubleshoot some hard production problem, so we try everything to minimize these common coding errors.
- **AProperties**: this is a wrapper for [commons-configuration](https://commons.apache.org/proper/commons-configuration/) that handles thread safety, adds useful error messages for incorrect or missing properties and provides additional concenience transformations to make handling properties of all sorts easy (providing implementations such as URLProperties, SystemProperties, FileProperties)
- **NativeLibrary**: to allow packaging of native libraries for various target architectures inside your modules (e.g. used by a portscanner module to integrate [jpcapng](https://sourceforge.net/projects/jpcapng/))
- **SerializingSoftReference**: a soft reference that instead of discarding the reference, serializing it to disk instead of evicting it in order to spare some memory until the reference is accessed again
- **BeanValidator**: to conveniently verify the consistency of your value object where ever you might need to do so using the [BeanValidation Annotations](http://beanvalidation.org/)

## Integration Module

For application that also rely on IO (Input/Output of files and streams) and IPC (Inter-Process-Communication), the invesdwin-context-integration module provides a few tools that make the life easier here. It integrates [spring-integration](https://projects.spring.io/spring-integration/) which is an optional framework with which you can build a pipes and filters architecture for your integration workflow. Other invesdwin integration modules extend this to provide support for JMS, AMPQ, REST, WebServices, Spring-Batch, Hadoop and so on. This modules though only provides the core functionality:
- **integration.log**: per convention we want all our ingoing and outgoing messages to be available in human readable format to debug complex integration issues. For this normally all messages get logged to `log/integration.log`. You can disable this for improved performance by changing the loglevel for `de.invesdwin.MESSAGES` in your logback config. The MessageLoggingAspect intercepts all spring-integration @Gateway and @ServiceActivator beans, so no need to figure out how to get those messages.
- **NetworkUtil**: sometimes when accessing web content or consuming remote services over the internet and we get some hiccups we want to know if the service has a problem or the internet as a whole is currently down. This can be determined with this util. The appropriate action might be to make the process simply wait for the internet to come back online again before retrying, which can be accomplished with this. Or maybe you need to know your external IP-Address (e.g. to register your service instance with a registry) which can only be retrieved by asking a remote website like [whatismyip.com](https://www.whatismyip.com/) when you are behind a router. This information is conveniently available here.
- **@Retry**: the RetryAspect retries all method invocations where this annotation is found (alternatively there is the ARetryingCallable) depending on the exception that was thrown. If some IO or integration related exception if encountered, the method call is retried with a default backoff policy. You can also throw RetryLaterException or RetryLaterRuntimeException inside your method to decide for a retry on some other exception or condition.
- **IRetryHook**: this hook interface allows you to abort retries (by throwing another exception in onBeforeRetry) or do additional retry logic, like sending out an email to an administrator when a system is down or trying to reinitialize some remote service automatically (e.g. when a dynamic service vanished and you have to switch to a different instance which you rediscover from the central service registry). The RetryOriginator object should provide all meta-information needed to decide about special cases in your retry logic.
- **Marshallers**: convert to/from [XML](https://en.wikipedia.org/wiki/XML)/[JSON](https://en.wikipedia.org/wiki/JSON) with [JAXB](https://en.wikipedia.org/wiki/Java_Architecture_for_XML_Binding)/[Jackson](https://github.com/FasterXML/jackson-databind). Initially [gson](https://github.com/google/gson) was used for JSON processing, but jackson was found to be faster by a magnitude and to provide better configurability even though it is a bit less easy and intuitive to use. 
- **CSV**: the ABeanCsvReader, ABeanCsvWriter and other classes provide some utilities to easily read/write CSV files. This utilizes the popular [spring-batch](http://projects.spring.io/spring-batch/) FlatFileItemReader and Mapper functionality. Though a complete spring-batch integration is found in a different invesdwin integration module.

## Webserver Module

This module packages an embedded [jetty server](http://www.eclipse.org/jetty/) to provide a servlet container for web services and web frameworks either during testing by annotating your test with @WebServerTest or to deploy your application with a distribution that contains an embedded webserver. You can also call WebserverContextLocation.activate()/.deactivate() before the application is bootstrapped to enable/disable the embedded webserver programmatically. 
- To provide a convenience entry point for your web application distribution, just define the main class as `de.invesdwin.context.webserver.Main`. 
- The property `de.invesdwin.context.integration.IntegrationProperties.WEBSERVER_BIND_URI` with its default value `http://localhost:9001` is referenced from the invesdwin-context-integration module to setup the server. 
	- Change the port in your developer/distribution properties override to suit your needs. When the server starts, it logs where it is listening to. 
	- You can even change the protocol to `https://` to enable SSL support. Though you should make sure to change the `de.invesdwin.context.webserver.WebserverProperties.KEYSTORE_*` properties to switch to a real certificate instead of the auto generated one inside the module itself. Or use the more common approach of settings up a reverse proxy on your apache webserver that adds ssl for your website (see [chapter 4.6](http://invesdwin.de/nowicket/installation) from the invesdwin-NoWicket documentation.
