/* 
 * HttpRequestProxy.java 
 * 
 * Created on November 3, 2008, 9:53 AM 
 */  
  
package cn.com.mozat.net;  
  
import java.io.BufferedReader;  
import java.io.IOException;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.util.HashMap;  
import java.util.Iterator;  
import java.util.Map;  
import java.util.Set;  
  
import org.apache.commons.httpclient.Header;  
import org.apache.commons.httpclient.HttpClient;  
import org.apache.commons.httpclient.HttpException;  
import org.apache.commons.httpclient.HttpMethod;  
import org.apache.commons.httpclient.NameValuePair;  
import org.apache.commons.httpclient.SimpleHttpConnectionManager;  
import org.apache.commons.httpclient.methods.GetMethod;  
import org.apache.commons.httpclient.methods.PostMethod;  
  
import cn.com.mozat.exception.CustomException;  
  
/** 
 *  
 * @author bird  email:lihongfu-84@163.com 
 * 
 * 2008-11-4  09:49:48 
 */  
public class HttpRequestProxy{  
    //��ʱ���  
    private static int connectTimeOut = 60000;  
 //��connectionmanager����httpclientconnectionʱ�Ƿ�ر�����  
    private static boolean alwaysClose = false;  
 //�������ݱ����ʽ  
    private String encoding = "UTF-8";  
      
    private final HttpClient client = new HttpClient(new SimpleHttpConnectionManager(alwaysClose));  
   
    public HttpClient getHttpClient(){  
        return client;  
    }  
        
    /** 
     * �÷��� 
     * HttpRequestProxy hrp = new HttpRequestProxy(); 
     * hrp.doRequest("http://www.163.com",null,null,"gbk"); 
     *  
     * @param url  �������Դ�գң� 
     * @param postData  POST����ʱform����װ������ û��ʱ��null 
     * @param header   request����ʱ������ͷ��Ϣ(header) û��ʱ��null 
     * @param encoding response���ص���Ϣ�����ʽ û��ʱ��null 
     * @return  response���ص��ı����� 
     * @throws CustomException  
     */  
    public String doRequest(String url,Map postData,Map header,String encoding) throws CustomException{  
     String responseString = null;  
     //ͷ��������Ϣ  
     Header[] headers = null;  
     if(header != null){  
      Set entrySet = header.entrySet();  
         int dataLength = entrySet.size();  
          headers= new Header[dataLength];  
         int i = 0;  
         for(Iterator itor = entrySet.iterator();itor.hasNext();){  
          Map.Entry entry = (Map.Entry)itor.next();  
          headers[i++] = new Header(entry.getKey().toString(),entry.getValue().toString());  
         }  
     }  
     //post��ʽ  
        if(postData!=null){  
         PostMethod postRequest = new PostMethod(url.trim());  
         if(headers != null){  
          for(int i = 0;i < headers.length;i++){  
           postRequest.setRequestHeader(headers[i]);  
          }  
         }  
         Set entrySet = postData.entrySet();  
         int dataLength = entrySet.size();  
         NameValuePair[] params = new NameValuePair[dataLength];  
         int i = 0;  
         for(Iterator itor = entrySet.iterator();itor.hasNext();){  
          Map.Entry entry = (Map.Entry)itor.next();  
          params[i++] = new NameValuePair(entry.getKey().toString(),entry.getValue().toString());  
         }  
         postRequest.setRequestBody(params);  
         try {  
    responseString = this.executeMethod(postRequest,encoding);  
   } catch (CustomException e) {  
    throw e;  
   } finally{  
    postRequest.releaseConnection();  
   }  
        }  
      //get��ʽ  
        if(postData == null){  
         GetMethod getRequest = new GetMethod(url.trim());  
         if(headers != null){  
          for(int i = 0;i < headers.length;i++){  
           getRequest.setRequestHeader(headers[i]);  
          }  
         }  
         try {  
    responseString = this.executeMethod(getRequest,encoding);  
   } catch (CustomException e) {  
                e.printStackTrace();  
    throw e;  
   }finally{  
    getRequest.releaseConnection();  
   }  
        }  
   
        return responseString;  
    }  
  
