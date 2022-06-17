package es.rickyepoderi.apacheds;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.LimitExceededException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author rmartinc
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "myDS",
        partitions = {
            @CreatePartition(name = "test", suffix = "dc=rickyepoderi,dc=es")
        })
@CreateLdapServer(
        transports = {
            @CreateTransport(protocol = "LDAP", address = "localhost", port = 10389)}
)
@ApplyLdifFiles({"data.ldif"})
public class ReferralsTest extends AbstractLdapTestUnit {

    private static final int REFERRAL_LIMIT = 5;

    @Test
    public void test() throws Exception {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://localhost:10389");
        env.put("com.sun.jndi.ldap.read.timeout", "5000");
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put(Context.REFERRAL, "follow");
        env.put("java.naming.ldap.referral.limit", Integer.toString(REFERRAL_LIMIT));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
        env.put(Context.SECURITY_CREDENTIALS, "secret");
        LdapContext ctx = null;
        int results = 0;
        try {
            System.err.print("# Connecting as manager... ");
            ctx = new InitialLdapContext(env, null);
            System.err.println("OK");
            System.err.println("# Searching using base search entry...");
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setReturningAttributes(new String[0]);
            NamingEnumeration<SearchResult> ne = ctx.search("ou=People1,dc=rickyepoderi,dc=es", "(objectclass=inetorgperson)", sc);
            while (ne.hasMore()) {
                results++;
                SearchResult sr = ne.next();
                System.out.println("dn: " + sr.getNameInNamespace());
                NamingEnumeration<? extends Attribute> attrs = sr.getAttributes().getAll();
                while (attrs.hasMore()) {
                    Attribute attr = attrs.next();
                    NamingEnumeration<?> values = attr.getAll();
                    while (values.hasMore()) {
                        Object value = values.next();
                        System.out.println(attr.getID() + ": " + value);
                    }
                }
                if (ne.hasMore()) {
                    System.out.println("");
                }                
            }
        } catch (LimitExceededException e) {
            MatcherAssert.assertThat("java.naming.ldap.referral.limit not ensured",
                        results, Matchers.is(REFERRAL_LIMIT + 1));
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }
}
