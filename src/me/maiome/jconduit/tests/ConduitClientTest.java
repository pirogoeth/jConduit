package me.maiome.jconduit.tests;

import me.maiome.jconduit.conduit.*;
import me.maiome.jconduit.json.*;
import me.maiome.jconduit.util.*;

import java.io.*;
import java.security.*;
import java.net.*;
import java.util.*;

public class ConduitClientTest {

    private ConduitClient client;
    private String apiURL = "http://phabricator.maio.me/api/";
    private String username = "jc-test";
    private String certificate = "o3ngxzajwysw4utrjiigpt4ael65jejrsymrlcwjjuwantec3iz4xa7xcnyyh6stvater7sbrudfp5q6h2zcpeoqakectbq6jvu53t6vc7vsaxsg7radyjcoi37eakyztg6mpxosi37upnwzgawx3jiy3wbh2yhbjmpt7pd76oezwsmcnodffei5mld52eesbdnsrd6p5kpysec5gyb273s5dwigikchfz7jx7eighbovjwot7vz6vfus5zqvda";
    private String provisionId = "5jejkfktqkhv4mlqb37a";
    private String provisionPhid = "PHID-PROV-5jejkfktqkhv4mlqb37a";
    private String resolvePhid = "PHID-USER-957e147c72cc5f1fc255";
    private String[] resolvePhids = {"PHID-USER-957e147c72cc5f1fc255", "PHID-USER-nstulhan5psvbqhpmhh4"};
    private String filePhid = "PHID-FILE-l3egvph7n6nxnfqp4rnl";

    public ConduitClientTest() {
        ConduitUtil.setApiUrl(this.apiURL);
        ConduitUtil.setUsername(this.username);
        ConduitUtil.setCertificate(this.certificate);
        this.client = ConduitClient.fromCertificate(this.username, this.certificate, this.apiURL);
    }

    public String getClientID() {
        String clientID = "";
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            String macAddr = ConduitClient.byteMap(ni.getHardwareAddress());
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(macAddr.getBytes());
            clientID = ConduitClient.byteMap(md.digest());
        } catch (java.lang.Exception e) {
            return "";
        }
        return clientID;
    }

    public void runConduitTests() {
        JSONObject respObj;
        System.out.println("==================== Running ConduitClient Tests ====================");
        // Test 1: Ping conduitAPI.
        System.out.println(" 1. Pinging ConduitAPI...");
        try {
            respObj = this.client.call("conduit.ping", new HashMap<String, Object>());
            respObj = this.client.getPreviousResponse();
            System.out.println(" => Conduit response: " + respObj.getString("result"));
        } catch (ConduitException e) {
            e.printStackTrace();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
        // Test 2: fetch userinfo.
        System.out.println(" 2. Fetching user information...");
        try {
            respObj = this.client.call("user.whoami", new HashMap<String, Object>());
            System.out.println(" => I am logged in as '" + respObj.getString("userName") + "'.");
            System.out.println(" => My real name is '" + respObj.getString("realName") + "'.");
        } catch (ConduitException e) {
            e.printStackTrace();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
        // Test 3: fetch cct provision info. (may not be applicable to all phab. instances)
        System.out.println(" 3. Contacting provisioning API...");
        System.out.println(" => Trying to store CID...");
        try {
            Map<String, Object> argMap = new HashMap<String, Object>();
            argMap.put("provision_phid_fragment", this.provisionId);
            argMap.put("client_id", this.getClientID());
            respObj = this.client.call("provision.storecid", argMap);
            try {
                String result = Boolean.toString(respObj.getBoolean("success"));
                System.out.println(" ==> API returned: " + result);
            } catch (java.lang.Exception e) {
                e.printStackTrace();
            }
        } catch (ConduitException e) {
            System.out.println(" ==> " + e.getMessage());
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
        System.out.println(" => Trying to validate provision...");
        try {
            Map<String, Object> argMap = new HashMap<String, Object>();
            argMap.put("provision_phid_fragment", this.provisionId);
            argMap.put("client_id", this.getClientID());
            respObj = this.client.call("provision.validate", argMap);
            try {
                String result = Boolean.toString(respObj.getBoolean("success"));
                System.out.println(" ==> API returned: " + result);
            } catch (java.lang.Exception e) {
                e.printStackTrace();
            }
        } catch (ConduitException e) {
            System.out.println(" ==> " + e.getMessage());
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
        System.out.println(" 4. Trying to resolve a UserPHID..");
        try {
            String resolvedName = ConduitUtil.resolvePhabricatorUserPHID(this.resolvePhid);
            System.out.println(" ==> " + this.resolvePhid + " is " + resolvedName + ".");
        } catch (java.lang.Exception e) {
            System.out.println(" ==> " + e.getMessage());
        }
        System.out.println(" 5. Trying to resolve some UserPHIDs..");
        try {
            List<String> resolvedNames = ConduitUtil.resolvePhabricatorUserPHIDs(this.resolvePhids);
            for (int i = 0; i < resolvedNames.size(); i++) {
                System.out.println(" ==> " + this.resolvePhids[i] + " is " + resolvedNames.get(i));
            }
        } catch (java.lang.Exception e) {
            System.out.println(" ==> " + e.getMessage());
        }
        System.out.println(" 6. Trying to resolve a bad UserPHID..");
        try {
            String resolvedName = ConduitUtil.resolvePhabricatorUserPHID(this.provisionPhid);
            if (resolvedName.equals("")) {
                System.out.println(" ==> Pass!");
            } else {
                System.out.println(" ==> Fail!");
            }
        } catch (java.lang.Exception e) {
            System.out.println(" ==> " + e.getMessage());
        }
        System.out.println(" 7. Trying to download a file..");
        try {
            File downloadedFile = ConduitUtil.downloadFile(this.filePhid, "/tmp/jConduit-logo.png");
            if (downloadedFile.length() != 253309) {
                System.out.println(" => Fail!");
            } else {
                System.out.println(" => Success!");
            }
            downloadedFile.deleteOnExit();
        } catch (java.lang.Exception e) {
            System.out.println(" ==> " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ConduitClientTest cct = new ConduitClientTest();
        cct.runConduitTests();
    }
}