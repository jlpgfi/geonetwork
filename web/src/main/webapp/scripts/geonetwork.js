// VARIABLE DECLARATIONS

var getGNServiceURL = function(service) {
	return Env.locService+"/"+service;
};

function init() {};

function translate(text) {
	return translations[text] || text;
};

/**
 * Replaces parameters in a string (defined like $1, $2, ...) with the values provided in the params array
 *
 * @param text
 * @param params
 */
function replaceStringParams(text, params) {
    var newText = text;

    for(var i = 0; i < params.length; i++) {
        newText = newText.replace("$" + (i+1), params[i]);
    }

    return newText;
}

// Read a cookie
function get_cookie ( cookie_name )
{
  var results = document.cookie.match ( cookie_name + '=(.*?)(;|$)' );

  if ( results )
    return ( unescape ( results[1] ) );
  else
    return null;
};

// New browser windows
	function popNew(a)
	{
		msgWindow=window.open(a,"displayWindow","location=no, toolbar=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, width=800, height=600")
		msgWindow.focus()
	}

	function openPage(what,type)
	{
		msgWindow=window.open(what,type,"location=yes, toolbar=yes, directories=yes, status=yes, menubar=yes, scrollbars=yes, resizable=yes, width=800, height=600")
		msgWindow.focus()
	}

	function popFeedback(a)
	{
		msgWindow=window.open(a,"feedbackWindow","location=no, toolbar=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, width=800, height=600")
		msgWindow.focus()
	}

	function popWindow(a)
	{
		msgWindow=window.open(a,"popWindow","location=no, toolbar=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, width=800, height=600")
		msgWindow.focus()
	}

	function popInterMap(a)
	{
		msgWindow=window.open(a,"InterMap","location=no, toolbar=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, width=800, height=600")
		msgWindow.focus()
	}

// Forms
	function goSubmit(form_name, permitAjax) {
        permitAjax = permitAjax == undefined ? true : false;

		if (!permitAjax || typeof Ext == 'undefined' || $('editForm') === null) {
			document.forms[form_name].submit();
		} else {
		  var metadataId = document.mainForm.id.value;
		  var divToRestore = null;
		  if (opener) {
		  	divToRestore = opener.document.getElementById(metadataId);
		  }

		  disableEditForm();

		  var myAjax = new Ajax.Request(
			document.mainForm.action,
		  	{
		  		method: 'post',
		  		parameters: $('editForm').serialize(true),
		  		onSuccess: function(req) {
		  			var html = req.responseText;
		  			if (divToRestore) divToRestore.removeClassName('editing');
		  			if (html.startsWith("<?xml") < 0) { // service returns xml on success
		  				alert(translate("errorSaveFailed") + html);
		  			}
					
		  			setBunload(false);
		  			location.replace(getGNServiceURL('metadata.edit?id='+metadataId));
		  		},
		  		onFailure: function(req) { 
		  			alert(translate("errorSaveFailed") + "/ status " + req.status + " text: " + req.statusText + " - " + translate("tryAgain"));
		  			$('editorBusy').hide();
		  			setBunload(true); // reset warning for window destroy
		  		}
		  	}
		  );
		}
	}

	function goReset(form_name)
	{
		document.forms[form_name].reset();
	}

	function entSub(form_name) {
		if (window.event && window.event.keyCode == 13)
			goSubmit(form_name);
		else
			return true;
	}
    
// Navigation
	function goBack()
	{
		history.back();
	}

	function processCancel() {
		document.close();
	}

	function load(url)
	{
		document.location.href = url;
	}

	function doConfirm(url, message)
	{
		if(confirm(message))
		{
			load(url);
			return true;
		}
		return false;
	}

/**********************************************************
 * Download support 
 **********************************************************/

	function feedbackSubmit()
	{
		var f = $('feedbackf');
		if (isWhitespace(f.comments.value)) {
			f.comments.value = translate('noComment');
		}

		if (isWhitespace(f.name.value) || isWhitespace(f.org.value)) {
			alert(translate("addName"));
			return;
		} else if (!isEmail(f.email.value)) {
			alert(translate("checkEmail"));
			return;
		} 

		Modalbox.show(getGNServiceURL('file.download'),{height: 400, width: 600, params: f.serialize(true)});
	}

	function doDownload(id, all) {
		var list = $('downloadlist').getElementsByTagName('INPUT');
		var pars = '&id='+id+'&access=private';

		var selected = false;
		for (var i=0; i<list.length; i++) {
			if (list[i].checked || all != null) {
				selected = true;
				var name = list[i].getAttribute('name');
				pars += '&fname='+name;
			}
		}

		if (!selected) {
			alert(translate("selectOneFile"));
			return;
		}

		Modalbox.show(getGNServiceURL('file.disclaimer') + "?" + pars, {height: 400, width: 600});
	}


