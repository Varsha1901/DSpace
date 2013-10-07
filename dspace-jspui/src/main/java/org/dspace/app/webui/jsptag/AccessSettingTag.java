/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Tag to display embargo settings
 *
 * @author Keiji Suzuki
 * @version $Revision$
 */
public class AccessSettingTag extends TagSupport
{
	/** log4j category */
    private static Logger log = Logger.getLogger(AccessSettingTag.class);

    /** is advanced form enabled? */
    private static final boolean advanced = ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

    /** Name of the restricted group */
    private static final String restrictedGroup = ConfigurationManager.getProperty("webui.submission.restrictstep.groups");

    /** the SubmittionInfo */
    private transient SubmissionInfo subInfo = null;

    /** the target DSpaceObject */
    private transient DSpaceObject dso = null;

    /** the target ResourcePolicy */
    private transient ResourcePolicy rp = null;

    /** disable the radio button for open/embargo access */
    private boolean embargo = false;

    /** hide the embargo date and reason fields */
    private boolean hidden = false;

    /** add the policy button */
    private boolean addpolicy = false;


    public AccessSettingTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
        String legend = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.legend");
        String label_name = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_name");
        String label_group = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_group");
        String label_embargo = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_embargo");
        String label_date = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_date");
        String radio0 = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.radio0");
        String radio1 = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.radio1");
        String radio_help = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.radio_help");
        String label_reason = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.label_reason");
        String button_confirm = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.access-setting.button_confirm");

        JspWriter out = pageContext.getOut();
        StringBuffer sb = new StringBuffer();

        try
        {
            HttpServletRequest hrq = (HttpServletRequest) pageContext.getRequest();
            Context context = UIUtil.obtainContext(hrq);
    
            // get startDate and reason of the resource policy of the target DSpaceObject
            List<ResourcePolicy> policies = null;
            if (!advanced && dso != null)
            {
                policies = AuthorizeManager.findPoliciesByDSOAndType(context, dso, ResourcePolicy.TYPE_CUSTOM);
            }
            else if (rp != null)
            {
                policies = new ArrayList<ResourcePolicy>();
                policies.add(rp);
            }

            String name = "";
            int group_id = 0; 
            String startDate = "";
            String reason = "";
            String radio0Checked = " checked=\"checked\"";
            String radio1Checked = "";
            String disabled      = " disabled=\"disabled\"";
            if (policies != null && policies.size() > 0)
            {
                ResourcePolicy rp = policies.get(0);
                name = (rp.getRpName() == null ? "" : rp.getRpName());
                group_id = rp.getGroup().getID();
                startDate = (rp.getStartDate() != null ? DateFormatUtils.format(rp.getStartDate(), "yyyy-MM-dd") : "");
                reason = (rp.getRpDescription() == null ? "" : rp.getRpDescription());
                if (!startDate.equals(""))
                {
                    radio0Checked = "";
                    radio1Checked = " checked=\"checked\"";
                    disabled      = "";
                }
            }

            // if advanced embargo is disabled, embargo date and reason fields are always enabled
            if (!advanced) {
                disabled = "";
            }

            sb.append("<center>\n");
            sb.append("<table class=\"miscTable\" width=\"80%\">\n");
            if (embargo)
            {
                // Name
                sb.append("<tr><th class=\"accessOdd\">").append(label_name).append("</th>\n");
                sb.append("<td class=\"accessOdd\">");
                sb.append("<input name=\"name\" id=\"policy_name\" type=\"text\" value=\"").append(name).append("\" />\n");
                sb.append("</td></tr>\n");
                // Group
                sb.append("<tr><th class=\"accessEven\">").append(label_group).append("</th>\n");
                sb.append("<td class=\"accessEven\"><select name=\"group_id\" id=\"select_group\">\n");

                Group[] groups = getGroups(context, hrq, subInfo);
                if (groups != null)
                {
                    for (Group group : groups)
                    {
                        sb.append("<option value=\"").append(group.getID()).append("\"");
                        if (group_id == group.getID()) {
                            sb.append(" selected=\"selected\"");
                        }
                        sb.append(">").append(group.getName()).append("</option>\n");
                    }
                }
                else
                {
                    sb.append("<option value=\"0\" selected=\"selected\">Anonymous</option>\n");
                }
                sb.append("</select>\n");
                sb.append("</td></tr>\n");
                // Select open or embargo
                sb.append("<tr><th class=\"accessOdd\">").append(label_embargo).append("</th>\n");
                sb.append("<td class=\"accessOdd\"><label><input name=\"open_access_radios\" type=\"radio\" value=\"0\"").append(radio0Checked).append(" />").append(radio0).append("</label>\n");
                sb.append("<label><input name=\"open_access_radios\" type=\"radio\" value=\"1\"").append(radio1Checked).append(" />").append(radio1).append("</label>\n");
                sb.append("</td></tr>\n");
            }

            // Embargo Date
            if (hidden)
            {
                sb.append("<input name=\"embargo_until_date\" id=\"embargo_until_date_hidden\" type=\"hidden\" value=\"").append(startDate).append("\" />\n");;
                sb.append("<input name=\"reason\" id=\"reason_hidden\" type=\"hidden\" value=\"").append(reason).append("\" />\n");
            }
            else
            {
                sb.append("<tr><th class=\"accessEven\">").append(label_date).append("</th>\n");
                sb.append("<td class=\"accessEven\">\n");
                sb.append("<input name=\"embargo_until_date\" id=\"embargo_until_date\" type=\"text\" value=\"").append(startDate).append("\"").append(disabled).append(" />\n");;
                sb.append(radio_help);
                sb.append("</td></tr>\n");
                // Reason
                sb.append("<tr>\n");
                sb.append("<th class=\"accessOdd\">").append(label_reason).append("</th>\n");
                sb.append("<td class=\"accessOdd\"><textarea name=\"reason\" id=\"reason\" cols=\"30\" rows=\"5\"").append(disabled).append(">").append(reason).append("</textarea>\n");
                sb.append("</td></tr>\n");
            }

            // Add policy button
            if (addpolicy)
            {
                sb.append("<tr>\n");
                sb.append("<td class=\"accessOdd\" colspan=\"2\" align=\"center\"><input name=\"submit_add_policy\" type=\"submit\" value=\"").append(button_confirm).append("\" />\n");
                sb.append("</</td></tr>\n");
            }
            sb.append("</table></center>\n");

            out.println(sb.toString());
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }
        catch (SQLException e)
        {
        	throw new JspException(e);
        }

