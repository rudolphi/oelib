/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package common.system.utils;

import common.formatting.StringFormatters;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

/**
 * Class that lets the server download WWW documents and perform HTTP requests
 * like a client, used for screenscraping. Use
 * {@link common.system.utils.HttpDownload} instead.
 *
 * @author fr
 */
@Deprecated()
public class DownloadByServer implements IFHttpDownload {

	public static final int STATUS_NOT_INITED = 0;
	public static final int STATUS_SUCCESS = 1;
	public static final int STATUS_FAILURE = 2;
	public static final String FILENAME_PREFIX = "filename=";
	public static final String GET = "GET";
	public static final String POST = "POST";
	public static final String PUT = "PUT";
	public static final String DELETE = "DELETE";
	// fields
	private String url;
	private URI uri;
	private HttpClient httpclient;
	private HttpRequestBase request;
	private HttpResponse response;
	private HttpEntity entity;
	private final Map<String, String> cookiesToSend;
	private Map<String, String> redirCookies;
	private Map<String, String> receivedCookies;
	private String receivedCookieString;
	private InputStream inputStream;
	private String data;
	private List<String> redirectChain;
	private byte[] bindata;
	private int status = STATUS_NOT_INITED;
	private String referer;
	private StatusLine statusLine;

	protected DownloadByServer(Object urlObject, Map<String, String> cookiesToSend, String referer, List<String> redirectChain) {
		// must use factory methods
		processURL(urlObject);
		this.referer = referer;
		this.cookiesToSend = cookiesToSend;
		this.redirectChain = redirectChain;
	}

	public static DownloadByServer perform(Object urlObject) {
		return perform(urlObject, (Map) null, (String) null);
	}

	public static DownloadByServer perform(Object urlObject,
			String referer) {
		return perform(urlObject, (Map) null, referer);
	}

	public static DownloadByServer perform(Object urlObject,
			Map<String, String> cookies) {
		return perform(urlObject, cookies, (String) null);
	}

	public static DownloadByServer perform(Object urlObject,
			Map<String, String> cookies,
			String referer) {
		return perform(urlObject, cookies, referer, null, null);
	}

	public static DownloadByServer perform(Object urlObject,
			Map<String, String> cookies,
			String referer, Map<String, Object> options) {
		return perform(urlObject, cookies, referer, null, null, options);
	}

	private static DownloadByServer perform(Object urlObject,
			Map<String, String> cookies,
			String referer,
			List<String> redirectChain) {
		return perform(urlObject, cookies, referer, null, null, redirectChain);
	}

	public static DownloadByServer perform(Object urlObject,
			Map<String, String> cookies,
			String referer, String username, String password) {
		return performRequest(GET, urlObject, null, cookies, null, null, referer, null, username, password, null, null);
	}

	public static DownloadByServer perform(Object urlObject,
			Map<String, String> cookies,
			String referer, String username, String password, Map<String, Object> options) {
		return performRequest(GET, urlObject, null, cookies, null, null, referer, null, username, password, options, null);
	}

	private static DownloadByServer perform(Object urlObject,
			Map<String, String> cookies,
			String referer, String username, String password,
			List<String> redirectChain) {
		return performRequest(GET, urlObject, null, cookies, null, null, referer, null, username, password, null, redirectChain);
	}

	private static DownloadByServer perform(Object urlObject,
			Map<String, String> cookies,
			String referer, String username, String password, Map<String, Object> options,
			List<String> redirectChain) {
		return performRequest(GET, urlObject, null, cookies, null, null, referer, null, username, password, options, redirectChain);
	}

	public static DownloadByServer performDelete(Object urlObject,
			Map<String, String> cookies,
			String referer, String username, String password) {
		return performRequest(DELETE, urlObject, null, cookies, null, null, referer, null, username, password, null, null);
	}

	public static DownloadByServer performPost(Object urlObject,
			Map<String, String> data) {
		return performPost(urlObject, data, (String) null);
	}

	public static DownloadByServer performPost(Object urlObject,
			Map<String, String> data,
			String referer) {
		return performPost(urlObject, data, (Map) null);
	}

	public static DownloadByServer performPost(Object urlObject,
			Map<String, String> data, Map<String, String> cookies) {
		return performPost(urlObject, data, cookies, (Map) null);
	}

