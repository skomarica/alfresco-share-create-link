package com.alfdev.alfresco.repo.web.scripts.doclib;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.repo.search.QueryParameterDefImpl;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * <p>
 * <code>CreateLinkPost</code> java web script controller. Web script accepts destination folder nodeRef as URL-path
 * parameter. JSON body contains list of <code>nodeRefs</code> to be linked and <code>parentId</code> (parent folder
 * nodeRef of specified nodeRefs). Creation of both file and folder links supported.
 * </p>
 * 
 * <p>
 * Implementation is based on Alfresco REST API java-script controller <code>copy-to.post.json.js</code> (execution
 * flow, parameters checks and generating result part) and Alfresco Explorer's "Paste As Link" Shelf's functionality
 * (link creation part).
 * </p>
 * 
 * @author Sinisa Komarica
 * 
 */
public class CreateLinkPost extends DeclarativeWebScript
{
	protected final Log logger = LogFactory.getLog(CreateLinkPost.class);

	private static final String LINK_NODE_EXTENSION = ".url";
	private static final String MSG_LINK_TO = "link_to";

	/** Shallow search for nodes with a name pattern */
	public static final String XPATH_QUERY_NODE_MATCH = "./*[like(@cm:name, $cm:name, false)]";

	protected NodeService nodeService;
	protected DictionaryService dictionaryService;
	protected SiteService siteService;
	protected SearchService searchService;
	protected MessageService messageService;
	protected NamespaceService namespaceService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
	{
		// Extract mandatory destination folder NodeRef from URL
		NodeRef destinationRef = null;
		Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();

		if (templateArgs.get("store_type") != null && templateArgs.get("store_id") != null
				&& templateArgs.get("id") != null)
		{
			destinationRef = new NodeRef(templateArgs.get("store_type"), templateArgs.get("store_id"),
					templateArgs.get("id"));
			if (!nodeService.exists(destinationRef))
			{
				status.setCode(Status.STATUS_NOT_FOUND);
				status.setRedirect(true);
				return null;
			}
		}
		else
		{
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No parent details found");
		}

		// Process the JSON post details
		JSONObject json = null;
		JSONParser parser = new JSONParser();
		try
		{
			json = (JSONObject) parser.parse(req.getContent().getContent());
		}
		catch (IOException io)
		{
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid JSON: " + io.getMessage());
		}
		catch (ParseException pe)
		{
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid JSON: " + pe.getMessage());
		}

		// Fetch necessary data from JSON
		List<?> sourceRefObjectList = (List<?>) json.get("nodeRefs");

		// Must have list of files
		if (sourceRefObjectList == null || sourceRefObjectList.isEmpty())
		{
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid JSON: No files.");
		}

		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

		for (int i = 0; i < sourceRefObjectList.size(); i++)
		{
			String sourceRefStr = (String) sourceRefObjectList.get(i);

			// default result
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("nodeRef", sourceRefStr);
			result.put("action", "createLink");
			result.put("success", false);

			try
			{
				NodeRef sourceRef = new NodeRef(sourceRefStr);
				if (!nodeService.exists(sourceRef))
				{
					throw new AlfrescoRuntimeException("NodeRef " + sourceRef + " does not exist");
				}
				else
				{
					String sourceName = (String) nodeService.getProperty(sourceRef, ContentModel.PROP_NAME);
					result.put("id", sourceName);
					result.put("type", dictionaryService.isSubClass(nodeService.getType(sourceRef),
							ContentModel.TYPE_FOLDER) ? "folder" : "document");

					NodeRef linkRef = createLink(sourceRef, destinationRef);

					if (linkRef != null)
					{
						result.put("nodeRef", linkRef.toString());
						result.put("success", true);

						// Retain the name of the site source node is currently in. Null if it's not in a site.
						String fromSite = getSiteShortName(sourceRef);

						// Retain the name of the site the link is currently in. Null if it's not in a site.
						String toSite = getSiteShortName(linkRef);

						// If this was an inter-site copy, we'll need to clean up the permissions on the node
						if ((fromSite != null && toSite != null && !fromSite.equals(toSite)))
						{
							siteService.cleanSitePermissions(linkRef, null);
						}
					}
					else
					{
						logger.error("Link not created, probably there's a node with " + sourceName + " in folder "
								+ destinationRef);
					}
				}
			}
			catch (Exception e)
			{
				logger.error("Exception while creating link for link to " + sourceRefStr + " in " + destinationRef, e);

				result.put("id", i);
				result.put("nodeRef", sourceRefStr);
				result.put("success", false);
			}

			results.add(result);
		}

		boolean overallSuccess = true;
		int successCount = 0;
		int failureCount = 0;

		for (int i = 0; i < results.size(); i++)
		{
			boolean success = (Boolean) results.get(i).get("success");
			overallSuccess = overallSuccess && success;
			if (success)
				successCount++;
			else
				failureCount++;
		}

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("overallSuccess", overallSuccess);
		model.put("successCount", successCount);
		model.put("failureCount", failureCount);
		model.put("results", results);
		return model;
	}