 private String executeMethod(HttpMethod request, String encoding) throws CustomException{  
  String responseContent = null;  
  InputStream responseStream = null;  
  BufferedReader rd = null;  
  try {  
   this.getHttpClient().executeMethod(request);  
   if(encoding != null){  
    responseStream = request.getResponseBodyAsStream();  
     rd = new BufferedReader(new InputStreamReader(responseStream,  
                      encoding));  
              String tempLine = rd.readLine();  
              StringBuffer tempStr = new StringBuffer();  
              String crlf=System.getProperty("line.separator");  
              while (tempLine != null)  
              {  
                  tempStr.append(tempLine);  
                  tempStr.append(crlf);  
                  tempLine = rd.readLine();  
              }  
              responseContent = tempStr.toString();  
   }else  
    responseContent = request.getResponseBodyAsString();  
             
   Header locationHeader = request.getResponseHeader("location");  
   //���ش���Ϊ302,301ʱ����ʾҳ�漺���ض�������������location��url������  
   //һЩ��¼��Ȩȡcookieʱ����Ҫ  
   if (locationHeader != null) {  
             String redirectUrl = locationHeader.getValue();  
             this.doRequest(redirectUrl, null, null,null);  
         }  
  } catch (HttpException e) {  
   throw new CustomException(e.getMessage());  
  } catch (IOException e) {  
   throw new CustomException(e.getMessage());  
  
  } finally{  
   if(rd != null)  
    try {  
     rd.close();  
    } catch (IOException e) {  
     throw new CustomException(e.getMessage());  
    }  
    if(responseStream != null)  
     try {  
      responseStream.close();  
     } catch (IOException e) {  
      throw new CustomException(e.getMessage());  
  
     }  
  }  
  return responseContent;  
 }  
   
     
 /** 
  * ������������,�������������������redirect��������ֵݹ���ѭ���ض��� 
  * ���Ե���д��һ�����󷽷� 
  * �������������urlΪ��http://localhost:8080/demo/index.jsp 
  * ���ش���Ϊ302 ͷ����Ϣ��locationֵΪ:http://localhost:8083/demo/index.jsp 
  * ��ʱhttpclient��Ϊ����ݹ���ѭ���ض����׳�CircularRedirectException�쳣 
  * @param url 
  * @return 
  * @throws CustomException  
  */  
 public String doSpecialRequest(String url,int count,String encoding) throws CustomException{  
  String str = null;  
  InputStream responseStream = null;  
  BufferedReader rd = null;  
  GetMethod getRequest = new GetMethod(url);  
  //�ر�httpclient�Զ��ض�����  
  getRequest.setFollowRedirects(false);  
  try {  
     
   this.client.executeMethod(getRequest);  
   Header header = getRequest.getResponseHeader("location");  
   if(header!= null){  
    //�����ض����ģգң̣�countͬʱ��1  
    this.doSpecialRequest(header.getValue(),count+1, encoding);  
   }  
   //������count��Ϊ��־λ����countΪ0ʱ�ŷ�������ģգң��ı�,  
   //�����Ϳ��Ժ������еĵݹ��ض���ʱ�����ı����������������  
   if(count == 0){  
    getRequest = new GetMethod(url);  
    getRequest.setFollowRedirects(false);  
    this.client.executeMethod(getRequest);  
    responseStream = getRequest.getResponseBodyAsStream();  
    rd = new BufferedReader(new InputStreamReader(responseStream,  
                      encoding));  
             String tempLine = rd.readLine();  
             StringBuffer tempStr = new StringBuffer();  
             String crlf=System.getProperty("line.separator");  
             while (tempLine != null)  
             {  
                 tempStr.append(tempLine);  
                 tempStr.append(crlf);  
                 tempLine = rd.readLine();  
             }  
             str = tempStr.toString();  
   }  
     
  } catch (HttpException e) {  
   throw new CustomException(e.getMessage());  
  } catch (IOException e) {  
   throw new CustomException(e.getMessage());  
  } finally{  
   getRequest.releaseConnection();  
   if(rd !=null)  
    try {  
     rd.close();  
    } catch (IOException e) {  
     throw new CustomException(e.getMessage());  
    }  
    if(responseStream !=null)  
     try {  
      responseStream.close();  
     } catch (IOException e) {  
      throw new CustomException(e.getMessage());  
     }  
  }  
  return str;  
 }  
   
   
   
   
 public static void main(String[] args) throws Exception{  
  HttpRequestProxy hrp = new HttpRequestProxy();  
   Map header = new HashMap();  
         header.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 1.7; .NET CLR 1.1.4322; CIBA; .NET CLR 2.0.50727)");  
  String str = hrp.doRequest(  
    "http://www.cma-cgm.com/en/eBusiness/Tracking/Default.aspx?BolNumber=GZ2108827",  
     null, header,null);  
  System.out.println(str.contains("row_CRXU1587647"));  
//  System.out.println(str);  
 }  
     
}  