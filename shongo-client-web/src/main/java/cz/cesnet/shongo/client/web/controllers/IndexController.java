package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.Design;
import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import cz.cesnet.shongo.client.web.support.interceptors.IgnoreDateTimeZone;
import cz.cesnet.shongo.controller.ControllerConnectException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Index controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class IndexController
{
    @Resource
    private Design design;

    /**
     * Handle main (index) view.
     */
    @RequestMapping(value = ClientWebUrl.HOME, method = RequestMethod.GET)
    public ModelAndView handleIndexView(
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes)
    {
        // Redirect authentication requests until the "redirect_uri" is fixed
        if (request.getParameter("code") != null || request.getParameter("error") != null) {
            redirectAttributes.addAllAttributes(request.getParameterMap());
            return new ModelAndView("redirect:" + ClientWebUrl.LOGIN);
        }
        ModelAndView modelAndView = new ModelAndView((authentication != null ? "indexAuthenticated" : "indexAnonymous"));
        modelAndView.addObject("mainContent", design.renderTemplateMain(request));
        return modelAndView;
    }

    /**
     * Handle help view.
     */
    @RequestMapping(value = ClientWebUrl.LOGGED, method = RequestMethod.GET)
    @IgnoreDateTimeZone
    @ResponseBody
    public String handleLogged(HttpServletResponse response)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OpenIDConnectAuthenticationToken) {
            return "YES";
        }
        else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "NO";
        }
    }

    /**
     * Handle help view.
     */
    @RequestMapping(value = ClientWebUrl.HELP, method = RequestMethod.GET)
    @IgnoreDateTimeZone
    public String handleHelpView()
    {
        return "help";
    }

    /**
     * Handle only layout view.
     */
    @RequestMapping(value = "/layout", method = RequestMethod.GET)
    @IgnoreDateTimeZone
    public String handleLayoutView()
    {
        return "development";
    }

    /**
     * Handle development view.
     */
    @RequestMapping(value = "/development", method = RequestMethod.GET)
    public String handleDevelopmentView()
    {
        return "development";
    }
}
