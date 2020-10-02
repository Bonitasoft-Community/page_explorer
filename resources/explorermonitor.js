'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('explorermonitorapp', [ 'ui.bootstrap','ngSanitize' ]);


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
appCommand.controller('ExplorerControler',
	function ( $http, $scope,$sce,$filter ) {

	this.pingdate='';
	this.pinginfo='';
	this.listevents='';
	this.inprogress=false;
	
	this.navbaractiv = "Cases";
	this.getNavClass = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'ng-isolate-scope active';
		return 'ng-isolate-scope';
	}

	this.getNavStyle = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'border: 1px solid #c2c2c2;border-bottom-color: transparent;';
		return 'background-color:#cbcbcb';
	}
	
	
	this.searchcases =  { "active": true, 
			"archive":true,
			"external":true, 
			"year": "", 
			"text": "", 
			"caseid":"", 
			"processname":"", 
			"startdatebeg":"",
			"startdateend":"",
			"datebeg":"",
			"enddateend":"",			
			"enddate":"",
			"show" : true}
	this.cases={};
	this.searchCases = function()
	{
		
		var self=this;
		self.inprogress=true;
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		var json = encodeURIComponent(angular.toJson(this.searchcases, true));
		self.listevents='';
		// console.log("operationJob Call HTTP");


		$http.get( '?page=custompage_explorer&action=searchCases&paramjson=' + json+'&t='+Date.now())
				.success( function ( jsonResult, statusHttp, headers, config ) {
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					console.log("history",jsonResult);
					self.cases.list 	= jsonResult.list;
					self.cases.count 	= jsonResult.count;
					self.cases.first 	= jsonResult.first;
					self.cases.last 	= jsonResult.last;
			         
					
					self.inprogress=false;
						
						
				})
				.error( function() {
					
					self.inprogress=false;
					});
				
	}

	// -----------------------------------------------------------------------------------------
	//  										Parameter
	// -----------------------------------------------------------------------------------------
	
	this.parameter = {};
	
	// -----------------------------------------------------------------------------------------
	//  										Autocomplete
	// -----------------------------------------------------------------------------------------
	this.autocomplete={};
	
	this.queryUser = function(searchText) {
		var self=this;
		console.log("QueryUser HTTP CALL["+searchText+"]");
		
		self.autocomplete.inprogress=true;
		self.autocomplete.search = searchText;
		self.inprogress=true;
		
		var param={ 'userfilter' :  self.autocomplete.search};
		
		var json = encodeURI( angular.toJson( param, false));
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		
		return $http.get( '?page=custompage_searchCases&action=queryusers&paramjson='+json+'&t='+d.getTime() )
		.then( function ( jsonResult, statusHttp, headers, config ) {
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
			console.log("QueryUser HTTP SUCCESS.1 - result= "+angular.toJson(jsonResult, false));
			self.autocomplete.inprogress=false;
		 	self.autocomplete.listUsers =  jsonResult.data.listUsers;
			console.log("QueryUser HTTP SUCCESS length="+self.autocomplete.listUsers.length);
			self.inprogress=false;
	
			return self.autocomplete.listUsers;
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
			{ columnid: 'name', title: 'Name'},
			{ columnid: 'version', title: 'Version'},
			{ columnid: 'state', title: 'State'},
			{ columnid: 'deployeddate', title: 'Deployed date'},
			],         
		};  
	
        //get current system date.         
        var date = new Date();  
        $scope.CurrentDateTime = $filter('date')(new Date().getTime(), 'MM/dd/yyyy HH:mm:ss');          
		var trackingJson = this.listprocesses
        //Create XLS format using alasql.js file.  
        alasql('SELECT * INTO XLS("Process_' + $scope.CurrentDateTime + '.xls",?) FROM ?', [mystyle, trackingJson]);  
    };
    

	// -----------------------------------------------------------------------------------------
	//  										Properties
	// -----------------------------------------------------------------------------------------
	this.propsFirstName='';
	this.saveParameters = function() {
		var self=this;
		self.inprogress=true;
		
		var json = encodeURI( angular.toJson( self.parameter, false));
		
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		
		$http.get( '?page=custompage_searchCases&action=saveparameters&paramjson='+json +'&t='+d.getTime())
				.success( function ( jsonResult, statusHttp, headers, config ) {
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
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
	
	this.loadparameters =function() {
		var self=this;
		self.inprogress=true;
		
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		
		$http.get( '?page=custompage_searchCases&action=loadparameters&t='+d.getTime() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					self.parameter 		= jsonResult;
					self.listevents		= jsonResult.listevents;
					self.inprogress		= false;
		
				})
				.error( function() {
					});
	}
	this.init = function() {
		this.searchcases.show=true;
		this.loadparameters();
	}
	this.init();
	
	
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