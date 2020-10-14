package org.bonitasoft.explorer.external;

public class DatabaseDefinition {

    /**
     * List of variable
     * The Explorer is not responsible to create the database, see the Truckmilk job MilkMoveArchive
     * - data may be moved even the explorer is not used, so truckmilk is consistent
     * - the table construction is useful in truckmilk for any another job
     * 
     */
    /**
     * ProcessInstance
     */
    public static final String BDE_TABLE_PROCESSINSTANCE = "Bonita_Process";

    public static final String BDE_PROCESSINSTANCE_STRINGINDEX5 = "StringIndex5";
    public static final String BDE_PROCESSINSTANCE_STRINGINDEX4 = "StringIndex4";
    public static final String BDE_PROCESSINSTANCE_STRINGINDEX3 = "StringIndex3";
    public static final String BDE_PROCESSINSTANCE_STRINGINDEX2 = "StringIndex2";
    public static final String BDE_PROCESSINSTANCE_STRINGINDEX1 = "StringIndex1";
    public static final String BDE_PROCESSINSTANCE_ARCHIVEDATE = "ArchivedDate";
    public static final String BDE_PROCESSINSTANCE_STARTEDBY = "StartedBy";
    public static final String BDE_PROCESSINSTANCE_STARTEDBYNAME = "StartedByName";
    public static final String BDE_PROCESSINSTANCE_STARTEDBYSUBSTITUTE = "StartedBySubstitute";
    public static final String BDE_PROCESSINSTANCE_STARTEDBYSUBSTITUTENAME = "StartedBySubstituteName";
    public static final String BDE_PROCESSINSTANCE_END_DATE = "EndDate";
    public static final String BDE_PROCESSINSTANCE_START_DATE = "StartDate";
    public static final String BDE_PROCESSINSTANCE_PROCESSINSTANCEID = "processinstanceid";
    public static final String BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID = "rootprocessinstanceid";
    public static final String BDE_PROCESSINSTANCE_PARENTPROCESSINSTANCEID = "parentprocessinstanceid";
    public static final String BDE_PROCESSINSTANCE_PROCESSDEFINITIONVERSION = "processdefinitionversion";
    public static final String BDE_PROCESSINSTANCE_PROCESSDEFINITIONNAME = "processdefinitionname";
    public static final String BDE_PROCESSINSTANCE_PROCESSDEFINITIONID = "processdefinitionid";
    public static final String BDE_PROCESSINSTANCE_TENANTID = "tenantid";

    /**
     * Datainstance
     */
    public static final String BDE_TABLE_DATAINSTANCE = "Bonita_Data";

    public static final String BDE_DATAINSTANCE_TENANTID = "tenantid";
    public static final String BDE_DATAINSTANCE_NAME = "name";
    public static final String BDE_DATAINSTANCE_SCOPE = "scope";
    public static final String BDE_DATAINSTANCE_ID = "id";
    public static final String BDE_DATAINSTANCE_DESCRIPTION = "description";

    public static final String BDE_DATAINSTANCE_PROCESSINSTANCEID = "processinstanceid";
    // variable can be local : in that circunstance, the ACTIVITYID is set
    public static final String BDE_DATAINSTANCE_ACTIVITYID = "activityid";

    public static final String BDE_DATAINSTANCE_CONTAINERTYPE = "containertype";
    public static final String BDE_DATAINSTANCE_CONTAINERTYPE_V_PROCESS = "PROCESS";
    public static final String BDE_DATAINSTANCE_CONTAINERTYPE_V_ACTIVITY = "ACTIVITY";
    public static final String BDE_DATAINSTANCE_CONTAINERTYPE_V_BDM = "BDM";

    public static final String BDE_DATAINSTANCE_ARCHIVEDATE = "archivedate";

    public static final String BDE_DATAINSTANCE_CLASSNAME = "classname";
    public static final String BDE_DATAINSTANCE_VALUE = "value";
    public static final String BDE_DATAINSTANCE_FLOATVALUE = "floatvalue";
    public static final String BDE_DATAINSTANCE_DOUBLEVALUE = "doublevalue";
    public static final String BDE_DATAINSTANCE_BOOLEANVALUE = "booleanvalue";
    public static final String BDE_DATAINSTANCE_DATEVALUE = "datevalue";
    public static final String BDE_DATAINSTANCE_LONGVALUE = "longvalue";

    // For a BDM variable. BDM Name is the equivalent of the ClassName     
    public static final String BDE_DATAINSTANCE_BDMNAME = "bdmname";
    public static final String BDE_DATAINSTANCE_BDMISMULTIPLE = "bdmismultiple";
    public static final String BDE_DATAINSTANCE_BDMINDEX = "bdmindex";
    public static final String BDE_DATAINSTANCE_BDMPERSISTENCEID = "bdmpersistenceid";

    /**
     * FlowNode
     */
    public static final String BDE_TABLE_FLOWNODEINSTANCE = "Bonita_Flownode";

