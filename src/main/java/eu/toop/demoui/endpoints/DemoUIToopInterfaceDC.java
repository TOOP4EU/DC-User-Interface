package eu.toop.demoui.endpoints;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.serialize.SerializationHelper;
import com.vaadin.ui.UI;

import eu.toop.commons.exchange.message.ToopMessageBuilder;
import eu.toop.commons.exchange.message.ToopResponseMessage;
import eu.toop.commons.exchange.mock.MSDataRequest;
import eu.toop.demoui.bean.Organization;
import eu.toop.demoui.view.StartView;
import eu.toop.iface.IToopInterfaceDC;

public class DemoUIToopInterfaceDC implements IToopInterfaceDC {

  private final UI _ui;

  public DemoUIToopInterfaceDC(final UI ui) {
    this._ui = ui;
  }

  @Override
  public void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    final String pathInfo = req.getPathInfo();
    final PrintWriter aPW = resp.getWriter();
    aPW.write("<html><body>OK</body></html>");
    aPW.flush();

    try {
      final ToopResponseMessage bundleRead = ToopMessageBuilder.parseResponseMessage(req.getInputStream(),
          SerializationHelper::getDeserializedObject, SerializationHelper::getDeserializedObject,
          SerializationHelper::getDeserializedObject, SerializationHelper::getDeserializedObject);
      _ui.access(() -> {
        // Push a new organization bean to the UI
        if (_ui.getNavigator().getCurrentView() instanceof StartView) {
          final StartView startView = (StartView) _ui.getNavigator().getCurrentView();
          final Organization organization = new Organization();
          // TODO: Real values are read from a retrieved ToopMessageBundle, however
          // the correct values have to be read instead. These are just placeholders.
          organization.setCompanyName(((MSDataRequest) bundleRead.getMsDataRequest()).getIdentifier());
          organization.setCompanyType(((MSDataRequest) bundleRead.getMsDataRequest()).getIdentifier());
          startView.organizationForm.setOrganizationBean(organization);
        }
      });
    } catch (final Exception e) {

    }
  }
}
