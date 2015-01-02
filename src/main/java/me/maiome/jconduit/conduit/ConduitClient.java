package me.maiome.jconduit.conduit;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import me.maiome.jconduit.json.*;

/**
 * Provides a client that can easily connect to Phabricator's conduit API and run methods with a map of arguments.
 */
public class ConduitClient {

    /**
     * The URL of the Conduit API that the client will attempt to connect to.
     */
    public final String apiURL;

    /**
     * This will hold the conduit session key after authentication.
     */
    private String sessionKey;

    /**
     * This will hold the ID of the current connection.
     */
    private int connectionID;

    /**
     * Keeps track of if we're authenticated or not.
     */
    private boolean authenticated;

    /**
     * Holds the previously retrieved response object.
     */
    private JSONObject previousResponse = new JSONObject();

    /**
     * Defaults constructor.
     *
     * @params none
     */
    private ConduitClient() {
        this("http://phabricator.maio.me/api", "", -1);
    }

    /**
     * Constructor for an unauthenticated conduit client.
     *
     * @params String apiURL
     */
    private ConduitClient(final String apiURL) {
        this(apiURL, "", -1);
    }

    /**
     * Constructor for a fully authenticated conduit client.
     * 
     * @params String apiURL, String sessionKey, int connectionID
     */
    private ConduitClient(final String apiURL, final String sessionKey, final int connectionID) {
        this.apiURL = apiURL;
        this.sessionKey = sessionKey;
        this.connectionID = connectionID;

        this.authenticated = (sessionKey != null && connectionID != -1);
    }

    /**
     * Maps byte arrays into a String; useful for making SHA hashes from MessageDigest more friendly.
     * 
     * @params byte[] bytes
     * @returns String
     */
    public static String byteMap(byte[] bytes) {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            data.append(String.format("%02x", bytes[i]));
        }
        return data.toString();
    }

    /**
     * Retrieves the user's certificate from the conduit API, given the user's token.
     * Returns a certificate map that is usable by #fromCertificateMap()
     * 
     * @params String token, String apiURL
     * @returns Map<String, Object>
     */
    public static Map<String, Object> getCertificate(String token, String apiURL) {
        Map<String, Object> argMap = new HashMap<String, Object>();
        argMap.put("host", apiURL);
        argMap.put("token", token);

        ConduitClient client;
        JSONObject response = new JSONObject();

        try {
            client = new ConduitClient(apiURL);
            response = client.call("conduit.getcertificate", argMap);
        } catch (ConduitException e) {
            e.printStackTrace();
            return new HashMap<String, Object>();
        }

        Map<String, Object> resultMap = new HashMap<String, Object>();

        try {
            resultMap.put("username", response.getString("username"));
            resultMap.put("certificate", response.getString("certificate"));
        } catch (java.lang.Exception e) {
            return new HashMap<String, Object>();
        }

        return resultMap;
    }

    /**
     * Creates an authenticated conduit client with a username and certificate.
     * 
     * @params String username, String certificate, String apiURL
     * @returns ConduitClient
     */
    public static ConduitClient fromCertificate(final String username, final String certificate, final String apiURL) {
        ConduitClient client = new ConduitClient(apiURL);
        long time = System.currentTimeMillis() / 1000;

        String authSignature = "";

        try {
            MessageDigest tokenDigest = MessageDigest.getInstance("SHA-1");
            tokenDigest.update((Long.toString(time) + certificate).getBytes());
            authSignature = ConduitClient.byteMap(tokenDigest.digest());
        } catch (java.lang.Exception e) { }

        Map<String, Object> handshakeArgs = new HashMap<String, Object>();
        handshakeArgs.put("client", "jconduit");
        handshakeArgs.put("clientVersion", 6);
        handshakeArgs.put("user", username);
        handshakeArgs.put("authToken", time);
        handshakeArgs.put("authSignature", authSignature);
        handshakeArgs.put("host", apiURL);

        JSONObject response;

        try {
            response = client.call("conduit.connect", handshakeArgs);
            return new ConduitClient(apiURL, response.getString("sessionKey"), response.getInt("connectionID"));
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a conduit client from the information in a certificate map.
     * 
     * @params Map<String, Object> certificateMap, String apiURL
     * @returns ConduitClient
     */
    public static ConduitClient fromCertificateMap(final Map<String, Object> certificateMap, final String apiURL) {
        return fromCertificate((String) certificateMap.get("username"), (String) certificateMap.get("certificate"), apiURL);
    }

    /**
     * Reads all data from a URLConnection and returns it as a String.
     * 
     * @params URLConnection uc
     * @returns String
     */
    private String readAllFromURLConnection(URLConnection uc) {
        try {
            StringBuilder data = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String input;
            while ((input = in.readLine()) != null) {
                data.append(input);
            }
            in.close();
            return data.toString();
        } catch (java.lang.Exception e) {
            return "";
        }
    }

    /**
     * Returns the previously retrieved result object.
     * 
     * @params none
     * @returns JSONObject
     */
    public JSONObject getPreviousResponse() {
        return this.previousResponse;
    }

    /**
     * Runs a conduit call with the given method and arguments. Will attach session information if found.
     * 
     * @params String method, Map<String, Object> argumentMap
     * @returns JSONObject
     */
    public JSONObject call(String method, Map<String, Object> argMap) throws ConduitException {
        if (this.authenticated) {
            Map<String, Object> authMap = new HashMap<String, Object>();
            authMap.put("sessionKey", this.sessionKey);
            authMap.put("connectionID", this.connectionID);
            argMap.put("__conduit__", authMap);
        }

        JSONObject argObject = new JSONObject(argMap);

        try {
            return this.call(method, argObject);
        } catch (ConduitException e) {
            throw e;
        }
    }

    /**
     * Actual internal call to conduit to send the args and receive the response.
     * 
     * @params String method, JSONObject args
     * @returns JSONObject
     */
    private JSONObject call(String method, JSONObject args) throws ConduitException {
        try {
            URL url = new URL(this.apiURL + method);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write("params=" + args.toString());
            out.close();

            String data = this.readAllFromURLConnection(connection);
            JSONObject respObj = this.previousResponse = new JSONObject(data);
            if (respObj.isNull("result") && !(respObj.isNull("error_code"))) {
                throw new ConduitException(respObj.getString("error_code"), respObj.getString("error_info"));
            }
            return respObj.getJSONObject("result");
        } catch (ConduitException e) {
            throw e;
        } catch (java.lang.Exception e) {
            return new JSONObject();
        }
    }
}
