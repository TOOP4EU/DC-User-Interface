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
package eu.toop.demoui.layouts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.timing.StopWatch;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.dataexchange.v140.TDEErrorType;
import eu.toop.commons.dataexchange.v140.TDETOOPRequestType;
import eu.toop.commons.error.ToopErrorException;
import eu.toop.demoui.DCToToopInterfaceMapper;
import eu.toop.demoui.DCUIConfig;
import eu.toop.demoui.bean.DocumentDataBean;
import eu.toop.demoui.builder.TOOPRequestMaker;
import eu.toop.demoui.builder.model.Request;
import eu.toop.demoui.endpoints.DemoUIToopInterfaceHelper;
import eu.toop.demoui.view.BaseView;
import eu.toop.iface.ToopInterfaceConfig;
import eu.toop.kafkaclient.ToopKafkaClient;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.TextType;

public class MaritimePage extends CustomLayout {

    private final BaseView view;
    private final ProgressBar spinner = new ProgressBar ();

    private final ComboBox<String> countryCodeField = new ComboBox<> ();
    private final ComboBox<EPredefinedDocumentTypeIdentifier> documentTypeField = new ComboBox<> ();
    private final TextField naturalPersonIdentifierField = new TextField ();
    private final TextField naturalPersonFirstNameField = new TextField ();
    private final TextField naturalPersonFamilyNameField = new TextField ();
    private final TextField IMOIdentifierField = new TextField ();
    private final TextField documentIdentifierField = new TextField();
    private final TextField dataProviderScheme = new TextField ();
    private final TextField dataProviderName = new TextField ();
    private final Label IMOIdentifierLabel = new Label("IMO Identifier");
    private final Label naturalPersonIdentifierLabel = new Label("Natural Person Identifier");
    private final Label naturalPersonFirstNameLabel = new Label("Natural Person First Name");
    private final Label naturalPersonFamilyNameLabel = new Label("Natural Person Family Name");

    private Label errorLabel = null;
    private Label conceptErrorsLabel = null;
    private final Button sendDocumentRequestButton = new Button ("Send Document Request");

    private Label requestIdLabel = null;
    private boolean responseReceived = false;
    private final StopWatch sw = StopWatch.createdStopped ();
    private Timer timeoutTimer = new Timer ();

    public MaritimePage(final BaseView view) {
        super("MaritimePage");
        this.view = view;

        setHeight ("100%");

        resetError ();

        spinner.setCaption ("Please wait while your request for data is processed...");
        spinner.setStyleName ("spinner");
        spinner.setIndeterminate (true);
        spinner.setVisible (false);
        addComponent (spinner, "spinner");

        dataProviderScheme.setVisible (false);
        dataProviderName.setVisible (false);

        dataProviderScheme.setPlaceholder ("Data Provider Scheme");
        // dataProviderScheme.setReadOnly(true);
        dataProviderName.setPlaceholder ("Data Provider Name");
        // dataProviderName.setReadOnly(true);

        countryCodeField.setStyleName ("countryCodeDropdown");
        countryCodeField.setItems (DCUIConfig.getMaritimeCountryCodes());
        documentTypeField.setStyleName ("documentTypeDropdown");
        final List<EPredefinedDocumentTypeIdentifier> aDocTypes = new ArrayList<> ();
        /* add only Request documents and Maritime related types*/
        for (final EPredefinedDocumentTypeIdentifier e : EPredefinedDocumentTypeIdentifier.values ())
            if (e.getID ().contains ("::Request##") && (e.getID().contains("shipcertificate-list")|| e.getID().contains("crewcertificate-list"))) {
                aDocTypes.add (e);
            }

        documentTypeField.setItems (aDocTypes);
        // Don't allow new items
        documentTypeField.setNewItemHandler (null);
        documentTypeField.addValueChangeListener( valueChangeEvent -> {
            ToopKafkaClient.send (EErrorLevel.INFO, () -> "Doc Type value changed to: " + valueChangeEvent.getValue());
            if (valueChangeEvent.getValue().toString().contains("SHIP")) {
                naturalPersonFirstNameField.setVisible(false);
                naturalPersonFamilyNameField.setVisible(false);
                naturalPersonIdentifierField.setVisible(false);
                naturalPersonFamilyNameLabel.setVisible(false);
                naturalPersonFirstNameLabel.setVisible(false);
                naturalPersonIdentifierLabel.setVisible(false);
                IMOIdentifierField.setVisible(true);
                IMOIdentifierLabel.setVisible(true);
            } else {
                naturalPersonFirstNameField.setVisible(true);
                naturalPersonFamilyNameField.setVisible(true);
                naturalPersonIdentifierField.setVisible(true);
                naturalPersonFamilyNameLabel.setVisible(true);
                naturalPersonFirstNameLabel.setVisible(true);
                naturalPersonIdentifierLabel.setVisible(true);
                IMOIdentifierField.setVisible(false);
                IMOIdentifierLabel.setVisible(false);
            }
        });

        addComponent (countryCodeField, "countryCodeField");
        addComponent (documentTypeField, "documentTypeField");
        addComponent (naturalPersonIdentifierField, "naturalPersonIdentifierField");
        addComponent (naturalPersonFirstNameField, "naturalPersonFirstNameField");
        addComponent (naturalPersonFamilyNameField, "naturalPersonFamilyNameField");
        addComponent(naturalPersonFamilyNameLabel, "naturalPersonFamilyNameLabel");
        addComponent(naturalPersonFirstNameLabel, "naturalPersonFirstNameLabel");
        addComponent(naturalPersonIdentifierLabel, "naturalPersonIdentifierLabel");

        addComponent (IMOIdentifierField, "IMOIdentifierField");
        addComponent(IMOIdentifierLabel, "IMOIdentifierLabel");
//        addComponent (legalPersonCompanyNameField, "legalPersonCompanyNameField");
        addComponent(documentIdentifierField, "documentIdentifierField");
        addComponent (dataProviderScheme, "dataProviderScheme");
        addComponent (dataProviderName, "dataProviderName");

        /* Document Request Button - styles - listener*/
        sendDocumentRequestButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        sendDocumentRequestButton.addStyleName ("ConsentAgreeButton");
        sendDocumentRequestButton.addClickListener (new MaritimePage.SendRequest());

//        addComponent (dataProvidersFindButton, "dataProvidersFindButton");
//        addComponent (dataProvidersManualButton, "dataProvidersManualButton");
        addComponent (sendDocumentRequestButton, "sendDocumentRequestButton");
    }

