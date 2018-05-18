package eu.toop.demoui.layouts;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.helger.datetime.util.PDTXMLConverter;
import com.vaadin.ui.themes.ValoTheme;
import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.codelist.EPredefinedProcessIdentifier;
import eu.toop.commons.dataexchange.TDEAddressType;
import eu.toop.commons.dataexchange.TDEDataRequestSubjectType;
import eu.toop.commons.dataexchange.TDENaturalPersonType;
import eu.toop.demoui.view.BaseView;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.CodeType;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.IdentifierType;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.TextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.string.StringHelper;
import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.jaxb.ToopXSDHelper;
import eu.toop.iface.ToopInterfaceClient;
import eu.toop.kafkaclient.ToopKafkaClient;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class ConfirmToopDataFetchingPage extends Window {

  private static final Logger s_aLogger = LoggerFactory.getLogger (ConfirmToopDataFetchingPage.class);

  public ConfirmToopDataFetchingPage (final BaseView view) {

    final Window subWindow = new Window ("Sub-window");
    final VerticalLayout subContent = new VerticalLayout ();
    subWindow.setContent (subContent);

    subWindow.setWidth ("800px");

    subWindow.setModal (true);
    subWindow.setCaption (null);
    subWindow.setResizable (false);
    subWindow.setClosable (false);

    // Put some components in it
    final ConfirmToopDataFetchingTable confirmToopDataFetchingTable = new ConfirmToopDataFetchingTable ();
    subContent.addComponent (confirmToopDataFetchingTable);

    final Button proceedButton = new Button ("Please request this information through TOOP", (event) -> {
      onConsent ();
      subWindow.close ();

      // Send the request to the Message-Processor
      try {
        final List<ConceptValue> conceptList = new ArrayList<> ();

        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaAddress"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaSSNumber"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaBusinessCode"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaVATNumber"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaCompanyType"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaRegistrationDate"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaRegistrationNumber"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaCompanyName"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaCompanyNaceCode"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaActivityDeclaration"));
        conceptList.add (new ConceptValue ("http://example.register.fre/freedonia-business-register",
            "FreedoniaRegistrationAuthority"));

        // Notify the logger and Package-Tracker that we are sending a TOOP Message!
        ToopKafkaClient.send (EErrorLevel.INFO,
            () -> "[DC] Requesting concepts: "
                + StringHelper.getImplodedMapped (", ", conceptList,
                x -> x.getNamespace () + "#" + x.getValue ()));

/*
        TDENaturalPersonType naturalPersonType = new TDENaturalPersonType ();

        if (view.getIdentity ().getFamilyName () != null) {
          naturalPersonType.setFamilyName (new TextType(view.getIdentity ().getFamilyName ()));
        }
        if (view.getIdentity ().getFirstName () != null) {
          naturalPersonType.setFirstName (new TextType(view.getIdentity ().getFirstName ()));
        }
        if (view.getIdentity ().getIdentifier () != null) {
          IdentifierType identifierType = new IdentifierType(view.getIdentity ().getIdentifier ());
          naturalPersonType.setPersonIdentifier (identifierType);
        }
        if (view.getIdentity ().getBirthDate () != null) {
          XMLGregorianCalendar birthDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(view.getIdentity ().getBirthDate ().toString());
          naturalPersonType.setBirthDate (birthDate);
        } else {
          LocalDate date = LocalDate.parse("1999-11-11");
          XMLGregorianCalendar birthDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(date.toString ());
          naturalPersonType.setBirthDate (birthDate);
        }*/

        final TDEDataRequestSubjectType aDS = new TDEDataRequestSubjectType ();
        aDS.setDataRequestSubjectTypeCode (ToopXSDHelper.createCode ("12345"));
        {
          final TDENaturalPersonType aNP = new TDENaturalPersonType ();
          aNP.setPersonIdentifier (ToopXSDHelper.createIdentifier (view.getIdentity ().getIdentifier ()));
          aNP.setFamilyName (ToopXSDHelper.createText (view.getIdentity ().getFamilyName ()));
          aNP.setFirstName (ToopXSDHelper.createText (view.getIdentity ().getFirstName ()));
          aNP.setBirthDate (PDTXMLConverter.getXMLCalendarDateNow ());
          final TDEAddressType aAddress = new TDEAddressType ();
          // Destination country to use
          aAddress.setCountryCode (ToopXSDHelper.createCode ("SV"));
          aNP.setNaturalPersonLegalAddress (aAddress);
          aDS.setNaturalPerson (aNP);
        }

        ToopInterfaceClient.createRequestAndSendToToopConnector (aDS,
            ToopXSDHelper.createIdentifier ("iso6523-actorid-upis",
            "9999:freedonia"),
            "SV",
            EPredefinedDocumentTypeIdentifier.REQUEST_REGISTEREDORGANIZATION,
            EPredefinedProcessIdentifier.DATAREQUESTRESPONSE, conceptList);
      } catch (final IOException ex) {
        // Convert from checked to unchecked
        throw new UncheckedIOException (ex);
      }
    });
    proceedButton.addStyleName (ValoTheme.BUTTON_BORDERLESS);
    proceedButton.addStyleName ("ConsentAgreeButton");

    final Button cancelButton = new Button ("Thanks, I will provide this information myself", (event) -> {
      onSelfProvide ();
      subWindow.close ();
    });
    cancelButton.addStyleName (ValoTheme.BUTTON_BORDERLESS);
    cancelButton.addStyleName ("ConsentCancelButton");

    subContent.addComponent (proceedButton);
    subContent.addComponent (cancelButton);

    // Center it in the browser window
    subWindow.center ();

    // Open it in the UI
    view.getUI ().addWindow (subWindow);
  }

  protected void onConsent () {

  }

  protected void onSelfProvide () {

  }
}
