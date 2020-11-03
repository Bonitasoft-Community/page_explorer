'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('explorermonitorapp', [ 'ui.bootstrap','ngSanitize', 'ngMaterial' ]);


/* Material : for the autocomplete
 * need 
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular-animate.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular-aria.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular-messages.min.js"></script>

  <!-- Angular Material Library -->
  <script src="https://ajax.googleapis.com/ajax/libs/angular_material/1.1.0/angular-material.min.js">
 */



// --------------------------------------------------------------------------
//
// Controler Explorer
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller("ExplorerControler",
	function ( $http, $scope,$sce,$filter ) {

	this.listevents="";
	this.inprogress=false;
	
	this.navbaractiv = "Cases";
	this.getNavClass = function( tabtodisplay, navbaractive )
	{
		if (navbaractive === tabtodisplay)
			return "ng-isolate-scope active";
		return "ng-isolate-scope";
	}

	this.getNavStyle = function( tabtodisplay, navbaractive )
	{
		if (navbaractive === tabtodisplay)
			return "border: 1px solid #c2c2c2;border-bottom-color: transparent;";
		return "background-color:#cbcbcb";
	}
	
	<!-- Search case is all parameters to search -->
	this.searchcasesinitial =  { "opencase": true, 
			"archivedcase":true,
			"externalcase":true, 
			"showstartdate" : "true",
			"showenddate" : "true",
			"datewithtime" : "false",
			"filtreprocessdisplay" : 'autocomplete',
			"year": "", 
			"text": "", 
			"caseid": null, 
			"processname":"", 
			"startdatebeg": null,
			"startdateend": null,
			"enddatestart": null,
			"enddateend": null,			
			"visibility" : "user",
			"caseperpages": "25",
			"pagenumber": 1,
			"orderby" : "caseid",
			"orderdirection":"asc",
			"showColumns" : {
				"origin" : true,
				"caseid" : true,
				"process" : true,
				"startdate" : true,
				"startedby" : true,
				"enddate" : true,
				"searchbykey1" : true,
				"searchbykey2" : true,
				"searchbykey3" : true,
				"searchbykey4" : true,
				"searchbykey5" : true 
			}			
	
	};
	this.searchcases =  angular.copy(this.searchcasesinitial);
	this.cases={};
	
	
	this.searchCases = function()
	{
		this.searchcases.pagenumber=1;
		this.loadCases();			
	}
	this.refresh = function() {
		console.log("Pagination="+this.searchcases.pagenumber)
		this.loadCases();
	}
	
	this.loadCases = function() {
		var self=this;
		self.inprogress=true;
		self.originprogress="searcase";
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		var json = encodeURIComponent(angular.toJson(this.searchcases, true));
		self.listevents="";
		console.log("loadCases json="+json);


		$http.get( "?page=custompage_explorer&action=searchCases&paramjson=" + json+"&t="+Date.now())
				.success( function ( jsonResult, statusHttp, headers, config ) {
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === "string") {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					// console.log("search",jsonResult);
					self.cases.list 		= jsonResult.list;
					self.cases.count 		= jsonResult.count;
					self.cases.first 		= jsonResult.first;
					self.cases.last 		= jsonResult.last;
					self.cases.totalchronos = jsonResult.totalchronos;
					
					self.inprogress=false;
						
						
				})
				.error( function() {
					
					self.inprogress=false;
					});
	}
	

	// -----------------------------------------------------------------------------------------
	//  										GUI Case
	// -----------------------------------------------------------------------------------------

	 
	this.casegui= { "showcasepanel": false,"showsearchpanel": true,   "showfilters":true,
			"navbaractiv" : "Overview",
			"showcaseurl":"",			
			"archivecase" : {
				"variables" : [ {"name":"Hello", "value":"the word"} ]
			} };
	/*
			{ "field" [ 
				{"name":"Hello", "value":"the word"}], 
			  "doc": [ {"name", "invice"}]}};
	}
	*/
	this.resetCaseFilter = function() {
		this.searchcases =  angular.copy(this.searchcasesinitial);
	}

	this.showExternalOrigin = function() {
		if (this.parameter.typedatasource === 'NOEXTERNALARCHIVE') {
			this.searchcases.externalcase=false;		
			return false;
		}
		return true;
	}
	
	this.showPagination= function() {
		var source=0;
		if (this.searchcases.opencase)
			source ++;
		if (this.searchcases.archivedcase) 
			source ++;
		if (this.searchcases.externalcase) 
			source ++;
		if (source>1)
			return false;
		return true;
	}
	this.showCasePanel=function( show ) {
		this.casegui.showcasepanel = show;
		this.casegui.showsearchpanel= ! show;
		
	}
	this.showCaseOverview = function ( caseselected ) {
		console.log("ShowCaseOverview");
		var self=this;
		self.showCasePanel( true );
		self.casegui.showcaseurl = caseselected.urloverview;
		self.casegui.showcaseexturl = caseselected.urlextoverview;
		self.casegui.caseselected = caseselected;
		self.casegui.navbaractiv = "Overview";
		// the case maybe a External archive, then ask the content
		

		self.inprogress=true;
		self.casegui.caseexturlcontent="";
		self.originprogress="showCaseOverview";
		
		var param = { "caseid" : caseselected.caseid, "scope" : caseselected.scope };			
		var json = encodeURIComponent(angular.toJson(param, true));
		
		$http.get( "?page=custompage_explorer&action=loadcase&paramjson=" + json+"&t="+Date.now())
		.success( function ( jsonResult, statusHttp, headers, config ) {
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === "string") {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
			self.inprogress=false;
			console.log("ShowCaseOverview.load case"+jsonResult);
			self.casegui.externalcase 	= jsonResult.externalcase;
			self.casegui.tasks  		= jsonResult.tasks;
			self.casegui.comments  		= jsonResult.comments;
		  })
		.error( function ( jsonResult, statusHttp, headers, config ) {
			console.log("ERROR LOAD CASE"+jsonResult);
			self.inprogress=false;
		  });
		
		
		/** This does not work due to the CORS
		console.log("load case ext with Access-Control-Allow-Origin="+self.casegui.showcaseexturl);
		if (self.casegui.showcaseexturl !=null) {
			// Access-Control-Allow-Origin
			var httpConfig = {
		            cache: false, 
		            headers : { "Access-Control-Allow-Origin": "*"}
		        };
			$http.get( self.casegui.showcaseexturl, httpConfig )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				console.log("Result case url content = "+jsonResult);
				self.casegui.caseexturlcontent= jsonResult;
			});
		}
		*/
	}
	this.refreshDate= new Date();

	this.downloadCaseDocument = function(doc)
	{
		
		// ["+parameterdef.name+"]");
		var param={"docid": doc.id, "caseid":doc.processinstance};			
		var json = encodeURIComponent(angular.toJson(param, true));
		// do not calculate a new date now:we will have a recursive call in
		// Angular
		console.log("downloadParameterFile: ?page=custompage_explorer&action=downloaddoc&paramjson=" + json+'&t='+this.refreshDate.getTime());
		return "?page=custompage_explorer&action=downloaddoc&paramjson=" + json+'&t='+this.refreshDate.getTime();
	}

	// -----------------------------------------------------------------------------------------
	//  										save default filter
	// -----------------------------------------------------------------------------------------

	this.saveCaseFilter = function() {
		var self=this;
		self.inprogress=true;
		self.originprogress="saveCaseFilter";

		var json = encodeURI( angular.toJson( self.searchcases, false));
		
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		
		$http.get( "?page=custompage_searchCases&action=savecasefilter&paramjson="+json +"&t="+d.getTime())
				.success( function ( jsonResult, statusHttp, headers, config ) {
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === "string") {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					console.log("history",jsonResult);
					self.listevents		= jsonResult.listevents;
					self.inprogress=false;
				})
				.error( function() {
					});
	}
	
	this.user={ "isadmin":false};
	
	this.loadCaseFilter =function() {
		var self=this;
		self.inprogress=true;
		self.originprogress="loadCaseFilter";

		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		
		$http.get( "?page=custompage_searchCases&action=loadcasefilter&t="+d.getTime() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === "string") {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					self.searchcases 	= jsonResult.searchcases;
					self.transformSearchCase();
					self.listevents		= jsonResult.listevents;
					self.inprogress		= false;
				})
				.error( function() {
					});
	}
	// -----------------------------------------------------------------------------------------
	//  										Parameter
	// -----------------------------------------------------------------------------------------
	
	this.parameter = {};
	this.saveParameters = function() {
		var self=this;
		self.inprogress=true;
		self.originprogress="saveParameters";

		var json = encodeURI( angular.toJson( self.parameter, false));
		
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		
		$http.get( "?page=custompage_searchCases&action=saveparameters&paramjson="+json +"&t="+d.getTime())
				.success( function ( jsonResult, statusHttp, headers, config ) {
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === "string") {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					console.log("history",jsonResult);
					self.listeventssavesearchcase		= jsonResult.listevents;
					self.inprogress=false;
				})
				.error( function() {
					});
	}
	this.user={ "isadmin":false};
	
	this.loadparamandfilter =function() {
		var self=this;
		self.inprogress=true;
		self.originprogress="loadparamandfilter";

		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		
		$http.get( "?page=custompage_searchCases&action=loadparamandfilter&t="+d.getTime() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === "string") {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					self.parameter 			= jsonResult.parameters;
					self.searchcases		= jsonResult.searchcases;
					self.transformSearchCase();
					self.listevents			= jsonResult.listevents;
					self.inprogress			= false;
					self.user 				= jsonResult.user;
					self.listprocessesname 	= jsonResult.listprocessesname;
					
					// initialise the default show columns
					if (! self.parameter.showColumns) 						
						self.parameter.showColumns= angular.copy(self.searchcasesinitial.showColumns); 
					if (! self.parameter.showColumns.caseid) 
						self.parameter.showColumns.caseid=true;

				})
				.error( function() {
					});
	}
	
	this.transformSearchCase = function() {
		if (this.searchcases.startdatebeg)
			this.searchcases.startdatebeg = new Date( this.searchcases.startdatebeg );
		if (this.searchcases.startdateend)
			this.searchcases.startdateend = new Date( this.searchcases.startdateend );
		if (this.searchcases.enddatebeg)
			this.searchcases.enddatebeg = new Date( this.searchcases.enddatebeg );
		if (this.searchcases.enddateend)
			this.searchcases.enddateend = new Date( this.searchcases.enddateend );
		if (! this.searchcases.caseperpages)
			this.searchcases.caseperpages = this.searchcasesinitial.caseperpages;
		if (! this.searchcases.caseperpages || this.searchcases.caseperpages =="" || this.searchcases.caseperpages =="0" )
			this.searchcases.caseperpages = this.searchcasesinitial.caseperpages;
		if (! this.searchcases.opencase && ! this.searchcases.archivedcase && !  this.searchcases.externalcase) {
			this.searchcases.opencase		= this.searchcasesinitial.opencase;
			this.searchcases.archivedcase	= this.searchcasesinitial.archivedcase;
			this.searchcases.externalcase	= this.searchcasesinitial.externalcase;
		}
		
		if (! this.searchcases.visibility)
			this.searchcases.visibility = this.searchcasesinitial.visibility;
		if (! this.searchcases.orderby)
			this.searchcases.orderby = this.searchcasesinitial.orderby;
		if (! this.searchcases.orderdirection)
			this.searchcases.orderdirection = this.searchcasesinitial.orderdirection;	
		
	}
	// -----------------------------------------------------------------------------------------
	//  										Autocomplete
	// -----------------------------------------------------------------------------------------
	this.autocomplete={};
	
	this.query = function(queryName, searchText) {
		var self=this;
		console.log("QueryUser HTTP CALL["+searchText+"]");
		
		self.autocomplete.inprogress=true;

		self.autocomplete.search = searchText;
		self.inprogress=true;
		self.originprogress="queryusers";
		
		var param={ "userfilter" :  self.autocomplete.search};
		
		var json = encodeURI( angular.toJson( param, false));
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		
		return $http.get( "?page=custompage_searchCases&action="+queryName+"&paramjson="+json+"&t="+d.getTime() )
		.then( function ( jsonResult, statusHttp, headers, config ) {
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === "string") {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
			console.log("QueryUser HTTP SUCCESS.1 - result= "+angular.toJson(jsonResult, false));
			self.autocomplete.inprogress=false;
		 	self.autocomplete.listprocesses =  jsonResult.data.listProcesses;
			console.log("QueryUser HTTP SUCCESS length="+self.autocomplete.listprocesses.length);
			self.inprogress=false;
	
			return self.autocomplete.listprocesses;
		},  function ( jsonResult ) {
			console.log("QueryUser HTTP THEN");
		});

	  };
	  
	// -----------------------------------------------------------------------------------------
	//  										Excel
	// -----------------------------------------------------------------------------------------

	this.exportData = function () 
	{  
		//Start*To Export SearchTable data in excel  
	// create XLS template with your field.  
		var mystyle = {         
        headers:true,        
			columns: [  
			{ columnid: "name", title: "Name"},
			{ columnid: "version", title: "Version"},
			{ columnid: "state", title: "State"},
			{ columnid: "deployeddate", title: "Deployed date"},
			],         
		};  
	
        //get current system date.         
        var date = new Date();  
        $scope.CurrentDateTime = $filter("date")(new Date().getTime(), "MM/dd/yyyy HH:mm:ss");          
		var trackingJson = this.listprocesses
        //Create XLS format using alasql.js file.  
        alasql('SELECT * INTO XLS("Process_' + $scope.CurrentDateTime + '.xls",?) FROM ?', [mystyle, trackingJson]);  
    };
    

	// -----------------------------------------------------------------------------------------
	//  										Properties
	// -----------------------------------------------------------------------------------------
	this.propsFirstName="";

	this.init = function() {
		this.searchcases.show=true;
		this.loadparamandfilter();
    
	}
	this.init();
	
	this.getUrlContent = function ( urlContent ) {		
		return $sce.trustAsHtml(  urlContent );
	}
	
	<!-- Manage the event -->
	this.getListEvents = function ( listevents ) {
		return $sce.trustAsHtml(  listevents );
	}
	<!-- Manage the Modal -->
	this.isshowDialog=false;
	this.openDialog = function()
	{
		this.isshowDialog=true;
	};
	this.closeDialog = function()
	{
		this.isshowDialog=false;
	}

});



})();