    private Button getCertificateButton(String uri) {
        Button button = new Button(VaadinIcons.ARROW_CIRCLE_DOWN_O);
        button.addStyleName(ValoTheme.BUTTON_SMALL);
        button.addClickListener(e -> sendCertficateRequest(uri));
        return button;
    }

    private void sendCertficateRequest(String uri) {
        Notification.show(uri);

        responseReceived = false;
        resetError ();
        removeMainCompanyForm ();
        removeKeyValueForm ();
        sw.restart ();

        try {

            // SET DOCUMENTTYPEFIELD value from list to certificate...

            Request formValues;

            if(documentTypeField.getValue().toString().contains("SHIPCERTIFICATE")) {
                documentTypeField.setValue(EPredefinedDocumentTypeIdentifier.REQUEST_SHIPCERTIFICATE);
                formValues = new Request(countryCodeField.getValue(), EPredefinedDocumentTypeIdentifier.REQUEST_SHIPCERTIFICATE);

            } else {
                documentTypeField.setValue(EPredefinedDocumentTypeIdentifier.REQUEST_CREWCERTIFICATE);
                formValues = new Request(countryCodeField.getValue(), EPredefinedDocumentTypeIdentifier.REQUEST_CREWCERTIFICATE);
            }
            /* 2nd step: set always the DocumentIdentifier as the certificate's URI */
            formValues.setDocumentIdentifier(uri);

            if(!naturalPersonIdentifierField.isEmpty()) {
                formValues.setNaturalPersonIdentifier(naturalPersonIdentifierField.getValue());
            }
            if (!naturalPersonFirstNameField.isEmpty()) {
                formValues.setNaturalPersonFirstName(naturalPersonFirstNameField.getValue());
            }
            if(!naturalPersonFamilyNameField.isEmpty()) {
                formValues.setNaturalPersonFamilyName(naturalPersonFamilyNameField.getValue());
            }
            if(!IMOIdentifierField.isEmpty()) {
                formValues.setId(IMOIdentifierField.getValue());
            }

            ToopKafkaClient.send (EErrorLevel.INFO, () -> "[DC] Requesting document.");

            TOOPRequestMaker makeRequest = new TOOPRequestMaker(formValues);
            final TDETOOPRequestType aRequest = makeRequest.createTOOPRequest();

            requestIdLabel = new Label (aRequest.getDocumentUniversalUniqueIdentifier().getValue());
            addComponent (requestIdLabel, "requestId");

            DemoUIToopInterfaceHelper.dumpRequest (aRequest);

            ToopKafkaClient.send (EErrorLevel.INFO,
                    () -> "[DC] Sending request to TC: " + ToopInterfaceConfig.getToopConnectorDCUrl ());

            DCToToopInterfaceMapper.sendRequest (aRequest, getUI ());

            spinner.setVisible (false);
            // setEnabled (false);

            // Fake response
            if (timeoutTimer == null)
            {
                timeoutTimer = new Timer ();
                timeoutTimer.schedule (new TimerTask ()
                {
                    @Override
                    public void run ()
                    {
                        if (!responseReceived)
                            setErrorTimeout ();
                    }
                }, 60000);
            }
        } catch (final IOException | ToopErrorException ex) {
            // Convert from checked to unchecked
            throw new RuntimeException (ex);
        }


    }


    class SendRequest implements Button.ClickListener {

        public SendRequest () { }

