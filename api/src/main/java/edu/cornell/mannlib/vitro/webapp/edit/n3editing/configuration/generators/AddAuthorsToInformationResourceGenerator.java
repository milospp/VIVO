/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.i18n.I18n;
import edu.cornell.mannlib.vitro.webapp.i18n.I18nBundle;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.jena.QueryUtils;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.PublicationHasAuthorValidator;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationUtils;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.validators.AntiXssValidation;

/**
 * This is a slightly unusual generator that is used by Manage Authors on
 * information resources.
 *
 * It is intended to always be an add, and never an update.
 */
public class AddAuthorsToInformationResourceGenerator extends VivoBaseGenerator implements EditConfigurationGenerator {

    private static Log log = LogFactory.getLog(AddAuthorsToInformationResourceGenerator.class);

    private static final String prefixes =
            "@prefix core: <" + vivoCore + "> .\n" +
            "@prefix foaf: <" + foaf + "> .  \n";
    private static final String vcardFailPattern = "@prefix fail_pattern: ?createVCard .\n";
    private static final String personInstanceFailPattern = "@prefix fail_pattern: ?createPersonInstance .\n";

    @Override
    public EditConfigurationVTwo getEditConfiguration(VitroRequest vreq,
            HttpSession session) {
        EditConfigurationVTwo editConfiguration = new EditConfigurationVTwo();
        initBasics(editConfiguration, vreq);
        initPropertyParameters(vreq, session, editConfiguration);

        //Overriding URL to return to
        setUrlToReturnTo(editConfiguration, vreq);

        //set variable names
        editConfiguration.setVarNameForSubject("infoResource");
        editConfiguration.setVarNameForPredicate("predicate");
        editConfiguration.setVarNameForObject("authorshipUri");

        // Required N3
        editConfiguration.setN3Required( list( newAuthorship ) );

        // Optional N3
        editConfiguration.setN3Optional( generateN3Optional());

        editConfiguration.addNewResource("authorshipUri", DEFAULT_NS_TOKEN);
        editConfiguration.addNewResource("newPerson", DEFAULT_NS_TOKEN);
        editConfiguration.addNewResource("newOrg", DEFAULT_NS_TOKEN);
        editConfiguration.addNewResource("vcardPerson", DEFAULT_NS_TOKEN);
        editConfiguration.addNewResource("vcardName", DEFAULT_NS_TOKEN);

        //In scope
        setUrisAndLiteralsInScope(editConfiguration, vreq);

        //on Form
        setUrisAndLiteralsOnForm(editConfiguration, vreq);

        //Sparql queries
        setSparqlQueries(editConfiguration, vreq);

        //set fields
        setFields(editConfiguration, vreq, EditConfigurationUtils.getPredicateUri(vreq));

        //template file
        editConfiguration.setTemplate("addAuthorsToInformationResource.ftl");
        //add validators
        editConfiguration.addValidator(new PublicationHasAuthorValidator());

        //Adding additional data, specifically edit mode
        addFormSpecificData(editConfiguration, vreq);

        editConfiguration.addValidator(new AntiXssValidation());

        //NOITCE this generator does not run prepare() since it
        //is never an update and has no SPARQL for existing

        I18nBundle i18n = I18n.bundle(vreq);
        String title = i18n.text("manage_authors");
        Individual subject = vreq.getWebappDaoFactory().getIndividualDao().getIndividualByURI(editConfiguration.getSubjectUri());
        if( subject != null && subject.getName() != null ){
            title += " - " + subject.getName();
        }
        editConfiguration.addNewResource("pageTitle", title);

        return editConfiguration;
    }

    private void setUrlToReturnTo(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        editConfiguration.setUrlPatternToReturnTo(EditConfigurationUtils.getFormUrlWithoutContext(vreq));
    }

    private static final String newAuthorship = prefixes +
        "?authorshipUri a core:Authorship ;\n" +
        "  core:relates ?infoResource .\n" +
        "?infoResource core:relatedBy ?authorshipUri .";