	/**
	 * Creates link to <code>sourceRef</code> folder or document, in <code>destRef</code> folder. Implementation based
	 * on Alfresco Explorer "Paste As Link" Shelf's functionality.
	 * 
	 * @see org.alfresco.web.bean.clipboard.ClipboardItem#paste(javax.faces.context.FacesContext, java.lang.String, int)
	 * 
	 * @param sourceRef
	 * @param destRef
	 * @return
	 */
	public NodeRef createLink(NodeRef sourceRef, NodeRef destRef)
	{
		NodeRef result = null;

		String sourceName = (String) nodeService.getProperty(sourceRef, ContentModel.PROP_NAME);

		// copy as link
		String linkTo = messageService.getMessage(MSG_LINK_TO);
		String targetName = linkTo + ' ' + sourceName;

		// LINK operation
		if (logger.isDebugEnabled())
			logger.debug("Attempting to create a link to node: " + sourceRef + " within a folder: " + destRef);

		// we create a special Link Object node that has a property to reference the original
		// create the node using the nodeService
		if (!checkExists(targetName + LINK_NODE_EXTENSION, destRef))
		{
			{
				Map<QName, Serializable> props = new HashMap<QName, Serializable>(2, 1.0f);
				String targetNewName = targetName + LINK_NODE_EXTENSION;

				// common properties
				props.put(ContentModel.PROP_NAME, targetNewName);
				props.put(ContentModel.PROP_LINK_DESTINATION, sourceRef);

				if (dictionaryService.isSubClass(nodeService.getType(sourceRef), ContentModel.TYPE_CONTENT))
				{
					// create File Link node
					ChildAssociationRef childRef = nodeService.createNode(destRef, ContentModel.ASSOC_CONTAINS,
							QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, targetNewName),
							ApplicationModel.TYPE_FILELINK, props);

					// apply the titled aspect - title and description
					Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>(2, 1.0f);
					titledProps.put(ContentModel.PROP_TITLE, targetName);
					titledProps.put(ContentModel.PROP_DESCRIPTION, targetName);
					nodeService.addAspect(childRef.getChildRef(), ContentModel.ASPECT_TITLED, titledProps);

					result = childRef.getChildRef();
				}
				else
				{
					// create Folder link node
					ChildAssociationRef childRef = nodeService.createNode(destRef, ContentModel.ASSOC_CONTAINS,
							QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, targetNewName),
							ApplicationModel.TYPE_FOLDERLINK, props);

					// apply the uifacets aspect - icon, title and description props
					Map<QName, Serializable> uiFacetsProps = new HashMap<QName, Serializable>(4, 1.0f);
					uiFacetsProps.put(ApplicationModel.PROP_ICON, "space-icon-link");
					uiFacetsProps.put(ContentModel.PROP_TITLE, targetName);
					uiFacetsProps.put(ContentModel.PROP_DESCRIPTION, targetName);
					nodeService.addAspect(childRef.getChildRef(), ApplicationModel.ASPECT_UIFACETS, uiFacetsProps);

					result = childRef.getChildRef();
				}
			}
		}

		return result;
	}

	/**
	 * Check if node with specified <code>name</code> exists within a <code>parent</code> folder.
	 * 
	 * @param name
	 * @param parent
	 * @return
	 */
	public boolean checkExists(String name, NodeRef parent)
	{
		QueryParameterDefinition[] params = new QueryParameterDefinition[1];
		params[0] = new QueryParameterDefImpl(ContentModel.PROP_NAME,
				dictionaryService.getDataType(DataTypeDefinition.TEXT), true, name);

		// execute the query
		List<NodeRef> nodeRefs = searchService.selectNodes(parent, XPATH_QUERY_NODE_MATCH, params, namespaceService,
				false);

		return (nodeRefs.size() != 0);
	}

	/**
	 * Returns the short name of the site this node is located within. If the node is not located within a site null is
	 * returned.
	 * 
	 * @return The short name of the site this node is located within, null if the node is not located within a site.
	 */
	public String getSiteShortName(NodeRef nodeRef)
	{
		String result = null;

		Path path = nodeService.getPath(nodeRef);

		if (logger.isDebugEnabled())
			logger.debug("Determing if node is within a site using path: " + path);

		for (int i = 0; i < path.size(); i++)
		{
			if ("st:sites".equals(path.get(i).getPrefixedString(namespaceService)))
			{
				// we now know the node is in a site, find the next element in the array (if there is one)
				if ((i + 1) < path.size())
				{
					// get the site name
					Path.Element siteName = path.get(i + 1);

					// remove the "cm:" prefix and add to result object
					result = ISO9075.decode(siteName.getPrefixedString(namespaceService).substring(3));
				}

				break;
			}
		}

		if (logger.isDebugEnabled())
		{
			logger.debug(result != null ? "Node is in the site named \"" + result + "\"" : "Node is not in a site");
		}

		return result;
	}

	// Spring setters
	public void setNodeService(NodeService nodeService)
	{
		this.nodeService = nodeService;
	}

	public void setDictionaryService(DictionaryService dictionaryService)
	{
		this.dictionaryService = dictionaryService;
	}

	public void setSearchService(SearchService searchService)
	{
		this.searchService = searchService;
	}

	public void setSiteService(SiteService siteService)
	{
		this.siteService = siteService;
	}

	public void setMessageService(MessageService messageService)
	{
		this.messageService = messageService;
	}

	public void setNamespaceService(NamespaceService namespaceService)
	{
		this.namespaceService = namespaceService;
	}

}
