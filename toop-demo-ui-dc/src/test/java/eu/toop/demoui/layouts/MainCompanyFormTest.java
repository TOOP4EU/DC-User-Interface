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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;

import eu.toop.demoui.bean.ToopDataBean;

public class MainCompanyFormTest {
  @Test
  public void testBasic () {

    final String activityDeclaration = "Manufacture of other electrical equipment";
    final String address = "Gamlavegen 234, 321 44, Velma, Elonia";
    final String businessCode = "JF 234556-6213";
    final String companyNaceCode = "C27.9";
    final String companyName = "Zizi mat";
    final String companyType = "Limited";
    final String legalStatus = "Active";
    final String legalStatusEffectiveDate = "2012-01-12";
    final String registrationAuthority = "Elonia Tax Agency";
    final String registrationDate = "2012-01-12";
    final String registrationNumber = "009987 665543";
    final String ssNumber = "PKL 0987 6548";
    final String vatNumber = "09897656";

    final String newActivityDeclaration = "New Manufacture of other electrical equipment";
    final String newAddress = "New Gamlavegen 234, 321 44, Velma, Elonia";
    final String newBusinessCode = "New JF 234556-6213";
    final String newCompanyNaceCode = "New C27.9";
    final String newCompanyName = "New Zizi mat";
    final String newCompanyType = "New Limited";
    final String newLegalStatus = "New Active";
    final String newLegalStatusEffectiveDate = "New 2012-01-12";
    final String newRegistrationAuthority = "New Elonia Tax Agency";
    final String newRegistrationDate = "New 2012-01-12";
    final String newRegistrationNumber = "New 009987 665543";
    final String newSsNumber = "New PKL 0987 6548";
    final String newVatNumber = "New 09897656";

    final ToopDataBean toopDataBean = new ToopDataBean();
    toopDataBean.setActivityDeclaration (activityDeclaration);
    toopDataBean.setAddress (address);
    toopDataBean.setBusinessCode (businessCode);
    toopDataBean.setCompanyNaceCode (companyNaceCode);
    toopDataBean.setCompanyName (companyName);
    toopDataBean.setCompanyType (companyType);
    toopDataBean.setLegalStatus (legalStatus);
    toopDataBean.setLegalStatusEffectiveDate (legalStatusEffectiveDate);
    toopDataBean.setRegistrationAuthority (registrationAuthority);
    toopDataBean.setRegistrationDate (registrationDate);
    toopDataBean.setRegistrationNumber (registrationNumber);
    toopDataBean.setSSNumber (ssNumber);
    toopDataBean.setVATNumber (vatNumber);

    final MainCompanyForm mainCompanyForm = new MainCompanyForm (toopDataBean, false);

    for (final Component comp : mainCompanyForm) {
      if (comp instanceof TextField) {
        final TextField textField = (TextField)comp;
        switch (textField.getCaption ()) {
          case "Address":
            textField.setValue (newAddress);
            break;
          case "SS number":
            textField.setValue (newSsNumber);
            break;
          case "Company code":
            textField.setValue (newBusinessCode);
            break;
          case "VAT number":
            textField.setValue (newVatNumber);
            break;
          case "Company type":
            textField.setValue (newCompanyType);
            break;
          case "Legal status":
            textField.setValue (newLegalStatus);
            break;
          case "Legal status effective date":
            textField.setValue (newLegalStatusEffectiveDate);
            break;
          case "Registration date":
            textField.setValue (newRegistrationDate);
            break;
          case "Registration number":
            textField.setValue (newRegistrationNumber);
            break;
          case "Company name":
            textField.setValue (newCompanyName);
            break;
          case "Company nace code":
            textField.setValue (newCompanyNaceCode);
            break;
          case "Activity declaration":
            textField.setValue (newActivityDeclaration);
            break;
          case "Registration authority":
            textField.setValue (newRegistrationAuthority);
            break;
        }
      }
    }
    mainCompanyForm.save();

    assertEquals (newActivityDeclaration, toopDataBean.getActivityDeclaration ());
    assertEquals (newAddress, toopDataBean.getAddress ());
    assertEquals (newBusinessCode, toopDataBean.getBusinessCode ());
    assertEquals (newCompanyNaceCode, toopDataBean.getCompanyNaceCode ());
    assertEquals (newCompanyName, toopDataBean.getCompanyName ());
    assertEquals (newCompanyType, toopDataBean.getCompanyType ());
    assertEquals (newLegalStatus, toopDataBean.getLegalStatus ());
    assertEquals (newLegalStatusEffectiveDate, toopDataBean.getLegalStatusEffectiveDate ());
    assertEquals (newRegistrationAuthority, toopDataBean.getRegistrationAuthority ());
    assertEquals (newRegistrationDate, toopDataBean.getRegistrationDate ());
    assertEquals (newRegistrationNumber, toopDataBean.getRegistrationNumber ());
    assertEquals (newSsNumber, toopDataBean.getSSNumber ());
    assertEquals (newVatNumber, toopDataBean.getVATNumber ());
  }
}