        @Override
        public void buttonClick (final Button.ClickEvent clickEvent) {

            responseReceived = false;
            resetError ();
            removeMainCompanyForm ();
            removeKeyValueForm ();
            sw.restart ();

            try {

                Request formValues = new Request(countryCodeField.getValue(), documentTypeField.getValue());

                if(!naturalPersonIdentifierField.isEmpty()) {
                    formValues.setNaturalPersonIdentifier(naturalPersonIdentifierField.getValue());
                }
                if (!naturalPersonFirstNameField.isEmpty()) {
                    formValues.setNaturalPersonFirstName(naturalPersonFirstNameField.getValue());
                }
                if(!naturalPersonFamilyNameField.isEmpty()) {
                    formValues.setNaturalPersonFamilyName(naturalPersonFamilyNameField.getValue());
                }
                if(!IMOIdentifierField.isEmpty()) {
                    formValues.setId(IMOIdentifierField.getValue());
                }
                if (!documentIdentifierField.isEmpty()) {
                    formValues.setDocumentIdentifier(documentIdentifierField.getValue());
                }

                ToopKafkaClient.send (EErrorLevel.INFO, () -> "[DC] Requesting document.");

                TOOPRequestMaker makeRequest = new TOOPRequestMaker(formValues);
                final TDETOOPRequestType aRequest = makeRequest.createTOOPRequest();

                requestIdLabel = new Label (aRequest.getDocumentUniversalUniqueIdentifier().getValue());
                addComponent (requestIdLabel, "requestId");

                DemoUIToopInterfaceHelper.dumpRequest (aRequest);

                ToopKafkaClient.send (EErrorLevel.INFO,
                        () -> "[DC] Sending request to TC: " + ToopInterfaceConfig.getToopConnectorDCUrl ());

                DCToToopInterfaceMapper.sendRequest (aRequest, getUI ());

                spinner.setVisible (false);
                // setEnabled (false);

                // Fake response
                if (timeoutTimer == null)
                {
                    timeoutTimer = new Timer ();
                    timeoutTimer.schedule (new TimerTask ()
                    {
                        @Override
                        public void run ()
                        {
                            if (!responseReceived)
                                setErrorTimeout ();
                        }
                    }, 60000);
                }
            } catch (final IOException | ToopErrorException ex) {
                // Convert from checked to unchecked
                throw new RuntimeException (ex);
            }
        }
    }

    private void _onResponseReceived () {
        responseReceived = true;
        if (timeoutTimer != null)
        {
            timeoutTimer.cancel ();
            timeoutTimer = null;
        }
        spinner.setVisible(false);
        sw.stop ();
        addComponent (new Label ("Last request took " + sw.getMillis () + " milliseconds"), "durationText");
    }

    public void setError (final List<TDEErrorType> errors) {
        _onResponseReceived ();

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

        _onResponseReceived ();

        final StringBuilder sb = new StringBuilder ();
        sb.append (" Error: ").append ("\n");
        sb.append (" - ").append ("Timeout").append ("\n");

        errorLabel = new Label (sb.toString ());
        errorLabel.setContentMode (ContentMode.PREFORMATTED);
        addComponent (errorLabel, "errorLabel");
    }

    public void setConceptErrors (final String conceptErrors) {

        _onResponseReceived ();

        conceptErrorsLabel = new Label (conceptErrors);
        conceptErrorsLabel.setContentMode (ContentMode.PREFORMATTED);
        addComponent (conceptErrorsLabel, "conceptErrorsLabel");
    }

    public void resetError () {
        removeComponent ("errorLabel");
    }

    public void addDocumentCertificateList(List<DocumentDataBean> docResponseList) {
        _onResponseReceived();

        Grid<DocumentDataBean> grid = new Grid<>();
        grid.setItems(docResponseList);
        grid.addComponentColumn(DocumentDataBean -> {
            Button certificateButton = getCertificateButton(DocumentDataBean.getDocumentURI());
            return certificateButton;
        });
        grid.addColumn(DocumentDataBean::getDocumentName).setCaption("Document Name");
        grid.addColumn(DocumentDataBean::getDocumentDescription).setCaption("Document Description");
        grid.addColumn(DocumentDataBean::getDocumentIssuePlace).setCaption("Issue Place");
        grid.addColumn(DocumentDataBean::getDocumentIssueDate).setCaption("Issue Date");
        grid.addColumn(DocumentDataBean::getDocumentIdentifier).setCaption("Document Identifier");
        grid.addColumn(DocumentDataBean::getDocumentURI).setCaption("URI");
        grid.addColumn(DocumentDataBean::getDocumentMIMEType).setCaption("MIME Type code");
        grid.setWidth("1500");
//        grid.setSizeFull();
//        return grid;
        addComponent(grid, "documentCertificateList");

    }

    public void removeMainCompanyForm () {
        removeComponent ("mainCompanyForm");
    }


    public void removeKeyValueForm () {
        removeComponent ("keyValueForm");
    }

    public String getRequestId () {
        if (requestIdLabel != null) {
            return requestIdLabel.getValue ();
        }
        return null;
    }



}
