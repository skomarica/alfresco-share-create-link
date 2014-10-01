Alfresco Share "Create Link" Action
===================================

Description
-----------
**"Create Link"** is a custom Alfresco Share Document Library action, similar to "Copy to...", but instead of copying, it creates a link (shortcut, reference) to the document.

With standard Alfresco Share, if you want a document to be accessible from multiple locations, it must be physically copied. "Create Link" action allows a single document stored in Alfresco to be accessible from multiple locations without duplicating the document. A user who is able to access the primary document is able to create a reference in any number of locations that links back to the physical document. Any changes made to the physical document or its metadata are immediately accessible from all referenced locations. Once you have a link created, you can use standard Alfresco Share actions to delete a link without affecting primary document, or to navigate to the folder where primary document resides.

This project uses Alfresco (Maven) SDK. Implementation is based on Alfresco Explorer "Paste as Link" Shelf's functionality, which means that links created by either actions should behave the same way.

Installation
------------
You can find ready-to-install alfresco and share AMP's on [releases](https://github.com/skomarica/alfresco-share-create-link/releases) page.

Alternatively, you can clone this repository and use `mvn install` to build AMP's from the latest source code.

> ###Compatibility
|Module version|Alfresco version|Java version|Alfresco (Maven) SDK|
|--------------|----------------|------------|------------------|
|v1.0.0|4.2.x, 5.0.a|Java 7|1.1.1|

Using
-----
Using "Create Link" action is similar to using "Copy to..." or "Move to..." out-of-the box Alfresco Share actions:

1. Navigate to a document in your Document Library (e.g. "My File.docx") you want to create link to and execute "Create Link" action
2. Select a target folder where you want your link to be created and confirm the dialog (you will need write permissions on selected folder)
3. A node of type "app:filelink" pointing to "My File.docx" will be created within the selected folder, with appropriate thumbnail and name like "Link to: My File.docx"

License, support, maintenance
-----------------------------
This is an open-source project, distributed under Apache License (v2.0). It's supplied free of charge, with no formal support, maintenance or warranty. If you find a bug related to this functionality or you have some nice enhancement in mind, feel free to raise an issue or, even better, implement a solution and contribute it back to the project.

FAQ
---
> #### What is an AMP file and how can I install it?
An [AMP](https://wiki.alfresco.com/wiki/AMP_Files) file is a standard way of distributing Alfresco extensions. Use [Module Management Tool](https://wiki.alfresco.com/wiki/Module_Management_Tool) to apply "create-link-repo.amp" to "alfresco.war", and "create-link-share.amp" to "share.war" application. [Official documentation](http://docs.alfresco.com/) is always a good reference.

> #### What is Alfresco (Maven) SDK and how can I use it?
There is a great [tutorial](http://ecmarchitect.com/alfresco-developer-series-tutorials/maven-sdk/tutorial/tutorial.html) on how to get started with Alfresco Maven SDK. [Official documentation](http://docs.alfresco.com/) is always a good reference.

> #### Can I create a link to folder?
Although the creation of links to folder is supported by backend mechanism, I did not intentionally add "Create Link" action to the folder-browse and folder-details action groups. The reason for this is the way Alfresco Share application works with links (even with those created using Alfresco Explorer Shelf "Paste as Link" functionality):
* Document link points to the document details view which is fine and expected
* Folder link points to the folder details view and clicking the folder link will not navigate you to the linked folder path, displaying folder's content, but to the folder details view

> #### Why am I getting an error "Link creation could not be completed"?
Common reasons for getting such error message are:
* You have not correctly installed "create-link-repo.amp" to "alfresco.war"
* You already have a link with the same name within a selected folder

