package ml.alternet.security.web.jetty.auth;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.jaas.JAASUserPrincipal;
import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.eclipse.jetty.jaas.callback.RequestParameterCallback;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import ml.alternet.security.auth.Credentials;

public class EnhancedJaasLoginService extends JAASLoginService {

    private static final Logger LOG = Log.getLogger(EnhancedJaasLoginService.class);

    @Override
    public UserIdentity login(String username, Object credentials) {
        List<EnhancedPasswordCallback> epcList = new ArrayList<>();
        try {
            CallbackHandler callbackHandler = null;
            if (_callbackHandlerClass == null) {
                callbackHandler = new CallbackHandler() {
                    @Override
                    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                        for (int i = 0; i < callbacks.length ; i++ ) {
                            Callback callback = callbacks[i];
//                            if (callback instanceof CredentialsCallback) {
//                                ((CredentialsCallback) callback).setCryptFormat(getCryptFormats());
//                            }
                            if (! (callback instanceof EnhancedPasswordCallback)
                                    && callback instanceof PasswordCallback) {
                                PasswordCallback pc = (PasswordCallback) callback;
                                if (credentials instanceof Credentials) {
                                    EnhancedPasswordCallback epc = new EnhancedPasswordCallback(pc.getPrompt(), pc.isEchoOn());
                                    callbacks[i] = epc;
                                    callback = epc;
                                    epcList.add(epc);
                                } else {
                                    ((PasswordCallback)callback).setPassword(credentials.toString().toCharArray());
                                }
                            }
                            if (callback instanceof NameCallback) {
                                ((NameCallback)callback).setName(username);
                            } else if (callback instanceof EnhancedPasswordCallback) {
                                Credentials c = (Credentials) credentials;
                                ((EnhancedPasswordCallback)callback).setPassword(c.getPassword());
                            } else if (callback instanceof ObjectCallback) {
                                ((ObjectCallback)callback).setObject(credentials);
                            } else if (callback instanceof RequestParameterCallback) {
                                HttpChannel<?> channel = HttpChannel.getCurrentHttpChannel();
                                if (channel == null) {
                                    return;
                                }
                                Request request = channel.getRequest();
                                if (request != null) {
                                    RequestParameterCallback rpc = (RequestParameterCallback)callback;
                                    rpc.setParameterValues(Arrays.asList(request.getParameterValues(rpc.getParameterName())));
                                }
                            }
                            else {
                                throw new UnsupportedCallbackException(callback);
                            }
                        }
                    }
                };
            } else {
                @SuppressWarnings("unchecked")
                Class<CallbackHandler> clazz = Loader.loadClass(getClass(), _callbackHandlerClass);
                callbackHandler = clazz.newInstance();
            }
            //set up the login context
            //TODO jaspi requires we provide the Configuration parameter
            Subject subject = new Subject();
            LoginContext loginContext = new LoginContext(_loginModuleName, subject, callbackHandler);

            loginContext.login();

            //login success
            JAASUserPrincipal userPrincipal = new JAASUserPrincipal(username, subject, loginContext);
            subject.getPrincipals().add(userPrincipal);

            return _identityService.newUserIdentity(subject,userPrincipal,getGroups(subject));
        } catch (LoginException e) {
            LOG.warn(e);
        } catch (InstantiationException e) {
            LOG.warn(e);
        } catch (IllegalAccessException e) {
            LOG.warn(e);
        } catch (ClassNotFoundException e) {
            LOG.warn(e);
        } finally {
            for (EnhancedPasswordCallback epc: epcList) {
                epc.destroy();
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String[] getGroups (Subject subject) {
        //get all the roles of the various types
        String[] roleClassNames = getRoleClassNames();
        Collection<String> groups = new LinkedHashSet<>();
        try
        {
            for (String roleClassName : roleClassNames)
            {
                Class load_class = Thread.currentThread().getContextClassLoader().loadClass(roleClassName);
                Set<Principal> rolesForType = subject.getPrincipals(load_class);
                for (Principal principal : rolesForType)
                {
                    groups.add(principal.getName());
                }
            }
            return groups.toArray(new String[groups.size()]);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

}
