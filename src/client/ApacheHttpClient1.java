package client;
import java.io.*;
import java.net.*;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
 
/**
 * A simple Java REST GET example using the Apache HTTP library.
 * This executes a call against the Yahoo Weather API service, which is
 * actually an RSS service (<a href="http://developer.yahoo.com/weather/" title="http://developer.yahoo.com/weather/">http://developer.yahoo.com/weather/</a>).
 * 
 * Try this Twitter API URL for another example (it returns JSON results):
 * <a href="http://search.twitter.com/search.json?q=%40apple" title="http://search.twitter.com/search.json?q=%40apple">http://search.twitter.com/search.json?q=%40apple</a>
 * (see this url for more twitter info: <a href="https://dev.twitter.com/docs/using-search" title="https://dev.twitter.com/docs/using-search">https://dev.twitter.com/docs/using-search</a>)
 * 
 * Apache HttpClient: <a href="http://hc.apache.org/httpclient-3.x/" title="http://hc.apache.org/httpclient-3.x/">http://hc.apache.org/httpclient-3.x/</a>
 *
 */
public class ApacheHttpClient1 {
 
  public static void main(String[] args) {
    DefaultHttpClient httpclient = new DefaultHttpClient();
    try {
      // specify the host, protocol, and port
      HttpHost target = new HttpHost("127.0.0.1", 4321, "http");
      DataOutputStream socketOut = new DataOutputStream(client.getOutputStream());
      DataInputStream  socketIn  = new DataInputStream(client.getInputStream());
      DataInputStream  console   = new DataInputStream(System.in);
      System.out.println("Connected to " + host + ". Enter text:");

      // specify the get request
      HttpGet getRequest = new HttpGet("/forecastrss?p=80020&u=f");
 
      System.out.println("executing request to " + target);
      System.out.println("hahahaha1");

      HttpResponse httpResponse = httpclient.execute(target, getRequest);
      System.out.println("hahahaha");
      HttpEntity entity = httpResponse.getEntity();
 
      System.out.println("----------------------------------------");
      System.out.println(httpResponse.getStatusLine());
      Header[] headers = httpResponse.getAllHeaders();
      for (int i = 0; i < headers.length; i++) {
        System.out.println(headers[i]);
      }
      System.out.println("----------------------------------------");
 
      if (entity != null) {
        System.out.println(EntityUtils.toString(entity));
      }
 
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // When HttpClient instance is no longer needed,
      // shut down the connection manager to ensure
      // immediate deallocation of all system resources
      httpclient.getConnectionManager().shutdown();
    }
  }
}