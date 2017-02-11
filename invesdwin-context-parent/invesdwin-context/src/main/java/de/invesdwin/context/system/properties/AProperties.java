package de.invesdwin.context.system.properties;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.springframework.util.SocketUtils;

import de.invesdwin.context.ContextProperties;
import de.invesdwin.context.log.Log;
import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.lang.Strings;
import de.invesdwin.util.lang.uri.Addresses;
import de.invesdwin.util.lang.uri.URIs;
import de.invesdwin.util.math.decimal.Decimal;
import de.invesdwin.util.time.duration.Duration;
import de.invesdwin.util.time.fdate.FDate;
import de.invesdwin.util.time.fdate.FDates;
import de.invesdwin.util.time.fdate.FTimeUnit;

@ThreadSafe
public abstract class AProperties implements IProperties {

    private final Log log = new Log(this);

    @GuardedBy("this")
    private AbstractConfiguration delegate;

    protected abstract AbstractConfiguration createDelegate();

    public synchronized AbstractConfiguration getDelegate() {
        if (delegate == null) {
            delegate = createDelegate();
            updateDelimiterParsingDisabled(true); //delimiter parsing is evil!
        }
        return delegate;
    }

    private void updateDelimiterParsingDisabled(final boolean value) {
        delegate.setDelimiterParsingDisabled(value);
        if (delegate instanceof AbstractFileConfiguration) {
            final AbstractFileConfiguration conf = (AbstractFileConfiguration) delegate;
            if (conf.getFile() != null && conf.getFile().exists()
                    || conf.getURL() != null && URIs.connect(conf.getURL()).isDownloadPossible()) {
                try {
                    //need to refresh here, or else the setting is not applied
                    conf.refresh();
                } catch (final ConfigurationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void setProperty(final String key, final String value) {
        getDelegate().setProperty(prefix(key), value);
    }

    @Override
    public synchronized boolean containsKey(final String key) {
        final String keyPath = prefix(key);
        return getDelegate().containsKey(keyPath);
    }

    @Override
    public synchronized boolean containsValue(final String key) {
        if (containsKey(key)) {
            final Object property = getDelegate().getProperty(prefix(key));
            return !Strings.isBlank(Strings.asString(property));
        } else {
            return false;
        }
    }

    @Override
    public synchronized Boolean getBoolean(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getBoolean(keyPath, null));
    }

    @Override
    public void setBoolean(final String key, final Boolean value) {
        final String keyPath = prefix(key);
        setProperty(keyPath, Strings.asString(value));
    }

    @Override
    public synchronized Byte getByte(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getByte(keyPath, null));
    }

    @Override
    public synchronized Double getDouble(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getDouble(keyPath, null));
    }

    @Override
    public synchronized Float getFloat(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getFloat(keyPath, null));
    }

