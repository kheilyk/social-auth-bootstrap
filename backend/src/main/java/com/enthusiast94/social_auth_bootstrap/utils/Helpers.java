package com.enthusiast94.social_auth_bootstrap.utils;

import com.enthusiast94.social_auth_bootstrap.models.AccessToken;
import com.enthusiast94.social_auth_bootstrap.services.AccessTokenService;
import org.apache.commons.codec.digest.DigestUtils;
import spark.Request;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.halt;

/**
 * Created by ManasB on 7/28/2015.
 */
public class Helpers {

    public static HashMap<String, String> bodyParams(String body) throws UnsupportedEncodingException {
        body = URLDecoder.decode(body, "UTF-8");

        HashMap<String, String> bodyParams = new HashMap<>();

        String[] split = body.split("&");
        for (String el : split) {
            String[] split2 = el.split("=");
            if (split2.length == 2) {
                bodyParams.put(split2[0], split2[1]);
            } else {
                bodyParams.put(split2[0], "");
            }
        }

        return bodyParams;
    }

    public static String stringifyParams(Map<String, String> queryParams) {
        int pos = 0;

        String paramsString = "";

        for (String key : queryParams.keySet()) {
            if (pos == 0) {
                paramsString  += key + "=" + queryParams.get(key);
            } else {
                paramsString  += "&" + key + "=" + queryParams.get(key);
            }

            pos++;
        }

        return paramsString;
    }

    public static String httpGet(String urlString, Map<String, String> queryParams, Map<String, String> headers) throws Exception {
        // add query params to url
        urlString += "?" + stringifyParams(queryParams);

        // setup url connection
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        if (headers != null) {
            for (String key : headers.keySet()) {
                conn.setRequestProperty(key, headers.get(key));
            }
        }

        // read response
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();

        String line = "";
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();

        return response.toString();
    }

    public static String httpPost(String urlString, Map<String, String> postParams) throws Exception {
        // setup url connection
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        // convert post params to string
        String postData = stringifyParams(postParams);

        // write post data to connection
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(postData);
        writer.close();

        // read response
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();

        String line = "";
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();

        return response.toString();
    }

    /**
     * Helper method that checks if a valid access token is provided. Also appends the access token object to Request
     * attributes.
     */
    public static void requireAuthentication(Request req, AccessTokenService accessTokenService) {
        String authHeader = req.headers("Authorization");

        if (authHeader == null) {
            halt(new ApiResponse(401, "Authorization header not found.", null).toJson());
            return;
        }

        if (!authHeader.startsWith("Token")) {
            halt(new ApiResponse(401, "Invalid access token.", null).toJson());
            return;
        }

        if (authHeader.length() < (36 /* Access Token is 36 characters long */ + "Token".length() + 1 /* SPACE after 'Token' */)) {
            halt(new ApiResponse(401, "Invalid access token.", null).toJson());
            return;
        }

        String accessTokenValue = authHeader.substring("Token".length()+1, authHeader.length());
        AccessToken accessToken = accessTokenService.getAccessTokenByValue(accessTokenValue);

        if (accessToken == null) {
            halt(new ApiResponse(401, "Invalid access token.", null).toJson());
            return;
        }

        // add access token to attributes list so that it can be reused by other routes
        req.attribute("accessToken", accessToken);
    }

    public static String getGravatar(String email) {
        return "http://www.gravatar.com/avatar/" + DigestUtils.md5Hex(email.toLowerCase());
    }
}