    private static final String authorshipRank = prefixes +
        "?authorshipUri core:rank ?rank .";

    //first name, middle name, last name, and new perseon for new author being created, and n3 for existing person
    //if existing person selected as author
    private List<String> generateN3Optional() {
        return list(
                newPersonFirstName ,
                newPersonMiddleName,
                newPersonLastName,
                newPerson,
                
                newPersonVCardFirstName ,
                newPersonVCardMiddleName,
                newPersonVCardLastName,
                newPersonVCard,
                
                authorshipRank,
                existingPerson,
                newOrg,
                existingOrg);
    }

    private static final String newPersonFirstName = prefixes + personInstanceFailPattern +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?newPerson <http://purl.obolibrary.org/obo/ARG_2000028>  ?vcardPerson . \n" +
            "?vcardPerson <http://purl.obolibrary.org/obo/ARG_2000029>  ?newPerson . \n" +
            "?vcardPerson a <http://www.w3.org/2006/vcard/ns#Individual> . \n" +
            "?vcardPerson vcard:hasName  ?vcardName . \n" +
            "?vcardName a <http://www.w3.org/2006/vcard/ns#Name> . \n" +
            "?vcardName vcard:givenName ?firstName .";

    private static final String newPersonMiddleName = prefixes + personInstanceFailPattern +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?newPerson <http://purl.obolibrary.org/obo/ARG_2000028>  ?vcardPerson . \n" +
            "?vcardPerson <http://purl.obolibrary.org/obo/ARG_2000029>  ?newPerson . \n" +
            "?vcardPerson a vcard:Individual . \n" +
            "?vcardPerson vcard:hasName  ?vcardName . \n" +
            "?vcardName a vcard:Name . \n" +
            "?vcardName <http://vivoweb.org/ontology/core#middleName> ?middleName .";

    private static final String newPersonLastName = prefixes + personInstanceFailPattern +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?newPerson <http://purl.obolibrary.org/obo/ARG_2000028>  ?vcardPerson . \n" +
            "?vcardPerson <http://purl.obolibrary.org/obo/ARG_2000029>  ?newPerson . \n" +
            "?vcardPerson a <http://www.w3.org/2006/vcard/ns#Individual> . \n" +
            "?vcardPerson vcard:hasName  ?vcardName . \n" +
            "?vcardName a <http://www.w3.org/2006/vcard/ns#Name> . \n" +
            "?vcardName vcard:familyName ?lastName .";

    private static final String newPerson =  prefixes + personInstanceFailPattern +
        "?newPerson a foaf:Person ;\n" +
        "<" + RDFS.label.getURI() + "> ?label .\n" +
        "?authorshipUri core:relates ?newPerson .\n" +
        "?newPerson core:relatedBy ?authorshipUri . ";

    // Changes here for creating vcards for external authors
    private static final String newPersonVCardFirstName = prefixes + vcardFailPattern +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?vcardPerson a <http://www.w3.org/2006/vcard/ns#Individual> . \n" +
            "?vcardPerson vcard:hasName  ?vcardName . \n" +
            "?vcardName a <http://www.w3.org/2006/vcard/ns#Name> . \n" +
            "?vcardName vcard:givenName ?firstName .";

    private static final String newPersonVCardMiddleName = prefixes + vcardFailPattern +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?vcardPerson a vcard:Individual . \n" +
            "?vcardPerson vcard:hasName  ?vcardName . \n" +
            "?vcardName a vcard:Name . \n" +
            "?vcardName <http://vivoweb.org/ontology/core#middleName> ?middleName .";

    private static final String newPersonVCardLastName = prefixes + vcardFailPattern +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?vcardPerson a <http://www.w3.org/2006/vcard/ns#Individual> . \n" +
            "?vcardPerson vcard:hasName  ?vcardName . \n" +
            "?vcardName a <http://www.w3.org/2006/vcard/ns#Name> . \n" +
            "?vcardName vcard:familyName ?lastName .";

