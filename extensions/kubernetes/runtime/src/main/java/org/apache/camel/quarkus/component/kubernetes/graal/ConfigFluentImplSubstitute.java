/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.kubernetes.graal;

import java.util.List;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.fabric8.kubernetes.client.ConfigFluentImplConfigurer;
import org.apache.camel.CamelContext;
import org.apache.camel.support.component.PropertyConfigurerSupport;

@TargetClass(ConfigFluentImplConfigurer.class)
final class ConfigFluentImplSubstitute {

    @Substitute
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        io.fabric8.kubernetes.client.ConfigBuilder target = (io.fabric8.kubernetes.client.ConfigBuilder) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "apiversion":
        case "ApiVersion":
            target.withApiVersion(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "authprovider":
        case "AuthProvider":
            target.withAuthProvider(PropertyConfigurerSupport.property(camelContext,
                    io.fabric8.kubernetes.api.model.AuthProviderConfig.class, value));
            return true;
        case "autoconfigure":
        case "AutoConfigure":
            throw new RuntimeException("Not supported in the native mode!");
        case "cacertdata":
        case "CaCertData":
            target.withCaCertData(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "cacertfile":
        case "CaCertFile":
            target.withCaCertFile(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "clientcertdata":
        case "ClientCertData":
            target.withClientCertData(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "clientcertfile":
        case "ClientCertFile":
            target.withClientCertFile(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "clientkeyalgo":
        case "ClientKeyAlgo":
            target.withClientKeyAlgo(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "clientkeydata":
        case "ClientKeyData":
            target.withClientKeyData(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "clientkeyfile":
        case "ClientKeyFile":
            target.withClientKeyFile(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "clientkeypassphrase":
        case "ClientKeyPassphrase":
            target.withClientKeyPassphrase(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "connectiontimeout":
        case "ConnectionTimeout":
            target.withConnectionTimeout(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "contexts":
        case "Contexts":
            target.withContexts(PropertyConfigurerSupport.property(camelContext, java.util.List.class, value));
            return true;
        case "currentcontext":
        case "CurrentContext":
            target.withCurrentContext(PropertyConfigurerSupport.property(camelContext,
                    io.fabric8.kubernetes.api.model.NamedContext.class, value));
            return true;
        case "customheaders":
        case "CustomHeaders":
            target.withCustomHeaders(PropertyConfigurerSupport.property(camelContext, java.util.Map.class, value));
            return true;
        case "defaultnamespace":
        case "DefaultNamespace":
            target.withDefaultNamespace(PropertyConfigurerSupport.property(camelContext, boolean.class, value));
            return true;
        case "disablehostnameverification":
        case "DisableHostnameVerification":
            target.withDisableHostnameVerification(PropertyConfigurerSupport.property(camelContext, boolean.class, value));
            return true;
        case "errormessages":
        case "ErrorMessages":
            target.withErrorMessages(PropertyConfigurerSupport.property(camelContext, java.util.Map.class, value));
            return true;
        case "file":
        case "File":
            throw new RuntimeException("Not supported in the native mode!");
        case "http2disable":
        case "Http2Disable":
            target.withHttp2Disable(PropertyConfigurerSupport.property(camelContext, boolean.class, value));
            return true;
        case "httpproxy":
        case "HttpProxy":
            target.withHttpProxy(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "httpsproxy":
        case "HttpsProxy":
            target.withHttpsProxy(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "impersonateextras":
        case "ImpersonateExtras":
            target.withImpersonateExtras(PropertyConfigurerSupport.property(camelContext, java.util.Map.class, value));
            return true;
        case "impersonategroups":
        case "ImpersonateGroups":
            target.withImpersonateGroups(PropertyConfigurerSupport.property(camelContext, java.lang.String[].class, value));
            return true;
        case "impersonateusername":
        case "ImpersonateUsername":
            target.withImpersonateUsername(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "keystorefile":
        case "KeyStoreFile":
            target.withKeyStoreFile(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "keystorepassphrase":
        case "KeyStorePassphrase":
            target.withKeyStorePassphrase(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "logginginterval":
        case "LoggingInterval":
            target.withLoggingInterval(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "masterurl":
        case "MasterUrl":
            target.withMasterUrl(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "maxconcurrentrequests":
        case "MaxConcurrentRequests":
            target.withMaxConcurrentRequests(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "maxconcurrentrequestsperhost":
        case "MaxConcurrentRequestsPerHost":
            target.withMaxConcurrentRequestsPerHost(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "namespace":
        case "Namespace":
            target.withNamespace(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "noproxy":
        case "NoProxy":
            target.withNoProxy(PropertyConfigurerSupport.property(camelContext, java.lang.String[].class, value));
            return true;
        case "oauthtoken":
        case "OauthToken":
            target.withOauthToken(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "oauthtokenprovider":
        case "OauthTokenProvider":
            target.withOauthTokenProvider(PropertyConfigurerSupport.property(camelContext,
                    io.fabric8.kubernetes.client.OAuthTokenProvider.class, value));
            return true;
        case "password":
        case "Password":
            target.withPassword(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "proxypassword":
        case "ProxyPassword":
            target.withProxyPassword(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "proxyusername":
        case "ProxyUsername":
            target.withProxyUsername(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "requestretrybackoffinterval":
        case "RequestRetryBackoffInterval":
            target.withRequestRetryBackoffInterval(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "requestretrybackofflimit":
        case "RequestRetryBackoffLimit":
            target.withRequestRetryBackoffLimit(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "requesttimeout":
        case "RequestTimeout":
            target.withRequestTimeout(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "rollingtimeout":
        case "RollingTimeout":
            target.withRollingTimeout(PropertyConfigurerSupport.property(camelContext, long.class, value));
            return true;
        case "scaletimeout":
        case "ScaleTimeout":
            target.withScaleTimeout(PropertyConfigurerSupport.property(camelContext, long.class, value));
            return true;
        case "tlsversions":
        case "TlsVersions":
            target.withTlsVersions(PropertyConfigurerSupport.property(camelContext,
                    io.fabric8.kubernetes.client.http.TlsVersion[].class, value));
            return true;
        case "trustcerts":
        case "TrustCerts":
            target.withTrustCerts(PropertyConfigurerSupport.property(camelContext, boolean.class, value));
            return true;
        case "truststorefile":
        case "TrustStoreFile":
            target.withTrustStoreFile(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "truststorepassphrase":
        case "TrustStorePassphrase":
            target.withTrustStorePassphrase(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "uploadconnectiontimeout":
        case "UploadConnectionTimeout":
            target.withUploadConnectionTimeout(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "uploadrequesttimeout":
        case "UploadRequestTimeout":
            target.withUploadRequestTimeout(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "useragent":
        case "UserAgent":
            target.withUserAgent(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "username":
        case "Username":
            target.withUsername(PropertyConfigurerSupport.property(camelContext, java.lang.String.class, value));
            return true;
        case "watchreconnectinterval":
        case "WatchReconnectInterval":
            target.withWatchReconnectInterval(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "watchreconnectlimit":
        case "WatchReconnectLimit":
            target.withWatchReconnectLimit(PropertyConfigurerSupport.property(camelContext, int.class, value));
            return true;
        case "websocketpinginterval":
        case "WebsocketPingInterval":
            target.withWebsocketPingInterval(PropertyConfigurerSupport.property(camelContext, long.class, value));
            return true;
        case "websockettimeout":
        case "WebsocketTimeout":
            target.withWebsocketTimeout(PropertyConfigurerSupport.property(camelContext, long.class, value));
            return true;
        default:
            return false;
        }
    }

    @Substitute
    public Object getOptionValue(Object obj, String name, boolean ignoreCase) {
        io.fabric8.kubernetes.client.ConfigBuilder target = (io.fabric8.kubernetes.client.ConfigBuilder) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "apiversion":
        case "ApiVersion":
            return target.getApiVersion();
        case "authprovider":
        case "AuthProvider":
            return target.getAuthProvider();
        case "autoconfigure":
        case "AutoConfigure":
            throw new RuntimeException("Not supported in the native mode!");
        case "cacertdata":
        case "CaCertData":
            return target.getCaCertData();
        case "cacertfile":
        case "CaCertFile":
            return target.getCaCertFile();
        case "clientcertdata":
        case "ClientCertData":
            return target.getClientCertData();
        case "clientcertfile":
        case "ClientCertFile":
            return target.getClientCertFile();
        case "clientkeyalgo":
        case "ClientKeyAlgo":
            return target.getClientKeyAlgo();
        case "clientkeydata":
        case "ClientKeyData":
            return target.getClientKeyData();
        case "clientkeyfile":
        case "ClientKeyFile":
            return target.getClientKeyFile();
        case "clientkeypassphrase":
        case "ClientKeyPassphrase":
            return target.getClientKeyPassphrase();
        case "connectiontimeout":
        case "ConnectionTimeout":
            return target.getConnectionTimeout();
        case "contexts":
        case "Contexts":
            return target.getContexts();
        case "currentcontext":
        case "CurrentContext":
            return target.getCurrentContext();
        case "customheaders":
        case "CustomHeaders":
            return target.getCustomHeaders();
        case "defaultnamespace":
        case "DefaultNamespace":
            return target.isDefaultNamespace();
        case "disablehostnameverification":
        case "DisableHostnameVerification":
            return target.isDisableHostnameVerification();
        case "errormessages":
        case "ErrorMessages":
            return target.getErrorMessages();
        case "file":
        case "File":
            throw new RuntimeException("Not supported in the native mode!");
        case "http2disable":
        case "Http2Disable":
            return target.isHttp2Disable();
        case "httpproxy":
        case "HttpProxy":
            return target.getHttpProxy();
        case "httpsproxy":
        case "HttpsProxy":
            return target.getHttpsProxy();
        case "impersonateextras":
        case "ImpersonateExtras":
            return target.getImpersonateExtras();
        case "impersonategroups":
        case "ImpersonateGroups":
            return target.getImpersonateGroups();
        case "impersonateusername":
        case "ImpersonateUsername":
            return target.getImpersonateUsername();
        case "keystorefile":
        case "KeyStoreFile":
            return target.getKeyStoreFile();
        case "keystorepassphrase":
        case "KeyStorePassphrase":
            return target.getKeyStorePassphrase();
        case "logginginterval":
        case "LoggingInterval":
            return target.getLoggingInterval();
        case "masterurl":
        case "MasterUrl":
            return target.getMasterUrl();
        case "maxconcurrentrequests":
        case "MaxConcurrentRequests":
            return target.getMaxConcurrentRequests();
        case "maxconcurrentrequestsperhost":
        case "MaxConcurrentRequestsPerHost":
            return target.getMaxConcurrentRequestsPerHost();
        case "namespace":
        case "Namespace":
            return target.getNamespace();
        case "noproxy":
        case "NoProxy":
            return target.getNoProxy();
        case "oauthtoken":
        case "OauthToken":
            return target.getOauthToken();
        case "oauthtokenprovider":
        case "OauthTokenProvider":
            return target.getOauthTokenProvider();
        case "password":
        case "Password":
            return target.getPassword();
        case "proxypassword":
        case "ProxyPassword":
            return target.getProxyPassword();
        case "proxyusername":
        case "ProxyUsername":
            return target.getProxyUsername();
        case "requestretrybackoffinterval":
        case "RequestRetryBackoffInterval":
            return target.getRequestRetryBackoffInterval();
        case "requestretrybackofflimit":
        case "RequestRetryBackoffLimit":
            return target.getRequestRetryBackoffLimit();
        case "requesttimeout":
        case "RequestTimeout":
            return target.getRequestTimeout();
        case "rollingtimeout":
        case "RollingTimeout":
            return target.getRollingTimeout();
        case "scaletimeout":
        case "ScaleTimeout":
            return target.getScaleTimeout();
        case "tlsversions":
        case "TlsVersions":
            return target.getTlsVersions();
        case "trustcerts":
        case "TrustCerts":
            return target.isTrustCerts();
        case "truststorefile":
        case "TrustStoreFile":
            return target.getTrustStoreFile();
        case "truststorepassphrase":
        case "TrustStorePassphrase":
            return target.getTrustStorePassphrase();
        case "uploadconnectiontimeout":
        case "UploadConnectionTimeout":
            return target.getUploadConnectionTimeout();
        case "uploadrequesttimeout":
        case "UploadRequestTimeout":
            return target.getUploadRequestTimeout();
        case "useragent":
        case "UserAgent":
            return target.getUserAgent();
        case "username":
        case "Username":
            return target.getUsername();
        case "watchreconnectinterval":
        case "WatchReconnectInterval":
            return target.getWatchReconnectInterval();
        case "watchreconnectlimit":
        case "WatchReconnectLimit":
            return target.getWatchReconnectLimit();
        case "websocketpinginterval":
        case "WebsocketPingInterval":
            return target.getWebsocketPingInterval();
        case "websockettimeout":
        case "WebsocketTimeout":
            return target.getWebsocketTimeout();
        default:
            return null;
        }
    }
}
