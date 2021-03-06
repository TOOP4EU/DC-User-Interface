/**
 * Copyright (C) 2018-2020 toop.eu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.toop.demoui;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.http.CHttpHeader;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import eu.toop.demoui.view.Maritime;

/**
 * This UI is the application entry point. A UI may either represent a browser
 * window (or tab) or some part of an HTML page where a Vaadin application is
 * embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is
 * intended to be overridden to add component to the user interface and
 * initialize non-component functionality.
 */
@Theme ("DCUITheme")
public class DCUI extends UI {
  @Override
  protected void init (final VaadinRequest vaadinRequest) {
    // Add a custom request handler
    VaadinSession.getCurrent ().addRequestHandler ( (session, request, response) -> {

      if ("/redirectToEidModule".equals (request.getPathInfo ())) {
        // Redirect to eID Module
        response.setStatus (HttpServletResponse.SC_TEMPORARY_REDIRECT);
        response.setHeader (CHttpHeader.LOCATION, DCUIConfig.getEidModuleURL ());

        return true;
      }
      return false;
    });

    setPollInterval (1000);
    getPage ().setTitle ("TOOP Demo Maritime Interface");

    final Navigator navigator = new Navigator (this, this);

    final Maritime maritime = new Maritime();
    navigator.addView("", maritime);

  }

  @Override
  public void close () {
    super.close ();
  }

  @WebServlet (urlPatterns = { "/ui/*", "/VAADIN/*" }, asyncSupported = true)
  @VaadinServletConfiguration (ui = DCUI.class, productionMode = false)
  public static class DCUIServlet extends VaadinServlet {
    // empty
  }
}
