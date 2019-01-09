/**
 * Copyright (C) 2018 toop.eu
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
package eu.toop.demoui.endpoints;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import com.helger.commons.error.level.EErrorLevel;

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.codelist.ReverseDocumentTypeMapping;
import eu.toop.commons.dataexchange.v120.TDEAddressType;
import eu.toop.commons.dataexchange.v120.TDEConceptRequestType;
import eu.toop.commons.dataexchange.v120.TDEDataElementRequestType;
import eu.toop.commons.dataexchange.v120.TDEDataElementResponseValueType;
import eu.toop.commons.dataexchange.v120.TDEDataProviderType;
import eu.toop.commons.dataexchange.v120.TDEDataRequestSubjectType;
import eu.toop.commons.dataexchange.v120.TDETOOPRequestType;
import eu.toop.commons.dataexchange.v120.TDETOOPResponseType;
import eu.toop.commons.error.ToopErrorException;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.commons.jaxb.ToopWriter;
import eu.toop.commons.jaxb.ToopXSDHelper;
import eu.toop.demoui.DCUIConfig;
import eu.toop.iface.IToopInterfaceDP;
import eu.toop.iface.ToopInterfaceClient;
import eu.toop.kafkaclient.ToopKafkaClient;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.TextType;

public class DemoUIToopInterfaceDP implements IToopInterfaceDP {

  private static boolean _canUseConcept (@Nonnull final TDEConceptRequestType aConcept) {
    // This class can only deliver to "DP" concept types without child entries
    return aConcept.hasNoConceptRequestEntries () && "DP".equals (aConcept.getConceptTypeCode ().getValue ());
  }

  private static void _applyStaticDataset (final TDEDataRequestSubjectType ds,
      @Nonnull final TDEConceptRequestType aConcept, @Nonnull final String sLogPrefix,
      final DCUIConfig.Dataset dataset) {

    final TextType conceptName = aConcept.getConceptName ();
    final TDEDataElementResponseValueType aValue = new TDEDataElementResponseValueType ();
    aConcept.getDataElementResponseValue ().add (aValue);

    if (conceptName == null) {
      aValue.setErrorIndicator (ToopXSDHelper.createIndicator (true));
      aValue.setErrorCode (ToopXSDHelper.createCode ("MockError from DemoDP: Concept name missing"));
      ToopKafkaClient.send (EErrorLevel.ERROR, () -> sLogPrefix + "Concept name missing: " + aConcept);
      return;
    }

    aValue.setAlternativeResponseIndicator (ToopXSDHelper.createIndicator (false));
    aValue.setErrorIndicator (ToopXSDHelper.createIndicator (false));

    if (dataset != null) {
      final String conceptValue = dataset.getConceptValue (conceptName.getValue ());

      if (conceptValue == null) {
        aValue.setErrorIndicator (ToopXSDHelper.createIndicator (true));
        aValue.setErrorCode (ToopXSDHelper
            .createCode ("MockError from DemoDP: Concept [" + conceptName.getValue () + "] is missing in dataset"));
        ToopKafkaClient.send (EErrorLevel.ERROR, () -> sLogPrefix + "Failed to populate concept: Concept ["
            + conceptName.getValue () + "] is missing in dataset");
        return;
      }

      aValue.setResponseDescription (ToopXSDHelper.createText (conceptValue));

      ToopKafkaClient.send (EErrorLevel.INFO,
          () -> sLogPrefix + "Populated concept [" + conceptName.getValue () + "]: [" + conceptValue + "]");
    } else {

      aValue.setErrorIndicator (ToopXSDHelper.createIndicator (true));
      aValue.setErrorCode (ToopXSDHelper.createCode ("MockError from DemoDP: No dataset found"));
    }
  }

  @Nonnull
  private static TDETOOPResponseType _createResponseFromRequest (@Nonnull final TDETOOPRequestType aRequest,
      @Nonnull final String sLogPrefix) {
    // build response
    final TDETOOPResponseType aResponse = ToopMessageBuilder.createResponse (aRequest);
    {
      // Required for response
      final TDEDataProviderType p = new TDEDataProviderType ();
      p.setDPIdentifier (ToopXSDHelper.createIdentifier (DCUIConfig.getResponderIdentifierScheme (),
          DCUIConfig.getResponderIdentifierValue ()));
      p.setDPName (ToopXSDHelper.createText ("EloniaDP"));
      p.setDPElectronicAddressIdentifier (ToopXSDHelper.createIdentifier ("elonia@register.example.org"));
      final TDEAddressType pa = new TDEAddressType ();
      pa.setCountryCode (ToopXSDHelper.createCode (DCUIConfig.getProviderCountryCode ()));
      p.setDPLegalAddress (pa);
      aResponse.addDataProvider (p);
    }

    // Document type must be switch from request to response
    final EPredefinedDocumentTypeIdentifier eRequestDocType = EPredefinedDocumentTypeIdentifier
        .getFromDocumentTypeIdentifierOrNull (aRequest.getDocumentTypeIdentifier ().getSchemeID (),
            aRequest.getDocumentTypeIdentifier ().getValue ());
    if (eRequestDocType != null) {
      try {
        final EPredefinedDocumentTypeIdentifier eResponseDocType = ReverseDocumentTypeMapping
            .getReverseDocumentType (eRequestDocType);

        // Set new doc type in response
        ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Switching document type '"
            + eRequestDocType.getURIEncoded () + "' to '" + eResponseDocType.getURIEncoded () + "'");
        aResponse.setDocumentTypeIdentifier (
            ToopXSDHelper.createIdentifier (eResponseDocType.getScheme (), eResponseDocType.getID ()));
      } catch (final IllegalArgumentException ex) {
        // Found no reverse document type
        ToopKafkaClient.send (EErrorLevel.INFO,
            () -> sLogPrefix + "Found no response document type for '"
                + aRequest.getDocumentTypeIdentifier ().getSchemeID () + "::"
                + aRequest.getDocumentTypeIdentifier ().getValue () + "'");
      }
    }
    return aResponse;
  }

  private static void applyConceptValues (final TDEDataRequestSubjectType ds, final TDEDataElementRequestType aDER,
      final String sLogPrefix, final DCUIConfig.Dataset dataset) {

    final TDEConceptRequestType aFirstLevelConcept = aDER.getConceptRequest ();
    if (aFirstLevelConcept != null) {
      for (final TDEConceptRequestType aSecondLevelConcept : aFirstLevelConcept.getConceptRequest ()) {
        for (final TDEConceptRequestType aThirdLevelConcept : aSecondLevelConcept.getConceptRequest ()) {
          if (_canUseConcept (aThirdLevelConcept)) {
            _applyStaticDataset (ds, aThirdLevelConcept, sLogPrefix, dataset);
          } else {
            // 3 level nesting is maximum
            ToopKafkaClient.send (EErrorLevel.ERROR,
                () -> sLogPrefix + "A third level concept that is unusable - weird: " + aThirdLevelConcept);
          }
        }
      }
    }
  }

  public void onToopRequest (@Nonnull final TDETOOPRequestType aRequest) throws IOException {

    final String sRequestID = aRequest.getDataRequestIdentifier ().getValue ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DP Backend Request");

    dumpRequest (aRequest);

    // Record matching to dataset

    // Try to find dataset for natural person
    final TDEDataRequestSubjectType ds = aRequest.getDataRequestSubject ();
    String naturalPersonIdentifier = null;
    String legalEntityIdentifier = null;

    if (ds.getNaturalPerson () != null) {
      if (ds.getNaturalPerson ().getPersonIdentifier () != null) {
        if (!ds.getNaturalPerson ().getPersonIdentifier ().getValue ().isEmpty ()) {
          ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Record matching natural person: "
              + ds.getNaturalPerson ().getPersonIdentifier ().getValue ());
          naturalPersonIdentifier = ds.getNaturalPerson ().getPersonIdentifier ().getValue ();
        }
      }
    }

    if (ds.getLegalEntity () != null) {
      if (ds.getLegalEntity ().getLegalPersonUniqueIdentifier () != null) {
        if (!ds.getLegalEntity ().getLegalPersonUniqueIdentifier ().getValue ().isEmpty ()) {
          ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Record matching legal person: "
              + ds.getLegalEntity ().getLegalPersonUniqueIdentifier ().getValue ());
          legalEntityIdentifier = ds.getLegalEntity ().getLegalPersonUniqueIdentifier ().getValue ();
        }
      }
    }

    // Get datasets from config
    final DCUIConfig dcuiConfig = new DCUIConfig ();

    final List<DCUIConfig.Dataset> datasets = dcuiConfig.getDatasetsByIdentifier (naturalPersonIdentifier,
        legalEntityIdentifier);

    DCUIConfig.Dataset dataset = null;
    if (datasets.size () > 0) {
      dataset = datasets.get (0);
    } else {
      ToopKafkaClient.send (EErrorLevel.ERROR, () -> sLogPrefix + "No dataset found");
    }

    // build response
    final TDETOOPResponseType aResponse = _createResponseFromRequest (aRequest, sLogPrefix);

    // add all the mapped values in the response
    for (final TDEDataElementRequestType aDER : aResponse.getDataElementRequest ()) {
      applyConceptValues (aRequest.getDataRequestSubject (), aDER, sLogPrefix, dataset);
    }

    // send back to toop-connector at /from-dp
    // The URL must be configured in toop-interface.properties file
    try {
      dumpResponse (aResponse);
      ToopInterfaceClient.sendResponseToToopConnector (aResponse);
    } catch (final ToopErrorException ex) {
      throw new RuntimeException (ex);
    }
  }

  private void dumpRequest (@Nonnull final TDETOOPRequestType aRequest) {

    FileWriter fw = null;
    try {

      final DateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS");
      final String filePath = String.format ("%s/request-dump-%s.log", DCUIConfig.getDumpResponseDirectory (),
          dateFormat.format (new Date ()));

      final String requestXml = ToopWriter.request ().getAsString (aRequest);
      fw = new FileWriter (filePath);
      if (requestXml != null) {
        fw.write (requestXml);
      }
    } catch (final IOException e) {
      e.printStackTrace ();
    } finally {
      if (fw != null) {
        try {
          fw.close ();
        } catch (final IOException e) {
          e.printStackTrace ();
        }
      }
    }
  }

  private void dumpResponse (@Nonnull final TDETOOPResponseType aResponse) {

    FileWriter fw = null;
    try {

      final DateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS");
      final String filePath = String.format ("%s/response-dump-%s.log", DCUIConfig.getDumpResponseDirectory (),
          dateFormat.format (new Date ()));

      final String responseXml = ToopWriter.response ().getAsString (aResponse);
      fw = new FileWriter (filePath);
      if (responseXml != null) {
        fw.write (responseXml);
      }
    } catch (final IOException e) {
      e.printStackTrace ();
    } finally {
      if (fw != null) {
        try {
          fw.close ();
        } catch (final IOException e) {
          e.printStackTrace ();
        }
      }
    }
  }
}
