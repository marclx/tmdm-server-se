package org.talend.mdm.bulkload.client;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

/**
 * Bulkload amount items client
 *
 */
public class BulkloadClientUtil {

    public static void bulkload(String url,
                                String cluster,
                                String concept,
                                String datamodel,
                                boolean validate,
                                boolean smartpk,
                                InputStream itemdata,
                                String username,
                                String password,
                                String transactionId,
                                String universe) throws Exception {
        HostConfiguration config = new HostConfiguration();
        URI uri = new URI(url, false, "UTF-8"); //$NON-NLS-1$
        config.setHost(uri);

        NameValuePair[] parameters = {new NameValuePair("cluster", cluster), //$NON-NLS-1$
                new NameValuePair("concept", concept),  //$NON-NLS-1$
                new NameValuePair("datamodel", datamodel),   //$NON-NLS-1$
                new NameValuePair("validate", String.valueOf(validate)), //$NON-NLS-1$
                new NameValuePair("action", "load"), //$NON-NLS-1$ //$NON-NLS-2$
                new NameValuePair("smartpk", String.valueOf(smartpk))}; //$NON-NLS-1$

        HttpClient client = new HttpClient();
        String user = universe == null || universe.trim().length() == 0 ? username : universe + "/" + username; //$NON-NLS-1$
        client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        HttpClientParams clientParams = client.getParams();
        clientParams.setAuthenticationPreemptive(true);
        clientParams.setCookiePolicy(CookiePolicy.IGNORE_COOKIES);

        PutMethod putMethod = new PutMethod();
        // This setPath call is *really* important (if not set, request will be sent to the JBoss root '/')
        putMethod.setPath(url);
        String responseBody;
        try {
            // Configuration
            putMethod.setRequestHeader("Content-Type", "text/xml; charset=utf8"); //$NON-NLS-1$ //$NON-NLS-2$
            if (transactionId != null) {
                putMethod.setRequestHeader("transaction-id", transactionId); //$NON-NLS-1$
            }
            putMethod.setQueryString(parameters);
            putMethod.setContentChunked(true);
            // Set the content of the PUT request
            putMethod.setRequestEntity(new InputStreamRequestEntity(itemdata));

            client.executeMethod(config, putMethod);
            responseBody = putMethod.getResponseBodyAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            putMethod.releaseConnection();
        }

        int statusCode = putMethod.getStatusCode();
        if (statusCode >= 500) {
            throw new BulkloadException(responseBody);
        } else if (statusCode >= 400) {
            throw new BulkloadException("Could not send data to MDM (HTTP status code: " + statusCode + ").");
        }
    }

    public static InputStreamMerger bulkload(String url,
                                             String cluster,
                                             String concept,
                                             String dataModel,
                                             boolean validate,
                                             boolean smartPK,
                                             String username,
                                             String password,
                                             String transactionId,
                                             String universe,
                                             AtomicInteger startedBulkloadCount) {
        InputStreamMerger merger = new InputStreamMerger();

        Runnable loadRunnable = new AsyncLoadRunnable(url,
                cluster,
                concept,
                dataModel,
                validate,
                smartPK,
                merger,
                username,
                password,
                transactionId,
                universe,
                startedBulkloadCount);
        Thread loadThread = new Thread(loadRunnable);
        loadThread.start();
        merger.setConsumerThread(loadThread); // Allow merger to wait for loadThread completion.
        return merger;
    }

    private static class AsyncLoadRunnable implements Runnable {

        private final String url;

        private final String cluster;

        private final String concept;

        private final String dataModel;

        private final boolean validate;

        private final boolean smartPK;

        private final InputStreamMerger inputStream;

        private final String userName;

        private final String password;

        private final String transactionId;

        private final String universe;

        private final AtomicInteger startedBulkloadCount;

        public AsyncLoadRunnable(String url,
                                 String cluster,
                                 String concept,
                                 String dataModel,
                                 boolean validate,
                                 boolean smartPK,
                                 InputStreamMerger inputStream,
                                 String userName,
                                 String password,
                                 String transactionId,
                                 String universe,
                                 AtomicInteger startedBulkloadCount) {
            this.url = url;
            this.cluster = cluster;
            this.concept = concept;
            this.dataModel = dataModel;
            this.validate = validate;
            this.smartPK = smartPK;
            this.inputStream = inputStream;
            this.userName = userName;
            this.password = password;
            this.transactionId = transactionId;
            this.universe = universe;
            this.startedBulkloadCount = startedBulkloadCount;
        }

        public void run() {
            try {
                startedBulkloadCount.incrementAndGet();
                bulkload(url,
                        cluster,
                        concept,
                        dataModel,
                        validate,
                        smartPK,
                        inputStream,
                        userName,
                        password,
                        transactionId,
                        universe);
            } catch (Throwable e) {
                inputStream.reportFailure(e);
            } finally {
                startedBulkloadCount.decrementAndGet();
                synchronized (startedBulkloadCount) {
                    startedBulkloadCount.notifyAll();
                }
            }
        }
    }
}
