 (function() {
    YAHOO.Bubbling.fire("registerAction",
    {
        actionName: "onAlfdevActionCreateLink",
        fn: function alfdev_onActionCreateLink(record) {
		
         if (!this.modules.createLink)
         {
            this.modules.createLink = new Alfresco.module.alfdev.DoclibCreateLink(this.id + "-createLink");
         }

         var DLGF = Alfresco.module.DoclibGlobalFolder;

         var allowedViewModes =
         [
            DLGF.VIEW_MODE_RECENT_SITES,
            DLGF.VIEW_MODE_FAVOURITE_SITES,
            DLGF.VIEW_MODE_SITE,
            DLGF.VIEW_MODE_SHARED
         ];

         if (this.options.repositoryBrowsing === true)
         {
            allowedViewModes.push(DLGF.VIEW_MODE_REPOSITORY);
         }

         allowedViewModes.push(DLGF.VIEW_MODE_USERHOME)

         this.modules.createLink.setOptions(
         {
            allowedViewModes: allowedViewModes,
            mode: "create-link",
            siteId: this.options.siteId,
            containerId: this.options.containerId,
            path: this.currentPath,
            files: record,
            rootNode: this.options.rootNode,
            parentId: this.getParentNodeRef(record)
         }).showDialog();
        }
    });
})();