	public static DownloadByServer performPost(Object urlObject,
			Map<String, String> data, Map<String, String> cookies,
			String referer) {
		return performPost(urlObject, data, cookies, null, referer);
	}

	public static DownloadByServer performPost(Object urlObject,
			Map<String, String> data, Map<String, String> cookies, Map<String, ByteArrayBody> files) {
		return performPost(urlObject, data, cookies, files, (String) null);
	}

	public static DownloadByServer performPost(Object urlObject,
			Map<String, String> data, Map<String, String> cookies, Map<String, ByteArrayBody> files,
			String referer) {
		return performPost(urlObject, data, cookies, files, referer, null, null);
	}

	public static DownloadByServer performPost(Object urlObject,
			Map<String, String> data, Map<String, String> cookies, Map<String, ByteArrayBody> files,
			String referer, String username, String password) {
		return performRequest(POST, urlObject, data, cookies, files, null, null, referer, null, username, password);
	}

	public static DownloadByServer performPost(Object urlObject, String postBody, ContentType postContentType, Map<String, String> cookies, String referer, Map<String, String> headerLines) {
		return performRequest(POST, urlObject, null, cookies, null, postBody, postContentType, referer, headerLines, null, null);
	}

	public static DownloadByServer performSoap(Object urlObject, String soapAction, String soapData) {
		try {
			return performSoap(urlObject, soapAction, new StringEntity(soapData));
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(DownloadByServer.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	public static DownloadByServer performSoap(Object urlObject, String soapAction, byte[] soapData) {
		return performSoap(urlObject, soapAction, new ByteArrayEntity(soapData));
	}

	public static DownloadByServer performSoap(Object urlObject, String soapAction, InputStream soapData) {
		return performSoap(urlObject, soapAction, new InputStreamEntity(soapData));
	}

	public static DownloadByServer performSoap(Object urlObject, String soapAction, HttpEntity soapData) {
		DownloadByServer retval = new DownloadByServer(urlObject, null, null, null);

		try {
			HttpPost request = new HttpPost(retval.uri);
			request.setHeader("Content-Type", "application/soap+xml; charset=utf-8");
			request.setHeader("SOAPAction", soapAction);
			if (!soapData.isRepeatable()) {
				soapData = new BufferedHttpEntity(soapData);
			}
			request.setEntity(soapData);

			retval.performRequest(request, null, null, null);

			return retval;
		} catch (Exception ex) {
			Logger.getLogger(DownloadByServer.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	public static DownloadByServer performJson(Object urlObject, String jsonString, Map<String, String> cookies, String referer, Map<String, String> headerLines) {
		return performRequest(POST, urlObject, null, cookies, null, jsonString, null, referer, headerLines, null, null);
	}

	public static DownloadByServer performPut(Object urlObject, Map<String, String> cookies, InputStream fileContent, String username, String password) {
		DownloadByServer retval = new DownloadByServer(urlObject, cookies, null, null);

		try {
			HttpPut request = new HttpPut(retval.uri);

			request.setEntity(new BufferedHttpEntity(new InputStreamEntity(fileContent)));

			retval.addHeaderLinesToRequest(request);
			retval.performRequest(request, username, password, null);

			return retval;
		} catch (Exception ex) {
			Logger.getLogger(DownloadByServer.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	private static DownloadByServer performRequest(String method, Object urlObject,
			Map<String, String> data, Map<String, String> cookies, Map<String, ByteArrayBody> files, String jsonString, ContentType postContentType,
			String referer, Map<String, String> headerLines, String username, String password) {
		return performRequest(method, urlObject, data, cookies, files, jsonString, postContentType, referer, headerLines, username, password, null, null);
	}

	private static DownloadByServer performRequest(String method, Object urlObject,
			Map<String, String> data, Map<String, String> cookies, Map<String, ByteArrayBody> files, String postBody,
			String referer, Map<String, String> headerLines, String username, String password, Map<String, Object> options,
			List<String> redirectChain) {
		return performRequest(method, urlObject, data, cookies, files, postBody, null, referer, headerLines, username, password, options, redirectChain);
	}

	private static DownloadByServer performRequest(String method, Object urlObject,
			Map<String, String> data, Map<String, String> cookies, Map<String, ByteArrayBody> files, String postBody, ContentType postContentType,
			String referer, Map<String, String> headerLines, String username, String password, Map<String, Object> options,
			List<String> redirectChain) {
		DownloadByServer retval = new DownloadByServer(urlObject, cookies, referer, redirectChain);

		try {
			HttpRequestBase request;
			if (GET.equals(method)) {
				request = new HttpGet(retval.uri);
			} else if (DELETE.equals(method)) {
				request = new HttpDelete(retval.uri);
			} else if (POST.equals(method)) {
				HttpPost httpPost = new HttpPost(retval.uri);
				request = httpPost;

				if (files != null) {
					// POST and
					MultipartEntity mpEntity = new MultipartEntity();
					if (data != null) {
						for (Map.Entry<String, String> entry : data.entrySet()) {
							mpEntity.addPart(entry.getKey(), new StringBody(entry.getValue()));
						}
					}
					// also FILE
					for (Map.Entry<String, ByteArrayBody> entry : files.entrySet()) {
						mpEntity.addPart(entry.getKey(), entry.getValue());
					}
					httpPost.setEntity(mpEntity);
				} else if (postBody != null) {
					if (postContentType == null) {
						postContentType = ContentType.APPLICATION_JSON;
					}
					StringEntity formData = new StringEntity(postBody, postContentType);
					httpPost.setEntity(formData);
				} else {
					// normal POST
					List<NameValuePair> nvps = new ArrayList<NameValuePair>();
					if (data != null) {
						for (Map.Entry<String, String> entry : data.entrySet()) {
							nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
						}
					}

					UrlEncodedFormEntity formData = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
					httpPost.setEntity(formData);
				}
			} else {
				return null;
			}

			retval.addHeaderLinesToRequest(request);

			if (headerLines != null) {
				for (Map.Entry<String, String> entry : headerLines.entrySet()) {
					request.setHeader(entry.getKey(), entry.getValue());
				}
			}

			retval.performRequest(request, username, password, options);
			Header newLocation = retval.getFirstHeader("location");
			if (newLocation != null) {
				// add any new cookies or overwrite existing ones
				Map<String, String> receivedCookies = retval.getCookiesHashMap();
				if (cookies == null) {
					cookies = receivedCookies;
				} else if (receivedCookies != null) {
					cookies.putAll(receivedCookies);
				}
				// use GET on new location
				String newLocationValue = newLocation.getValue();
				try {
					URL newUrl = new URL(retval.uri.toURL(), newLocationValue);
					return perform(newUrl, cookies, referer);
				} catch (Exception ex) {
					return perform(newLocationValue, cookies, referer, retval.redirectChain);
				}
			}

			return retval;
		} catch (Exception ex) {
			Logger.getLogger(DownloadByServer.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	public static String encodeURL(String location) {
		System.out.println("Navigating to " + location);
		String newLocation = StringFormatters.fixUrlEncoding(location);
		if (!newLocation.equals(location)) {
			System.out.println("Fix URL to " + newLocation);
		}
		return newLocation;
	}

	private void processURL(Object urlObject) {
		if (urlObject instanceof URI) {
			uri = (URI) urlObject;
			url = uri.toString();
		} else {
			if (urlObject instanceof String) {
				url = encodeURL((String) urlObject);
			} else if (urlObject instanceof URL) {
				url = ((URL) urlObject).toString();
			} else {
				throw new UnsupportedOperationException("urlObject must be String or URL.");
			}
			uri = URI.create(url);
		}
		initRedirChain();
		redirectChain.add(url);
	}

	private void initRedirChain() {
		if (redirectChain == null) {
			// begin redirect tracking
			redirectChain = new ArrayList<String>();
		}
	}

	public Header[] getHeaders(String name) {
		if (response != null) {
			return response.getHeaders(name);
		}
		return new Header[0];
	}

	public Header getFirstHeader(String name) {
		if (response != null) {
			return response.getFirstHeader(name);
		}
		return null;
	}

	public Header getLastHeader(String name) {
		if (response != null) {
			return response.getLastHeader(name);
		}
		return null;
	}

	public List<String> getRedirectChain() {
		return redirectChain;
	}

	@Override
	public String getUrl() {
		return url;
	}

	public String getLastUrl() {
		if (redirectChain != null) {
			int size = redirectChain.size();
			if (size > 0) {
				return redirectChain.get(size - 1);
			}
		}
		return url;
	}

	private void performRequest(HttpRequestBase request, String username, String password, Map<String, Object> options) {
		this.request = request;
		try {
			this.httpclient = HttpDownload.fac.getDefaultHttpClient(request.getURI()); // URI is used to determine which proxy to use

			HttpClient realClient = getRealHttpClient();
			if (username != null && password != null && realClient instanceof DefaultHttpClient) {
				DefaultHttpClient dHttpclient = (DefaultHttpClient) realClient;
				CredentialsProvider credentialsProvider = dHttpclient.getCredentialsProvider();
				credentialsProvider.setCredentials(
						new AuthScope(uri.getHost(), uri.getPort()),
						new UsernamePasswordCredentials(username, password));
			}

			if (options != null) {
				HttpParams params = httpclient.getParams();
				for (Map.Entry<String, Object> entry : options.entrySet()) {
					params.setParameter(entry.getKey(), entry.getValue());
				}
			}

			response = httpclient.execute(request);
			statusLine = response.getStatusLine();
			entity = response.getEntity();

			// get cookies from redirect
			if (realClient instanceof AbstractHttpClient) {
				RedirectStrategy redirectStrategy = ((AbstractHttpClient) realClient).getRedirectStrategy();
				if (redirectStrategy instanceof TolerantRedirectStrategy) {
					TolerantRedirectStrategy tolerantRedirectStrategy = (TolerantRedirectStrategy) redirectStrategy;
					List<String> redirChain = tolerantRedirectStrategy.getRedirectChain();
					if (redirChain != null) {
						initRedirChain();
						redirectChain.addAll(redirChain);
					}
					redirCookies = tolerantRedirectStrategy.getReceivedCookies();
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
			status = STATUS_FAILURE;
		}
	}

	protected HttpClient getRealHttpClient() {
		if (httpclient instanceof DecompressingHttpClient) {
			return ((DecompressingHttpClient) httpclient).getHttpClient();
		}
		return httpclient;
	}

	private void addHeaderLinesToRequest(HttpRequestBase request) {
		// required by Kinesis
		request.setHeader(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
//		request.setHeader(new BasicHeader("Accept-Encoding", "gzip, deflate"));
		// get German texts
		request.setHeader(new BasicHeader("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3"));
		if (referer != null) {
			request.setHeader(new BasicHeader("Referer", referer));
		}
		addCookiesToRequest(request);
	}

	private void addCookiesToRequest(HttpRequestBase request) {
		if (MapUtils.isEmpty(cookiesToSend)) {
			return;
		}
		// write cookie header
		List<String> cookieNvps = new ArrayList<String>();
		for (Map.Entry<String, String> entry : cookiesToSend.entrySet()) {
			cookieNvps.add(entry.getKey() + "=" + entry.getValue());
		}
		request.setHeader(new BasicHeader("Cookie", StringUtils.join(cookieNvps, ";")));
	}

	public Map<String, String> getCookiesHashMap() {
		if (receivedCookies == null) {
			getCookiesBackend();
		}
		return receivedCookies;
	}

	public String getCookiesString() {
		if (receivedCookieString == null) {
			getCookiesBackend();
		}
		return receivedCookieString;
	}

	protected static void addCookiesToMap(HttpResponse response, Map<String, String> receivedCookies) {
		if (response != null) {
			Header[] headers = response.getHeaders("Set-Cookie");
			for (int i = 0; i < headers.length; i++) {
				Header header = headers[i];
				String cookieNvp = StringUtils.substringBefore(header.getValue(), ";");
				receivedCookies.put(
						StringUtils.substringBefore(cookieNvp, "="),
						StringUtils.substringAfter(cookieNvp, "="));
			}
		}
	}

	private void getCookiesBackend() {
		this.receivedCookies = new HashMap<String, String>();

		// pre-set cookies
		if (cookiesToSend != null) {
			receivedCookies.putAll(cookiesToSend);
		}

		// collect all cookies from redirects
		if (redirCookies != null) {
			receivedCookies.putAll(redirCookies);
		}

		// parse cookies from header
		addCookiesToMap(response, receivedCookies);

		// avoid doubles
		List<String> receivedCookieList = new ArrayList<String>();
		for (Map.Entry<String, String> entry : receivedCookies.entrySet()) {
			receivedCookieList.add(entry.getKey() + "=" + entry.getValue());
		}
		this.receivedCookieString = StringUtils.join(receivedCookieList, ";");
	}

	public InputStream getInputStream() {
		if (status == STATUS_FAILURE // failed
				|| data != null || bindata != null) { // too late
			return null;
		}
		if (inputStream == null) {
			try {
				inputStream = entity.getContent();
			} catch (IOException ex) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
			} catch (IllegalStateException ex) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
			}
		}
		return inputStream;
	}

	@Override
	public byte[] getBindata() {
		return getBindata(Long.MAX_VALUE);
	}

	public byte[] getBindata(long maxSize) {
		if (data != null) {
			return data.getBytes();
		}
		if (bindata == null) {
			long copied = 0;
			InputStream is = null;
			try {
				is = getInputStream();
				if (is != null) {
					// code copied to inline, to realize size limit
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					copied = IOUtils.copyLarge(is, baos, 0, maxSize);
					bindata = baos.toByteArray();

					status = STATUS_SUCCESS;
				} else {
					status = STATUS_FAILURE;
				}
			} catch (Exception ex) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
				status = STATUS_FAILURE;
			} finally {
				closeConnection(is, copied >= maxSize);
			}
		}
		return bindata;
	}

	@Override
	public String getData() {
		return getData(null);
	}

	public String getData(long maxSize) {
		return getData(null, maxSize);
	}

	public String getData(String encoding) {
		return getData(encoding, Long.MAX_VALUE);
	}

	public String getData(String encoding, long maxSize) {
		if (bindata != null) {
			if (encoding == null) {
				return new String(bindata);
			}

			try {
				return new String(bindata, encoding);
			} catch (UnsupportedEncodingException ex) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
			}
		}
		if (data == null) {
			long copied = 0;
			InputStream is = null;
			try {
				is = getInputStream();
				if (is != null) {
					Charset charset = Charsets.toCharset(encoding);
					if (maxSize < Long.MAX_VALUE) {
						// code copied to inline, to realize size limit
						StringBuilderWriter sw = new StringBuilderWriter();
						InputStreamReader in = new InputStreamReader(is, charset);
						copied = IOUtils.copyLarge(in, sw, 0, maxSize);
						data = sw.toString();
					} else {
						// more efficient
						data = IOUtils.toString(is, charset);
						copied = maxSize;
					}
					status = STATUS_SUCCESS;
				} else {
					status = STATUS_FAILURE;
				}
			} catch (Exception ex) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
				status = STATUS_FAILURE;
			} finally {
				closeConnection(is, copied >= maxSize);
			}
		}
		return data;
	}

	protected void closeConnection(final InputStream is, boolean doAbort) {
		if (doAbort) {
			// otherwise close will read infinitely
			request.abort();
			HttpClient realHttpClient = getRealHttpClient();
			if (realHttpClient instanceof CloseableHttpClient) {
				try {
					((CloseableHttpClient) realHttpClient).close();
				} catch (IOException ex) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
				}
			}
		}

		IOUtils.closeQuietly(is);
		request.releaseConnection();
	}

	@Override
	public String getMimeType() {
		return StringUtils.substringBefore(entity.getContentType().getValue(), ";"); // remove any charset info if present
	}

	@Override
	public String getFilename() {
		Header lastHeader = this.response.getLastHeader("Content-Disposition");
		if (lastHeader != null) {
			String filenameByServer = lastHeader.getValue();
			if (filenameByServer != null && !filenameByServer.isEmpty() && filenameByServer.contains(FILENAME_PREFIX)) {
				// cut away
				filenameByServer = StringUtils.substringAfter(filenameByServer, FILENAME_PREFIX);

				// unquote if required
				filenameByServer = StringFormatters.removeStart(filenameByServer, "\"", "\'");
				filenameByServer = StringFormatters.removeEnd(filenameByServer, "\"", "\'");

				return filenameByServer;
			}
		}
		// cutAway everything before last / and after first ?
		return StringFormatters.cutAwaySearch(StringFormatters.cutAwayPath(this.url));
	}

	@Override
	public int getStatus() {
		return status;
	}

	public boolean isFailed() {
		if (status == STATUS_FAILURE) {
			return true;
		}
		int statusCode = statusLine.getStatusCode();
		return (statusCode >= 400 && statusCode < 600);
	}

	public int getStatusCode() {
		if (statusLine != null) {
			return statusLine.getStatusCode();
		}
		return -1;
	}

	public String getStatusText() {
		if (statusLine != null) {
			return statusLine.getReasonPhrase();
		}
		return null;
	}
}
