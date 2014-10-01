Alfresco Share "Create Link" Action
===================================

Description
-----------
**"Create Link"** is a custom Alfresco Share Document Library action, similar to "Copy to...", but instead of copying, it creates a link (shortcut, reference) to the document.

With standard Alfresco Share, if you want a document to be accessible from multiple locations, it must be physically copied. "Create Link" action allows a single document stored in Alfresco to be accessible from multiple locations without duplicating the document. A user who is able to access the primary document is able to create a reference in any number of locations that links back to the physical document. Any changes made to the physical document or its metadata are immediately accessible from all referenced locations.

This project uses Alfresco (Maven) SDK. Implementation is based on Alfresco Explorer "Paste as Link" Shelf's functionality, which means that links created by either actions should behave the same way.

Installation
------------
You can find ready-to-install alfresco and share AMP's on [releases](https://github.com/skomarica/alfresco-share-create-link/releases) page.

Alternatively, you can clone this repository and use `mvn install` to build AMP's from the latest source code.

> ###Compatibility
|Module version|Alfresco version|Java version|Alfresco Maven SDK|
|--------------|----------------|------------|------------------|
|v1.0.0|4.2.x|Java 7|1.1.1|
|v1.0.0|5.0.a|Java 7|1.1.1|

Using
-----
Using "Create Link" action is similar to using "Copy to..." or "Move to..." out-of-the box Alfresco Share actions:

1. Navigate to document in your Document Library (e.g. "My File.docx") you want to create link to and execute "Create Link" action
2. Select a target folder where you want your link to be created and confirm the dialog (you will need write permissions on selected folder)
3. The node of type "app:filelink" pointing to "My File.docx" will be created within a selected folder, with appropriate thumbnail and name like "Link to: My File.docx"

FAQ
---
> #### What is an AMP file and how can I install it?
An [AMP](https://wiki.alfresco.com/wiki/AMP_Files) file is a standard way of distributing Alfresco extensions. Use [Module Management Tool](https://wiki.alfresco.com/wiki/Module_Management_Tool) to apply "create-link-repo.amp" to "alfresco.war", and "create-link-share.amp" to "share.war" application. [Official documentation](http://docs.alfresco.com/) is always a good reference.

> #### What is Alfresco (Maven) SDK and how can I use it?
There is a great [tutorial](http://ecmarchitect.com/alfresco-developer-series-tutorials/maven-sdk/tutorial/tutorial.html) on how to get started with Alfresco Maven SDK. [Official documentation](http://docs.alfresco.com/) is always a good reference.

> #### Can I create a link to folder?
Although the creation of links to folder is supported, I did not add an action to folder-browse and folder-details action groups. This is caused by the way alfresco works with links in Share (even with those created using Alfresco Explorer Shelf "Paste as Link" functionality):
* Document link opens document details view which is expected
* Folder link opens folder details view (does not actually display content of the folder), which may not be expected by the user when clicks on folder link
