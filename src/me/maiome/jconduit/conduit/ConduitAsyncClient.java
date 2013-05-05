package me.maiome.jconduit.conduit;

import java.util.*;
import me.maiome.jconduit.json.*;
import me.maiome.jconduit.util.*;

public abstract class ConduitAsyncClient extends Thread {

    // max number of wait retries; data should be received fairly quickly, so this should be fairly low.
    private static final int MAX_RETRIES = 10;
    // delay between attempts to finish call
    private static final int CALL_COMPLETION_DELAY = 500;
    // connection lock
    private static final Object threadConnectionLock = new Object();
    // our event timer
    private static final Timer callTimer = new Timer("ConduitAsyncClient Call Completion", true);
    // connection information
    protected String clientUsername;
    protected String clientCertificate;
    protected String clientApiUrl;
    protected String method;
    protected Map<String, Object> argMap = new HashMap<String, Object>();
    private boolean ready = false;
    // conduit client instance
    protected ConduitClient conduitClient;
    // async state variables
    private boolean completed = false;
    private JSONObject response = null;

    // waiting task for call completion
    private TimerTask waitingTask = new TimerTask() {
        int waitRetries = 0;

        @Override
        public void run() {
            if (completed) {
                onFetchCompleted();
            } else if (!completed) {
                if (this.waitRetries >= MAX_RETRIES) {
                    onFetchTimeout();
                    return;
                }
                scheduleWaitingTask();
                this.waitRetries++;
            }
        }
    };

    /**
     * Constructs the async pseudo-client.
     *
     * @params String method, Map<String, Object> argMap
     */
    public ConduitAsyncClient(String method, Map<String, Object> argMap) {
        this.method = method;
        this.argMap = argMap;
    }

    /**
     * Schedules the call completion task with our timer, according to CALL_COMPLETION_DELAY.
     *
     * @params none
     * @returns none
     */
    private void scheduleWaitingTask() {
        callTimer.schedule(this.waitingTask, CALL_COMPLETION_DELAY);
    }

    /**
     * Sets the connection information that we give to the ConduitClient for communication.
     *
     * @params String username, String certificate, String apiUrl
     * @returns none
     */
    public void setConnectionInfo(String username, String certificate, String apiUrl) {
        this.clientUsername = username;
        this.clientCertificate = certificate;
        this.clientApiUrl = apiUrl;
        this.ready = true;
    }

    /**
     * Returns the response that should have been set in the overridden run() method.
     *
     * @params none
     * @returns JSONObject
     */
    public JSONObject getResponse() {
        return this.response;
    }

    /**
     * Sets the response and applies the flag that signals the completion call. This *MUST* be run
     * for the whole system to work correctly! Otherwise, the completion callback will never be reached and the
     * timeout callback will most likely run.
     *
     * @params JSONObject response
     * @returns none
     */
    public void setResponse(JSONObject obj) {
        this.response = obj;
        this.completed = true;
    }

    /**
     * Runs the thread. Does some checking, and then starts the waiting task.
     *
     * @params none
     * @returns none
     */
    @Override
    public synchronized void start() {
        if (!ready) {
            throw new IllegalArgumentException("You must provide a username, certificate, and Conduit API url to connect.");
        }
        this.conduitClient = ConduitClient.fromCertificate(this.clientUsername, this.clientCertificate, this.clientApiUrl);
        synchronized(threadConnectionLock) {
            System.out.println("Running overridden start.."); // debug
            super.start();
            this.scheduleWaitingTask();
        }
    }

    /**
     * This is just a very basic Conduit data fetcher. Will work with most Conduit methods that return data in +respObj['result']+.
     * This CAN be safely overridden to create a more complex data fetcher.
     *
     * @params none
     * @returns none
     */
    @Override
    public void run() {
        if (this.conduitClient == null) {
            setResponse(null);
        }
        try {
            JSONObject respObj = this.conduitClient.call(method, argMap);
            setResponse(respObj);
        } catch (ConduitException e) {
            Map<String, Object> exceptionMap = new HashMap<String, Object>();
            exceptionMap.put("exception", e.toString());
            JSONObject exceptionResp = new JSONObject(exceptionMap);
            setResponse(exceptionResp);
            return;
        }
    }

    /**
     * [ABSTRACT] Conduit call completion callback. Override this to provide a data handler to run after the data is successfully retrieved.
     * This *MUST* be overridden.
     *
     * @params none
     * @returns none
     */
    public abstract void onFetchCompleted();

    /**
     * [ABSTRACT] Conduit call timeout callback. Override this to provide an error handler in the case that the conduit call times out.
     * This *MUST* be overridden.
     *
     * @params none
     * @returns none
     */
    public abstract void onFetchTimeout();
}