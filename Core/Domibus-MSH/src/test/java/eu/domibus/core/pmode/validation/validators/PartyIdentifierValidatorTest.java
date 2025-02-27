package eu.domibus.core.pmode.validation.validators;

import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.Identifier;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import mockit.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Soumya Chandran
 * @since 4.2
 */
public class PartyIdentifierValidatorTest {

    @Tested
    PartyIdentifierValidator partyIdentifierValidator;

    @Injectable
    PModeValidationHelper pModeValidationHelper;

    @Injectable
    BusinessProcessValidator businessProcessValidator;

    @Test
    public void testValidate(@Injectable Configuration pMode, @Injectable Party party) {
        List<Party> parties = new ArrayList<>();
        parties.add(party);
        List<ValidationIssue> issues = new ArrayList<>();
        new Expectations(partyIdentifierValidator) {{
            pMode.getBusinessProcesses().getParties();
            result = parties;

        }};
        partyIdentifierValidator.validate(pMode);

        new Verifications() {{
            partyIdentifierValidator.validateDuplicatePartyIdentifiers((Party) any);
            partyIdentifierValidator.validateForbiddenCharactersInParty(issues, (Party) any);
        }};
    }


    @Test
    public void testValidateDuplicatePartyIdentifiers(@Injectable Party party1,
                                                      @Injectable Identifier identifier1
    ) {


        List<Identifier> identifiers = new ArrayList<>();

        identifiers.add(identifier1);
        identifiers.add(identifier1);

        new Expectations(partyIdentifierValidator) {{
            party1.getName();
            result = "blue_gw";
            identifier1.getPartyId();
            result = "domibus-blue";
            party1.getIdentifiers();
            result = identifiers;
        }};

        //tested method
        partyIdentifierValidator.validateDuplicatePartyIdentifiers(party1);

        new FullVerifications(partyIdentifierValidator) {{
            partyIdentifierValidator.createIssue(identifier1.getPartyId(), party1.getName(), anyString);
        }};
    }

    @Test
    public void testValidateDuplicateIdentifiersInAllParties(@Injectable Party party1,
                                                             @Injectable Party party2,
                                                             @Injectable Identifier identifier) {


        List<Party> allParties = new ArrayList<>();
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(identifier);

        allParties.add(party1);
        allParties.add(party2);

        final String partyId = "domibus-blue";
        new Expectations(partyIdentifierValidator) {{
            identifier.getPartyId();
            result = partyId;
            party1.getIdentifiers();
            result = identifiers;
            identifier.getPartyId();
            result = partyId;
            party2.getIdentifiers();
            result = identifiers;
        }};
        //tested method
        partyIdentifierValidator.validateDuplicateIdentifiersInAllParties(allParties);

        new FullVerifications(partyIdentifierValidator) {{
            partyIdentifierValidator.createIssue(partyId, partyId, anyString);
        }};
    }

    @Test
    public void testGetDuplicateIdentifiers(@Injectable Party party1,
                                            @Injectable Party party2,
                                            @Injectable Identifier identifier) {


        List<Party> allParties = new ArrayList<>();

        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(identifier);
        Set<Identifier> identifierSet = new HashSet<>(identifiers);
        allParties.add(party1);
        allParties.add(party2);

        List<Identifier> identifierList = partyIdentifierValidator.getDuplicateIdentifiers(identifierSet, party1);

        new FullVerifications(partyIdentifierValidator) {{
            Assert.assertNotNull(identifierList);
        }};
    }

    @Test
    public void testCreateIssue(@Injectable Party party, @Injectable Identifier identifier) {
        String partyId = "domibus-blue";
        String name = "blue-gw";
        String message = "duplicate identifier";

        //tested method
        partyIdentifierValidator.createIssue(partyId, name, message);

        new FullVerifications(partyIdentifierValidator) {{
            pModeValidationHelper.createValidationIssue(message, partyId, name);
        }};
    }

    @Test
    public void validateForbiddenCharactersInParty(@Injectable Party party, @Injectable Identifier identifier, @Injectable ValidationIssue issue) {
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(identifier);
        List<ValidationIssue> issues = new ArrayList<>();
        issues.add(issue);
        new Expectations(partyIdentifierValidator) {{
            party.getName();
            result = "party3<img src=http://localhost/222/333>";
            identifier.getPartyId();
            result = "domibus-blue<img src=http://localhost/333/333>";

            identifier.getPartyIdType().getName();
            result = "partyTypeUrn2&lt;img src=http://localhost/166/111>";

            identifier.getPartyIdType().getValue();
            result = "urn:oasis:names:tc:ebcore:partyid-type:unregistered2<img src=http://localhost/133/211>";

            party.getIdentifiers();
            result = identifiers;
        }};
        partyIdentifierValidator.validateForbiddenCharactersInParty(issues, party);
        new Verifications() {{
            businessProcessValidator.validateForbiddenCharacters(issues, anyString, anyString);
            times = 4;
        }};
    }

    @Test
    public void validateDuplicatePartyNameInAllParties(@Injectable Party party1,
                                                       @Injectable Party party2) {
        List<Party> allParties = new ArrayList<>();
        allParties.add(party1);
        allParties.add(party2);

        new Expectations() {{
            party1.getName();
            result = "blue_gw";
            party2.getName();
            result = "BLUE_gw";
        }};

        partyIdentifierValidator.validateDuplicatePartyNameInAllParties(party1, allParties);

        new FullVerifications() {{
            pModeValidationHelper.createValidationIssue("Duplicate party name [%s] found in parties", "blue_gw");
        }};
    }
}