/**********************************************************
 * Massive Operations are called through this routine
 **********************************************************/

	function massiveOperation(service, title, width, message)
	{

		if (message != null) {
			if(!confirm(message))
				return;
		}

		var url = Env.locService +'/' + service;
		Modalbox.show(url,{title: title, width: width, height: 400, afterHide: function() {
                if ($("simple_search_pnl").visible()) {
                    runSimpleSearch();

                } else if ($("advanced_search_pnl").visible()) {
                    runAdvancedSearch();

                } else {
                  $('search-results-content').hide();
                }
		
		        runRssSearch();
		
            }});
	}

/**********************************************************
 * Select Actions 
 **********************************************************/

	function oActionsInit(name,id) {
	    if (id === undefined) {
	    	id = "";
	    }
	    $(name+'Ele'+id).style.width = $(name+id).getWidth();
	    $(name+'Ele'+id).style.top = $(name+id).positionedOffset().top + $(name+id).getHeight() + "px";
	    $(name+'Ele'+id).style.left = $(name+id).positionedOffset().left + "px";
	}


	function oActions(name,id) {
		var on        = "../../images/plus.gif";
        var off       = "../../images/minus.png";
		
		if (id === undefined) {
			id = "";
		}

		oActionsInit (name, id);

	  	if ($(name+'Ele'+id).style.display == 'none') {
	    	$(name+'Ele'+id).style.display = 'block';
	    	$(name+'Img'+id).src = off;
	  	} else {
	    	$(name+'Ele'+id).style.display = 'none';
	    	$(name+'Img'+id).src = on;
	  	}
	}

	function actionOnSelect(msg) {
		if ($('nbselected').innerHTML == 0 && $('oAcOsEle').style.display == 'none') {
			alert(msg);
		} else {
			oActions('oAcOs');
		}
	}

/**********************************************************************
 * Massive Ownership Transfer stuff
 **********************************************************************/

	function checkMassiveNewOwner(action,title) {
		if ($('user').value == '') {
			alert(translate("selectNewOwner"));
			return false;
		}
		if ($('group').value == '') {
			alert(translate("selectOwnerGroup"));
			return false;
		}
		Modalbox.show(getGNServiceURL(action),{title: title, params: $('massivenewowner').serialize(true), afterHide: function() {
                if ($("simple_search_pnl").visible()) {

                    runSimpleSearch();

                } else if ($("advanced_search_pnl").visible()) {
                    runAdvancedSearch();

                } else {
                  $('search-results-content').hide();
                }
		
		        runRssSearch();
        }});
	}

	function addGroups(xmlRes) {
		var list = xml.children(xmlRes, 'group');
		$('group').options.length = 0; // clear out the options
		for (var i=0; i<list.length; i++) {
			var id     = xml.evalXPath(list[i], 'id');
			var name	 = xml.evalXPath(list[i], 'name');
			var opt = document.createElement('option');
			opt.text  = name;
			opt.value = id;
			if (list.length == 1) opt.selected = true;
			$('group').options.add(opt);
		}
	}

	function addGroupsCallback_OK(xmlRes) {
		if (xmlRes.nodeName == 'error') {
			ker.showError(translate('cannotRetrieveGroup'), xmlRes);
			$('group').options.length = 0; // clear out the options
			$('group').value = ''; 
			var user = $('user'); 
			for (i=0;i<user.options.length;i++) {
				user.options[i].selected = false;
			}
		} else {
			addGroups(xmlRes);
		}
	}
	
	function doGroups(userid) {
		var request = ker.createRequest('id',userid);
		ker.send('xml.usergroups.list', request, addGroupsCallback_OK);
	}

/**********************************************************************
 * User self-registration actions
 **********************************************************************/

	function processRegSub(url)
	{
		// check start
		var invalid = " "; // Invalid character is a space
		var minLength = 6; // Minimum length
            
		if (document.userregisterform.name.value.length == 0) {
			alert(translate('firstNameMandatory'));
			return;
		} 
		if (isWhitespace(document.userregisterform.name.value)) {
			alert(translate('firstNameMandatory'));
			return;
		}    
		if (document.userregisterform.name.value.indexOf(invalid) > -1) {
			alert(translate('spacesNot'));
			return;
		}	
			
		if (document.userregisterform.surname.value.length == 0) {
			alert(translate('lastNameMandatory'));
			return;
		}  
		if (isWhitespace(document.userregisterform.surname.value)) {
			alert(translate('lastNameMandatory'));
			return;
		}
		if (document.userregisterform.surname.value.indexOf(invalid) > -1) {
			alert(translate('spacesNot'));
			return;
		}
			
		if (!isEmail(document.userregisterform.email.value)) {
			alert(translate('emailAddressInvalid'));
			return;
		}
			
		var myAjax = new Ajax.Request(
			getGNServiceURL(url), 
				{
					method: 'post',
					parameters: $('userregisterform').serialize(true), 
						onSuccess: function(req) {
            	var output = req.responseText;
							var title = translate('yourRegistration');
        			Modalbox.show(output,{title: title, width: 300});
						},
						onFailure: function(req) {
            	alert(translate("registrationFailed") + " " + req.responseText + " status: " + req.status + " - " + translate("tryAgain"));
						}
				}
		);
	}
	
	
	
	