    @Override
    public synchronized Integer getInteger(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getInteger(keyPath, null));
    }

    @Override
    public synchronized void setInteger(final String key, final Integer value) {
        final String keyPath = prefix(key);
        setProperty(keyPath, Strings.asString(value));
    }

    @Override
    public synchronized Long getLong(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getLong(keyPath, null));
    }

    @Override
    public synchronized Short getShort(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getShort(keyPath, null));
    }

    @Override
    public synchronized BigDecimal getBigDecimal(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getBigDecimal(keyPath));
    }

    @Override
    public synchronized BigInteger getBigInteger(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getBigInteger(keyPath));
    }

    @Override
    public synchronized Decimal getDecimal(final String key) {
        return Decimal.valueOf(getBigDecimal(key));
    }

    @Override
    public synchronized String getString(final String key) {
        final String keyPath = prefix(key);
        return maybeThrowIfMissing(keyPath, getDelegate().getString(keyPath));
    }

    @Override
    public synchronized String getStringWithSecurityWarning(final String key, final String defaultPasswordWarning) {
        final String keyPath = prefix(key);
        final String password = maybeThrowIfMissing(keyPath, getDelegate().getString(keyPath));
        if (!ContextProperties.IS_TEST_ENVIRONMENT && defaultPasswordWarning != null
                && defaultPasswordWarning.equals(password)) {
            log.warn(
                    "Property [%s] is currently set to the default value [%s] in a production environment, "
                            + "please override this value because otherwise you might expose yourself to a security risk. "
                            + "See the invesdwin-context documentation for details on how to do this.",
                    keyPath, defaultPasswordWarning);
        }
        return password;
    }

    @Override
    public synchronized <T extends Enum<T>> T getEnum(final Class<T> enumType, final String key) {
        final String value = getString(key);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    getErrorMessage(key, value, Duration.class, "Expected format: " + getEnumFormat(FTimeUnit.class)));
        }
    }

    @Override
    public synchronized void setEnum(final String key, final Enum<?> value) {
        setProperty(key, value.name());
    }

    @Override
    public synchronized void setString(final String key, final String value) {
        setProperty(key, value);
    }

    @Override
    public synchronized String[] getStringArray(final String key) {
        final String keyPath = prefix(key);
        //provoke exception if something is not set
        if (getString(keyPath) == null) {
            return null;
        }
        final boolean delimiterParsinDisabledPreviously = getDelegate().isDelimiterParsingDisabled();
        try {
            updateDelimiterParsingDisabled(false);
            return getDelegate().getStringArray(prefix(keyPath));
        } finally {
            updateDelimiterParsingDisabled(delimiterParsinDisabledPreviously);
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized List<String> getList(final String key) {
        final String keyPath = prefix(key);
        if (getString(keyPath) == null) {
            return null;
        }
        final boolean delimiterParsinDisabledPreviously = getDelegate().isDelimiterParsingDisabled();
        try {
            updateDelimiterParsingDisabled(false);
            return (List) getDelegate().getList(keyPath);
        } finally {
            updateDelimiterParsingDisabled(delimiterParsinDisabledPreviously);
        }
    }

    @Override
    public synchronized void setList(final String key, final List<String> value) {
        final String keyPath = prefix(key);
        final String valueStr = Strings.asString(value, String.valueOf(getDelegate().getListDelimiter()));
        getDelegate().setProperty(keyPath, valueStr);
    }

    @Override
    public synchronized Set<String> getSet(final String key) {
        final List<String> list = getList(key);
        if (list == null) {
            return null;
        } else {
            return new LinkedHashSet<String>(list);
        }
    }

    @Override
    public synchronized void setSet(final String key, final Set<String> value) {
        if (value == null) {
            setList(key, null);
        } else {
            setList(key, new ArrayList<String>(value));
        }
    }

    private String prefix(final String key) {
        return Strings.eventuallyAddPrefix(key, getPropertyPrefix());
    }

    protected String getPropertyPrefix() {
        return null;
    }

    @Override
    public synchronized FDate getDate(final String key) {
        final String value = getString(key);
        return FDate.valueOf(value, FDate.FORMAT_ISO_DATE_TIME_MS);
    }

    @Override
    public synchronized void setDate(final String key, final FDate value) {
        setProperty(key, FDates.toString(value, FDate.FORMAT_ISO_DATE_TIME_MS));
    }

    @Override
    public synchronized Duration getDuration(final String key) {
        final String value = getString(key);
        Duration ret;
        try {
            final String[] values = value.split(" ");
            final int duration = Integer.valueOf(values[0]);
            final FTimeUnit unit = FTimeUnit.valueOf(values[1]);
            ret = new Duration(duration, unit);
        } catch (final NumberFormatException e) {
            ret = null;
        } catch (final IllegalArgumentException e) {
            ret = null;
        } catch (final IndexOutOfBoundsException e) {
            ret = null;
        }
        if (ret != null) {
            return ret;
        } else {
            throw new IllegalArgumentException(getErrorMessage(key, value, Duration.class,
                    "Expected format: <NUMBER> " + getEnumFormat(FTimeUnit.class)));
        }

    }

    @Override
    public synchronized URL getURL(final String key, final boolean validatePort) {
        try {
            return getURI(key, validatePort).toURL();
        } catch (final Throwable t) {
            final String str = getString(key);
            throw new IllegalArgumentException(getErrorMessage(key, str, URL.class, t.getMessage()), t);
        }
    }

    @Override
    public synchronized URI getURI(final String key, final boolean validatePort) {
        final String str = getString(key);
        try {
            URI uri = URIs.asUri(str);
            if (uri.getHost() == null) {
                //without a protocol, calls to getHost and getPort will fail, thus add a fake one!
                uri = URIs.asUri("p://" + str);
            }
            Assertions.assertThat(uri.getHost())
                    .as("Unable to get host from URI (maybe you forgot to add <protocol>:// or so?): " + uri)
                    .isNotNull();
            if (validatePort) {
                final int port = uri.getPort();
                if (port == 0) {
                    final int randomPort = SocketUtils.findAvailableTcpPort();
                    //override property
                    uri = URIs.setPort(uri, randomPort);
                    setProperty(key, uri.toString());
                } else {
                    Assertions.assertThat(port)
                            .as("Unable to get port from URI (maybe you forgot to add <protocol>:// or so?): " + uri)
                            .isGreaterThan(0);
                }
            }
            return uri;
        } catch (final Throwable t) {
            throw new IllegalArgumentException(getErrorMessage(key, str, URI.class, t.getMessage()), t);
        }
    }

    @Override
    public synchronized InetAddress getInetAddress(final String key) {
        return Addresses.asAddress(getString(key));
    }

    @Override
    public synchronized InetSocketAddress getInetSocketAddress(final String key) {
        final String value = getString(key);
        final String[] split = Strings.split(value, ":");
        Throwable cause = null;
        if (split.length == 2) {
            try {
                final String host = split[0];
                final int port = Integer.parseInt(split[1]);
                return Addresses.asAddress(host, port);
            } catch (final NumberFormatException e) {
                cause = e;
            }
        }
        throw new IllegalArgumentException(
                getErrorMessage(key, value, InetSocketAddress.class, "Expected format: <HOST>:<PORT>"), cause);
    }

    @Override
    public synchronized File getFile(final String key) {
        return new File(getString(key));
    }

    @Override
    public synchronized String getEnumFormat(final Class<? extends Enum<?>> enumType) {
        final String delimiter = " | ";
        final StringBuilder sb = new StringBuilder("(");
        for (final Enum<?> value : enumType.getEnumConstants()) {
            sb.append(value.name());
            sb.append(delimiter);
        }
        return Strings.removeEnd(sb.toString(), delimiter) + ")";
    }

    @Override
    public synchronized String getErrorMessage(final String key, final Object value, final Class<?> expectedType,
            final String message) {
        Assertions.assertThat(key).isNotNull();
        String error = "Property " + prefix(key);
        if (value != null) {
            error += " [" + value + "]";
        }
        if (expectedType != null) {
            error += " is not of type " + expectedType.getSimpleName();
        }
        error += ".";
        if (Strings.isNotBlank(message)) {
            error += Strings.eventuallyAddPrefix(message, " ");
        }
        return error;
    }

    private <T> T maybeThrowIfMissing(final String key, final T value) {
        if (getDelegate().isThrowExceptionOnMissing() && value == null) {
            //CHECKSTYLE:OFF we explicitly want the stacktrace here
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
            //CHECKSTYLE:ON
        } else {
            return value;
        }
    }

}