    // Changes here for creating vcards for external authors
    private static final String newPersonVCard =  prefixes + vcardFailPattern +
            "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .  \n" +
            "?vcardPerson a vcard:Individual ;\n" +
            "<" + RDFS.label.getURI() + "> ?label .\n" +
            "?authorshipUri core:relates ?vcardPerson .\n" +
            "?vcardPerson core:relatedBy ?authorshipUri . ";

    private static final String existingPerson = prefixes +
        "?authorshipUri core:relates ?personUri .\n" +
        "?personUri core:relatedBy ?authorshipUri .";

    private static final String newOrg =  prefixes +
        "?newOrg a foaf:Organization ;\n" +
        "<" + RDFS.label.getURI() + "> ?orgName .\n" +
        "?authorshipUri core:relates ?newOrg .\n" +
        "?newOrg core:relatedBy ?authorshipUri . ";

    private static final String existingOrg = prefixes +
        "?authorshipUri core:relates ?orgUri .\n" +
        "?orgUri core:relatedBy ?authorshipUri .";

    /** Set URIS and Literals In Scope and on form and supporting methods     */
    private void setUrisAndLiteralsInScope(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        //Uris in scope always contain subject and predicate
        HashMap<String, List<String>> urisInScope = new HashMap<String, List<String>>();
        urisInScope.put(editConfiguration.getVarNameForSubject(),
                Arrays.asList(new String[]{editConfiguration.getSubjectUri()}));
        urisInScope.put(editConfiguration.getVarNameForPredicate(),
                Arrays.asList(new String[]{editConfiguration.getPredicateUri()}));
        editConfiguration.setUrisInScope(urisInScope);
        //no literals in scope
    }

    private void setUrisAndLiteralsOnForm(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        List<String> urisOnForm = new ArrayList<String>();
        //If an existing person is being used as an author, need to get the person uri
        urisOnForm.add("personUri");
        urisOnForm.add("orgUri");

        urisOnForm.add("createPersonInstance");
        urisOnForm.add("createVCard");
        editConfiguration.setUrisOnform(urisOnForm);

        //for person who is not in system, need to add first name, last name and middle name
        //Also need to store authorship rank and label of author
        List<String> literalsOnForm = list("firstName",
                "middleName",
                "lastName",
                "rank",
                "orgName",
                "label");
        editConfiguration.setLiteralsOnForm(literalsOnForm);
    }

    /** Set SPARQL Queries and supporting methods. */
    private void setSparqlQueries(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        //Sparql queries are all empty for existing values
        //This form is different from the others that it gets multiple authors on the same page
        //and that information will be queried and stored in the additional form specific data
        editConfiguration.setSparqlForExistingUris(new HashMap<String, String>());
        editConfiguration.setSparqlForExistingLiterals(new HashMap<String, String>());
        editConfiguration.setSparqlForAdditionalUrisInScope(new HashMap<String, String>());
        editConfiguration.setSparqlForAdditionalLiteralsInScope(new HashMap<String, String>());
    }

    /**
     *
     * Set Fields and supporting methods
     */

    private void setFields(EditConfigurationVTwo editConfiguration, VitroRequest vreq, String predicateUri) {
        setLabelField(editConfiguration);
        setFirstNameField(editConfiguration);
        setMiddleNameField(editConfiguration);
        setLastNameField(editConfiguration);
        setRankField(editConfiguration);
        setPersonUriField(editConfiguration);
        setOrgUriField(editConfiguration);
        setOrgNameField(editConfiguration);
        setPatternFailFields(editConfiguration);
    }

