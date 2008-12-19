package onepoint.project.modules.external_applications;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import onepoint.project.modules.external_applications.exceptions.OpExternalApplicationException;
import onepoint.project.util.Pair;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

public class OpWebApplicationConnection {
   
   public static InputStream sendHttpRequest(String url,
         List<Pair<String, String>> parameters, int timeout)
         throws OpExternalApplicationException {
      
      HttpClient client = new HttpClient();
      HttpClientParams clientParams = new HttpClientParams();
      clientParams.setSoTimeout(timeout);
      clientParams.setConnectionManagerTimeout(timeout);
      client.setParams(clientParams);
      
      GetMethod method = new GetMethod(url);
      
      NameValuePair[] params = new NameValuePair[parameters.size()];
      Iterator<Pair<String, String>> pit = parameters.iterator();
      int i = 0;
      while (pit.hasNext()) {
         Pair<String, String> p = pit.next();
         params[i] = new NameValuePair(p.getFirst(), p.getSecond());
         i++;
      }
      
      method.setQueryString(params);  
      int responseCode;
      try {
         String x = method.getQueryString();
         responseCode = client.executeMethod(method);
      } catch (HttpException e) {
         throw new OpExternalApplicationException(OpExternalApplicationException.HTTP_EXCEPTION, "", url, e);
      } catch (IOException e) {
         throw new OpExternalApplicationException(OpExternalApplicationException.REQUEST_IO_EXCEPTION, "", url, e);
      }
      if (responseCode == HttpStatus.SC_OK) {
         try {
            return method.getResponseBodyAsStream();
         } catch (IOException e) {
            throw new OpExternalApplicationException(OpExternalApplicationException.RESPONSE_IO_EXCEPTION, "", url, e);
         }
      }
      else {
         throw new OpExternalApplicationException(OpExternalApplicationException.CONNECTION_EXCEPTION, "", method.getStatusLine().toString());
      }
   }

}
