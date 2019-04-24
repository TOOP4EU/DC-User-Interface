/**
 * Copyright (C) 2018-2019 toop.eu
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
package eu.toop.demoui.layouts;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.Nonnull;

import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.string.StringHelper;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.codelist.EPredefinedProcessIdentifier;
import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.dataexchange.v140.*;
import eu.toop.commons.error.ToopErrorException;
import eu.toop.commons.exchange.ToopMessageBuilder140;
import eu.toop.commons.jaxb.ToopWriter;
import eu.toop.commons.jaxb.ToopXSDHelper140;
import eu.toop.demoui.DCUIConfig;
import eu.toop.demoui.view.BaseView;
import eu.toop.iface.ToopInterfaceClient;
import eu.toop.iface.ToopInterfaceConfig;
import eu.toop.kafkaclient.ToopKafkaClient;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.TextType;

public class DynamicRequestPage extends CustomLayout {

  private final BaseView view;
  private final ProgressBar spinner = new ProgressBar ();
  private static final String TOOP_BUTTON_STYLE = ValoTheme.BUTTON_BORDERLESS + " freedonia";

  private final ComboBox<String> countryCodeField = new ComboBox<> ();
  private final ComboBox<EPredefinedDocumentTypeIdentifier> documentTypeField = new ComboBox<> ();
  private final TextField naturalPersonIdentifierField = new TextField ();
  private final TextField naturalPersonFirstNameField = new TextField ();
  private final TextField naturalPersonFamilyNameField = new TextField ();
  private final TextField legalPersonUniqueIdentifierField = new TextField ();
  private final TextField legalPersonCompanyNameField = new TextField ();
  private final TextField dataProviderScheme = new TextField ();
  private final TextField dataProviderName = new TextField ();
  private Label errorLabel = null;
  private Label conceptErrorsLabel = null;
  private final Button dataProvidersFindButton = new Button ("Find Data Providers");;
  private final Button dataProvidersManualButton = new Button ("Manually enter Data Provider");
  private final Button sendButton = new Button ("Send Data Element Request");
  private final Button sendDocumentRequestButton = new Button ("Send Document Request");

  private Label requestIdLabel = null;
  private boolean responseReceived = false;

  private final static List<ConceptValue> conceptList = new ArrayList<> ();

  static {
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaStreetAddress"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaSSNumber"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaCompanyCode"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaVATNumber"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaCompanyType"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaRegistrationDate"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaRegistrationNumber"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaCompanyName"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaNaceCode"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaActivityDescription"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaRegistrationAuthority"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaLegalStatus"));
    conceptList.add (new ConceptValue (DCUIConfig.getConceptNamespace (), "FreedoniaLegalStatusEffectiveDate"));
  }

  public DynamicRequestPage (final BaseView view) {

    super ("DynamicRequestPage");
    this.view = view;

    setHeight ("100%");

    spinner.setCaption ("Please wait while your request for data is processed...");
    spinner.setStyleName ("spinner");
    spinner.setIndeterminate (true);
    spinner.setVisible (false);
    addComponent (spinner, "spinner");

    dataProviderScheme.setVisible(false);
    dataProviderName.setVisible(false);

    dataProviderScheme.setPlaceholder("Data Provider Scheme");
    //dataProviderScheme.setReadOnly(true);
    dataProviderName.setPlaceholder("Data Provider Name");
    //dataProviderName.setReadOnly(true);

    countryCodeField.setStyleName("countryCodeDropdown");
    countryCodeField.setItems (DCUIConfig.getCountryCodes());
    documentTypeField.setStyleName("documentTypeDropdown");
    documentTypeField.setItems (Arrays.asList(
            EPredefinedDocumentTypeIdentifier.URN_EU_TOOP_NS_DATAEXCHANGE_1P40_REQUEST_URN_EU_TOOP_REQUEST_REGISTEREDORGANIZATION_1_40,
            EPredefinedDocumentTypeIdentifier.URN_EU_TOOP_NS_DATAEXCHANGE_1P40_REQUEST_URN_EU_TOOP_REQUEST_SHIPCERTIFICATE_LIST_1_40,
            EPredefinedDocumentTypeIdentifier.URN_EU_TOOP_NS_DATAEXCHANGE_1P40_REQUEST_URN_EU_TOOP_REQUEST_SHIPCERTIFICATE_1_40,
            EPredefinedDocumentTypeIdentifier.URN_EU_TOOP_NS_DATAEXCHANGE_1P40_REQUEST_URN_EU_TOOP_REQUEST_CREWCERTIFICATE_LIST_1_40,
            EPredefinedDocumentTypeIdentifier.URN_EU_TOOP_NS_DATAEXCHANGE_1P40_REQUEST_URN_EU_TOOP_REQUEST_CREWCERTIFICATE_1_40,
            EPredefinedDocumentTypeIdentifier.URN_EU_TOOP_NS_DATAEXCHANGE_1P40_REQUEST_URN_EU_TOOP_REQUEST_EVIDENCE_LIST_1_40,
            EPredefinedDocumentTypeIdentifier.URN_EU_TOOP_NS_DATAEXCHANGE_1P40_REQUEST_URN_EU_TOOP_REQUEST_EVIDENCE_1_40));

    addComponent (countryCodeField, "countryCodeField");
    addComponent (documentTypeField, "documentTypeField");
    addComponent (naturalPersonIdentifierField, "naturalPersonIdentifierField");
    addComponent (naturalPersonFirstNameField, "naturalPersonFirstNameField");
    addComponent (naturalPersonFamilyNameField, "naturalPersonFamilyNameField");
    addComponent (legalPersonUniqueIdentifierField, "legalPersonUniqueIdentifierField");
    addComponent (legalPersonCompanyNameField, "legalPersonCompanyNameField");
    addComponent (dataProviderScheme, "dataProviderScheme");
    addComponent (dataProviderName, "dataProviderName");

    dataProvidersFindButton.addStyleName (ValoTheme.BUTTON_BORDERLESS);
    dataProvidersFindButton.addClickListener((evt) -> {

      new DataProviderSelectionWindow (view, countryCodeField.getValue()) {
        @Override
        public void onSave (String participantScheme, String participantValue) {
          dataProviderScheme.setValue(participantScheme);
          dataProviderName.setValue(participantValue);

          dataProvidersFindButton.setVisible(false);
          dataProvidersManualButton.setVisible(false);
          dataProviderScheme.setVisible(true);
          dataProviderName.setVisible(true);
        }

        @Override
        public void onCancel () {
          dataProvidersFindButton.setVisible(true);
          dataProvidersManualButton.setVisible(true);
          dataProviderScheme.setVisible(false);
          dataProviderName.setVisible(false);
        }
      };
    });

    dataProvidersManualButton.addStyleName (ValoTheme.BUTTON_BORDERLESS);
    dataProvidersManualButton.addClickListener((evt) -> {
      ToopKafkaClient.send (EErrorLevel.INFO, () -> "[DC] Manual entry of data provider...");
      dataProvidersFindButton.setVisible(false);
      dataProvidersManualButton.setVisible(false);
      dataProviderScheme.setVisible(true);
      dataProviderName.setVisible(true);
      dataProviderScheme.setReadOnly(false);
      dataProviderName.setReadOnly(false);
    });

    sendButton.addStyleName (ValoTheme.BUTTON_BORDERLESS);
    sendButton.addStyleName ("ConsentAgreeButton");
    sendButton.addClickListener (new SendRequest(0));

    sendDocumentRequestButton.addStyleName (ValoTheme.BUTTON_BORDERLESS);
    sendDocumentRequestButton.addStyleName ("ConsentAgreeButton");
    sendDocumentRequestButton.addClickListener (new SendRequest(1));

    addComponent (dataProvidersFindButton, "dataProvidersFindButton");
    addComponent (dataProvidersManualButton, "dataProvidersManualButton");
    addComponent (sendButton, "sendButton");
    addComponent (sendDocumentRequestButton, "sendDocumentRequestButton");
  }


  class SendRequest implements Button.ClickListener {

    private final int type;

    public SendRequest(int type) {
      this.type = type;
    }

    @Override
    public void buttonClick (final Button.ClickEvent clickEvent) {
      try {
        final String identifierPrefix = countryCodeField.getValue () + "/" + DCUIConfig.getSenderCountryCode() + "/";

        if (type==0) {
          ToopKafkaClient.send(EErrorLevel.INFO, () -> "[DC] Requesting concepts: "
                  + StringHelper.getImplodedMapped(", ", conceptList, x -> x.getNamespace() + "#" + x.getValue()));
        } else {
          ToopKafkaClient.send(EErrorLevel.INFO, () -> "[DC] Requesting document.");
        }

        final TDEDataRequestSubjectType aDS = new TDEDataRequestSubjectType ();
        {
          final TDENaturalPersonType aNP = new TDENaturalPersonType ();
          aDS.setNaturalPerson (aNP);

          String naturalPersonIdentifier = "";
          if (!naturalPersonIdentifierField.isEmpty ()) {
            aDS.setDataRequestSubjectTypeCode (ToopXSDHelper140.createCode ("NP"));
            naturalPersonIdentifier = identifierPrefix + naturalPersonIdentifierField.getValue ();
          }
          aNP.setPersonIdentifier (ToopXSDHelper140.createIdentifierWithLOA (naturalPersonIdentifier));

          String naturalPersonFirstName = "";
          if (!naturalPersonFirstNameField.isEmpty ()) {
            naturalPersonFirstName = naturalPersonFirstNameField.getValue ();
          }
          aNP.setFirstName (ToopXSDHelper140.createTextWithLOA (naturalPersonFirstName));

          String naturalPersonFamilyName = "";
          if (!naturalPersonFamilyNameField.isEmpty ()) {
            naturalPersonFamilyName = naturalPersonFamilyNameField.getValue ();
          }
          aNP.setFamilyName (ToopXSDHelper140.createTextWithLOA (naturalPersonFamilyName));

          aNP.setBirthDate (ToopXSDHelper140.createDateWithLOANow ());

          final TDEAddressWithLOAType aAddress = new TDEAddressWithLOAType ();
          aAddress.setCountryCode (ToopXSDHelper140.createCodeWithLOA (countryCodeField.getValue ()));
          aNP.setNaturalPersonLegalAddress (aAddress);
        }

        if (!legalPersonUniqueIdentifierField.isEmpty ()) {
          aDS.setDataRequestSubjectTypeCode (ToopXSDHelper140.createCode ("LE"));
          final TDELegalPersonType aLE = new TDELegalPersonType ();
          aLE.setLegalPersonUniqueIdentifier (ToopXSDHelper140
              .createIdentifierWithLOA (identifierPrefix + legalPersonUniqueIdentifierField.getValue ()));
          
          String legalName = "";
          if (!legalPersonCompanyNameField.isEmpty ()) {
            legalName = legalPersonCompanyNameField.getValue ();
          }
          aLE.setLegalName (ToopXSDHelper140.createTextWithLOA (legalName));

          final TDEAddressWithLOAType aAddress = new TDEAddressWithLOAType ();
          aAddress.setCountryCode (ToopXSDHelper140.createCodeWithLOA (countryCodeField.getValue ()));
          aLE.setLegalPersonLegalAddress (aAddress);
          aDS.setLegalPerson (aLE);
        }

        final String srcCountryCode = DCUIConfig.getSenderCountryCode();
        final TDETOOPRequestType aRequest = ToopMessageBuilder140.createMockRequest (aDS, srcCountryCode,
            countryCodeField.getValue (),
            ToopXSDHelper140.createIdentifier (DCUIConfig.getSenderIdentifierScheme (),
                DCUIConfig.getSenderIdentifierValue ()),
                documentTypeField.getValue(),
                EPredefinedProcessIdentifier.DATAREQUESTRESPONSE,
                (type == 0 ? conceptList : null));

        final UUID uuid = UUID.randomUUID ();
        requestIdLabel = new Label (uuid.toString ());
        addComponent (requestIdLabel, "requestId");
        aRequest.setDocumentUniversalUniqueIdentifier (ToopXSDHelper140.createIdentifier ("demo-agency", "toop-doctypeid-qns", uuid.toString ()));
        aRequest.setSpecificationIdentifier (ToopXSDHelper140.createIdentifier("toop-doctypeid-qns", "urn:eu:toop:ns:dataexchange-1p40::Request"));

        if (type == 1) {
          final TDEDocumentRequestType documentRequestType = new TDEDocumentRequestType();
          documentRequestType.setDocumentURI(ToopXSDHelper140.createIdentifier("https://koolitus.emde.ee/cc/b0/67/123456"));
          documentRequestType.setDocumentRequestIdentifier(ToopXSDHelper140.createIdentifier("demo-agency", "toop-doctypeid-qns", "600db318-02c2-4c43-b272-f9268615c076"));
          documentRequestType.setDocumentRequestTypeCode(ToopXSDHelper140.createCode("ETR"));
          aRequest.addDocumentRequest(documentRequestType);
        }

        if (!dataProviderScheme.isEmpty() && !dataProviderName.isEmpty()) {
          final TDERoutingInformationType routingInformation = aRequest.getRoutingInformation();
          routingInformation.setDataProviderElectronicAddressIdentifier(
                  ToopXSDHelper140.createIdentifier(dataProviderScheme.getValue(), dataProviderName.getValue()));
          aRequest.setRoutingInformation(routingInformation);

          ToopKafkaClient.send (EErrorLevel.INFO,
                  () -> String.format("[DC] Set routing information to specific data provider: [%s, %s]",
                          dataProviderScheme.getValue(),
                          dataProviderName.getValue()));
        }

        try {
          dumpRequest (aRequest);
        } catch (final Exception e) {
          System.out.println ("Failed to dump request xml");
        }

        ToopKafkaClient.send (EErrorLevel.INFO,
                () -> "[DC] Sending request to TC: " + ToopInterfaceConfig.getToopConnectorDCUrl ());

        ToopInterfaceClient.sendRequestToToopConnector (aRequest);

        spinner.setVisible (true);
        setEnabled (false);

        // Fake response
        new java.util.Timer ().schedule (new java.util.TimerTask () {
          @Override
          public void run () {
            if (!responseReceived) {
              setErrorTimeout ();
            }
          }
        }, 60000);
      } catch (final IOException | ToopErrorException ex) {
        // Convert from checked to unchecked
        throw new RuntimeException (ex);
      }
    }
  }

  public void setError (final List<TDEErrorType> errors) {

    responseReceived = true;
    spinner.setVisible (false);

    final StringBuilder sb = new StringBuilder ();

    for (final TDEErrorType tdeErrorType : errors) {
      sb.append (" Error: ").append ("\n");

      if (tdeErrorType.getOrigin () != null) {
        sb.append (" - Origin: ").append (tdeErrorType.getOrigin ().getValue ()).append ("\n");
      } else {
        sb.append (" - Origin: ").append ("N/A").append ("\n");
      }

      if (tdeErrorType.getCategory () != null) {
        sb.append (" - Category: ").append (tdeErrorType.getCategory ().getValue ()).append ("\n");
      } else {
        sb.append (" - Category: ").append ("N/A").append ("\n");
      }

      if (tdeErrorType.getErrorCode () != null) {
        sb.append (" - Error Code: ").append (tdeErrorType.getErrorCode ().getValue ()).append ("\n");
      } else {
        sb.append (" - Error Code: ").append ("N/A").append ("\n");
      }

      if (tdeErrorType.getSeverity () != null) {
        sb.append (" - Severity: ").append (tdeErrorType.getSeverity ().getValue ()).append ("\n");
      } else {
        sb.append (" - Severity: ").append ("N/A").append ("\n");
      }

      for (final TextType errorText : tdeErrorType.getErrorText ()) {

        if (errorText != null) {
          sb.append (" - Error Text: ").append (errorText.getValue ()).append ("\n");
        } else {
          sb.append (" - Error Text: ").append ("N/A").append ("\n");
        }
      }
    }

    errorLabel = new Label (sb.toString ());
    errorLabel.setContentMode (ContentMode.PREFORMATTED);
    addComponent (errorLabel, "errorLabel");
  }

  public void setErrorTimeout () {

    responseReceived = true;
    spinner.setVisible (false);

    final StringBuilder sb = new StringBuilder ();
    sb.append (" Error: ").append ("\n");
    sb.append (" - ").append ("Timeout").append ("\n");

    errorLabel = new Label (sb.toString ());
    errorLabel.setContentMode (ContentMode.PREFORMATTED);
    addComponent (errorLabel, "errorLabel");
  }

  public void setConceptErrors (final String conceptErrors) {

    responseReceived = true;
    spinner.setVisible (false);

    conceptErrorsLabel = new Label (conceptErrors);
    conceptErrorsLabel.setContentMode (ContentMode.PREFORMATTED);
    addComponent (conceptErrorsLabel, "conceptErrorsLabel");
  }

  public void addMainCompanyForm () {

    responseReceived = true;
    spinner.setVisible (false);

    final MainCompanyForm mainCompanyForm = new MainCompanyForm (view.getMainCompany (), false);

    final BaseForm baseForm = new BaseForm (mainCompanyForm, "Company details");
    addComponent (baseForm, "mainCompanyForm");
    view.setMainCompanyForm (mainCompanyForm);
  }

  private void dumpRequest (@Nonnull final TDETOOPRequestType aRequest) {
    try {
      final DateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS");
      final String filePath = String.format ("%s/request-dump-%s.log", DCUIConfig.getDumpResponseDirectory (),
          dateFormat.format (new Date ()));

      final String requestXml = ToopWriter.request140 ().getAsString (aRequest);
      if (requestXml != null) {
        try (final FileWriter fw = new FileWriter (filePath)) {
          fw.write (requestXml);
        }
      }
    } catch (final IOException e) {
      e.printStackTrace ();
    }
  }

  public String getRequestId () {
    if (requestIdLabel != null) {
      return requestIdLabel.getValue ();
    }
    return null;
  }
}
