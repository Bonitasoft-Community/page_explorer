package org.bonitasoft.explorer.external;


public class DatabaseDefinition {
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

    public static final String BDE_DATAINSTANCE_TENANTID = "TENANTID";
    public static final String BDE_DATAINSTANCE_NAME = "NAME";
    public static final String BDE_DATAINSTANCE_SCOPE = "SCOPE";
    public static final String BDE_DATAINSTANCE_ID = "ID";
    public static final String BDE_DATAINSTANCE_DESCRIPTION = "DESCRIPTION";

    public static final String BDE_DATAINSTANCE_PROCESSINSTANCEID = "PROCESSINSTANCEID";
    // variable can be local : in that circunstance, the ACTIVITYID is set
    public static final String BDE_DATAINSTANCE_ACTIVITYID = "ACTIVITYID";

    public static final String BDE_DATAINSTANCE_CONTAINERTYPE = "CONTAINERTYPE";
    public static final String BDE_DATAINSTANCE_ARCHIVEDATE = "ARCHIVEDATE";

    public static final String BDE_DATAINSTANCE_CLASSNAME = "CLASSNAME";
    public static final String BDE_DATAINSTANCE_VALUE = "VALUE";
    public static final String BDE_DATAINSTANCE_FLOATVALUE = "FLOATVALUE";
    public static final String BDE_DATAINSTANCE_DOUBLEVALUE = "DOUBLEVALUE";
    public static final String BDE_DATAINSTANCE_BOOLEANVALUE = "BOOLEANVALUE";
    public static final String BDE_DATAINSTANCE_DATEVALUE = "DATEVALUE";
    public static final String BDE_DATAINSTANCE_LONGVALUE = "LONGVALUE";

    // For a BDM variable. BDM Name is the equivalent of the ClassName     
    public static final String BDE_DATAINSTANCE_BDMNAME = "BDMNAME";
    public static final String BDE_DATAINSTANCE_BDMISMULTIPLE = "BDMISMULTIPLE";
    public static final String BDE_DATAINSTANCE_BDMINDEX = "BDMINDEX";
    public static final String BDE_DATAINSTANCE_BDMPERSISTENCEID = "BDMPERSISTENCEID";

    /**
     * FlowNode
     */
    public static final String BDE_TABLE_FLOWNODEINSTANCE = "Bonita_Flownode";

    public static final String BDE_FLOWNODEINSTANCE_TENANTID = "TENANTID";
    public static final String BDE_FLOWNODEINSTANCE_ID = "ID";
    public static final String BDE_FLOWNODEINSTANCE_FLOWNODEDEFINITIONID = "FLOWNODEFINITIONID";
    public static final String BDE_FLOWNODEINSTANCE_KIND = "KIND";
    public static final String BDE_FLOWNODEINSTANCE_ARCHIVEDATE = "ARCHIVEDATE";
    public static final String BDE_FLOWNODEINSTANCE_PROCESSINSTANCEID = "PROCESSINSTANCEID";
    public static final String BDE_FLOWNODEINSTANCE_PARENTCONTAINERID = "PARENTCONTAINERID";
    public static final String BDE_FLOWNODEINSTANCE_SOURCEOBJECTID = "SOURCEOBJECTID";
    public static final String BDE_FLOWNODEINSTANCE_NAME = "NAME";
    public static final String BDE_FLOWNODEINSTANCE_DISPLAYNAME = "DISPLAYNAME";
    public static final String BDE_FLOWNODEINSTANCE_STATENAME = "STATENAME";
    public static final String BDE_FLOWNODEINSTANCE_REACHEDSTATEDATE = "REACHEDSTATEDATE";

    public static final String BDE_FLOWNODEINSTANCE_GATEWAYTYPE = "GATEWAYTYPE";
    public static final String BDE_FLOWNODEINSTANCE_LOOP_COUNTER = "LOOPCOUNTER";
    public static final String BDE_FLOWNODEINSTANCE_NUMBEROFINSTANCES = "NUMBEROFINSTANCES";
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
    public static final String BDE_FLOWNODEINSTANCE_EXECUTEDBY = "EXECUTEDBY";
    public static final String BDE_FLOWNODEINSTANCE_EXECUTEDBYSUBSTITUTE = "EXECUTEDBYSUBSTITUTE";
    // public static final String BDE_FLOWNODEINSTANCE_ACTIVITYINSTANCEID
    // ABORTING
    // TRIGGEREDBYEVENT
    // INTERRUPTING

    /**
     * Document
     */
    public static final String BDE_TABLE_DOCUMENT = "Bonita_Document";

    public static final String BDE_DOCUMENTINSTANCE_TENANTID = "TENANTID";
    public static final String BDE_DOCUMENTINSTANCE_ID = "ID";
    public static final String BDE_DOCUMENTINSTANCE_NAME = "NAME";
    public static final String BDE_DOCUMENTINSTANCE_PROCESSINSTANCEID = "PROCESSINSTANCEID";
    public static final String BDE_DOCUMENTINSTANCE_VERSION = "VERSION";
    public static final String BDE_DOCUMENTINSTANCE_ARCHIVEDATE = "ARCHIVEDATE";

    public static final String BDE_DOCUMENTINSTANCE_INDEX = "DOCINDEX";
    public static final String BDE_DOCUMENTINSTANCE_AUTHOR = "AUTHOR";
    public static final String BDE_DOCUMENTINSTANCE_FILENAME = "FILENAME";
    public static final String BDE_DOCUMENTINSTANCE_MIMETYPE = "MIMETYPE";
    public static final String BDE_DOCUMENTINSTANCE_URL = "URL";
    public static final String BDE_DOCUMENTINSTANCE_HASCONTENT = "HASCONTENT";
    public static final String BDE_DOCUMENTINSTANCE_CONTENT = "CONTENT";

}