    private void setLabelField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                setName("label").
                setValidators(list("datatype:" + RDF.dtLangString.getURI())).
                setRangeDatatypeUri(RDF.dtLangString.getURI())
                );
    }


    private void setFirstNameField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                setName("firstName").
                setValidators(list("datatype:" + RDF.dtLangString.getURI())).
                setRangeDatatypeUri(RDF.dtLangString.getURI())
                );
    }

   private void setPatternFailFields(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().setName("createVCard"));
        editConfiguration.addField(new FieldVTwo().setName("createPersonInstance"));
    }

    private void setMiddleNameField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                setName("middleName").
                setValidators(list("datatype:" + RDF.dtLangString.getURI())).
                setRangeDatatypeUri(RDF.dtLangString.getURI())
                );
    }

    private void setLastNameField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                setName("lastName").
                setValidators(list("datatype:" + RDF.dtLangString.getURI())).
                setRangeDatatypeUri(RDF.dtLangString.getURI())
                );
    }

    private void setRankField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                setName("rank").
                setValidators(list("nonempty")).
                setRangeDatatypeUri(XSD.xint.toString())
                );
    }


    private void setPersonUriField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                setName("personUri")
                //.setObjectClassUri(personClass)
                );
    }

    private void setOrgUriField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                setName("orgUri")
                //.setObjectClassUri(personClass)
                );
    }

    private void setOrgNameField(EditConfigurationVTwo editConfiguration) {
        editConfiguration.addField(new FieldVTwo().
                setName("orgName").
                setValidators(list("datatype:" + RDF.dtLangString.getURI())).
                setRangeDatatypeUri(RDF.dtLangString.getURI())
                );
    }

    //Form specific data
    public void addFormSpecificData(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
        HashMap<String, Object> formSpecificData = new HashMap<String, Object>();
        //Get the existing authorships
        formSpecificData.put("existingAuthorInfo", getExistingAuthorships(editConfiguration.getSubjectUri(), vreq));
        formSpecificData.put("newRank", getMaxRank(editConfiguration.getSubjectUri(), vreq) + 1);
        formSpecificData.put("rankPredicate", "http://vivoweb.org/ontology/core#rank");
        editConfiguration.setFormSpecificData(formSpecificData);
    }

    private static String AUTHORSHIPS_MODEL = " \n"
            + "PREFIX core: <http://vivoweb.org/ontology/core#>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
            + "PREFIX vcard:  <http://www.w3.org/2006/vcard/ns#>\n"
            + "CONSTRUCT\n"
            + "{\n"
            + "    ?subject core:relatedBy ?authorshipURI .\n"
            + "    ?authorshipURI a core:Authorship .\n"
            + "    ?authorshipURI core:relates ?authorURI .\n"
            + "    ?authorshipURI core:rank ?rank .\n"
            + "    ?authorURI a ?type .\n"
            + "    ?authorURI rdfs:label ?authorName .\n"
            + "    ?authorURI vcard:hasName ?vName .\n"
            + "    ?vName vcard:givenName ?firstName .\n"
            + "    ?vName vcard:familyName ?lastName .\n"
            + "    ?vName core:middleName ?middleName .\n"
            + "}\n"
            + "WHERE\n"
            + "{\n"
            + "    {\n"
            + "        ?subject core:relatedBy ?authorshipURI .\n"
            + "        ?authorshipURI a core:Authorship .\n"
            + "        ?authorshipURI core:relates ?authorURI .\n"
            + "        ?authorURI a foaf:Agent .\n"
            + "        ?authorURI a ?type .\n"
            + "    }\n"
            + "    UNION\n"
            + "    {\n"
            + "        ?subject core:relatedBy ?authorshipURI .\n"
            + "        ?authorshipURI a core:Authorship .\n"
            + "        ?authorshipURI core:relates ?authorURI .\n"
            + "        ?authorURI a foaf:Agent .\n"
            + "        ?authorURI rdfs:label ?authorName\n"
            + "    }\n"
            + "    UNION\n"
            + "    {\n"
            + "        ?subject core:relatedBy ?authorshipURI .\n"
            + "        ?authorshipURI a core:Authorship .\n"
            + "        ?authorshipURI core:rank ?rank\n"
            + "    }\n"
            + "    UNION\n"
            + "    {\n"
            + "        ?subject core:relatedBy ?authorshipURI .\n"
            + "        ?authorshipURI a core:Authorship .\n"
            + "        ?authorshipURI core:relates ?authorURI .\n"
            + "        ?authorURI a vcard:Individual .\n"
            + "        ?authorURI a ?type .\n"
            + "        ?authorURI vcard:hasName ?vName .\n"
            + "        ?vName vcard:givenName ?firstName .\n"
            + "        ?vName vcard:familyName ?lastName .\n"
            + "    }\n"
            + "    UNION\n"
            + "    {\n"
            + "         ?subject core:relatedBy ?authorshipURI .\n"
            + "         ?authorshipURI a core:Authorship .\n"
            + "         ?authorshipURI core:relates ?authorURI .\n"
            + "         ?authorURI a vcard:Individual .\n"
            + "         ?authorURI a ?type .\n"
            + "         ?authorURI vcard:hasName ?vName .\n"
            + "         ?vName core:middleName ?middleName .\n"
            + "    }\n"
            + "}\n";

    private static String AUTHORSHIPS_QUERY = " \n"
        + "PREFIX core: <http://vivoweb.org/ontology/core#> \n"
        + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
        + "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
        + "PREFIX vcard:  <http://www.w3.org/2006/vcard/ns#> \n"
        + "SELECT ?authorshipURI (REPLACE(STR(?authorshipURI),\"^.*(#)(.*)$\", \"$2\") AS ?authorshipName) ?authorURI ?authorName ?rank \n"
        + "WHERE { { \n"
        + "  ?subject core:relatedBy ?authorshipURI . \n"
        + "  ?authorshipURI a core:Authorship . \n"
        + "  ?authorshipURI core:relates ?authorURI . \n"
        + "  ?authorURI a foaf:Agent . \n"
        + "  OPTIONAL { ?authorURI rdfs:label ?authorName } \n"
        + "  OPTIONAL { ?authorshipURI core:rank ?rank } \n"
        + "} UNION {  \n"
        + "     ?subject core:relatedBy ?authorshipURI .  \n"
        + "     ?authorshipURI a core:Authorship .  \n"
        + "     ?authorshipURI core:relates ?authorURI .  \n"
        + "     ?authorURI a vcard:Individual .  \n"
        + "     ?authorURI vcard:hasName ?vName . \n"
        + "     ?vName vcard:givenName ?firstName . \n"
        + "     ?vName vcard:familyName ?lastName . \n"
        + "     OPTIONAL { ?vName core:middleName ?middleName . } \n"
        + "     OPTIONAL { ?authorshipURI core:rank ?rank }  \n"
        + "     bind ( COALESCE(?firstName, \"\") As ?firstName1) . \n"
        + "     bind ( COALESCE(?middleName, \"\") As ?middleName1) . \n"
        + "     bind ( COALESCE(?lastName, \"\") As ?lastName1) . \n"
        + "     bind (concat(str(str(?lastName1) + \", \"),str(str(?middleName1) + \" \"),str(?firstName1)) as ?authorName) . \n"
        + "} } ORDER BY ?rank";


    private List<AuthorshipInfo> getExistingAuthorships(String subjectUri, VitroRequest vreq) {
        RDFService rdfService = vreq.getRDFService();

        List<Map<String, String>> authorships = new ArrayList<Map<String, String>>();
        try {
            String constructStr = QueryUtils.subUriForQueryVar(AUTHORSHIPS_MODEL, "subject", subjectUri);

            Model constructedModel = ModelFactory.createDefaultModel();
            rdfService.sparqlConstructQuery(constructStr, constructedModel);

            String queryStr = QueryUtils.subUriForQueryVar(getAuthorshipsQuery, "subject", subjectUri);
            log.debug("Query string is: " + queryStr);

            QueryExecution qe = QueryExecutionFactory.create(queryStr, constructedModel);
            try {
                ResultSet results = qe.execSelect();
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    RDFNode node = soln.get("authorshipURI");
                    if (node.isURIResource()) {
                        authorships.add(QueryUtils.querySolutionToStringValueMap(soln));
                    }
                }
            } finally {
                qe.close();
            }
        } catch (Exception e) {
            log.error(e, e);
        }
        authorships = QueryUtils.removeDuplicatesMapsFromList(authorships, "authorShipURI", "authorURI");
        log.debug("authorships = " + authorships);
        return getAuthorshipInfo(authorships);
    }

    private static String MAX_RANK_QUERY = ""
        + "PREFIX core: <http://vivoweb.org/ontology/core#> \n"
        + "SELECT DISTINCT ?rank WHERE { \n"
        + "    ?subject core:relatedBy ?authorship . \n"
        + "    ?authorship a core:Authorship . \n"
        + "    ?authorship core:rank ?rank .\n"
        + "} ORDER BY DESC(?rank) LIMIT 1";

    private int getMaxRank(String subjectUri, VitroRequest vreq) {

        int maxRank = 0; // default value
        String queryStr = QueryUtils.subUriForQueryVar(getMaxRankQueryStr, "subject", subjectUri);
        log.debug("maxRank query string is: " + queryStr);
        try {
            ResultSet results = QueryUtils.getQueryResults(queryStr, vreq);
            if (results != null && results.hasNext()) { // there is at most one result
                QuerySolution soln = results.next();
                RDFNode node = soln.get("rank");
                if (node != null && node.isLiteral()) {
                    // node.asLiteral().getInt() won't return an xsd:string that
                    // can be parsed as an int.
                    int rank = Integer.parseInt(node.asLiteral().getLexicalForm());
                    if (rank > maxRank) {
                        log.debug("setting maxRank to " + rank);
                        maxRank = rank;
                    }
                }
            }
        } catch (NumberFormatException e) {
            log.error("Invalid rank returned from query: not an integer value.");
        } catch (Exception e) {
            log.error(e, e);
        }
        log.debug("maxRank is: " + maxRank);
        return maxRank;
    }

    private List<AuthorshipInfo> getAuthorshipInfo(
            List<Map<String, String>> authorships) {
        List<AuthorshipInfo> info = new ArrayList<AuthorshipInfo>();
         String authorshipUri =  "";
         String authorshipName = "";
         String authorUri = "";
         String authorName = "";

        for ( Map<String, String> authorship : authorships ) {
            for (Entry<String, String> entry : authorship.entrySet() ) {
                    if ( entry.getKey().equals("authorshipURI") ) {
                        authorshipUri = entry.getValue();
                    }
                    else if ( entry.getKey().equals("authorshipName") ) {
                        authorshipName = entry.getValue();
                    }
                    else if ( entry.getKey().equals("authorURI") ) {
                        authorUri = entry.getValue();
                    }
                    else if ( entry.getKey().equals("authorName") ) {
                        authorName = entry.getValue();
                    }
             }

             AuthorshipInfo aaInfo = new AuthorshipInfo(authorshipUri, authorshipName, authorUri, authorName);
            info.add(aaInfo);
         }
         log.debug("info = " + info);
         return info;
    }

    // This is the information about authors the form will require
    public class AuthorshipInfo {
        // This is the authorship node information
        private String authorshipUri;
        private String authorshipName;
        // Author information for authorship node
        private String authorUri;
        private String authorName;

        public AuthorshipInfo(String inputAuthorshipUri, String inputAuthorshipName, String inputAuthorUri,
                String inputAuthorName) {
            authorshipUri = inputAuthorshipUri;
            authorshipName = inputAuthorshipName;
            authorUri = inputAuthorUri;
            authorName = inputAuthorName;

        }

        // Getters - specifically required for Freemarker template's access to POJO
        public String getAuthorshipUri() {
            return authorshipUri;
        }

        public String getAuthorshipName() {
            return authorshipName;
        }

        public String getAuthorUri() {
            return authorUri;
        }

        public String getAuthorName() {
            return authorName;
        }
    }

    private static final String DEFAULT_NS_TOKEN = null; // null forces the default NS

    private String getMaxRankQueryStr = MAX_RANK_QUERY;

    private String getAuthorshipsQuery = AUTHORSHIPS_QUERY;

}
