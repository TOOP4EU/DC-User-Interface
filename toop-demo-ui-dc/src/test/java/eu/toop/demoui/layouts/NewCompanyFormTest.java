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

import eu.toop.demoui.bean.NewCompany;

public class NewCompanyFormTest {
  @Test
  public void testBasic () {

    final String exemptions = "Test exemptions";
    final String hazardousMaterials = "Test hazardous materials";
    final String producerComplianceScheme = "Test producer compliance scheme";
    final String wasteDisposalProcess = "Test waste disposal process";

    final String newExemptions = "New Test exemptions";
    final String newHazardousMaterials = "New Test hazardous materials";
    final String newProducerComplianceScheme = "New Test producer compliance scheme";
    final String newWasteDisposalProcess = "New Test waste disposal process";

    final NewCompany newCompany = new NewCompany ();
    newCompany.setExemptions (exemptions);
    newCompany.setHazardousMaterials (hazardousMaterials);
    newCompany.setProducerComplianceScheme (producerComplianceScheme);
    newCompany.setWasteDisposalProcess (wasteDisposalProcess);

    final NewCompanyForm newCompanyForm = new NewCompanyForm (newCompany, false);

    for (final Component comp : newCompanyForm) {
      if (comp instanceof TextField) {
        final TextField textField = (TextField)comp;
        switch (textField.getCaption ()) {
          case "Exemptions":
            textField.setValue (newExemptions);
            break;
          case "Hazardous materials":
            textField.setValue (newHazardousMaterials);
            break;
          case "Producer compliance scheme":
            textField.setValue (newProducerComplianceScheme);
            break;
          case "Waste disposal process":
            textField.setValue (newWasteDisposalProcess);
            break;
        }
      }
    }
    newCompanyForm.save();

    assertEquals (newExemptions, newCompany.getExemptions ());
    assertEquals (newHazardousMaterials, newCompany.getHazardousMaterials ());
    assertEquals (newProducerComplianceScheme, newCompany.getProducerComplianceScheme ());
    assertEquals (newWasteDisposalProcess, newCompany.getWasteDisposalProcess ());
  }
}