/**
 * Display a popup, update the content if needed.
 * modal box are collapsibled and centered.
 */
function displayBox(content, contentDivId, modal) {
	var id = contentDivId + "Box";
	var w = Ext.getCmp(id);
	if (w == undefined) {
		w = new Ext.Window({
	        title: translate(contentDivId),
	        id: id,
	        layout: 'fit',
	        modal: modal,
	        constrain: true,
	        width: 400,
	        collapsible: (modal?false:true),
	        autoScroll: true,
	        iconCls: contentDivId + 'Icon',
	        closeAction: 'hide',
	        onEsc: 'hide',
	        listeners: {
	            hide: function() {
	                this.hide();
	            }
	        },
	        contentEl: contentDivId
	    });
	}
    if (w) {
    	if (content != null) {
    		$(contentDivId).innerHTML = '';
    		$(contentDivId).innerHTML = content;
    		$(contentDivId).style.display = 'block'
    	}
    	w.show();
    	w.setHeight(345);
    	w.anchorTo(Ext.getBody(), (modal?'c-c':'tr-tr'));	// Align top right if not modal, or center
    }
}

/**
 * PMT GeoBretagne-Specific
 */

function redirectToExternalApp(destUrl, id) {
  var myAjax = new Ajax.Request(
      getGNServiceURL("metadata.service.extract"), {
        method: 'get',
        parameters: (typeof id == 'undefined') ? '' : 'id=' + id,
        onSuccess: function(req) {
          var jsFromXml = req.responseXML || new OpenLayers.Format.XML().read(req.responseText);
          var jsonObject = {services: [], layers: []};
          /*
           * Implementing rules from the wiki :
           *
           *  1. if multiple WMC docs are selected in
           *  GeoNetwork the latter will refuse to open the MapFish app
           *
           *  2. if a WMC doc and WMS items (layers or services) are selected
           *  in GeoNetwork the latter will refuse to open the MapFish app
           *
           *  3. if WMS services are selected the MapFish app will open a dialog
           *  window for the user to select layers
           *
           */
          var wmcCount = 0;
          var wmsCount = 0;
          Ext.each(jsFromXml.getElementsByTagName('service'), function(item, index, array) {
              var owsType = item.getAttribute('owstype');
              jsonObject.services.push({
                text: item.getAttribute('text'),
                metadataURL: Env.host + Env.locService + "/metadata.show?id=" + item.getAttribute('mdid'),
                owstype: owsType,
                owsurl: item.getAttribute('owsurl')
              });

              switch (owsType) {
                case 'WMC':
                  wmcCount += 1;
                  break;

                case 'WMS':
                  wmsCount += 1;
                  break;
              }
          });

          Ext.each(jsFromXml.getElementsByTagName('layer'), function(item, index, array) {
            var owsType = item.getAttribute('owstype');

            jsonObject.layers.push({
              layername: item.getAttribute('layername'),
              metadataURL: Env.host + Env.locService + "/metadata.show?id=" + item.getAttribute('mdid'),
              owstype: owsType,
              owsurl: item.getAttribute('owsurl')
            });

            switch (owsType) {
              case 'WMC':
                wmcCount += 1;
                break;
              case 'WMS':
                wmsCount += 1;
                break;
            }
          });

          /* Checking inputs - rule #1 */
          if (wmcCount > 1) {
            alert(translate("invalidSelectionMoreThanOneWMC"));
            return;
          }
          /* rule #2 */
          if ((wmcCount > 0) && (wmsCount > 0)) {
            alert(translate("invalidSelectionOneWMCandOneOrMoreWMS"));
           return;
          }
          /* new rule : No data (no WMS nor WMC) available into
           * selected MDs. Alerting the user
           */
          if ((wmcCount == 0) && (wmsCount == 0)) {
            alert(translate("invalidSelectionnoWMCnorWMS"));
            return;
          }

          var form = Ext.DomHelper.append(Ext.getBody(), {
            tag: 'form',
            action: destUrl,
            target: "_blank",
            method: 'post'
          });

          var input = Ext.DomHelper.append(form, {
            tag: 'input',
            name: 'data'
          });

          input.value = new OpenLayers.Format.JSON().write(jsonObject);
          form.submit();
          Ext.removeNode(form);
        },
        onFailure: function(req) {
          alert("Erreur lors de la récupération des services WxS des Métadonnées");
        }
      } // End object
    ); // End Ajax request
}
