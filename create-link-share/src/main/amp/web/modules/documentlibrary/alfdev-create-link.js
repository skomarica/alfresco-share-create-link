Alfresco.module.alfdev = Alfresco.module.alfdev || {};

/**
 * Document Library "Create Link" module for Document Library.
 * 
 * @namespace Alfresco.module.alfdev
 * @class Alfresco.module.alfdev.DoclibCreateLink
 */
(function()
{
   Alfresco.module.alfdev.DoclibCreateLink = function(htmlId)
   {
      Alfresco.module.alfdev.DoclibCreateLink.superclass.constructor.call(this, htmlId);

      // Re-register with our own name
      this.name = "Alfresco.module.alfdev.DoclibCreateLink";
      var DLGF = Alfresco.module.DoclibGlobalFolder;

      Alfresco.util.ComponentManager.reregister(this);

      this.options = YAHOO.lang.merge(this.options,
      {
         allowedViewModes:
         [
            DLGF.VIEW_MODE_SITE,
            DLGF.VIEW_MODE_RECENT_SITES,
            DLGF.VIEW_MODE_FAVOURITE_SITES,
            DLGF.VIEW_MODE_SHARED,
            DLGF.VIEW_MODE_REPOSITORY,
            DLGF.VIEW_MODE_USERHOME
         ],
         extendedTemplateUrl: Alfresco.constants.URL_SERVICECONTEXT + "modules/documentlibrary/alfdev/create-link"
      });

      return this;
   };

   YAHOO.extend(Alfresco.module.alfdev.DoclibCreateLink, Alfresco.module.DoclibGlobalFolder,
   {
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @override
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.module.alfdev.DoclibCreateLink} returns 'this' for method chaining
       */
      setOptions: function DLCL_setOptions(obj)
      {
         var myOptions = {};

         myOptions.dataWebScript = "alfdev/create-link";
         
         myOptions.viewMode = Alfresco.util.isValueSet(this.options.siteId) ? Alfresco.module.DoclibGlobalFolder.VIEW_MODE_RECENT_SITES : Alfresco.module.DoclibGlobalFolder.VIEW_MODE_REPOSITORY;
         // Actions module
         this.modules.actions = new Alfresco.module.DoclibActions();

         return Alfresco.module.alfdev.DoclibCreateLink.superclass.setOptions.call(this, YAHOO.lang.merge(myOptions, obj));
      },

      /**
       * Event callback when superclass' dialog template has been loaded
       *
       * @method onTemplateLoaded
       * @override
       * @param response {object} Server response from load template XHR request
       */
      onTemplateLoaded: function DLCL_onTemplateLoaded(response)
      {
         // Load the UI template, which only will bring in new i18n-messages, from the server
         Alfresco.util.Ajax.request(
         {
            url: this.options.extendedTemplateUrl,
            dataObj:
            {
               htmlid: this.id
            },
            successCallback:
            {
               fn: this.onExtendedTemplateLoaded,
               obj: response,
               scope: this
            },
            failureMessage: "Could not load 'copy-link-to' template:" + this.options.extendedTemplateUrl,
            execScripts: true
         });
      },

      /**
       * Event callback when this class' template has been loaded
       *
       * @method onExtendedTemplateLoaded
       * @override
       * @param response {object} Server response from load template XHR request
       */
      onExtendedTemplateLoaded: function DLCL_onExtendedTemplateLoaded(response, superClassResponse)
      {
         // Now that we have loaded this components i18n messages let the original template get rendered.
         Alfresco.module.alfdev.DoclibCreateLink.superclass.onTemplateLoaded.call(this, superClassResponse);
      },

      /**
       * YUI WIDGET EVENT HANDLERS
       * Handlers for standard events fired from YUI widgets, e.g. "click"
       */

      /**
       * Dialog OK button event handler
       *
       * @method onOK
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onOK: function DLCL_onOK(e, p_obj)
      {
         var files, multipleFiles = [], params, i, j,
            eventSuffix = "LinkCreated";

         // Single/multi files into array of nodeRefs
         if (YAHOO.lang.isArray(this.options.files))
         {
            files = this.options.files;
         }
         else
         {
            files = [this.options.files];
         }
         for (i = 0, j = files.length; i < j; i++)
         {
            multipleFiles.push(files[i].node.nodeRef);
         }
         
         // Success callback function
         var fnSuccess = function DLCL__onOK_success(p_data)
         {
            var result,
               successCount = p_data.json.successCount,
               failureCount = p_data.json.failureCount;

            this.widgets.dialog.hide();

            // Did the operation succeed?
            if (!p_data.json.overallSuccess)
            {
               Alfresco.util.PopupManager.displayMessage(
               {
                  text: this.msg("message.failure")
               });
               return;
            }

            YAHOO.Bubbling.fire("files" + eventSuffix,
            {
               destination: this.currentPath,
               successCount: successCount,
               failureCount: failureCount
            });
            
            for (var i = 0, j = p_data.json.totalResults; i < j; i++)
            {
               result = p_data.json.results[i];
               
               if (result.success)
               {
                  YAHOO.Bubbling.fire((result.type == "folder" ? "folder" : "file") + eventSuffix,
                  {
                     multiple: true,
                     nodeRef: result.nodeRef,
                     destination: this.currentPath
                  });
               }
            }

            Alfresco.util.PopupManager.displayMessage(
            {
               text: this.msg("message.success", successCount)
            });
            YAHOO.Bubbling.fire("metadataRefresh");
         };

         // Failure callback function
         var fnFailure = function DLCL__onOK_failure(p_data)
         {
            this.widgets.dialog.hide();

            Alfresco.util.PopupManager.displayMessage(
            {
               text: this.msg("message.failure")
            });
         };

         // Construct webscript URI based on current viewMode
         var webscriptName = this.options.dataWebScript + "/node/{nodeRef}",
            nodeRef = new Alfresco.util.NodeRef(this.selectedNode.data.nodeRef);
         
         // Construct the data object for the genericAction call
         this.modules.actions.genericAction(
         {
            success:
            {
               callback:
               {
                  fn: fnSuccess,
                  scope: this
               }
            },
            failure:
            {
               callback:
               {
                  fn: fnFailure,
                  scope: this
               }
            },
            webscript:
            {
               method: Alfresco.util.Ajax.POST,
               name: webscriptName,
               params:
               {
                  nodeRef: nodeRef.uri
               }
            },
            wait:
            {
               message: this.msg("message.please-wait")
            },
            config:
            {
               requestContentType: Alfresco.util.Ajax.JSON,
               dataObj:
               {
                  nodeRefs: multipleFiles,
		          parentId: this.options.parentId
               }
            }
         });
         
         this.widgets.okButton.set("disabled", true);
         this.widgets.cancelButton.set("disabled", true);
      },

      /**
       * Gets a custom message depending on current view mode
       * and use superclasses
       *
       * @method msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @override
       */
      msg: function DLCL_msg(messageId)
      {
         var result = Alfresco.util.message.call(this, this.options.mode + "." + messageId, this.name, Array.prototype.slice.call(arguments).slice(1));
         if (result ==  (this.options.mode + "." + messageId))
         {
            result = Alfresco.util.message.call(this, messageId, this.name, Array.prototype.slice.call(arguments).slice(1))
         }
         if (result == messageId)
         {
            result = Alfresco.util.message(messageId, "Alfresco.module.DoclibGlobalFolder", Array.prototype.slice.call(arguments).slice(1));
         }
         return result;
      },

      
      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Internal show dialog function
       * @method _showDialog
       * @override
       */
      _showDialog: function DLCL__showDialog()
      {
         this.widgets.okButton.set("label", this.msg("button"));
         return Alfresco.module.alfdev.DoclibCreateLink.superclass._showDialog.apply(this, arguments);
      }
   });

   /* Dummy instance to load optional YUI components early */
   var dummyInstance = new Alfresco.module.alfdev.DoclibCreateLink("null");
})();