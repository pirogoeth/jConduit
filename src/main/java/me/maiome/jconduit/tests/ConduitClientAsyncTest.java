package me.maiome.jconduit.tests;

import me.maiome.jconduit.conduit.*;
import me.maiome.jconduit.json.*;
import me.maiome.jconduit.util.*;

import java.net.*;
import java.util.*;

public class ConduitClientAsyncTest {

    private ConduitAsyncClient client;
    private String apiUrl = "http://phabricator.maio.me/api/";
    private String username = "jc-test";
    private String certificate = "o3ngxzajwysw4utrjiigpt4ael65jejrsymrlcwjjuwantec3iz4xa7xcnyyh6stvater7sbrudfp5q6h2zcpeoqakectbq6jvu53t6vc7vsaxsg7radyjcoi37eakyztg6mpxosi37upnwzgawx3jiy3wbh2yhbjmpt7pd76oezwsmcnodffei5mld52eesbdnsrd6p5kpysec5gyb273s5dwigikchfz7jx7eighbovjwot7vz6vfus5zqvda";

    public ConduitClientAsyncTest() {
    }

    public void runConduitAsyncTests() {
        Map<String, Object> argMap = new HashMap<String, Object>();
        this.client = new ConduitAsyncClient("user.whoami", argMap) {
            @Override
            public void onFetchCompleted() {
                JSONObject respObj = getResponse();
                try {
                    System.out.println(" [*] I am logged in as '" + respObj.getString("userName") + "'.");
                    System.out.println(" [*] My real name is '" + respObj.getString("realName") + "'.");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFetchTimeout() {
                System.out.println(" [-] Fetch timed out!");
            }
        };
        this.client.setConnectionInfo(this.username, this.certificate, this.apiUrl);
        this.client.start();
        try {
            this.client.join();
        } catch (java.lang.Exception e) {
            System.out.println("Something happened -- could not join the client thread!");
            e.printStackTrace();
        }
    }
}