        return SKIP_BODY;
    }

    /**
     * Get the browseInfo
     *
     * @return the browseInfo
     */
    public SubmissionInfo getSubInfo()
    {
        return subInfo;
    }

    /**
     * Set the browseInfo
     *
     * @param browseInfo
     *            the browseInfo
     */
    public void setSubInfo(SubmissionInfo subInfo)
    {
        this.subInfo = subInfo;
    }

    /**
     * Get the dso
     *
     * @return the dso
     */
    public DSpaceObject getDso()
    {
        return dso;
    }

    /**
     * Set the the dso
     *
     * @param the dso
     *            the dso
     */
    public void setDso(DSpaceObject dso)
    {
        this.dso = dso;
    }

    /**
     * Get the rp
     *
     * @return the rp
     */
    public ResourcePolicy getRp()
    {
        return rp;
    }

    /**
     * Set the the rp
     *
     * @param the rp
     *            the rp
     */
    public void setRp(ResourcePolicy rp)
    {
        this.rp = rp;
    }

    /**
     * Get the display open/embargo setting radio flag
     *
     * @return radio
     */
    public boolean getEmbargo()
    {
        return embargo;
    }

    /**
     * Set the display open/embargo setting radio flag
     *
     * @param embargo
     *            boolean
     */
    public void setEmbargo(boolean embargo)
    {
        this.embargo = embargo;
    }

    /**
     * Get the hidden flag
     *
     * @return hidden
     */
    public boolean getHidden()
    {
        return hidden;
    }

    /**
     * Set the hidden flag
     *
     * @param hidden
     *            boolean
     */
    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    /**
     * Set the add_policy button flag
     *
     * @param addpolicy
     *            boolean
     */
    public void setAddpolicy(boolean addpolicy)
    {
        this.addpolicy = addpolicy;
    }

    /**
     * Get the add_policy button flag
     *
     * @return addpolicy
     */
    public boolean getAddpolicy()
    {
        return addpolicy;
    }

    public void release()
    {
        dso = null;
        subInfo = null;
        rp = null;
        embargo = false;
        hidden = false;
        addpolicy = false;
    }

    private Group[] getGroups(Context context, HttpServletRequest request, SubmissionInfo subInfo)
        throws SQLException
    {
        Group[] groups = null;
        // retrieve groups
        if (restrictedGroup != null)
        {
            Group uiGroup = Group.findByName(context, restrictedGroup);
            if (uiGroup != null)
            {
                groups = uiGroup.getMemberGroups();
            }
        }

        if (groups == null || groups.length == 0){
            groups = Group.findAll(context, Group.NAME);
        }

        return groups;
    }

}