    public static final String BDE_FLOWNODEINSTANCE_TENANTID = "tenantid";
    public static final String BDE_FLOWNODEINSTANCE_ID = "id";
    public static final String BDE_FLOWNODEINSTANCE_FLOWNODEDEFINITIONID = "flownodefinitionid";
    public static final String BDE_FLOWNODEINSTANCE_KIND = "kind";
    public static final String BDE_FLOWNODEINSTANCE_ARCHIVEDATE = "archivedate";
    public static final String BDE_FLOWNODEINSTANCE_ROOTCONTAINERID = "rootcontainerid";
    public static final String BDE_FLOWNODEINSTANCE_PROCESSINSTANCEID = "processinstanceid";
    public static final String BDE_FLOWNODEINSTANCE_PARENTCONTAINERID = "parentcontainerid";
    public static final String BDE_FLOWNODEINSTANCE_SOURCEOBJECTID = "sourceobjectid";
    public static final String BDE_FLOWNODEINSTANCE_NAME = "name";
    public static final String BDE_FLOWNODEINSTANCE_DISPLAYNAME = "displayname";
    public static final String BDE_FLOWNODEINSTANCE_DISPLAYDESCRIPTION="displaydescription";
    public static final String BDE_FLOWNODEINSTANCE_STATENAME = "statename";
    public static final String BDE_FLOWNODEINSTANCE_REACHEDSTATEDATE = "reachedstatedate";

    public static final String BDE_FLOWNODEINSTANCE_GATEWAYTYPE = "gatewaytype";
    public static final String BDE_FLOWNODEINSTANCE_LOOP_COUNTER = "loopcounter";
    public static final String BDE_FLOWNODEINSTANCE_NUMBEROFINSTANCES = "numberofinstances";
    // LOOP_MAX
    // LOOPCARDINALITY
    // LOOPDATAINPUTREF
    // LOOPDATAOUTPUTREF
    // DESCRIPTION
    // SEQUENTIAL
    //DATAINPUTITEMREF
    //DATAOUTPUTITEMREF
    // NBACTIVEINST
    // NBCOMPLETEDINST
    // NBTERMINATEDINST
    public static final String BDE_FLOWNODEINSTANCE_EXECUTEDBY = "executedby";
    public static final String BDE_FLOWNODEINSTANCE_EXECUTEDBYNAME = "executedbyname";
    public static final String BDE_FLOWNODEINSTANCE_EXECUTEDBYSUBSTITUTE = "executedbysubstitute";
    public static final String BDE_FLOWNODEINSTANCE_EXECUTEDBYSUBSTITUTENAME = "executedbysubstitutename";
    // public static final String BDE_FLOWNODEINSTANCE_ACTIVITYINSTANCEID
    // ABORTING
    // TRIGGEREDBYEVENT
    // INTERRUPTING

    /**
     * Document
     */
    public static final String BDE_TABLE_DOCUMENTINSTANCE = "Bonita_Document";

    public static final String BDE_DOCUMENTINSTANCE_TENANTID = "tenantid";
    public static final String BDE_DOCUMENTINSTANCE_ID = "id";
    public static final String BDE_DOCUMENTINSTANCE_NAME = "name";
    public static final String BDE_DOCUMENTINSTANCE_PROCESSINSTANCEID = "processinstanceid";
    public static final String BDE_DOCUMENTINSTANCE_VERSION = "version";
    public static final String BDE_DOCUMENTINSTANCE_ARCHIVEDATE = "archivedate";

    public static final String BDE_DOCUMENTINSTANCE_ISMULTIPLE = "ismultiple";
    public static final String BDE_DOCUMENTINSTANCE_INDEX = "docindex";
    public static final String BDE_DOCUMENTINSTANCE_AUTHOR = "author";
    public static final String BDE_DOCUMENTINSTANCE_FILENAME = "filename";
    public static final String BDE_DOCUMENTINSTANCE_MIMETYPE = "mimetype";
    public static final String BDE_DOCUMENTINSTANCE_URL = "url";
    public static final String BDE_DOCUMENTINSTANCE_HASCONTENT = "hascontent";
    public static final String BDE_DOCUMENTINSTANCE_CONTENT = "content";

    /**
     * COMMENTS
     */
    public static final String BDE_TABLE_COMMENTINSTANCE = "Bonita_Comment";

    public static final String BDE_COMMENTINSTANCE_TENANTID = "tenantid";
    public static final String BDE_COMMENTINSTANCE_ID = "id";
    public static final String BDE_COMMENTINSTANCE_USERID = "userid";
    public static final String BDE_COMMENTINSTANCE_USERNAME = "username";
    public static final String BDE_COMMENTINSTANCE_CONTENT = "content";
    public static final String BDE_COMMENTINSTANCE_POSTDATE = "postdate";
    public static final String BDE_COMMENTINSTANCE_PROCESSINSTANCEID = "processinstanceid";
}
