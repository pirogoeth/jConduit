package me.maiome.jconduit.util;

import java.util.*;

import me.maiome.jconduit.conduit.*;
import me.maiome.jconduit.json.*;

public class ConduitUtil {

    /**
     * API URL that all of these utility methods that need to access an instance will use.
     */
    private static String apiUrl = "http://phabricator.maio.me/api";

    /**
     * The username that the client will connect with.
     */
    private static String username = "";

    /**
     * The certificate that the client with authenticate with.
     */
    private static String certificate = "";

    /**
     * Sets the url that many of these utility methods will use.
     *
     * @params String apiUrl
     * @returns void
     */
    public static void setApiUrl(String newApiUrl) {
        apiUrl = newApiUrl;
    }

    /**
     * Returns the URL that these utility methods will be using.
     *
     * @returns String apiUrl
     */
    public static String getApiUrl() {
        return apiUrl;
    }

    /**
     * Sets the username to connect with.
     *
     * @params String username
     * @returns void
     */
    public static void setUsername(String newUsername) {
        username = newUsername;
    }

    /**
     * Gets the username that the utils will connect with.
     *
     * @returns String username
     */
    public static String getUsername() {
        return username;
    }

    /**
     * Sets the certificate to connect with.
     *
     * @params String certificate
     * @returns void
     */
    public static void setCertificate(String newCertificate) {
        certificate = newCertificate;
    }

    /**
     * Gets the certificate that the utils will connect with.
     *
     * @returns String certificate
     */
    public static String getCertificate() {
        return certificate;
    }

    /**
     * Counts the number of occurences of a character in a string.
     *
     * @params String haystack
     * @params char needle
     * @returns int count
     */
    public static int countCharacterOccurences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle) {
                count++;
            }
        }

        return count;
    }

    /**
     * Gets the PHID tag from the PHID.
     *
     * The PHID tag is the middle portion of the PHID which specifies
     * what application uses it.
     *
     * @params String PHID
     * @returns String PHID tag
     */
    public static String getPHIDTag(String PHID) {
        return PHID.split("\\-")[1];
    }

    /**
     * Resolves a users PHID and returns their username.
     *
     * @params String PHID
     * @returns String username
     */
    public static String resolvePhabricatorUserPHID(String PHID) {
        try {
            ConduitClient client = ConduitClient.fromCertificate(getUsername(), getCertificate(), getApiUrl());
            List<String> phidList = new ArrayList<String>();
            phidList.add(PHID);
            Map<String, Object> argMap = new HashMap<String, Object>();
            argMap.put("phids", phidList);
            JSONObject respObj = client.call("user.query", argMap);
            respObj = client.getPreviousResponse();
            return ((JSONObject) respObj.getJSONArray("result").get(0)).getString("userName");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Resolves a number of user PHIDs and returns an indexed array of their usernames.
     *
     * @params String[] PHIDs
     * @returns List<String> usernames
     */
    public static List<String> resolvePhabricatorUserPHIDs(String...phidArray) {
        try {
            List<String> usernames = new ArrayList<String>();
            for (int i = 0; i < phidArray.length; i++) {
                usernames.add(i, resolvePhabricatorUserPHID(phidArray[i]));
            }
            return usernames;
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            return new ArrayList<String>();
        }
    }
}