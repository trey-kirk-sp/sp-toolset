package sailpoint.scratch;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import sailpoint.api.SailPointContext;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.tools.Brand;
import sailpoint.tools.BrandingService;
import sailpoint.tools.BrandingServiceFactory;
import sailpoint.tools.GeneralException;

public class ScratchPad {
    
    private SailPointContext context = null;

    boolean reportObjectEquals (Object obj1, Object obj2, StringBuffer buff) {
        boolean isEqual = obj1 == obj2;
        buff.append("obj1 (").append(obj1).append(") == obj2 (").append(obj2).append(") ").append(isEqual).append("\n");
        return isEqual;
    }

    void reportDeepEquals (ProvisioningPlan plan1, ProvisioningPlan plan2, StringBuffer buff) {
        if (!reportObjectEquals(plan1, plan2, buff)) {
            List accts1 = plan1.getAccountRequests();
            List accts2 = plan2.getAccountRequests();
            int minSize = Math.min(accts1.size(), accts2.size());
            for (int i = 0; i < minSize; i++) {
                AccountRequest acct1 = (AccountRequest) accts1.get(i);
                AccountRequest acct2 = (AccountRequest) accts2.get(i);
                reportDeepEquals(acct1, acct2, buff);
            }
        }
    }

    void reportDeepEquals(AccountRequest acct1, AccountRequest acct2, StringBuffer buff) {
        if (!reportObjectEquals(acct1, acct2, buff)) {
            List attrs1 = acct1.getAttributeRequests();
            List attrs2 = acct2.getAttributeRequests();
            int minSize = Math.min(attrs1.size(), attrs2.size());
            for (int i = 0; i < minSize; i++) {
                AttributeRequest attr1 = (AttributeRequest) attrs1.get(i);
                AttributeRequest attr2 = (AttributeRequest) attrs2.get(i);
                reportDeepEquals(attr1, attr2, buff);
            }
        }
        
    }

    void reportDeepEquals(AttributeRequest attr1, AttributeRequest attr2, StringBuffer buff) {
        reportObjectEquals(attr1, attr2, buff);
        
    }

    public Object ScratchPad() throws GeneralException {
        StringBuilder sb = new StringBuilder();
        final Pattern STRIP_BAD_CHARS = Pattern.compile("[^a-zA-Z0-9.-]+");

        try {
            String appName;
            BrandingService bs = BrandingServiceFactory.getService();
            if (bs.getBrand() == Brand.AGS) {
                appName = bs.getApplicationShortName().toLowerCase();
            } else {
                appName = bs.getApplicationName().toLowerCase();
            }
            String efixFile = getFacesContext().getExternalContext().getRealPath("WEB-INF/efixes");
            if (efixFile != null) {
                File efixDir = new File(efixFile);
                if (efixDir != null && efixDir.isDirectory()) {
                    File[] efixFiles = efixDir.listFiles();
                    for (File efix:efixFiles) {
                        String efixName = STRIP_BAD_CHARS.matcher(efix.getName()).replaceAll("");
                        if (efix.isFile() && efixName.toLowerCase().startsWith(appName + "-") && efixName.toLowerCase().endsWith(".txt")) {
                            efixName = efixName.substring(0, efixName.lastIndexOf("."));
                            if (! efixName.isEmpty()) {
                                sb.append(efixName);
                                sb.append("<br />");
                            }
                        }
                    }
                }
            }
            if (sb.length() > 0) {
                return(sb.toString());
            }
        } catch (Exception e) {
            _log.warn("Error getting efix list", e);
        }
        return "None";

    }

}
